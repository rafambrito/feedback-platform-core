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

