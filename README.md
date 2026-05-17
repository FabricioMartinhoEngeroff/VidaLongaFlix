# VidaLongaFlix — Backend

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![AWS](https://img.shields.io/badge/AWS-EB_·_S3_·_RDS-orange?logo=amazonaws)
![CI/CD](https://img.shields.io/github/actions/workflow/status/FabricioMartinhoEngeroff/VidaLongaFlix/ci.yml?label=CI%2FCD&logo=githubactions)
![License](https://img.shields.io/badge/license-Private-red)

API REST da **VidaLongaFlix**, plataforma de streaming focada em saúde e longevidade.
Usuários assistem a vídeos de receitas saudáveis, exploram cardápios nutricionais e recebem notificações de novos conteúdos via WhatsApp.
Construída com **Java 17 + Spring Boot 3.5**, implantada na **AWS** com observabilidade completa via **Grafana Cloud**.

---

## Índice

- [Funcionalidades](#funcionalidades)
- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e execução local](#instalação-e-execução-local)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints principais](#endpoints-principais)
- [Arquitetura](#arquitetura)
- [CI/CD e infraestrutura](#cicd-e-infraestrutura)
- [Roadmap](#roadmap)
- [Contribuição](#contribuição)
- [Licença](#licença)

---

## Funcionalidades

- Cadastro e autenticação de usuários com JWT
- Controle de acesso por perfil: `ROLE_USER` e `ROLE_ADMIN`
- Catálogo de vídeos e cardápios organizados por categorias
- Upload de capas para S3 e streaming de vídeos
- Comentários em vídeos
- Notificações de novo conteúdo via WhatsApp webhook
- Observabilidade em produção: métricas, traces e logs no Grafana Cloud

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA, Actuator, Validation) |
| Banco (produção) | PostgreSQL 16 com SSL |
| Banco (desenvolvimento) | H2 in-memory |
| Segurança | Spring Security 6 + JWT + BCrypt |
| Storage | AWS S3 (prod) / disco local (dev) |
| Observabilidade | OpenTelemetry → Grafana Cloud (Mimir · Tempo · Loki) |
| Deploy | AWS Elastic Beanstalk + RDS + CloudFront |
| CI/CD | GitHub Actions |
| Containers | Docker (imagem multi-stage) |

---

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker (opcional, para rodar com observabilidade local)
- AWS CLI configurado (apenas para operações com S3)

---

## Instalação e execução local

```bash
# 1. Clone o repositório
git clone https://github.com/FabricioMartinhoEngeroff/VidaLongaFlix.git
cd VidaLongaFlix

# 2. Execute com o perfil local (H2 + storage em disco)
./mvnw spring-boot:run

# A API estará disponível em:
# http://localhost:8090/api
```

O perfil `local` já cria o banco H2 em memória e semeia automaticamente
36 vídeos, cardápios e categorias via `DataInitializer`.

### Rodar com Docker

```bash
docker build -t vidalongaflix-backend .
docker run -p 8090:8090 \
  -e SPRING_PROFILES_ACTIVE=local \
  vidalongaflix-backend
```

---

## Variáveis de ambiente

| Variável | Descrição | Obrigatório em prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` ou `prod` | Sim |
| `DB_URL` | JDBC URL do PostgreSQL | Sim |
| `DB_USERNAME` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `JWT_SECRET` | Chave secreta para assinar tokens | Sim |
| `ADMIN_EMAIL` | E-mail do admin criado no seed | Sim |
| `ADMIN_PASSWORD` | Senha do admin criado no seed | Sim |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas no CORS | Sim |
| `AWS_S3_BUCKET` | Nome do bucket S3 | Prod |
| `AWS_REGION` | Região AWS | Prod |

> Nunca commite valores reais. Em produção use as **Environment Properties** do Elastic Beanstalk.

---

## Endpoints principais

### Autenticação

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/register` | Cadastro de usuário | Público |
| `POST` | `/api/auth/login` | Login — retorna JWT | Público |

**Exemplo de login:**
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@vidalongaflix.com.br", "password": "Admin@123456"}'
```
Resposta:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

Use o token nas próximas requisições:
```
Authorization: Bearer <token>
```

---

### Vídeos

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/videos` | Lista todos os vídeos | Público |
| `GET` | `/api/videos/{id}` | Busca vídeo por ID | Público |
| `POST` | `/api/videos` | Cria vídeo | Admin |
| `PUT` | `/api/videos/{id}` | Atualiza vídeo | Admin |
| `DELETE` | `/api/videos/{id}` | Remove vídeo | Admin |

---

### Cardápios

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/menus` | Lista todos os cardápios | Público |
| `GET` | `/api/menus/{id}` | Busca cardápio por ID | Público |
| `POST` | `/api/menus` | Cria cardápio | Admin |

---

### Categorias

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/categories` | Lista categorias | Público |
| `POST` | `/api/categories` | Cria categoria | Admin |

---

### Comentários

| Método | Endpoint | Descrição | Acesso |
|---|---|---|---|
| `GET` | `/api/comments/video/{id}` | Comentários de um vídeo | Público |
| `POST` | `/api/comments` | Adiciona comentário | Autenticado |
| `DELETE` | `/api/comments/{id}` | Remove comentário | Admin |

---

## Arquitetura

```
Controller → Service → Repository → Entity (JPA)
```

- DTOs de request/response separados das entidades
- Perfis Spring distintos por ambiente: `local`, `prod`
- Secrets 100% externalizados via variáveis de ambiente
- Capas dos vídeos servidas via AWS S3 (URLs públicas)

```
src/
├── domain/          # Entidades e enums
├── application/     # DTOs e casos de uso
├── infra/
│   ├── config/      # DataInitializer, CORS, Observabilidade
│   ├── security/    # JWT filter, SecurityConfig
│   └── storage/     # MediaStorageService (S3 / local)
└── controllers/     # Endpoints REST
```

---

## CI/CD e infraestrutura

```
push → GitHub Actions → build → test → Docker build → push Docker Hub → deploy Elastic Beanstalk
```

- **Elastic Beanstalk**: deploy automático via `Dockerrun.aws.json`
- **RDS PostgreSQL**: banco gerenciado com SSL
- **S3 + CloudFront**: storage de capas e CDN
- **Grafana Cloud**: dashboards JVM, Golden Signals e SLI de disponibilidade

---

## Roadmap

### Segurança CI/CD
- [ ] Gitleaks — detecção de secrets em commits
- [ ] SonarCloud SAST — análise estática Java
- [ ] OWASP Dependency Check — CVEs em dependências

### Hardening
- [ ] HTTP Security Headers (CSP, HSTS, X-Frame-Options)
- [ ] Actuator restrito a `ROLE_ADMIN`
- [ ] Rate limiting nos demais endpoints
- [ ] SSRF Protection Service

### Escalabilidade
- [ ] Cache com Redis
- [ ] Filas assíncronas com AWS SQS
- [ ] Paginação no catálogo

### Segurança Avançada
- [ ] AWS Secrets Manager com rotação automática
- [ ] Refresh Token com rotação
- [ ] DAST com OWASP ZAP no pipeline

---

## Contribuição

1. Crie uma branch a partir de `main`:
   ```bash
   git checkout -b feat/nome-da-feature
   ```
2. Faça suas alterações e commite seguindo o padrão [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   feat: descrição da funcionalidade
   fix: descrição do bug corrigido
   docs: atualização de documentação
   ```
3. Abra um Pull Request para `main` com descrição clara do que foi feito.

> Nunca commite direto na `main`.

---

## Licença

Distribuído sob licença privada.
Para uso, reprodução ou contribuição, entre em contato com o responsável pelo projeto.
