#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

LOCALSTACK_CONTAINER="fiap4-localstack"
COLLECTOR_URL="http://localhost:8080/feedback"

wait_healthy() {
  local container="$1"
  local timeout_sec="${2:-120}"
  local waited=0

  while true; do
    local health
    health="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' "${container}" 2>/dev/null || true)"
    if [[ "${health}" == "healthy" || "${health}" == "running" ]]; then
      return 0
    fi
    if (( waited >= timeout_sec )); then
      echo "Timeout esperando container saudável: ${container}" >&2
      return 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

wait_localstack_resources() {
  local timeout_sec="${1:-120}"
  local waited=0

  while true; do
    local queue_ok="false"
    local rule_ok="false"

    if docker exec "${LOCALSTACK_CONTAINER}" awslocal sqs get-queue-url --queue-name feedback-critico-queue >/dev/null 2>&1; then
      queue_ok="true"
    fi

    if docker exec "${LOCALSTACK_CONTAINER}" awslocal events list-rules --event-bus-name default --query 'Rules[?Name==`feedback-critico-to-sqs`].Name' --output text 2>/dev/null | grep -q feedback-critico-to-sqs; then
      rule_ok="true"
    fi

    if [[ "${queue_ok}" == "true" && "${rule_ok}" == "true" ]]; then
      return 0
    fi

    if (( waited >= timeout_sec )); then
      echo "Timeout aguardando recursos EventBridge/SQS no LocalStack" >&2
      return 1
    fi

    sleep 2
    waited=$((waited + 2))
  done
}

echo "[E2E] Subindo stack..."
docker compose up -d --build

wait_healthy "fiap4-localstack" 120
wait_localstack_resources 120
wait_healthy "fiap4-feedback-collector" 120
wait_healthy "fiap4-feedback-notifier" 120

echo "[E2E] Publicando feedback crítico no collector..."
REQUEST_PAYLOAD='{"cursoId":"curso-e2e","alunoId":"aluno-e2e","professorId":"prof-e2e","nota":1,"comentario":"feedback urgente para e2e"}'
RESPONSE="$(curl -sS -X POST "${COLLECTOR_URL}" -H 'Content-Type: application/json' -d "${REQUEST_PAYLOAD}")"

FEEDBACK_ID="$(python3 - <<'PY' "${RESPONSE}"
import json,sys
obj=json.loads(sys.argv[1])
print(obj.get('id',''))
PY
)"

if [[ -z "${FEEDBACK_ID}" ]]; then
  echo "Falha ao obter id do feedback na resposta do collector: ${RESPONSE}" >&2
  exit 1
fi

echo "[E2E] Feedback criado: ${FEEDBACK_ID}. Aguardando notificação ENVIADA no DynamoDB..."

FOUND_STATUS=""
for _ in $(seq 1 30); do
  SCAN_OUTPUT="$(docker exec "${LOCALSTACK_CONTAINER}" awslocal dynamodb scan \
    --table-name NotificacaoTable \
    --filter-expression 'feedbackId = :f' \
    --expression-attribute-values "{\":f\":{\"S\":\"${FEEDBACK_ID}\"}}" \
    --output json)"

  FOUND_STATUS="$(python3 - <<'PY' "${SCAN_OUTPUT}"
import json,sys
obj=json.loads(sys.argv[1])
items=obj.get('Items', [])
if not items:
    print('')
    raise SystemExit
status=items[0].get('status',{}).get('S','')
print(status)
PY
)"

  if [[ "${FOUND_STATUS}" == "ENVIADA" ]]; then
    break
  fi
  sleep 2
done

if [[ "${FOUND_STATUS}" != "ENVIADA" ]]; then
  echo "Falha E2E: notificação não encontrada com status ENVIADA para feedbackId=${FEEDBACK_ID}" >&2
  echo "Dica: verifique logs com: docker compose logs --tail=100 feedback-notifier" >&2
  exit 1
fi

echo "[E2E] Sucesso: notificação persistida com status ENVIADA para feedbackId=${FEEDBACK_ID}."
