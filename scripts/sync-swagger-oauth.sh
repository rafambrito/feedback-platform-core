#!/usr/bin/env bash
set -euo pipefail

STACK_NAME="${1:-feedback-platform}"
REGION="${2:-us-east-2}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS_OPENAPI="${ROOT_DIR}/docs/openapi.yaml"
ROOT_OPENAPI="${ROOT_DIR}/openapi.yaml"
DOCS_CONFIG="${ROOT_DIR}/docs/oauth-config.json"

get_output() {
  local key="$1"
  aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='${key}'].OutputValue | [0]" \
    --output text
}

CLIENT_ID="$(get_output "CognitoUserPoolClientId")"
AUTH_URL="$(get_output "CognitoOAuthAuthorizationUrl")"
TOKEN_URL="$(get_output "CognitoOAuthTokenUrl")"
REST_API_URL="$(get_output "RestApiUrl")"

if [[ -z "${CLIENT_ID}" || "${CLIENT_ID}" == "None" ]]; then
  echo "ERROR: CognitoUserPoolClientId not found in stack '${STACK_NAME}' (${REGION})." >&2
  exit 1
fi

if [[ -z "${AUTH_URL}" || "${AUTH_URL}" == "None" || -z "${TOKEN_URL}" || "${TOKEN_URL}" == "None" ]]; then
  echo "ERROR: OAuth URLs not found in stack '${STACK_NAME}' (${REGION})." >&2
  exit 1
fi

if [[ -z "${REST_API_URL}" || "${REST_API_URL}" == "None" ]]; then
  echo "ERROR: RestApiUrl not found in stack '${STACK_NAME}' (${REGION})." >&2
  exit 1
fi

update_openapi_urls() {
  local file_path="$1"
  [[ -f "${file_path}" ]] || return 0

  sed -i "0,/^[[:space:]]*- url: https:\/\/.*execute-api\..*amazonaws\.com\/dev/s//  - url: ${REST_API_URL//\//\\/}/" "${file_path}"
  sed -i "s|^\([[:space:]]*authorizationUrl:[[:space:]]*\).*|\1${AUTH_URL}|" "${file_path}"
  sed -i "s|^\([[:space:]]*tokenUrl:[[:space:]]*\).*|\1${TOKEN_URL}|" "${file_path}"
}

update_openapi_urls "${DOCS_OPENAPI}"
update_openapi_urls "${ROOT_OPENAPI}"

cat > "${DOCS_CONFIG}" <<EOF
{
  "stackName": "${STACK_NAME}",
  "region": "${REGION}",
  "clientId": "${CLIENT_ID}",
  "restApiUrl": "${REST_API_URL}",
  "authorizationUrl": "${AUTH_URL}",
  "tokenUrl": "${TOKEN_URL}",
  "oauth2RedirectUrl": "https://rafambrito.github.io/feedback-platform-core/oauth2-redirect.html",
  "openapiUrl": "./openapi.yaml"
}
EOF

echo "Swagger OAuth sync complete."
echo "- stack: ${STACK_NAME}"
echo "- region: ${REGION}"
echo "- clientId: ${CLIENT_ID}"
echo "- restApiUrl: ${REST_API_URL}"
echo "- docs config: ${DOCS_CONFIG}"
