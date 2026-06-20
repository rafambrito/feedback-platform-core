# Feedback Notifier Service

🔔 Servico responsavel por consumir eventos de feedback critico e disparar notificacoes.

## 👀 Visao Geral
Este microsservico integra com Amazon SQS para receber eventos criticos produzidos por outros servicos da plataforma e processa o payload para acionar notificacoes.

A aplicacao esta configurada para rodar na porta `8081`.

## ▶️ Como Rodar
Pre-requisitos:
- ☕ Java 21+
- 🛠️ Maven 3.9+

No diretorio do servico:

```bash
cd /home/rafael/git/feedback-platform-core/feedback-notifier
./mvnw quarkus:dev
```

Aplicacao disponivel em:
- 🌐 http://localhost:8081

## ☁️ Configuracao (SQS)
Variaveis de ambiente recomendadas para execucao:
- `SQS_QUEUE_URL`: URL da fila principal de eventos criticos
- `AWS_REGION`: regiao AWS da fila (ex: `us-east-1`)

Mapeamento para propriedades do Quarkus (se preferir configurar por arquivo):
- `aws.sqs.queue-url`
- `aws.sqs.region`
- `aws.sqs.endpoint-override` (opcional, util para LocalStack)
- `aws.sqs.dlq-url` (referencia de boas praticas para DLQ)
- `aws.sqs.max-receive-count` (tentativas antes de DLQ)

## 🧪 Testes
Rodar testes unitarios e integracao:

```bash
./mvnw verify
```

Rodar apenas compilacao rapida:

```bash
./mvnw -DskipTests compile
```

## 🧰 Dependencias Principais
- ⚡ Quarkus 3.x
- ☁️ Amazon SQS (extensao `quarkus-amazon-sqs`)
- 🧩 Jackson (desserializacao de payload JSON)

## 🐳 Build e Deploy com Docker
Gerar imagem:

```bash
docker build -t feedback-notifier:latest .
```

Executar container:

```bash
docker run --rm -p 8081:8081 feedback-notifier:latest
```
