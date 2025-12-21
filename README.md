# API Secundária - MVP do Sprint de Arquitetura de Software
Trabalho da Pós- graduacao em Engenharia de software da PUC Rio realizado por Mauro Sergio Lopes dos Santos Junior. 

Esse repositorio representa a API Secundária do MVP.

# Movies Provider Service

Serviço de microsserviços baseado em Scala que fornece informações de filmes através de APIs REST e GraphQL, com cache e integração com API externa.

## Visão Geral da Arquitetura

A aplicação segue uma arquitetura em camadas com clara separação de responsabilidades:

```
┌─────────────────┐    ┌─────────────────┐
│   HTTP REST     │    │    GraphQL      │
│   Porta 9090    │    │   Porta 8081    │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     │
            ┌────────▼────────┐
            │  Serviço de     │
            │  Cache Filmes   │
            └────────┬────────┘
                     │
          ┌──────────┼──────────┐
          │          │          │
    ┌─────▼─────┐   │   ┌──────▼──────┐
    │ Cache H2  │   │   │ Cliente OMDb│
    │ Database  │   │   │ API Externa │
    └───────────┘   │   └─────────────┘
                    │
            ┌───────▼───────┐
            │ BD Preferên.  │
            │   Usuários    │
            └───────────────┘
```

## Funcionalidades Principais

### 🎬 **Busca de Filmes**
- Busca filmes por título na API OMDb
- Cache automático no banco H2 para melhor performance
- Informações incluem: título, ano, sinopse, ID IMDb e gênero

### 🤖 **Sistema de Recomendação Inteligente**
- Analisa preferências do usuário (filmes avaliados com nota ≥ 4)
- Identifica gêneros favoritos automaticamente
- Sugere novos filmes baseados nos gêneros preferidos
- Nunca sugere filmes já avaliados pelo usuário

### 👤 **Gerenciamento de Preferências**
- Armazena avaliações de usuários (escala 1-5)
- Histórico completo de preferências por usuário
- Base para o sistema de recomendação

### 🔄 **Dupla Interface de API**
- **REST API**: Endpoints tradicionais HTTP
- **GraphQL API**: Consultas flexíveis e eficientes

## Fluxo de Inicialização

### 1. Main.scala - Ponto de Entrada
```scala
Main extends App
```
- Cria `ActorSystem` compartilhado
- Inicializa banco H2 com `Database.init()`
- Inicia serviços HTTP e GraphQL simultaneamente
- Gerencia shutdown gracioso

### 2. Inicialização do Banco
```scala
Database.init()
```
- Cria conexão com banco H2 em memória
- Configura tabelas: `movies` (cache) e `user_preferences`
- Usa ScalikeJDBC para operações de banco

### 3. Inicialização dos Serviços
Ambos os serviços iniciam simultaneamente:
- **MovieService** na porta `localhost:9090`
- **GraphQLServer** na porta `localhost:8081`

## Detalhes dos Componentes

### Modelos de Domínio

#### Movie.scala
```scala
case class Movie(title: String, year: String, plot: String, imdbID: String, genre: String)
case class MovieResponse(title: String, year: String, plot: String, imdbID: String, genre: String)
```
- Entidade principal representando dados do filme
- Usada em todas as camadas da aplicação

#### UserPreference.scala
```scala
case class UserPreference(id: Long, userId: String, movieId: String, rating: Int)
```
- Armazena avaliações e preferências dos usuários
- Inclui encoders/decoders JSON Circe

### Camada de Dados

#### Database.scala
**Gerenciamento de Conexão:**
- Banco H2 em memória (`jdbc:h2:mem:test`)
- Pool de conexões via ScalikeJDBC

**Operações Principais:**
- `getMovieByTitle(title)` - Recupera filmes do cache
- `saveMovie(movie)` - Armazena filmes usando `MERGE INTO ... KEY(title)`
- `addPreference()` / `getUserPreferences()` - Gerencia preferências
- `getUserPreferredGenres()` - Identifica gêneros favoritos do usuário

**Tabelas:**
```sql
movies: id, title, movie_year, plot, imdb_id, genre
user_preferences: id, user_id, movie_id, rating
```

### Integração Externa

#### OMDbClient.scala
**Propósito:** Busca dados de filmes da API OMDb quando não estão no cache

**Fluxo:**
1. Constrói requisição HTTP para `http://www.omdbapi.com/`
2. Usa cliente STTP com `HttpURLConnectionBackend`
3. Analisa resposta JSON usando Circe
4. Mapeia `OMDbMovie` para domínio `Movie`

**Configuração:**
- Chave API da variável de ambiente `OMDB_API_KEY` (padrão: "244ae6ab")

### Lógica de Negócio

#### MovieCacheService.scala
**Estratégia Cache-First:**
```scala
def getMovie(title: String): Future[Option[Movie]]
```

**Fluxo:**
1. Verifica cache local H2 via `Database.getMovieByTitle()`
2. Se encontrado → retorna filme do cache
3. Se não encontrado → chama `OMDbClient.getMovie()`
4. Se API externa retorna dados → salva no cache e retorna
5. Se nenhum dado encontrado → retorna `None`

#### MovieSuggestionService.scala
**Sistema de Recomendação por Gênero:**
```scala
def getSuggestionForUser(userId: String): Future[Option[Movie]]
```

**Algoritmo Inteligente:**
1. Obtém gêneros preferidos do usuário (filmes com nota ≥ 4)
2. Obtém lista de filmes já avaliados pelo usuário
3. Busca novo filme do mesmo gênero preferido via OMDb
4. Garante que nunca sugere filme já avaliado
5. Fallback: filme popular se usuário não tem preferências

