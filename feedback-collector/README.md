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
