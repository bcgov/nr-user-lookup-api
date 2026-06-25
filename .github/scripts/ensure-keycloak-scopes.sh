#!/usr/bin/env bash
#
# Ensure the OAuth2 client scopes this API enforces exist in the target Keycloak
# realm. Idempotent: lists the realm's client scopes and creates only the ones
# that are missing (existing scopes are left untouched).
#
# Authenticates with a confidential service-account client (client_credentials)
# in the same realm; that client needs the realm-management `manage-clients` role.
#
# Realm and endpoints are derived from KEYCLOAK_ISSUER_URI so there is a single
# source of truth (works with or without a `/auth` base path).
#
# Required environment:
#   KEYCLOAK_ISSUER_URI   e.g. https://dev.loginproxy.gov.bc.ca/auth/realms/my-realm
#   KC_SA_CLIENT_ID       service-account client id
#   KC_SA_CLIENT_SECRET   service-account client secret
#
# Keep SCOPES in sync with ca.bc.gov.nrs.userlookup.security.ApiScopes.
set -euo pipefail

: "${KEYCLOAK_ISSUER_URI:?KEYCLOAK_ISSUER_URI is required}"
: "${KC_SA_CLIENT_ID:?KC_SA_CLIENT_ID is required}"
: "${KC_SA_CLIENT_SECRET:?KC_SA_CLIENT_SECRET is required}"

SCOPES=(
  "user-lookup:idir:search"
  "user-lookup:idir:read"
  "user-lookup:business-bceid:read"
)

issuer="${KEYCLOAK_ISSUER_URI%/}"
realm="${issuer##*/realms/}"
base="${issuer%/realms/*}"
token_url="${issuer}/protocol/openid-connect/token"
scopes_url="${base}/admin/realms/${realm}/client-scopes"

echo "Keycloak realm: ${realm}"
echo "Client-scopes endpoint: ${scopes_url}"

# --- obtain an admin token via client_credentials ---------------------------
token="$(curl -sS -X POST "${token_url}" \
  -d grant_type=client_credentials \
  -d client_id="${KC_SA_CLIENT_ID}" \
  --data-urlencode "client_secret=${KC_SA_CLIENT_SECRET}" \
  | jq -r '.access_token // empty')"

if [ -z "${token}" ]; then
  echo "::error::Could not obtain a Keycloak admin token. Check the service-account client id/secret and that it has the realm-management 'manage-clients' role."
  exit 1
fi

# --- existing scope names ---------------------------------------------------
existing="$(curl -sS -H "Authorization: Bearer ${token}" "${scopes_url}" | jq -r '.[].name')"

created=0
for scope in "${SCOPES[@]}"; do
  if grep -qxF "${scope}" <<< "${existing}"; then
    echo "✓ exists: ${scope}"
    continue
  fi

  echo "+ creating: ${scope}"
  body="$(jq -n --arg name "${scope}" '{
    name: $name,
    protocol: "openid-connect",
    description: "Managed by nr-user-lookup-api CI",
    attributes: {
      "include.in.token.scope": "true",
      "display.on.consent.screen": "false"
    }
  }')"

  code="$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${scopes_url}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${body}")"

  if [ "${code}" != "201" ]; then
    echo "::error::Failed to create client scope '${scope}' (HTTP ${code})."
    exit 1
  fi
  created=$((created + 1))
done

echo "Done. ${created} scope(s) created, $(( ${#SCOPES[@]} - created )) already present."
