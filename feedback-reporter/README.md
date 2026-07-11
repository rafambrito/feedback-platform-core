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
- `GET /reports/weekly?courseId={id}`
- `GET /reports/weekly?courseId={id}&professorId={id}`

## ☁️ Configuracao (DynamoDB)
Propriedades principais:
- `aws.region`
- `aws.dynamodb.table-name`
- `aws.dynamodb.endpoint.override` (ex: LocalStack `http://localhost:4566`)

Indices recomendados para performance (T11):
- `aws.dynamodb.gsi.curso-name`
- `aws.dynamodb.gsi.professor-name`

Premissas de indice:
- GSI de curso com partition key `cursoId` (exemplo: `cursoId-index`)
- GSI de professor com partition key `professorId` (exemplo: `professorId-index`)
- Quando os GSIs estao configurados, o servico usa `Query` no DynamoDB em vez de `Scan`
- Se o indice nao existir ou falhar, o servico usa fallback seguro para `Scan`

Contrato semanal:
- `courseId` e obrigatorio
- `professorId` e opcional para restringir o relatorio semanal por professor
- Resposta inclui metricas agregadas (`totalFeedbacks`, `averageNota`, contadores por criticidade)
	e agrupamento `feedbacksByProfessor`

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
