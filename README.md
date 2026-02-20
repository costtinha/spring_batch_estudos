# 🛒 Spring Batch Training: Import and Export de Usuários

API REST Spring Boot com Spring Batch para importação e exportação de usuários em lote.
Implementa jobs para ler dados de CSV, persistir em PostgreSQL com cache Redis, e exportar de volta para CSV com particionamento para escalabilidade em grandes volumes de dados.
O foco é demonstrar processamento batch eficiente, com paralelismo e gerenciamento de transações para datasets grandes (ex: 10k+ usuários).
---

## 📋 Índice

- [Sobre o projeto](#-sobre-o-projeto)
- [Por que Spring Batch?](#-por-que-spring-batch)
- [Tecnologias](#-tecnologias)
- [Funcionalidades principais](#-funcionalidades-principais)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Execução](#-instalação-e-execução)
- [Endpoints da API](#-endpoints-api)
- [Exemplos de Requisições](#-exemplos-de-requisições)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Decisões & Aprendizados](#-decisões--aprendizados)
- [Playground / Interface GraphQL](#-playground--interface-graphql)
- [Licença](#-licença)

---

## 🚀 Sobre o projeto

Sistema backend para gerenciar importação e exportação de usuários em lote.
Lê arquivos CSV (ex: user_10k.csv), processa e persiste em banco PostgreSQL com cache Redis para otimização. Para exportação, usa particionamento para dividir o trabalho em chunks paralelizáveis, gerando arquivos CSV parciais e combinando no final.
Ideal para demonstrar conceitos de batch processing em portfólio ou estudos técnicos, com ênfase em escalabilidade para grandes volumes de dados.
---

## Por que Spring Batch?

| Abordagem        | Performance em escala    | Paralelismo | Complexidade |
|------------------|--------------------------|-------------|--------------|
| `Step Simples`   | Limitada                 | Baixo       | Baixa        |
| `Particionamento` | Alta (Divisão em slaves) | Alto        | Média        |

Implementado com partitioner customizado baseado em ranges de IDs (minId/maxId), permitindo processamento paralelo de grandes tabelas sem sobrecarregar o banco.

---

## 🛠 Tecnologias

| Tecnologia         | Versão   | Finalidade principal                              |
|--------------------|----------|---------------------------------------------------|
| Java               | 17+      | Linguagem                                         |
| Spring Boot        | 3.x      | Framework principal                               |
| Spring Batch       | 5.x      | Processamento em lote (jobs, steps, partitioning)                         |
| Spring Data JPA    | 3.x      | Persistência                                      |
| PostgreSQL / MySQL | 15+ / 8+ | Banco de dados (compatível com Classic Models)    |
| Redis              | —        | Cache para entidades de usuários                    |
| SLF4J  | —        | Logging detalhado                       |
|     Maven   |  3.9+        |     Build e dependências                                    |



---

## ✨ Funcionalidades principais

- Jobs de importação: Lê CSV, processa (ex: concatena nome), persiste no banco e cache Redis.
- Jobs de exportação: Lê do banco com paging, particiona ranges de IDs, gera CSVs parciais e combina no final.
- Particionamento paralelo com ThreadPoolTaskExecutor (core/max pool size configuráveis).
- Cache Redis para usuários (TTL 3600s) para otimização de leituras.
- Inicialização automática de schema batch no PostgreSQL.
- Controller REST para lançar jobs via HTTP.
- Logging detalhado para monitoramento de steps e partitions.
- Tratamento de erros em inicialização (ex: arquivo CSV não encontrado).

---

## 📦 Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker + Docker Compose (opcional)
- PostgreSQL rodando (configurável via environment variables)
- Redis rodando (opcional, mas recomendado para cache)
- Arquivo CSV de entrada (ex: /app/user_10k.csv para import)
---

## 🚀 Instalação e Execução

### Com Docker Compose (recomendado – se houver)

# Inicie PostgreSQL e Redis
```bash
docker run -d --name postgres -e POSTGRES_PASSWORD=15940898 -p 5432:5432 postgres
docker run -d --name redis -p 6379:6379 redis
```

# Clone o repositório
```bash
git clone https://github.com/costtinha/spring_batch_estudos.git
cd spring-batch-estudos
```
# Configure environment variables (opcional, defaults em application.yml)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=15940898

# Build e execução
./mvnw spring-boot:run

### Sem Docker
# Clone o repositório
```bash
git clone https://github.com/costtinha/spring_batch_estudos.git
cd spring-batch-estudos
```
# Configure PostgreSQL e Redis localmente
# (ajuste credenciais em src/main/resources/application.yml)

# Build e execução
./mvnw spring-boot:run



### Mutations
```bash
type Mutation {
  createOffice(input: CreateOfficeInput!): Office!
  deleteOffice(code: ID!): Office
  updateOffice(code: ID!, input: CreateOfficeInput!): Office!

  createOrderProduct(input: CreateOrderProductInput!): OrderProduct!
  deleteOrderProduct(input: OrderProductKeyInput!): OrderProduct!
  updateOrderProduct(orderId: ID!, productCode: ID!, input: UpdateOrderProduct!): OrderProduct
}
```


### Endpoints da API

| Método | Endpoint      | Descrição                      |
|--------|---------------|--------------------------------|
| `POST` | `/job/import` | Lança job de importação de CSV |
| `POST` | `/job/export` | Lança job de exportação de CSV |
| `GET`   | `/job/ping`    |   Verifica se a app está rodando|
Sem autenticação necessária (públicos).

### Exemplos de requisições

# Lançar job import:
```bash
curl -X POST http://localhost:8080/job/import
```
Resposta (200 OK)

```bash
Job iniciado com sucesso
```

# Lançar Export job
```bash
curl -X POST http://localhost:8080/job/export
```

Resposta (200 OK)

```bash
Job de exportação iniciado com sucesso
```

# Ping:
```bash
curl -X POST http://localhost:8080/job/ping
```
Resposta (200 OK)

```bash
Aplicação está rodando!
```
---

## 📁 Estrutura do Projeto

```
src/main/java/com/example/Spring/batch/Training/
├── cacheRepository/      # Repositórios Redis (ex: UserCacheRepository)
├── config/               # Configurações (BatchConfig, UserJobConfig, ParameterSettingListener, RepositoryConfig)
├── controller/           # JobLaucherController (lançamento de jobs via REST)
├── dtos/                 # UserCSV (DTO para leitura de CSV)
├── entity/               # User, UserCache (entidades JPA e Redis)
├── persistance/          # UserRepository (JPA)
└── SpringBatchTrainingApplication.java  # Classe principal (não fornecida, mas implícita)
resources/
├── application.yml       # Configurações de datasource, JPA, Redis, Batch
└── user_10k.csv          # Arquivo de dados a serem transformados
```


---

## 📚 Decisões e Aprendizados

Este projeto foi desenvolvido como estudo prático dos seguintes conceitos:

- Configuração de Spring Batch com PostgreSQL para metadados (JobRepository customizado).
- Particionamento para export: Divisão de ranges de IDs para paralelismo, com listener para configurar readers/writers por partition.
- Processamento de CSV: Uso de FlatFileItemReader/Writer com mapeamento para entidades.
- Cache Redis: Entidades duplicadas para cache (UserCache) com TTL para otimização.
- Inicialização de schema: DataSourceInitializer para rodar scripts SQL batch.
- Paralelismo: ThreadPoolTaskExecutor com shutdown graceful.
- Logging: SLF4J para rastrear steps, partitions e erros.
- Decisões: Foco em escalabilidade para grandes CSVs, evitando leitura total em memória; uso de chunks (ex: 5000 itens) para transações gerenciáveis.

---

## 📄 Licença

Este projeto é de uso educacional e está disponível sob a licença [MIT](LICENSE).

---

<p align="center">
  Desenvolvido por <a href="https://github.com/costtinha">Daniel Costa</a>
</p>
