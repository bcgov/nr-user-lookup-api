[![MIT License](https://img.shields.io/github/license/bcgov/nr-user-lookup-api.svg)](/LICENSE)
[![Merge](https://github.com/bcgov/nr-user-lookup-api/actions/workflows/merge.yml/badge.svg)](https://github.com/bcgov/nr-user-lookup-api/actions/workflows/merge.yml)
[![Analysis](https://github.com/bcgov/nr-user-lookup-api/actions/workflows/analysis.yml/badge.svg)](https://github.com/bcgov/nr-user-lookup-api/actions/workflows/analysis.yml)

# NR User Lookup API

A stateless **Spring Boot** service that proxies the BC Government **BCeID** SOAP web service
(which serves both IDIR and BCeID accounts) and exposes a small REST API for looking up IDIR
and BCeID business users. It is the Spring Boot successor to the former Node lookup proxy.

- **No database, no frontend** — a pure backend lookup proxy.
- **Incoming auth:** Keycloak-issued **JWT bearer tokens** (RS256 signature validation
  against the realm JWKS), authorized per-operation with **OAuth2 scopes**.
- **Outgoing auth:** HTTP Basic against the BCeID SOAP web service.

## Endpoints

The API is versioned in the path. All endpoints are under `/api/v1/user-lookup` and
require a bearer token carrying the listed scope. Interactive docs are at
`/swagger-ui/index.html` (see [API docs](#api-docs-openapi--swagger)).

| Method | Path | Required scope | Description |
|---|---|---|---|
| `POST` | `/api/v1/user-lookup/idir-users/search` | `user-lookup:idir:search` | Search IDIR users by firstName / lastName / userId (partial match). |
| `GET`  | `/api/v1/user-lookup/idir-account-detail` | `user-lookup:idir:read` | Get an IDIR user by exact `userId`. |
| `GET`  | `/api/v1/user-lookup/businessBceid` | `user-lookup:business-bceid:read` | Get a BCeID business user by exact `userId` or `userGuid`. |

The `POST .../idir-users/search` takes its search criteria as query parameters (`firstName`,
`lastName`, `userId`, the optional `*MatchMode` values of `Exact`/`Contains`/`StartsWith`, and
`pageSize`). At least one of `firstName`/`lastName`/`userId` is required; match mode defaults to
`Contains` and `pageSize` to `50`.

The requester identity (`requesterUserGuid`) is a fixed, internal server-side configuration
value (`BCEID_WEB_SERVICE_REQUESTER_USER_GUID`) used as an Internal/IDIR requester on every
outbound lookup — it is **not** accepted from callers.

Scopes map to the JWT `scope` claim via Spring's default `SCOPE_*` authority conversion. The
custom scopes (`user-lookup:idir:search`, `user-lookup:idir:read`,
`user-lookup:business-bceid:read`) are **created automatically in each environment's Keycloak
realm during deploy** (see [Deployment](#deployment)); they still need to be **assigned to each
calling client** for tokens to carry them — see
[Consuming the API](#consuming-the-api--provision-your-own-keycloak-client).

New API versions go under a new path prefix (`/api/v2/...`) backed by a parallel
`controller.v2` package, leaving `v1` clients untouched.

## API docs (OpenAPI / Swagger)

springdoc is enabled and both docs endpoints are **public** (no token needed to read them):

| What | Path |
|---|---|
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |

The UI has an **Authorize** dialog wired to a bearer-token scheme — paste a Keycloak
access token (no `Bearer ` prefix) to try the endpoints. Locally:
<http://localhost:8080/swagger-ui/index.html>.

## Consuming the API — provision your own Keycloak client

This service does **not** hand out credentials. Each consuming team registers its own
confidential client in the same Keycloak realm as this API (the realm behind
`KEYCLOAK_ISSUER_URI`, e.g. `standard` on `*.loginproxy.gov.bc.ca`) and calls with a
`client_credentials` token.

**Division of ownership:** this repo creates the *client scopes* in each realm at deploy
time (see [Keycloak scope provisioning](#keycloak-scope-provisioning)). Assigning them to
*your* client is yours to do — we never touch consumer clients.

1. **Create a confidential service-account client** in the realm (via the BC Gov
   Keycloak/loginproxy request process, or your own CI if you hold an admin service
   account with the realm-management `manage-clients` role). Settings:
   - `publicClient: false`, `serviceAccountsEnabled: true`
   - `standardFlowEnabled: false`, `directAccessGrantsEnabled: false`,
     `implicitFlowEnabled: false` — machine-to-machine only, no browser flows.
2. **Assign the scopes you need** as **default** client scopes (not *optional*), so every
   `client_credentials` token carries them without your code asking for them explicitly.
   Request only the endpoints you actually call:
   - `user-lookup:idir:search` — IDIR user search
   - `user-lookup:idir:read` — IDIR account detail
   - `user-lookup:business-bceid:read` — Business BCeID account detail

   The scopes must already exist in that realm — if one is missing, this API hasn't been
   deployed to that environment yet (or its scope-provisioning step was skipped).
3. **Mint a token** against the realm token endpoint and send it as a bearer token:

   ```bash
   TOKEN=$(curl -sS -X POST \
     "https://dev.loginproxy.gov.bc.ca/auth/realms/standard/protocol/openid-connect/token" \
     -d grant_type=client_credentials \
     -d client_id="$YOUR_CLIENT_ID" \
     --data-urlencode "client_secret=$YOUR_CLIENT_SECRET" | jq -r .access_token)

   curl -sS -H "Authorization: Bearer $TOKEN" \
     "$USER_LOOKUP_BASE_URL/api/v1/user-lookup/idir-account-detail?userId=JSMITH"
   ```

   Tokens are short-lived — cache and refresh shortly before expiry rather than fetching
   one per request. Missing/expired token → **401**; valid token without the endpoint's
   scope → **403**.

Notes for consumers:

- **Don't forward your end user's token.** This is a service-to-service API; the outbound
  BCeID requester identity is fixed server-side. Call it with your app's own service-account
  token and apply your own authorization to the end user first.
- **One client per app per environment** (dev/test/prod are separate realms/secrets).
- Keep the client secret in a secret store (OpenShift `Secret`, GitHub Actions secret) —
  never in the image or repo.

### Reference implementation

`bcgov/nr-fsp-new` consumes this API and automates the above in CI:

- `.github/scripts/ensure-keycloak-service-account.sh` — idempotently creates its
  `nr-fsp-backend` client, assigns the three scopes as default client scopes, and emits the
  client id/secret as masked step outputs for the deploy.
- `backend/src/main/java/ca/bc/gov/nrs/fsp/api/client/ClientCredentialsTokenSource.java` —
  a small token source that caches the access token and refreshes ~60s before expiry.
- Config surface: `USER_LOOKUP_BASE_URL`, `USER_LOOKUP_TOKEN_URL`, `USER_LOOKUP_CLIENT_ID`,
  `USER_LOOKUP_CLIENT_SECRET` (and an optional `USER_LOOKUP_SCOPE`, unused because the
  scopes are defaults on the client).

## Configuration

All configuration is via environment variables (see `backend/src/main/resources/application.yml`):

| Variable | Description | Default |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | Realm issuer URI for token validation (the JWKS URI is derived as `<issuer>/protocol/openid-connect/certs`) | `https://dev.loginproxy.gov.bc.ca/auth/realms/standard` |
| `BCEID_WEB_SERVICE_URL` | BCeID SOAP endpoint (a `?WSDL` suffix is stripped automatically) | — |
| `BCEID_WEB_SERVICE_OSID` | `onlineServiceId` (OSID) sent in each request | — |
| `BCEID_WEB_SERVICE_REQUESTER_USER_GUID` | Internal IDIR service-identity GUID used as the requester on every lookup | — |
| `BCEID_WEB_SERVICE_USERNAME` | HTTP Basic auth username | — |
| `BCEID_WEB_SERVICE_PASSWORD` | HTTP Basic auth password | — |
| `BCEID_WEB_SERVICE_SOAP_ACTION_PREFIX` | SOAPAction prefix; operation name is appended | `http://www.bceid.ca/webservices/Client/V10/BCeIDService/` |
| `SERVER_PORT` | HTTP port | `8080` |
| `APP_LOG_LEVEL` | Log level for `ca.bc.gov.nrs.userlookup` | `INFO` |

## Local development

Requires JDK 21 (a Maven wrapper is included).

```bash
cd backend
export BCEID_WEB_SERVICE_OSID=... BCEID_WEB_SERVICE_REQUESTER_USER_GUID=... \
  BCEID_WEB_SERVICE_USERNAME=... BCEID_WEB_SERVICE_PASSWORD=...
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then:
- Health: `curl http://localhost:8080/actuator/health`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

Run the tests:

```bash
cd backend
./mvnw clean verify
```

Or run the container:

```bash
docker compose up --build
```

## BCeID SOAP integration

The Node proxy parsed the live WSDL at runtime; this service instead hand-writes the small
subset of the BCeID schema it uses (`searchInternalAccount`, `getAccountDetail`) as JAXB
classes under `backend/src/main/java/ca/bc/gov/nrs/userlookup/client/soap` and POSTs directly
to the endpoint (no WSDL fetch, so no WSDL credentials are needed).

Two values are SOAP-specific and isolated to a single place each (so a mismatch is a one-line
fix). Both are confirmed working against the BCeID **dev** service; re-verify against the WSDL
if a different environment uses different values:
> - the XML **namespace** in `client/soap/package-info.java` (`BceidSoapNamespace.URI`) —
>   `http://www.bceid.ca/webservices/Client/V10/`, must equal the WSDL `targetNamespace`; and
> - `BCEID_WEB_SERVICE_SOAP_ACTION_PREFIX` — `http://www.bceid.ca/webservices/Client/V10/`
>   (the operation name is appended, e.g. `.../V10/getAccountDetail`).

## Deployment

Containerized via `backend/Dockerfile` (multi-stage Maven build → distroless-style Temurin JRE,
non-root, health check on `/actuator/health`) and deployed to OpenShift via
`backend/openshift.deploy.yml` and the GitHub Actions workflows in `.github/workflows`.

The deploy workflow expects these repository **variables** — `KEYCLOAK_ISSUER_URI`,
`BCEID_WEB_SERVICE_URL`, `oc_server` — and **secrets** — `oc_namespace`, `oc_token`,
`bceid_web_service_osid`, `bceid_web_service_requester_user_guid`,
`bceid_web_service_username`, `bceid_web_service_password`.

### Keycloak scope provisioning

During deploy, `.github/scripts/ensure-keycloak-scopes.sh` idempotently creates this API's
client scopes in the target realm (derived from `KEYCLOAK_ISSUER_URI`) — existing scopes are
left untouched. It's **opt-in**: the step runs only when the secret `keycloak_sa_client_id`
is set, authenticating with a confidential service-account client via `client_credentials`.

- **secret** `keycloak_sa_client_id` — the service-account client id
- **secret** `keycloak_sa_client_secret` — its client secret

The service-account client must hold the realm-management **`manage-clients`** role. Set these
per environment alongside the other deploy values (see the environment note above) so each
realm gets its own service account.

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Security (OAuth2 resource server) · Spring Web Services + JAXB ·
springdoc OpenAPI · Actuator + Micrometer/Prometheus · Undertow · Maven.
