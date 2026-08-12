#!/usr/bin/env bash
set -euo pipefail

# Destructive recovery drill for BAK-002.  All targets are disposable containers
# created by this script; it never resolves or touches the workspace Compose
# services.  Docker/Colima must already be available.

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
drill_dir="$(mktemp -d "${TMPDIR:-/tmp}/rdp-backup-drill.XXXXXX")"
drill_suffix="$(date +%s)-$$"
postgres_name="rdp-backup-drill-pg-${drill_suffix}"
mongo_name="rdp-backup-drill-mongo-${drill_suffix}"
postgres_password="rdp_backup_drill_pg_2026"
mongo_password="rdp_backup_drill_mongo_2026"
backup_key="rdp-backup-drill-hmac-key-2026-at-least-32-bytes"
offline_attestation="I_CONFIRM_ALL_APPLICATION_WRITERS_ARE_STOPPED"
bundle="${drill_dir}/recovery-point.tar"
tampered_bundle="${drill_dir}/recovery-point-tampered.tar"
operation_log="${drill_dir}/operations.jsonl"

cleanup() {
  docker rm -f "${postgres_name}" "${mongo_name}" >/dev/null 2>&1 || true
  rm -rf "${drill_dir}"
}
trap cleanup EXIT INT TERM

fail() {
  echo "[cross-store-drill] ERROR: $*" >&2
  exit 1
}

echo "[cross-store-drill] starting isolated PostgreSQL and MongoDB containers"
docker run -d --name "${postgres_name}" \
  -e POSTGRES_DB=rdp_platform \
  -e POSTGRES_USER=rdp \
  -e POSTGRES_PASSWORD="${postgres_password}" \
  "${RDP_POSTGRES_IMAGE:-m.daocloud.io/docker.io/library/postgres:17-alpine}" >/dev/null
docker run -d --name "${mongo_name}" \
  -e MONGO_INITDB_ROOT_USERNAME=rdp \
  -e MONGO_INITDB_ROOT_PASSWORD="${mongo_password}" \
  -e MONGO_INITDB_DATABASE=rdp_platform \
  "${RDP_MONGO_IMAGE:-m.daocloud.io/docker.io/library/mongo:8.0}" >/dev/null

for _ in $(seq 1 60); do
  if docker exec "${postgres_name}" pg_isready -U rdp -d rdp_platform >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec "${postgres_name}" pg_isready -U rdp -d rdp_platform >/dev/null 2>&1 || fail "PostgreSQL did not become ready"

for _ in $(seq 1 60); do
  if docker exec "${mongo_name}" mongosh --quiet \
    --username rdp --password "${mongo_password}" --authenticationDatabase admin \
    --eval "db.adminCommand({ping:1}).ok" admin 2>/dev/null | grep -q '^1$'; then
    break
  fi
  sleep 1
done
docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin \
  --eval "db.adminCommand({ping:1}).ok" admin 2>/dev/null | grep -q '^1$' || fail "MongoDB did not become ready"

echo "[cross-store-drill] seeding a minimal durable cross-store recovery point"
docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' <<'SQL'
CREATE TABLE flyway_schema_history(installed_rank integer PRIMARY KEY, version varchar(50), success boolean NOT NULL);
CREATE TABLE data_dataset(id bigint PRIMARY KEY, dataset_name text NOT NULL);
CREATE TABLE dataset_record_workflow(id bigint PRIMARY KEY, status text NOT NULL);
CREATE TABLE file_asset(id bigint PRIMARY KEY, status text NOT NULL, gridfs_id text);
CREATE TABLE trace_entity(id bigint PRIMARY KEY);
CREATE TABLE trace_relation(id bigint PRIMARY KEY);
CREATE TABLE device_measurement(id bigint PRIMARY KEY);
CREATE TABLE electronic_signature(id bigint PRIMARY KEY);
CREATE TABLE sys_audit_log(id bigint PRIMARY KEY);
CREATE TABLE cross_store_outbox(id bigint PRIMARY KEY, status text NOT NULL);
CREATE TABLE integration_job(id bigint PRIMARY KEY, status text NOT NULL);
CREATE TABLE file_upload_session(id bigint PRIMARY KEY, status text NOT NULL);
INSERT INTO flyway_schema_history VALUES (33, '33', TRUE);
INSERT INTO data_dataset VALUES (1, 'recovery-drill-dataset');
INSERT INTO dataset_record_workflow VALUES (11, 'PUBLISHED');
INSERT INTO file_asset VALUES (21, 'AVAILABLE', '64b64b64b64b64b64b64b641');
INSERT INTO trace_entity VALUES (31);
INSERT INTO trace_relation VALUES (41);
INSERT INTO device_measurement VALUES (51);
INSERT INTO electronic_signature VALUES (61);
INSERT INTO sys_audit_log VALUES (71);
INSERT INTO integration_job VALUES (81, 'COMPLETED');
INSERT INTO file_upload_session VALUES (91, 'COMPLETED');
SQL

docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin rdp_platform --eval '
const fileId=ObjectId("64b64b64b64b64b64b64b641");
db.dataset_data_1.insertOne({_id:ObjectId("64b64b64b64b64b64b64b642"), materialCode:"MAT-RESTORE-001", archived:false});
db.getCollection("fs.files").insertOne({_id:fileId, length:5, chunkSize:261120, uploadDate:new Date(), filename:"evidence.txt", metadata:{sha256:"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"}});
db.getCollection("fs.chunks").insertOne({files_id:fileId, n:0, data:BinData(0,"aGVsbG8=")});
' >/dev/null

common_args=(
  --postgres-container "${postgres_name}"
  --mongo-container "${mongo_name}"
  --backend-service rdp-backup-drill-no-backend
  --postgres-db rdp_platform
  --postgres-user rdp
  --mongo-db rdp_platform
  --mongo-user rdp
  --mongo-auth-db admin
  --offline-attestation "${offline_attestation}"
)

echo "[cross-store-drill] proving unsettled cross-store state is refused"
docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' \
  <<<"INSERT INTO cross_store_outbox VALUES (101, 'PENDING');"
if RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" create \
  "${common_args[@]}" --output "${drill_dir}/must-not-exist.tar" >/dev/null 2>&1; then
  fail "non-quiescent backup unexpectedly succeeded"
fi
[[ ! -e "${drill_dir}/must-not-exist.tar" ]] || fail "refused backup left an output bundle"
docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' \
  <<<"DELETE FROM cross_store_outbox WHERE id=101;"

echo "[cross-store-drill] proving incomplete GridFS and broken PG references are refused"
docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin rdp_platform \
  --eval 'db.getCollection("fs.chunks").deleteMany({files_id:ObjectId("64b64b64b64b64b64b64b641")});' >/dev/null
if RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" create \
  "${common_args[@]}" --output "${drill_dir}/incomplete-gridfs-must-not-exist.tar" >/dev/null 2>&1; then
  fail "backup with incomplete GridFS chunks unexpectedly succeeded"
fi
docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin rdp_platform \
  --eval 'db.getCollection("fs.chunks").insertOne({files_id:ObjectId("64b64b64b64b64b64b64b641"),n:0,data:BinData(0,"aGVsbG8=")});' >/dev/null

docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' \
  <<<"UPDATE file_asset SET gridfs_id='64b64b64b64b64b64b64b699' WHERE id=21;"
if RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" create \
  "${common_args[@]}" --output "${drill_dir}/broken-reference-must-not-exist.tar" >/dev/null 2>&1; then
  fail "backup with a broken PostgreSQL-to-GridFS reference unexpectedly succeeded"
fi
docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' \
  <<<"UPDATE file_asset SET gridfs_id='64b64b64b64b64b64b64b641' WHERE id=21;"

echo "[cross-store-drill] creating and verifying signed bundle"
create_json="$(RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" create \
  "${common_args[@]}" --output "${bundle}" --operation-log "${operation_log}")"
backup_id="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["backupId"])' <<<"${create_json}")"
[[ -n "${backup_id}" ]] || fail "create did not return a backupId"
python3 - "${bundle}" "${operation_log}" <<'PY'
import os
import stat
import sys

for path in sys.argv[1:]:
    assert stat.S_IMODE(os.stat(path).st_mode) == 0o600, (path, oct(stat.S_IMODE(os.stat(path).st_mode)))
PY
RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" verify \
  --bundle "${bundle}" >/dev/null
if RDP_BACKUP_HMAC_KEY="a-different-verification-key-at-least-32-bytes" \
  python3 "${workspace_dir}/tools/cross_store_backup.py" verify --bundle "${bundle}" >/dev/null 2>&1; then
  fail "backup unexpectedly verified with the wrong HMAC key"
