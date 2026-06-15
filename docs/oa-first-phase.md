# OA First-Phase Integration

This project has completed the first-phase OA integration for unified login only.

## Scope

Included:

- OAuth authorization code login with PKCE.
- SM4 `code_challenge` generated from `code_verifier`.
- `/oauth/token` exchange using form data.
- `/oauth/userinfo` lookup using both `Authorization` header and `access_token` form body.
- Local user lookup or creation.
- Existing project JWT issuance and frontend login state reuse.

Not included in phase one:

- Unified push for organization/user synchronization.
- OA online keep-alive through `/oauth/checkLogin`.
- Unified logout callback handling.

## OA Platform Registration

Register these URLs in the OA/unified identity platform:

- Application entry/index URL: `https://<backend-domain>/oa/login`
- Redirect URL: `https://<backend-domain>/oa/callback`
- Frontend success URL is not registered in OA; configure it in this project as `OA_FRONT_SUCCESS_URI`.

`/oa/login` is the fixed entry described in the integration document. It generates a fresh `code_verifier`, stores it for the current login attempt, builds the SM4 `code_challenge`, and redirects the browser to the OA authorize URL.

`/oa/callback` receives the OA `code`, exchanges it for an OA access token, fetches OA user info, creates or reuses the local project user, then redirects to the frontend with the project JWT. The callback accepts both `oaState` and standard `state` as the login state parameter for compatibility with different OA redirect configurations.

If OA login is disabled or required configuration is missing, `/oa/login` redirects to `OA_FRONT_SUCCESS_URI` with `oaError=...` so the frontend can display the issue instead of showing a backend 500 page.

## Backend Environment Variables

Set these variables before enabling OA login:

```text
OA_AUTH_ENABLED=true
OA_AUTH_BASE_URL=https://<oa-host>
OA_AUTH_CLIENT_ID=<clientId from OA, usually CID_...>
OA_AUTH_CLIENT_SECRET=<clientSecret from OA>
OA_AUTH_REDIRECT_URI=https://<backend-domain>/oa/callback
OA_FRONT_SUCCESS_URI=https://<frontend-domain>/
OA_AUTH_STATE_TTL_SECONDS=120
OA_AUTH_STATE_TRANSPORT=redirect-uri
```

The state TTL is aligned with the document's 120-second authorization code lifecycle.

`OA_AUTH_STATE_TRANSPORT` supports two values:

- `redirect-uri` (default): stores the login state in the `redirectUri` query string as `oaState`, matching the document's FAQ example that appends a per-login identifier to the redirect URL.
- `oauth-state`: sends the login state through the standard OAuth `state` parameter and keeps `redirectUri` fixed. Use this if the OA platform requires exact redirect URL matching and rejects dynamic redirect query parameters.

## Login Flow

1. User clicks `OA统一认证登录` on the current login page.
2. Browser opens `/oa/login`.
3. Backend redirects to:
   `/oauth/authorize?responseType=code&clientId=...&redirectUri=...&code_challenge=...&code_challenge_method=SM4`
4. OA redirects back to `/oa/callback?oaState=...&code=...` in the default mode, or `/oa/callback?state=...&code=...` in `oauth-state` mode. The backend accepts both callback parameter names.
5. Backend posts form data to `/oauth/token`:
   `grantType=authorization_code`, `code`, `clientId`, `clientSecret`, `redirectUri`, `code_verifier`.
6. Backend posts to `/oauth/userinfo` with:
   `Authorization: Bearer <accessToken>` and form field `access_token=<accessToken>`.
7. Backend creates or reuses a local user and signs the existing project JWT.
8. Frontend stores that JWT and continues using existing `Authorization: Bearer <jwt>` API calls.

## Verification Commands

Backend OA tests:

```bat
cd backend
mvn.cmd "-Dtest=org.example.just.service.oa.*Test" test
```

Backend package:

```bat
cd backend
mvn.cmd -DskipTests package
```

Full backend tests currently require local Redis on `127.0.0.1:6379` because existing JWT tests use Redis.

Frontend build requires `npm` and `node_modules` under `front`. The current environment did not have `npm`, so only script syntax checks were run.
