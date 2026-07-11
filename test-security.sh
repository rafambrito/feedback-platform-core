#!/usr/bin/env bash
set -euo pipefail

echo "=== SUITE DE SEGURANCA JWT (T14) ==="
echo "Executando cenarios token ausente/invalido/valido para collector, notifier e reporter"

mvn -q -pl feedback-collector -Dtest=FeedbackResourceSecurityTest test
mvn -q -pl feedback-notifier -Dtest=NotificationResourceSecurityTest test
mvn -q -pl feedback-reporter -Dtest=FeedbackReporterResourceSecurityTest test

echo ""
echo "=== RESUMO ==="
echo "SUCESSO: suite de seguranca JWT executada para os 3 servicos"