fi

echo "[cross-store-drill] proving a modified artifact is rejected"
python3 - "${bundle}" "${tampered_bundle}" <<'PY'
import io
import sys
import tarfile

source, target = sys.argv[1:]
with tarfile.open(source, "r") as original, tarfile.open(target, "w") as changed:
    for member in original.getmembers():
        payload = original.extractfile(member).read()
        if member.name == "postgres.dump":
            payload = bytes([payload[0] ^ 0x01]) + payload[1:]
        info = tarfile.TarInfo(member.name)
        info.size = len(payload)
        info.mode = 0o600
        changed.addfile(info, io.BytesIO(payload))
PY
if RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" verify \
  --bundle "${tampered_bundle}" >/dev/null 2>&1; then
  fail "tampered backup unexpectedly verified"
fi

echo "[cross-store-drill] corrupting both stores and proving wrong confirmation is non-destructive"
docker exec -i "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -v ON_ERROR_STOP=1 -U rdp -d rdp_platform' \
  <<<"UPDATE data_dataset SET dataset_name='corrupted'; DELETE FROM file_asset;"
docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin rdp_platform \
  --eval 'db.dataset_data_1.deleteMany({}); db.getCollection("fs.files").deleteMany({}); db.getCollection("fs.chunks").deleteMany({});' >/dev/null
if RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" restore \
  "${common_args[@]}" --bundle "${bundle}" --confirm-restore wrong-id >/dev/null 2>&1; then
  fail "restore with incorrect confirmation unexpectedly succeeded"
fi
corrupted_name="$(docker exec "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -At -U rdp -d rdp_platform -c "SELECT dataset_name FROM data_dataset WHERE id=1"')"
[[ "${corrupted_name}" == "corrupted" ]] || fail "incorrect confirmation modified PostgreSQL"

echo "[cross-store-drill] restoring both stores from the signed recovery point"
restore_json="$(RDP_BACKUP_HMAC_KEY="${backup_key}" python3 "${workspace_dir}/tools/cross_store_backup.py" restore \
  "${common_args[@]}" --bundle "${bundle}" --confirm-restore "${backup_id}" \
  --report "${drill_dir}/restore-report.json" --operation-log "${operation_log}")"
restore_status="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' <<<"${restore_json}")"
[[ "${restore_status}" == "COMPLETED" ]] || fail "restore did not complete"

restored_name="$(docker exec "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -At -U rdp -d rdp_platform -c "SELECT dataset_name FROM data_dataset WHERE id=1"')"
[[ "${restored_name}" == "recovery-drill-dataset" ]] || fail "PostgreSQL content was not restored"
restored_pg_ref="$(docker exec "${postgres_name}" sh -ceu 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec psql -X -At -U rdp -d rdp_platform -c "SELECT gridfs_id FROM file_asset WHERE id=21 AND status='"'"'AVAILABLE'"'"'"')"
[[ "${restored_pg_ref}" == "64b64b64b64b64b64b64b641" ]] || fail "PostgreSQL GridFS reference was not restored"
restored_mongo="$(docker exec "${mongo_name}" mongosh --quiet \
  --username rdp --password "${mongo_password}" --authenticationDatabase admin rdp_platform \
  --eval 'print([db.dataset_data_1.countDocuments({materialCode:"MAT-RESTORE-001"}),db.getCollection("fs.files").countDocuments({_id:ObjectId("64b64b64b64b64b64b64b641")}),db.getCollection("fs.chunks").countDocuments({files_id:ObjectId("64b64b64b64b64b64b64b641")})].join(","))')"
[[ "${restored_mongo}" == "1,1,1" ]] || fail "MongoDB/GridFS content was not restored: ${restored_mongo}"

python3 - "${operation_log}" <<'PY'
import json
import sys

events = [json.loads(line) for line in open(sys.argv[1], encoding="utf-8") if line.strip()]
assert [event["operation"] for event in events] == ["CREATE", "RESTORE"], events
assert all(event["status"] == "COMPLETED" for event in events), events
assert events[0]["backupId"] == events[1]["backupId"], events
PY

echo "[cross-store-drill] PASS: signed, quiesced PostgreSQL + MongoDB/GridFS recovery point restored"