**Gêneros Suportados:**
- Sci-Fi, Action, Drama, Comedy, Horror, Thriller
- Cada gênero tem lista curada de filmes populares

### Camada de API

#### API REST - MovieService.scala

**Endpoints:**
- `GET /movie/suggestion?userId={userId}` - Obter sugestão de filme para usuário
- `GET /movie/{title}` - Obter filme por título
- `POST /preferences` - Salvar preferência do usuário
- `GET /preferences/{userId}` - Obter preferências do usuário

**Características:**
- Servidor Akka HTTP com suporte JSON Circe
- Tratamento de erros com respostas de fallback
- Processamento assíncrono com Futures

#### API GraphQL - GraphQLServer.scala + MovieSchema.scala

**Definição do Schema:**
```graphql
type Movie {
  title: String
  year: String
  plot: String
  imdbID: String
  genre: String
}

type Query {
  movie(title: String!): Movie
  suggestion(userId: String!): Movie
}
```

**Componentes:**
- `MovieSchema` - Define schema GraphQL usando Sangria
- `MovieResolver` - Resolve consultas GraphQL para chamadas de serviço
- `GraphQLServer` - Servidor HTTP tratando requisições GraphQL

**Características:**
- POST `/graphql` - Executar consultas GraphQL
- GET `/graphql` - Informações do endpoint GraphQL
- Integração Sangria-Circe para marshalling JSON

## Exemplos de Fluxo de Requisição

### Requisição REST API
```
GET /movie/Inception
    ↓
MovieService.getMovie()
    ↓
MovieCacheService.getMovie()
    ↓
Database.getMovieByTitle() → Cache Hit/Miss
    ↓ (se miss)
OMDbClient.getMovie() → API Externa
    ↓
Database.saveMovie() → Atualização Cache
    ↓
Retorna MovieResponse
```

### Requisição GraphQL
```
POST /graphql
Body: {"query": "{ movie(title: \"Inception\") { title year } }"}
    ↓
GraphQLServer.executeGraphQL()
    ↓
Sangria Query Parser
    ↓
MovieResolver.getMovie()
    ↓
MovieCacheService.getMovie() → (mesmo fluxo REST)
    ↓
Retorna Resposta JSON
```

### Requisição de Sugestão de Filme
```
GET /movie/suggestion?userId=user123
    ↓
MovieService.getSuggestion()
    ↓
MovieSuggestionService.getSuggestionForUser()
    ↓
Database.getUserPreferredGenres() → Obtém gêneros favoritos
    ↓
Busca filme do mesmo gênero via OMDb → Novo filme
    ↓
Retorna MovieResponse
```

## Configuração

### Dependências (build.sbt)
- **Akka HTTP** - Framework servidor web
- **Sangria** - Implementação GraphQL
- **Circe** - Processamento JSON
- **ScalikeJDBC** - Acesso ao banco de dados
- **STTP** - Cliente HTTP
- **H2** - Banco de dados em memória

### Configuração da Aplicação (application.conf)
```hocon
akka {
  loglevel = "INFO"
  actor.provider = "akka.actor.LocalActorRefProvider"
  http.server {
    idle-timeout = 60s
    request-timeout = 40s
  }
}
```

## Executando a Aplicação

### Opção 1: Execução Local
```bash
sbt "runMain Main"
```

### Opção 2: Execução com Docker
```bash
# Construir a imagem Docker
docker build -t movies-provider-service .

# Executar o container
docker run -p 9090:9090 -p 8081:8081 movies-provider-service
```

### Opção 3: Execução com Docker Compose (Recomendado)
```bash
# No diretório raiz do projeto (arq-soft)
cd ..
docker-compose up --build

# Para executar em background
docker-compose up -d --build

# Para parar os serviços
docker-compose down
```

**Serviços Disponíveis:**
- API REST: http://localhost:9090/movie/{title}
- Sugestões de Filmes: http://localhost:9090/movie/suggestion?userId={userId}
- API GraphQL: http://localhost:8081/graphql

**Exemplos de Uso:**

**REST API:**
```bash
# Buscar filme
curl "http://localhost:9090/movie/Inception"

# Adicionar preferência
curl -X POST "http://localhost:9090/preferences" \
  -H "Content-Type: application/json" \
  -d '{"id":0,"userId":"user123","movieId":"tt1375666","rating":5}'

# Obter sugestão
curl "http://localhost:9090/movie/suggestion?userId=user123"
```

**GraphQL:**
```graphql
# Consulta de filme
{ movie(title: "Inception") { title year plot imdbID genre } }

# Consulta de sugestão
{ suggestion(userId: "user123") { title year plot imdbID genre } }
```

## Tratamento de Erros

- Falhas de conexão com banco → Falha na inicialização do serviço
- Falhas da API externa → Retorna dados do cache ou "Filme não encontrado"
- Consultas GraphQL inválidas → Retorna resposta de erro com detalhes
- Conflitos de porta → Aplicação falha ao iniciar com mensagem de erro clara

## Tecnologias Utilizadas

- **Scala 2.13** - Linguagem de programação
- **Akka HTTP** - Framework web reativo
- **Sangria** - Biblioteca GraphQL para Scala
- **H2 Database** - Banco de dados em memória
- **ScalikeJDBC** - Biblioteca de acesso a dados
- **Circe** - Biblioteca JSON para Scala
- **STTP** - Cliente HTTP para Scala
