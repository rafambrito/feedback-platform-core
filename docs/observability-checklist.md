# Observabilidade - Painel Minimo e Checklist Operacional

Este documento consolida o baseline de observabilidade para os servicos:
- feedback-collector
- feedback-notifier
- feedback-reporter

Objetivo:
- padronizar logs estruturados JSON com correlacao por request-id;
- definir painel minimo de metricas (invocacoes, erros, latencia);
- formalizar checklist operacional para validacao em ambiente local e AWS.

## 1. Convencoes de logs

Padrao adotado nos tres servicos:
- formato JSON habilitado via Quarkus;
- campos recomendados no payload de log da aplicacao:
  - service_name
  - status_code
  - error_code (quando aplicavel)
  - sub (quando autenticado)
  - request_id / trace_id (quando disponivel)
- correlacao por request-id:
  - entrada preferencial por header X-Request-Id;
  - fallback para X-Trace-Id (compatibilidade);
  - fallback final para UUID gerado no servico.

## 2. Painel minimo de metricas (CloudWatch/Grafana)

Widgets minimos por servico:

1. Invocacoes
- metrica: contagem de requisicoes (HTTP) por endpoint
- agregacao: sum por 1m e 5m
- alerta sugerido: queda abrupta de trafego > 80% por 15m

2. Erros
- metrica: taxa de erro 4xx e 5xx
- agregacao: percentual e contagem absoluta
- alerta sugerido: 5xx > 2% por 5m ou erro absoluto > 20 em 5m

3. Latencia
- metrica: p50, p95 e p99 de tempo de resposta
- agregacao: por endpoint principal
- alerta sugerido: p95 acima do SLO por 10m

### Endpoints-chave para monitorar
- collector:
  - POST /feedback
  - GET /feedback/{id}
- notifier:
  - POST /notifications/urgent
  - GET /notifications/{id}
  - POST /notifications/test/simulate
- reporter:
  - GET /reports/professor/{professorId}
  - GET /reports/curso/{cursoId}
  - GET /reports/weekly

## 3. Checklist operacional

### 3.1 Deploy e runtime
- [ ] Logs JSON habilitados nos tres servicos
- [ ] service_name consistente por servico
- [ ] request-id presente no caminho feliz e de erro
- [ ] Logs visiveis no CloudWatch Log Group correto por servico

### 3.2 Qualidade de metricas
- [ ] Invocacoes por endpoint aparecem no painel
- [ ] Erros 4xx/5xx segregados por servico
- [ ] p50/p95/p99 disponiveis para endpoints criticos
- [ ] Alarmes configurados e testados (simulacao controlada)

### 3.3 Operacao diaria
- [ ] Runbook com acao para alarmes de erro
- [ ] Runbook com acao para aumento de latencia
- [ ] Retencao de logs definida por ambiente (dev/hml/prd)
- [ ] Janela de revisao semanal de observabilidade definida

## 4. Validacao local rapida

1. Subir servicos localmente:
```bash
docker compose up -d --build
```

2. Realizar chamadas de smoke:
```bash
./scripts/e2e/collector_notifier_e2e.sh
```

3. Verificar logs JSON:
```bash
docker logs fiap4-feedback-collector | head -n 20
docker logs fiap4-feedback-notifier | head -n 20
docker logs fiap4-feedback-reporter | head -n 20
```

4. Confirmar presenca de request-id/trace-id em respostas de erro (401/404/500).

## 5. Mapeamento para backlog

- T15: padronizacao de logs JSON + correlacao por request-id (properties dos tres servicos).
- T16: definicao do painel minimo de metricas e checklist operacional (este documento + README raiz).
