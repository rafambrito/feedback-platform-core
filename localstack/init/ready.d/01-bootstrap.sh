#!/bin/bash
set -euo pipefail

AWS_CMD="awslocal"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
ACCOUNT_ID="000000000000"
EVENT_BUS="default"
RULE_NAME="feedback-critico-to-sqs"
QUEUE_NAME="feedback-critico-queue"
DLQ_NAME="feedback-critico-dlq"

ensure_table() {
  local table_name="$1"
  if ! ${AWS_CMD} dynamodb describe-table --table-name "${table_name}" >/dev/null 2>&1; then
    ${AWS_CMD} dynamodb create-table \
      --table-name "${table_name}" \
      --attribute-definitions AttributeName=id,AttributeType=S \
      --key-schema AttributeName=id,KeyType=HASH \
      --billing-mode PAY_PER_REQUEST >/dev/null
  fi
}

ensure_table "FeedbackTable"
ensure_table "NotificacaoTable"

if ! ${AWS_CMD} sqs get-queue-url --queue-name "${DLQ_NAME}" >/dev/null 2>&1; then
  ${AWS_CMD} sqs create-queue --queue-name "${DLQ_NAME}" >/dev/null
fi

DLQ_URL="$(${AWS_CMD} sqs get-queue-url --queue-name "${DLQ_NAME}" --query QueueUrl --output text)"
DLQ_ARN="$(${AWS_CMD} sqs get-queue-attributes --queue-url "${DLQ_URL}" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)"

if ! ${AWS_CMD} sqs get-queue-url --queue-name "${QUEUE_NAME}" >/dev/null 2>&1; then
  ${AWS_CMD} sqs create-queue --queue-name "${QUEUE_NAME}" >/dev/null
fi

QUEUE_URL="$(${AWS_CMD} sqs get-queue-url --queue-name "${QUEUE_NAME}" --query QueueUrl --output text)"
QUEUE_ARN="$(${AWS_CMD} sqs get-queue-attributes --queue-url "${QUEUE_URL}" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)"

${AWS_CMD} events put-rule \
  --name "${RULE_NAME}" \
  --event-bus-name "${EVENT_BUS}" \
  --event-pattern '{"source":["com.feedback.platform"],"detail-type":["FeedbackCriticoEvent"]}' >/dev/null

${AWS_CMD} events put-targets \
  --event-bus-name "${EVENT_BUS}" \
  --rule "${RULE_NAME}" \
  --targets "Id"="1","Arn"="${QUEUE_ARN}" >/dev/null

echo "LocalStack bootstrap concluido: FeedbackTable, NotificacaoTable, EventBridge e filas SQS prontas."
