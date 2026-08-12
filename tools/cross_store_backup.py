#!/usr/bin/env python3
"""Create, verify and restore a quiesced PostgreSQL + MongoDB/GridFS bundle.

The tool intentionally implements an offline recovery point: all application
writers must be stopped before either create or restore.  This is stronger and
more honest than claiming a distributed snapshot across two independent
databases.  Redis contains short-lived security state and is deliberately not
part of the durable BAK-002 bundle.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import hmac
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import tarfile
import tempfile
import uuid
from pathlib import Path, PurePosixPath
from typing import Any, Iterable


FORMAT_VERSION = 1
OFFLINE_ATTESTATION = "I_CONFIRM_ALL_APPLICATION_WRITERS_ARE_STOPPED"
KEY_TABLES = (
    "flyway_schema_history",
    "data_dataset",
    "dataset_record_workflow",
    "file_asset",
    "trace_entity",
    "trace_relation",
    "device_measurement",
    "electronic_signature",
    "sys_audit_log",
    "cross_store_outbox",
    "integration_job",
)
IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_.-]{0,127}$")
ENV_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,127}$")
CONTAINER_ID = re.compile(r"^[a-zA-Z0-9_.-]{1,128}$")
REQUIRED_ARTIFACTS = {
    "postgres.dump",
    "mongodb.archive.gz",
    "postgres-available-gridfs-ids.json",
    "mongo-gridfs-ids.json",
}


class BackupError(RuntimeError):
    pass


def log(message: str) -> None:
    print(f"[cross-store-backup] {message}", file=sys.stderr, flush=True)


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def require_name(value: str, label: str) -> str:
    if not IDENTIFIER.fullmatch(value or ""):
        raise BackupError(f"{label} contains unsupported characters")
    return value


def require_container(value: str, label: str) -> str:
    if not CONTAINER_ID.fullmatch(value or ""):
        raise BackupError(f"{label} is not a safe container identifier")
    return value


def require_env_name(value: str, label: str) -> str:
    if not ENV_IDENTIFIER.fullmatch(value or ""):
        raise BackupError(f"{label} is not a safe environment variable name")
    return value


def compose_command() -> list[str]:
    if shutil.which("docker-compose"):
        return ["docker-compose"]
    if shutil.which("docker"):
        probe = subprocess.run(["docker", "compose", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if probe.returncode == 0:
            return ["docker", "compose"]
    raise BackupError("docker-compose or docker compose is required")


def run(command: list[str], *, input_file: Path | None = None, output_file: Path | None = None,
        capture: bool = False, check: bool = True) -> str:
    log("exec " + " ".join(shlex.quote(part) for part in command[:5]) + (" …" if len(command) > 5 else ""))
    source = input_file.open("rb") if input_file else None
    target = output_file.open("wb") if output_file else (subprocess.PIPE if capture else None)
    try:
        completed = subprocess.run(
            command,
            stdin=source,
            stdout=target,
            stderr=subprocess.PIPE,
            check=False,
        )
    finally:
        if source:
            source.close()
        if output_file and target:
            target.close()
    if check and completed.returncode != 0:
        error = completed.stderr.decode("utf-8", errors="replace").strip()
        raise BackupError(f"command failed ({completed.returncode}): {error[-2000:]}")
    if capture:
        return (completed.stdout or b"").decode("utf-8", errors="strict").strip()
    return ""


class DockerTarget:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.postgres = self._container(args.postgres_container, args.postgres_service, required=True)
        self.mongo = self._container(args.mongo_container, args.mongo_service, required=True)
        self.backend = self._container(args.backend_container, args.backend_service, required=False)
        self._require_running(self.postgres, "PostgreSQL")
        self._require_running(self.mongo, "MongoDB")
        self._require_offline()

    def _compose_base(self) -> list[str]:
        command = compose_command()
        for compose_file in self.args.compose_file:
            command.extend(["-f", str(Path(compose_file).resolve())])
        if self.args.project_directory:
            command.extend(["--project-directory", str(Path(self.args.project_directory).resolve())])
        return command

    def _container(self, explicit: str | None, service: str, *, required: bool) -> str | None:
        if explicit:
            return require_container(explicit, service)
        try:
            value = run(self._compose_base() + ["ps", "-q", service], capture=True).splitlines()
        except BackupError:
            value = []
        if value:
            return require_container(value[-1].strip(), service)
        if required:
            raise BackupError(f"cannot resolve running compose service: {service}")
        return None

    def _running(self, container: str) -> bool:
        value = run(["docker", "inspect", "-f", "{{.State.Running}}", container], capture=True)
        return value.lower() == "true"

    def _require_running(self, container: str, label: str) -> None:
        if not self._running(container):
            raise BackupError(f"{label} container is not running")

    def _require_offline(self) -> None:
        if self.backend and self._running(self.backend):
            raise BackupError("backend container is running; stop every application writer before backup/restore")
        if not self.backend and self.args.offline_attestation != OFFLINE_ATTESTATION:
            raise BackupError(
                "backend container could not be resolved; pass the exact offline attestation only after all writers are stopped"
            )

    def pg_exec(self, *arguments: str, input_file: Path | None = None,
                output_file: Path | None = None, capture: bool = False) -> str:
        script = 'export PGPASSWORD="${' + self.args.postgres_password_env + ':-}"; exec "$@"'
        command = ["docker", "exec", "-i", self.postgres, "sh", "-ceu", script, "backup-pg", *arguments]
        return run(command, input_file=input_file, output_file=output_file, capture=capture)

    def mongo_exec(self, executable: str, *arguments: str, input_file: Path | None = None,
                   output_file: Path | None = None, capture: bool = False) -> str:
        password_env = self.args.mongo_password_env
        auth = []
        if self.args.mongo_user:
            auth = ["--username", self.args.mongo_user, "--password", '"${' + password_env + ':-}"',
                    "--authenticationDatabase", self.args.mongo_auth_db]
        rendered = [shlex.quote(executable)]
        for argument in [*auth, *arguments]:
            if argument.startswith('"${'):
                rendered.append(argument)
            else:
                rendered.append(shlex.quote(argument))
        script = "exec " + " ".join(rendered)
        command = ["docker", "exec", "-i", self.mongo, "sh", "-ceu", script]
        return run(command, input_file=input_file, output_file=output_file, capture=capture)

    def psql(self, sql: str, database: str | None = None) -> str:
        return self.pg_exec(
            "psql", "-X", "-v", "ON_ERROR_STOP=1", "-At", "-U", self.args.postgres_user,
            "-d", database or self.args.postgres_db, "-c", sql, capture=True,
        )

    def mongosh(self, javascript: str, database: str | None = None) -> str:
        return self.mongo_exec(
            "mongosh", "--quiet", database or self.args.mongo_db, "--eval", javascript, capture=True,
        )


def table_exists(target: DockerTarget, table: str) -> bool:
    return target.psql(f"SELECT to_regclass('public.{table}') IS NOT NULL").lower() == "t"


def table_count(target: DockerTarget, table: str) -> int | None:
    if not table_exists(target, table):
        return None
    return int(target.psql(f'SELECT count(*) FROM "{table}"'))


def pg_snapshot(target: DockerTarget, inventory_dir: Path) -> dict[str, Any]:
    counts = {table: table_count(target, table) for table in KEY_TABLES}
    flyway_version = None
    if counts.get("flyway_schema_history") is not None:
        flyway_version = target.psql(
            "SELECT version FROM flyway_schema_history WHERE success=TRUE ORDER BY installed_rank DESC LIMIT 1"
        ) or None
    recovery = json.loads(target.psql(
        "SELECT json_build_object('timestamp',clock_timestamp(),'walLsn',pg_current_wal_lsn())::text"
    ))
    file_refs: list[str] = []
    if counts.get("file_asset") is not None:
        raw = target.psql(
            "SELECT coalesce(json_agg(gridfs_id ORDER BY gridfs_id),'[]'::json)::text "
            "FROM file_asset WHERE status='AVAILABLE' AND gridfs_id IS NOT NULL"
        )
        file_refs = json.loads(raw)
    (inventory_dir / "postgres-available-gridfs-ids.json").write_bytes(canonical_json(file_refs) + b"\n")
    return {"flywayVersion": flyway_version, "recovery": recovery, "tableCounts": counts, "availableGridFsIds": len(file_refs)}


def mongo_snapshot(target: DockerTarget, inventory_dir: Path) -> dict[str, Any]:
    counts_script = """
