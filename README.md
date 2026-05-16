# VidaLong Flix — Backend

API REST da plataforma de streaming focada em saúde e longevidade.  
Responsável por autenticação, gerenciamento de conteúdo, armazenamento de mídia e observabilidade em produção.

---

## Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 — records, sealed classes, pattern matching |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Actuator, Validation) |
| Banco (produção) | PostgreSQL com SSL obrigatório |
| Banco (desenvolvimento) | H2 in-memory |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Segurança | Spring Security 6 + JWT + BCrypt |
| Storage | AWS S3 (prod) / disco local (dev) |
| Deploy | AWS Elastic Beanstalk + RDS + CloudFront |
| CI/CD | GitHub Actions |

---

## Banco de Dados

- PostgreSQL em produção, H2 para desenvolvimento e testes
- Migrations versionadas com Flyway
- UUIDs como chaves primárias
- Relacionamentos JPA: `@ManyToOne`, `@OneToMany`, `@ManyToMany`
- Paginação com `Pageable`

---

## Segurança

- Spring Security 6 com filter chain stateless
- JWT — geração, validação e filtro customizado
- RBAC com perfis `ROLE_USER` e `ROLE_ADMIN`
- BCrypt para hash de senhas
- CORS restrito por variável de ambiente
- Rate limiting no endpoint de login via Bucket4j
- Prevenção de SQL Injection por Prepared Statements / JPA
- Secrets exclusivamente via variáveis de ambiente — sem hardcode
- HTTPS forçado via CloudFront

---

## API REST

- CRUD completo: vídeos, menus, categorias, comentários, usuários
- Endpoints segmentados por nível de acesso: público, autenticado, admin
- Upload multipart para capas de vídeos e receitas
- Seed automático de dados (`DataInitializer`) no perfil dev
- Recursos estáticos servidos pelo Spring em `/assets/**`

---

## Armazenamento de Arquivos

`MediaStorageService` com modo duplo conforme o perfil ativo:

| Perfil | Backend de storage |
|---|---|
| `prod` | AWS S3 com SDK Java |
| `dev` | Disco local |

Assets organizados por tipo: `covers/`, `media/`

---

## Observabilidade

Pipeline OpenTelemetry integrado ao Grafana Cloud:

| Sinal | Destino |
|---|---|
| Métricas | OTLP → Mimir |
| Traces distribuídos | OTLP → Tempo |
| Logs estruturados | Loki |

- Spring Boot Actuator exposto em `/health`, `/metrics`, `/info`
- `SmartInitializingSingleton` para instalar o OpenTelemetry Logback Appender
- 3 dashboards importados: JVM Backend, Golden Signals, SLI Disponibilidade
- 2 SLOs configurados com burn rate alerts

---

## Infraestrutura & Cloud

- AWS Elastic Beanstalk — deploy automatizado via CI
- AWS RDS PostgreSQL — banco gerenciado
- AWS S3 + CloudFront — storage de mídia e CDN
- Docker com imagem multi-stage (builder + runtime slim)
- `Dockerrun.aws.json` gerado pelo pipeline para deploy no EB

---

## CI/CD

Pipeline no GitHub Actions com as seguintes fases em sequência:

```
build → test → Docker build → push Docker Hub → deploy Elastic Beanstalk
```

Variáveis de ambiente gerenciadas via EB Environment Properties.

---

## Arquitetura

```
Controller → Service → Repository → Entity
```

- DTOs de request/response separados das entidades JPA
- Perfis Spring distintos por ambiente (`dev`, `prod`)
- Configuração 100% externalizada — sem segredos no código
- WhatsApp webhook em `/whatsapp/webhook`
- `ContentNotificationsService` para notificações de novo conteúdo

---

## Roadmap — Planejado / Não Implementado

### Segurança no CI/CD

- [ ] Gitleaks — detecção de secrets em commits
- [ ] SonarCloud SAST — análise estática de código Java
- [ ] OWASP Dependency Check — CVEs em dependências
- [ ] Docker Scout — CVEs na imagem de produção

### Hardening

- [ ] HTTP Security Headers: CSP, HSTS, X-Frame-Options, X-Content-Type
- [ ] Actuator restrito a `ROLE_ADMIN`
- [ ] CORS: substituir `allowedHeaders("*")` por lista explícita
- [ ] Rate limiting nos demais endpoints (além do login)
- [ ] Validação completa de upload: tipo MIME, extensão, tamanho, path traversal
- [ ] BOLA/IDOR: validação de ownership por recurso
- [ ] RestTemplate com timeout configurado
- [ ] SSRF Protection Service

### Escalabilidade

- [ ] Cache com Redis
- [ ] Filas assíncronas com AWS SQS
- [ ] `@Max` em parâmetros de paginação

### Segurança Avançada

- [ ] AWS Secrets Manager com rotação automática
- [ ] Refresh Token com rotação
- [ ] JWT migrado para HttpOnly cookies
- [ ] DAST com OWASP ZAP no pipeline
- [ ] AWS WAF na frente do CloudFront
- [ ] MFA para contas admin

### Maturidade

- [ ] SBOM (Software Bill of Materials)
- [ ] AWS Security Hub
- [ ] Pentest externo
- [ ] OWASP SAMM assessment

---

## Licenca

Distribuido sob licenca privada. Consulte o time responsavel para mais informacoes.
