# Feedback Platform

**Fase 4 – Tech Challenge - FIAP Pós Tech – Arquitetura e Desenvolvimento em JAVA**

## 🧭 Visão geral

Ecossistema de microsserviços para gestão e análise de feedbacks acadêmicos. Desenvolvido para processar, notificar e reportar avaliações de cursos de forma ágil e resiliente, otimizando a tomada de decisão no ambiente acadêmico.

## 🚀 Componentes do Sistema

* 📝 **`feedback-collector`**: API de entrada responsável pela coleta e persistência de feedbacks no DynamoDB e publicação de eventos críticos.
* 🔔 **`feedback-notifier`**: Consumidor de mensagens (SQS) que processa e dispara notificações automáticas para feedbacks críticos.
* 📊 **`feedback-reporter`**: Interface de consulta que agrega e expõe relatórios detalhados por professor e curso.

---

## 🧱 Estrutura

```text
feedback-platform-core/
├── pom.xml             # Agregador Maven
├── docker-compose.yml  # Orquestração do ambiente
├── feedback-collector/
├── feedback-notifier/
└── feedback-reporter/
```

## 🛠️ Comandos de Desenvolvimento

### Build Unificado

Para compilar todos os serviços e suas dependências:

```bash
mvn clean install
```

### Subir o Ambiente Completo

Para iniciar toda a infraestrutura local (LocalStack + Microsserviços):

```bash
docker-compose up --build
```

O que o ambiente compõe:

- ☁️ **LocalStack**: Emula AWS (DynamoDB, SQS, EventBridge) na porta 4566.
- 📝 **Collector**: Disponível em http://localhost:8080.
- 🔔 **Notifier**: Disponível em http://localhost:8081.
- 📊 **Reporter**: Disponível em http://localhost:8082.

## ✅ Infraestrutura e Resiliência

Este projeto utiliza Health Checks nativos via docker-compose.

- Todos os serviços monitoram a saúde da porta de entrada correspondente.
- Os microsserviços utilizam `depends_on` com `condition: service_healthy`, garantindo que a aplicação só suba após o LocalStack estar totalmente operacional.

## 📈 Observabilidade

Padrão adotado para os 3 serviços:

- Logs estruturados em JSON
- Correlação por request-id (headers `X-Request-Id` e fallback para `X-Trace-Id`)
- Métricas mínimas de operação: invocações, erros (4xx/5xx) e latência (p50/p95/p99)

Checklist operacional e definição de painel mínimo:

- `docs/observability-checklist.md`

## 📘 Documentação da API

O contrato OpenAPI mantido na raiz do projeto está em `openapi.yaml` e documenta todos os endpoints expostos pelas Lambdas com:

- rotas, métodos e parâmetros
- request bodies e responses
- autenticação Bearer JWT com Amazon Cognito
- exemplos de payload para testes rápidos

Endpoint publicado em dev:

- `https://xy1fzhv8o3.execute-api.us-east-2.amazonaws.com/dev`

Observação importante sobre AWS SAM:

- o arquivo `openapi.yaml` é a referência legível do contrato para o time
- para manter compatibilidade com `sam validate` usando `Auth` Cognito, o mesmo contrato também foi embutido em `DefinitionBody` dentro de `template.yaml`, porque o SAM exige definição inline para aplicar authorizers Cognito

## 🔐 Autenticação Cognito

O `template.yaml` provisiona:

- `AWS::Cognito::UserPool`
- `AWS::Cognito::UserPoolClient`
- grupo `ADMIN`
- usuário padrão para testes

Credenciais padrão de teste:

- usuário: `admin@admin.com`
- senha: `Fiap2026Brito`

O usuário é criado como permanente via custom resource no deploy. Se quiser sobrescrever em outro ambiente, passe os parâmetros abaixo no deploy:

- `DefaultAdminUsername`
- `DefaultAdminEmail`
- `DefaultAdminPassword`

## 🧪 Como testar a API

### 1. Gerar token no Cognito

Exemplo com AWS CLI usando fluxo de senha:

```bash
aws cognito-idp initiate-auth \
	--region us-east-2 \
	--client-id <COGNITO_USER_POOL_CLIENT_ID> \
	--auth-flow USER_PASSWORD_AUTH \
	--auth-parameters USERNAME=admin@admin.com,PASSWORD=Fiap2026Brito
```

Use o `AccessToken` retornado no cabeçalho:

```bash
Authorization: Bearer <ACCESS_TOKEN>
```

### 2. Testar endpoints

Criar feedback:

```bash
curl -X POST 'https://xy1fzhv8o3.execute-api.us-east-2.amazonaws.com/dev/feedback' \
	-H 'Authorization: Bearer <ACCESS_TOKEN>' \
	-H 'Content-Type: application/json' \
	-d '{
		"cursoId": "1TIA",
		"alunoId": "aluno-001",
		"professorId": "prof-123",
		"nota": 9,
		"comentario": "Aula clara, com boa explicacao dos exercicios."
	}'
```

Consultar relatório semanal:

```bash
curl 'https://xy1fzhv8o3.execute-api.us-east-2.amazonaws.com/dev/reports/weekly?courseId=1TIA' \
	-H 'Authorization: Bearer <ACCESS_TOKEN>'
```

## ☁️ Fluxo SAM

Validar template:

```bash
sam validate --region us-east-2
```

Build da aplicação:

```bash
sam build --region us-east-2
```

Deploy guiado:

```bash
sam deploy --guided --region us-east-2
```

Se a stack `feedback-platform` ficar em `ROLLBACK_COMPLETE` após uma tentativa anterior, apague a stack antes de redeployar ou use o pipeline atualizado, que remove esse estado automaticamente.

Parâmetros recomendados no deploy:

```text
StageName=dev
DevJwtIssuer=https://cognito-idp.us-east-2.amazonaws.com/<USER_POOL_ID>
HmlJwtIssuer=https://cognito-idp.us-east-2.amazonaws.com/<USER_POOL_ID>
PrdJwtIssuer=https://cognito-idp.us-east-2.amazonaws.com/<USER_POOL_ID>
```

## ✅ Validação executada

Validações executadas neste repositório após as alterações:

- `sam validate --region us-east-2`: sucesso
- `sam build --region us-east-2`: sucesso

O deploy real em AWS não foi executado neste ambiente porque depende das suas credenciais, confirmação de stack e parâmetros finais de infraestrutura.