const names=db.getCollectionNames().filter(n=>!n.startsWith('system.')).sort();
print(JSON.stringify({operationTime:(db.runCommand({hello:1}).operationTime||'').toString(),
collections:Object.fromEntries(names.map(n=>[n,db.getCollection(n).countDocuments({})]))}));
"""
    snapshot = json.loads(target.mongosh(counts_script))
    ids_script = "print(JSON.stringify(db.getCollection('fs.files').find({}, {_id:1}).sort({_id:1}).toArray().map(x=>x._id.toString())))"
    grid_ids = json.loads(target.mongosh(ids_script)) if "fs.files" in snapshot["collections"] else []
    structure_script = r"""
const errors=[];
const files=db.getCollection('fs.files');
const chunks=db.getCollection('fs.chunks');
files.find({}, {_id:1,length:1,chunkSize:1}).forEach(file=>{
  const expected=file.length===0 ? 0 : Math.ceil(Number(file.length)/Number(file.chunkSize));
  const stats=chunks.aggregate([
    {$match:{files_id:file._id}},
    {$group:{_id:null,count:{$sum:1},bytes:{$sum:{$binarySize:'$data'}},min:{$min:'$n'},max:{$max:'$n'}}}
  ]).toArray()[0] || {count:0,bytes:0,min:null,max:null};
  const numbersOk=expected===0
    ? stats.count===0
    : stats.count===expected && stats.min===0 && stats.max===expected-1;
  if (!numbersOk || Number(stats.bytes)!==Number(file.length)) {
    errors.push({id:file._id.toString(),expectedChunks:expected,actualChunks:stats.count,expectedBytes:Number(file.length),actualBytes:Number(stats.bytes)});
  }
});
print(JSON.stringify({files:files.countDocuments({}),chunks:chunks.countDocuments({}),invalid:errors.length,sample:errors.slice(0,5)}));
"""
    structure = json.loads(target.mongosh(structure_script)) if "fs.files" in snapshot["collections"] else {
        "files": 0, "chunks": 0, "invalid": 0, "sample": []
    }
    if structure["invalid"]:
        raise BackupError(f"GridFS structure verification failed: {structure}")
    (inventory_dir / "mongo-gridfs-ids.json").write_bytes(canonical_json(grid_ids) + b"\n")
    snapshot["gridFsIds"] = len(grid_ids)
    snapshot["gridFsStructure"] = structure
    return snapshot


def quiescence(target: DockerTarget) -> dict[str, int]:
    checks: dict[str, tuple[str, str]] = {
        "unsettledOutbox": ("cross_store_outbox", "SELECT count(*) FROM cross_store_outbox WHERE status IN ('PENDING','PROCESSING','FAILED')"),
        "recordTransitions": ("dataset_record_workflow", "SELECT count(*) FROM dataset_record_workflow WHERE status IN ('CORRECTING','DELETING','ARCHIVING','RESTORING')"),
        "activeUploads": ("file_upload_session", "SELECT count(*) FROM file_upload_session WHERE status IN ('UPLOADING','COMPLETING')"),
        "integrationLeases": ("integration_job", "SELECT count(*) FROM integration_job WHERE status IN ('PROCESSING','RETRYING')"),
    }
    result: dict[str, int] = {}
    for name, (table, sql) in checks.items():
        result[name] = int(target.psql(sql)) if table_exists(target, table) else 0
    return result


def verify_cross_store_references(work_dir: Path) -> dict[str, int]:
    pg_ids = set(json.loads((work_dir / "postgres-available-gridfs-ids.json").read_text("utf-8")))
    mongo_ids = set(json.loads((work_dir / "mongo-gridfs-ids.json").read_text("utf-8")))
    missing = sorted(pg_ids - mongo_ids)
    if missing:
        sample = ", ".join(missing[:5])
        raise BackupError(f"{len(missing)} AVAILABLE file_asset rows have no GridFS file (sample: {sample})")
    return {"availableFileReferences": len(pg_ids), "gridFsFiles": len(mongo_ids), "missingGridFsReferences": 0}


def artifact_entry(path: Path) -> dict[str, Any]:
    return {"name": path.name, "sizeBytes": path.stat().st_size, "sha256": sha256_file(path)}


def signing_key() -> bytes:
    value = os.environ.get("RDP_BACKUP_HMAC_KEY", "").encode("utf-8")
    if len(value) < 32:
        raise BackupError("RDP_BACKUP_HMAC_KEY must contain at least 32 UTF-8 bytes")
    return value


def signing_key_id() -> str:
    value = os.environ.get("RDP_BACKUP_HMAC_KEY_ID", "backup-v1")
    if not re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", value):
        raise BackupError("RDP_BACKUP_HMAC_KEY_ID must be 1-64 safe identifier characters")
    return value


def write_operation_log(path: Path | None, event: dict[str, Any]) -> None:
    if not path:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
    with os.fdopen(descriptor, "a", encoding="utf-8") as stream:
        stream.write(json.dumps(event, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n")


def create_bundle(args: argparse.Namespace) -> dict[str, Any]:
    output = Path(args.output).resolve()
    if output.exists():
        raise BackupError(f"refusing to overwrite existing bundle: {output}")
    output.parent.mkdir(parents=True, exist_ok=True)
    target = DockerTarget(args)
    backup_id = str(uuid.uuid4())
    started = dt.datetime.now(dt.timezone.utc)
    with tempfile.TemporaryDirectory(prefix="rdp-cross-store-backup-") as temp_name:
        work = Path(temp_name)
        state = quiescence(target)
        if any(state.values()):
            raise BackupError(f"cross-store state is not quiescent: {state}")
        postgres_dump = work / "postgres.dump"
        mongo_dump = work / "mongodb.archive.gz"
        target.pg_exec(
            "pg_dump", "-U", args.postgres_user, "-d", args.postgres_db,
            "--format=custom", "--compress=9", "--no-owner", "--no-privileges",
            output_file=postgres_dump,
        )
        target.mongo_exec("mongodump", "--archive", "--gzip", "--db", args.mongo_db, output_file=mongo_dump)
        pg = pg_snapshot(target, work)
        if not pg["flywayVersion"]:
            raise BackupError("successful Flyway schema version is required for a restorable recovery point")
        mongo = mongo_snapshot(target, work)
        references = verify_cross_store_references(work)
        artifacts = [
            artifact_entry(postgres_dump), artifact_entry(mongo_dump),
            artifact_entry(work / "postgres-available-gridfs-ids.json"),
            artifact_entry(work / "mongo-gridfs-ids.json"),
        ]
        manifest = {
            "format": "RDP_CROSS_STORE_BACKUP",
            "formatVersion": FORMAT_VERSION,
            "backupId": backup_id,
            "hmacKeyId": signing_key_id(),
            "createdAt": dt.datetime.now(dt.timezone.utc).isoformat(),
            "offlineRecoveryPoint": True,
            "postgresDatabase": args.postgres_db,
            "mongoDatabase": args.mongo_db,
            "postgres": pg,
            "mongo": mongo,
            "quiescence": state,
            "crossStoreReferences": references,
            "artifacts": artifacts,
            "redisIncluded": False,
            "redisPolicy": "short-lived security state is rebuilt; existing sessions must not be trusted after restore",
        }
        manifest_bytes = canonical_json(manifest) + b"\n"
        (work / "manifest.json").write_bytes(manifest_bytes)
        signature = hmac.new(signing_key(), manifest_bytes, hashlib.sha256).hexdigest()
        (work / "manifest.hmac-sha256").write_text(signature + "\n", encoding="ascii")
        (work / "RESTORE-NOTICE.txt").write_text(
            "Restore only into an isolated/stopped environment. Verify the HMAC and all SHA-256 entries first.\n",
            encoding="utf-8",
        )
        with tarfile.open(output, "w") as archive:
            for path in sorted(work.iterdir(), key=lambda item: item.name):
                archive.add(path, arcname=path.name, recursive=False)
        os.chmod(output, 0o600)
    result = {
        "operation": "CREATE",
        "status": "COMPLETED",
        "backupId": backup_id,
        "bundle": str(output),
        "bundleSha256": sha256_file(output),
        "startedAt": started.isoformat(),
        "finishedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "flywayVersion": pg["flywayVersion"],
        "crossStoreReferences": references,
    }
    write_operation_log(Path(args.operation_log).resolve() if args.operation_log else None, result)
    return result


def extract_bundle(bundle: Path, destination: Path) -> None:
    with tarfile.open(bundle, "r") as archive:
        members = archive.getmembers()
        if len(members) > 32:
            raise BackupError("backup bundle contains too many entries")
        for member in members:
            pure = PurePosixPath(member.name)
            if member.issym() or member.islnk() or member.isdir() or pure.is_absolute() or ".." in pure.parts or len(pure.parts) != 1:
                raise BackupError(f"unsafe backup entry: {member.name}")
            source = archive.extractfile(member)
            if source is None:
                raise BackupError(f"backup entry is not a regular file: {member.name}")
            with (destination / member.name).open("wb") as output:
                shutil.copyfileobj(source, output, length=1024 * 1024)


def load_and_verify_bundle(bundle: Path, work: Path) -> dict[str, Any]:
    if not bundle.is_file():
        raise BackupError(f"bundle does not exist: {bundle}")
    extract_bundle(bundle, work)
    manifest_path = work / "manifest.json"
    signature_path = work / "manifest.hmac-sha256"
    if not manifest_path.is_file() or not signature_path.is_file():
        raise BackupError("manifest or manifest HMAC is missing")
    raw = manifest_path.read_bytes()
    expected_hmac = signature_path.read_text("ascii").strip().lower()
    actual_hmac = hmac.new(signing_key(), raw, hashlib.sha256).hexdigest()
    if not hmac.compare_digest(expected_hmac, actual_hmac):
        raise BackupError("manifest HMAC verification failed")
    manifest = json.loads(raw)
    if manifest.get("format") != "RDP_CROSS_STORE_BACKUP" or manifest.get("formatVersion") != FORMAT_VERSION:
        raise BackupError("unsupported backup format")
    if manifest.get("offlineRecoveryPoint") is not True or any(manifest.get("quiescence", {}).values()):
        raise BackupError("manifest is not a quiesced offline recovery point")
    artifacts = manifest.get("artifacts")
    if not isinstance(artifacts, list):
        raise BackupError("manifest artifact inventory is missing")
    names = [artifact.get("name", "") for artifact in artifacts if isinstance(artifact, dict)]
    if len(names) != len(set(names)) or set(names) != REQUIRED_ARTIFACTS:
        raise BackupError("manifest artifact inventory is incomplete or contains duplicates")
    for artifact in artifacts:
        name = artifact.get("name", "")
        path = work / name
        if not path.is_file() or path.stat().st_size != artifact.get("sizeBytes") or sha256_file(path) != artifact.get("sha256"):
            raise BackupError(f"artifact verification failed: {name}")
    verify_cross_store_references(work)
    return manifest


def compare_snapshot(expected: dict[str, Any], actual: dict[str, Any], label: str) -> None:
    expected_counts = expected["tableCounts"] if label == "PostgreSQL" else expected["collections"]
    actual_counts = actual["tableCounts"] if label == "PostgreSQL" else actual["collections"]
    if expected_counts != actual_counts:
        raise BackupError(f"{label} restored counts differ from manifest: expected={expected_counts}, actual={actual_counts}")


def restore_bundle(args: argparse.Namespace) -> dict[str, Any]:
    bundle = Path(args.bundle).resolve()
    target = DockerTarget(args)
    started = dt.datetime.now(dt.timezone.utc)
    with tempfile.TemporaryDirectory(prefix="rdp-cross-store-restore-") as temp_name:
        work = Path(temp_name)
        manifest = load_and_verify_bundle(bundle, work)
        if args.confirm_restore != manifest["backupId"]:
            raise BackupError("--confirm-restore must exactly equal the signed backupId")
        if args.postgres_db != manifest["postgresDatabase"] or args.mongo_db != manifest["mongoDatabase"]:
            raise BackupError("target database names must match the signed manifest; use isolated containers for drills")
        # Verify the database-native archives themselves before the first
        # destructive operation.  HMAC/SHA protects bytes, while these checks
        # protect recoverability when a dump was truncated before signing.
        pg_toc = target.pg_exec("pg_restore", "--list", input_file=work / "postgres.dump", capture=True)
        if not pg_toc:
            raise BackupError("PostgreSQL archive contains no restorable entries")
        target.mongo_exec(
            "mongorestore", "--dryRun", "--stopOnError", "--archive", "--gzip", "--db", args.mongo_db,
            input_file=work / "mongodb.archive.gz",
        )
        target.pg_exec("dropdb", "--if-exists", "--force", "-U", args.postgres_user, args.postgres_db)
        target.pg_exec("createdb", "-U", args.postgres_user, "-O", args.postgres_user, args.postgres_db)
        target.pg_exec(
            "pg_restore", "-U", args.postgres_user, "-d", args.postgres_db,
            "--no-owner", "--no-privileges", "--exit-on-error",
            input_file=work / "postgres.dump",
        )
        target.mongo_exec(
            "mongorestore", "--drop", "--archive", "--gzip", "--db", args.mongo_db,
            input_file=work / "mongodb.archive.gz",
        )
        pg = pg_snapshot(target, work)
        mongo = mongo_snapshot(target, work)
        compare_snapshot(manifest["postgres"], pg, "PostgreSQL")
        compare_snapshot(manifest["mongo"], mongo, "MongoDB")
        references = verify_cross_store_references(work)
        if pg["flywayVersion"] != manifest["postgres"]["flywayVersion"]:
            raise BackupError("restored Flyway version differs from manifest")
    result = {
        "operation": "RESTORE",
        "status": "COMPLETED",
        "backupId": manifest["backupId"],
        "bundle": str(bundle),
        "bundleSha256": sha256_file(bundle),
        "startedAt": started.isoformat(),
        "finishedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "flywayVersion": pg["flywayVersion"],
        "crossStoreReferences": references,
        "redisActionRequired": "flush transient sessions and require users to authenticate again",
    }
    report = Path(args.report).resolve() if args.report else bundle.with_suffix(bundle.suffix + ".restore-report.json")
    report.write_bytes(canonical_json(result) + b"\n")
    os.chmod(report, 0o600)
    write_operation_log(Path(args.operation_log).resolve() if args.operation_log else None, result)
    return result


def verify_bundle(args: argparse.Namespace) -> dict[str, Any]:
    bundle = Path(args.bundle).resolve()
    with tempfile.TemporaryDirectory(prefix="rdp-cross-store-verify-") as temp_name:
        manifest = load_and_verify_bundle(bundle, Path(temp_name))
    return {
        "operation": "VERIFY",
        "status": "COMPLETED",
        "backupId": manifest["backupId"],
        "bundle": str(bundle),
        "bundleSha256": sha256_file(bundle),
        "flywayVersion": manifest["postgres"]["flywayVersion"],
        "crossStoreReferences": manifest["crossStoreReferences"],
    }


def add_target_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--compose-file", action="append", default=["docker-compose.yml"], help="repeat for compose overrides")
    parser.add_argument("--project-directory", default=".")
    parser.add_argument("--postgres-container")
    parser.add_argument("--mongo-container")
    parser.add_argument("--backend-container")
    parser.add_argument("--postgres-service", default="postgres")
    parser.add_argument("--mongo-service", default="mongodb")
    parser.add_argument("--backend-service", default="backend")
    parser.add_argument("--postgres-db", default="rdp_platform", type=lambda value: require_name(value, "PostgreSQL database"))
    parser.add_argument("--postgres-user", default="rdp", type=lambda value: require_name(value, "PostgreSQL user"))
    parser.add_argument("--postgres-password-env", default="POSTGRES_PASSWORD", type=lambda value: require_env_name(value, "PostgreSQL password env"))
    parser.add_argument("--mongo-db", default="rdp_platform", type=lambda value: require_name(value, "MongoDB database"))
    parser.add_argument("--mongo-user", default="rdp", type=lambda value: require_name(value, "MongoDB user") if value else "")
    parser.add_argument("--mongo-auth-db", default="admin", type=lambda value: require_name(value, "MongoDB auth database"))
    parser.add_argument("--mongo-password-env", default="MONGO_INITDB_ROOT_PASSWORD", type=lambda value: require_env_name(value, "MongoDB password env"))
    parser.add_argument("--offline-attestation", help="required exact phrase when no stopped backend container can be resolved")


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)
    create = commands.add_parser("create", help="create a signed cross-store bundle")
    add_target_arguments(create)
    create.add_argument("--output", required=True)
    create.add_argument("--operation-log")

    verify = commands.add_parser("verify", help="verify bundle HMAC, hashes and inventories without touching databases")
    verify.add_argument("--bundle", required=True)

    restore = commands.add_parser("restore", help="destructively restore into an explicitly confirmed offline target")
    add_target_arguments(restore)
    restore.add_argument("--bundle", required=True)
    restore.add_argument("--confirm-restore", required=True)
    restore.add_argument("--report")
    restore.add_argument("--operation-log")
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        if args.command == "create":
            result = create_bundle(args)
        elif args.command == "verify":
            result = verify_bundle(args)
        else:
            result = restore_bundle(args)
        print(json.dumps(result, ensure_ascii=False, sort_keys=True))
        return 0
    except BackupError as error:
        event = {
            "operation": args.command.upper(),
            "status": "FAILED",
            "time": dt.datetime.now(dt.timezone.utc).isoformat(),
            "error": str(error),
        }
        if getattr(args, "operation_log", None):
            write_operation_log(Path(args.operation_log).resolve(), event)
        print(json.dumps(event, ensure_ascii=False, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
