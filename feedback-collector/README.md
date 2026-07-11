# Feedback Collector Service

📝 Microsservico responsavel pela coleta e processamento de feedbacks de cursos.
⚡ Este servico adota arquitetura orientada a eventos para desacoplamento e escalabilidade.

## 🧰 Tecnologias
- ☕ Java 21 e Quarkus 3.x
- ☁️ AWS SDK v2 (DynamoDB e EventBridge)
- 🧪 Testcontainers (testes de integracao)

## ✅ Pre-requisitos
- 🐳 Docker e Docker Compose
- ☕ Java 21+
- 🛠️ Maven 3.9+

## ▶️ Como rodar o projeto
Para iniciar o servico em modo de desenvolvimento (com Live Coding):

```bash
mvn quarkus:dev
```

## 📚 Documentacao (Swagger)
Apos iniciar a aplicacao, acesse a interface interativa:

- 🌐 http://localhost:8080/swagger-ui
- 🌐 http://localhost:8080/openapi

## 📥 Contrato do endpoint

Endpoint: `POST /feedback`

Payload esperado:

```json
{
	"cursoId": "CURSO-01",
	"alunoId": "ALUNO-01",
	"professorId": "PROF-01",
	"nota": 7,
	"comentario": "Aula muito clara"
}
```

Regras de validacao:
- `cursoId`, `alunoId`, `professorId`, `comentario`: obrigatorios e nao vazios
- `nota`: inteiro entre 0 e 10
- Campos extras sao rejeitados

Schema JSON de referencia:
- `src/main/resources/schema/feedback-request.schema.json`

### Respostas HTTP
- `201 Created`: feedback criado com sucesso
- `400 Bad Request`: payload invalido
- `401 Unauthorized`: token ausente ou invalido
- `404 Not Found`: feedback nao encontrado em `GET /feedback/{id}`

Exemplo de erro (`400`):

```json
{
	"error_code": "BAD_REQUEST",
	"message": "Dados de entrada invalidos"
}
```

Exemplo de erro (`401`):

```json
{
	"error_code": "TOKEN_INVALID",
	"message": "Authorization Bearer token is required",
	"trace_id": "..."
}
```

Exemplo de erro (`404`):

```json
{
	"error_code": "NOT_FOUND",
	"message": "Feedback nao encontrado"
}
```

## 🧪 Executando testes
Para rodar a suite completa de testes (incluindo integracao com DynamoDB local via Testcontainers):

```bash
mvn verify
```

## 🐳 Build da imagem Docker
Na raiz do modulo, execute:

```bash
docker build -t feedback-collector:latest .
```

## 🚀 Executar container

```bash
docker run --rm -p 8080:8080 feedback-collector:latest
```
