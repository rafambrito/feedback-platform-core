# Feedback Reporter Service

📊 Servico responsavel por consultar feedbacks no DynamoDB e expor relatorios por professor e curso.

## 👀 Visao Geral
Este microsservico disponibiliza endpoints de leitura para analise dos feedbacks coletados na plataforma.

A aplicacao esta configurada para rodar na porta `8082`.

## 🧰 Tecnologias
- ☕ Java 21 e Quarkus 3.x
- ☁️ Quarkiverse Amazon DynamoDB
- 🧩 Jackson para serializacao de respostas

## ▶️ Como Rodar
Pre-requisitos:
- ☕ Java 21+
- 🛠️ Maven 3.9+

No diretorio do servico:

```bash
cd /home/rafael/git/feedback-platform-core/feedback-reporter
mvn quarkus:dev
```

Aplicacao disponivel em:
- 🌐 http://localhost:8082

## 🔎 Endpoints
- `GET /reports/professor/{professorId}`
- `GET /reports/curso/{cursoId}`

## ☁️ Configuracao (DynamoDB)
Propriedades principais:
- `aws.region`
- `aws.dynamodb.table-name`
- `aws.dynamodb.endpoint.override` (ex: LocalStack `http://localhost:4566`)

## 🧪 Testes e Build
Compilacao rapida:

```bash
mvn -DskipTests compile
```

Build com testes:

```bash
mvn verify
```

## 🐳 Build e Execucao com Docker
Gerar imagem:

```bash
docker build -t feedback-reporter:latest .
```

Executar container:

```bash
docker run --rm -p 8082:8082 feedback-reporter:latest
```
