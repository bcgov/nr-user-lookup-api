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
require a bearer token carrying the listed scope. Interactive docs (with an "Authorize"
dialog) are at `/swagger-ui/index.html`.

| Method | Path | Required scope | Description |
|---|---|---|---|
| `POST` | `/api/v1/user-lookup/idir-users/search` | `IDIR_SEARCH` | Search IDIR users by firstName / lastName / userId (partial match). |
| `GET`  | `/api/v1/user-lookup/idir-account-detail` | `IDIR_READ` | Get an IDIR user by exact `userId`. |
| `GET`  | `/api/v1/user-lookup/businessBceid` | `BUSINESS_BCEID_READ` | Get a BCeID business user by exact `userId` or `userGuid`. |

The `POST .../idir-users/search` takes its search criteria as query parameters (`firstName`,
`lastName`, `userId`, the optional `*MatchMode` values of `Exact`/`Contains`/`StartsWith`, and
`pageSize`). At least one of `firstName`/`lastName`/`userId` is required; match mode defaults to
`Contains` and `pageSize` to `50`.

The requester identity (`requesterUserGuid`) is a fixed, internal server-side configuration
value (`BCEID_WEB_SERVICE_REQUESTER_USER_GUID`) used as an Internal/IDIR requester on every
outbound lookup — it is **not** accepted from callers.

Scopes map to the JWT `scope` claim via Spring's default `SCOPE_*` authority conversion; the
custom scopes (`IDIR_SEARCH`, `IDIR_READ`, `BUSINESS_BCEID_READ`) must be configured as client
scopes on the Keycloak integration and assigned to the calling client.

New API versions go under a new path prefix (`/api/v2/...`) backed by a parallel
`controller.v2` package, leaving `v1` clients untouched.

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

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Security (OAuth2 resource server) · Spring Web Services + JAXB ·
springdoc OpenAPI · Actuator + Micrometer/Prometheus · Undertow · Maven.
