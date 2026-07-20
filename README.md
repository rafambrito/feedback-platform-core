# Feedback Platform

**Tech Challenge – Fase 4**  
**FIAP Pós Tech – Arquitetura e Desenvolvimento em Java**

---

# 📖 Visão Geral

Feedback Platform é uma plataforma **serverless** para coleta, processamento e análise de feedbacks acadêmicos.

A solução foi desenvolvida utilizando microsserviços, AWS Lambda e serviços gerenciados da AWS para oferecer escalabilidade, baixo acoplamento e processamento orientado a eventos.

---

# 🔗 Links

### Swagger (Documentação da API)

https://rafambrito.github.io/feedback-platform-core/

### API (Ambiente DEV)

Resolva a URL atual a partir dos outputs da stack ativa:

```bash
aws cloudformation describe-stacks \
     --stack-name feedback-platform \
     --region us-east-2 \
     --query "Stacks[0].Outputs[?OutputKey=='RestApiUrl'].OutputValue | [0]" \
     --output text
```

---

# 🏗️ Arquitetura

A solução adota uma arquitetura **Serverless Event-Driven**.

Fluxo simplificado:

```text
                        GitHub Actions
                              │
                              ▼
                         AWS SAM Deploy
                              │
                              ▼
                         Amazon API Gateway
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
        Feedback Collector  Reporter     Notifier
               │                             │
               │                             ▼
               │                         Amazon SQS
               │                             │
               ▼                             ▼
          Amazon DynamoDB             Amazon SES
               │
               ▼
        Amazon EventBridge
```

---

# 🚀 Microsserviços

### 📝 feedback-collector

Responsável por:

- receber feedbacks
- validar dados
- persistir informações no DynamoDB
- publicar eventos críticos no EventBridge

---

### 🔔 feedback-notifier

Responsável por:

- consumir mensagens da fila SQS
- processar notificações
- enviar e-mails utilizando Amazon SES

---

### 📊 feedback-reporter

Responsável por:

- consultar feedbacks
- consolidar informações
- disponibilizar relatórios por professor e curso

---

# 🛠️ Tecnologias

- Java 21
- AWS Lambda
- AWS SAM
- Amazon API Gateway
- Amazon Cognito
- Amazon DynamoDB
- Amazon EventBridge
- Amazon SQS
- Amazon SES
- Maven
- Docker
- OpenAPI 3
- GitHub Actions

---

# 🧱 Estrutura do Projeto

```text
feedback-platform-core/
├── pom.xml
├── docker-compose.yml
├── template.yaml
├── openapi.yaml
├── docs/
│   ├── index.html
│   └── openapi.yaml
├── feedback-collector/
├── feedback-notifier/
└── feedback-reporter/
```

---

# 📘 Documentação da API

A API segue a especificação **OpenAPI 3**.

A documentação está disponível em:

- Swagger UI
- arquivo `openapi.yaml`

A documentação contempla:

- endpoints
- parâmetros
- payloads
- responses
- autenticação JWT
- exemplos de requisição

---

# 🔐 Autenticação

A API utiliza autenticação baseada em **JWT** através do **Amazon Cognito**.

Durante o deploy são provisionados automaticamente:

- User Pool
- User Pool Client
- Grupo ADMIN

Os dados do usuário administrador podem ser personalizados utilizando os parâmetros:

- `DefaultAdminUsername`
- `DefaultAdminEmail`
- `DefaultAdminPassword`

Para uso do botão **Authorize** no Swagger UI (GitHub Pages), o Cognito foi configurado com OAuth2 Authorization Code Flow e os seguintes callbacks:

- `https://rafambrito.github.io/oauth2-redirect.html`
- `https://rafambrito.github.io/`
- `https://rafambrito.github.io/feedback-platform-core/oauth2-redirect.html`
- `https://rafambrito.github.io/feedback-platform-core/`

Também foram habilitados os respectivos logout URLs:

- `https://rafambrito.github.io/`
- `https://rafambrito.github.io/feedback-platform-core/`

Endpoints OAuth2 do Cognito (Hosted UI):

- authorization endpoint: output `CognitoOAuthAuthorizationUrl`
- token endpoint: output `CognitoOAuthTokenUrl`

Para manter Swagger e OAuth sempre alinhados com a stack publicada, execute:

```bash
bash scripts/sync-swagger-oauth.sh feedback-platform us-east-2
```

---

# 🧪 Testando a API

## 1. Obter um Access Token

Exemplo utilizando AWS CLI:

```bash
aws cognito-idp initiate-auth \
  --region us-east-2 \
  --client-id <COGNITO_USER_POOL_CLIENT_ID> \
  --auth-flow USER_PASSWORD_AUTH \
  --auth-parameters USERNAME=<usuario>,PASSWORD=<senha>
```

Utilize o token retornado:

```text
Authorization: Bearer <ACCESS_TOKEN>
```

---

## 2. Criar um Feedback

Resolva a URL atual da API a partir da stack ativa:

```bash
API_BASE_URL=$(aws cloudformation describe-stacks \
     --stack-name feedback-platform \
     --region us-east-2 \
     --query "Stacks[0].Outputs[?OutputKey=='RestApiUrl'].OutputValue | [0]" \
     --output text)
```

```bash
curl -X POST \
"${API_BASE_URL}/feedback" \
-H 'Authorization: Bearer <ACCESS_TOKEN>' \
-H 'Content-Type: application/json' \
-d '{
  "cursoId":"1TIA",
  "alunoId":"aluno-001",
  "professorId":"prof-123",
  "nota":9,
  "comentario":"Aula clara, com boa explicação dos exercícios."
}'
```

---

## 3. Consultar Relatório

```bash
curl \
"${API_BASE_URL}/reports/weekly?courseId=1TIA" \
-H 'Authorization: Bearer <ACCESS_TOKEN>'
```

---

# ☁️ Desenvolvimento Local

## Build

```bash
mvn clean install
```

---

## Subir o ambiente

```bash
docker-compose up --build
```

O ambiente inicia:

- LocalStack
- Feedback Collector
- Feedback Notifier
- Feedback Reporter

---

# 🔍 Observabilidade

Padrão adotado em todos os microsserviços:

- logs estruturados em JSON
- correlação por Request ID
- métricas de latência
- métricas de erros
- métricas de invocações

Checklist operacional:

```text
docs/observability-checklist.md
```

---

# 🚀 Deploy AWS SAM

Validar:

```bash
sam validate --region us-east-2
```

Build:

```bash
sam build --region us-east-2
```

Deploy:

```bash
sam deploy \
     --template-file .aws-sam/build/template.yaml \
     --stack-name feedback-platform \
     --region us-east-2 \
     --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
     --resolve-s3 \
     --no-confirm-changeset \
     --no-fail-on-empty-changeset \
     --parameter-overrides \
          StageName=dev \
          DevJwtIssuer=https://cognito-idp.us-east-2.amazonaws.com/us-east-2_uqihO61Nf

# sincroniza client_id e URLs OAuth do Swagger com os Outputs reais da stack
./scripts/sync-swagger-oauth.sh feedback-platform us-east-2
```

Parâmetros recomendados:

```text
StageName=dev

DevJwtIssuer=https://cognito-idp.us-east-2.amazonaws.com/us-east-2_uqihO61Nf
```

---

# ✅ Funcionalidades

- Coleta de feedbacks
- Processamento orientado a eventos
- Persistência em DynamoDB
- Notificações automáticas
- Relatórios acadêmicos
- API protegida por Cognito
- Documentação OpenAPI
- Deploy automatizado com GitHub Actions
- Infraestrutura como código utilizando AWS SAM

---

# 👨‍💻 Autor

**Rafael Mendonça de Brito (RM369933)**

FIAP Pós Tech — Arquitetura e Desenvolvimento em Java