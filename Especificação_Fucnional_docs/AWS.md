# AWS — Infraestrutura do VidaLongaFlix

Documento de referência da arquitetura AWS do VidaLongaFlix.
Cobre conceitos, decisões de implementação e recursos provisionados.

---

## Visão Geral da Arquitetura

```
USUÁRIO (Brasil)
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│                  CLOUDFRONT — Borda AWS (Edge)               │
│                                                              │
│  d2efnb2x9r22my  →  Frontend Angular (vidalongaflix.com)    │
│  d4faq9yal6gv7   →  API Backend    (api.vidalongaflix.com)  │
│  d27rhgr1cr7axs  →  Mídia / Capas (imagens dos vídeos)      │
└──────────┬───────────────────┬───────────────────┬───────────┘
           │                   │                   │
           ▼                   ▼                   ▼
     ┌───────────┐    ┌─────────────────┐   ┌───────────────┐
     │    S3     │    │ Elastic         │   │      S3       │
     │ Frontend  │    │ Beanstalk       │   │    Mídia      │
     │(Angular   │    │ (Docker +       │   │(covers/       │
     │ HTML/JS)  │    │  Spring Boot)   │   │ videos/)      │
     └───────────┘    └────────┬────────┘   └───────────────┘
                               │ JDBC
                               ▼
                       ┌──────────────┐
                       │     RDS      │
                       │  PostgreSQL  │
                       │ (subnet      │
                       │  privada)    │
                       └──────────────┘
```

---

## 1. CloudFront — CDN (Content Delivery Network)

### Conceito

CloudFront é a rede de distribuição de conteúdo da AWS. Possui mais de 400 pontos de presença (edge locations) ao redor do mundo, incluindo São Paulo. Quando um usuário faz uma requisição:

- **1ª vez:** CloudFront busca o conteúdo na origem (S3 ou EB), armazena em cache no edge mais próximo e entrega.
- **Próximas vezes:** entrega direto do cache no edge — muito mais rápido, sem ir ao servidor de origem.

**Por que usar:** sem CloudFront, um usuário no Brasil busca imagens e páginas direto de servidores em Ohio (us-east-2), com latência de ~120–180ms. Com CloudFront São Paulo, a latência cai para ~20–40ms.

### Distribuições provisionadas

| Distribuição | Domínio | Origem | Finalidade |
|---|---|---|---|
| `E2VSSOH0WMXIYX` | `d2efnb2x9r22my.cloudfront.net` | S3 `vida-longa-flix-web-prod` | Frontend Angular |
| `E1FYA7YPIWQNTR` | `d4faq9yal6gv7.cloudfront.net` | Elastic Beanstalk | API (`api.vidalongaflix.com`) |
| `E3VXMJTM582YU8` | `d27rhgr1cr7axs.cloudfront.net` | S3 `vidalongaflix-media` | Capas dos vídeos e menus |

### Distribuição 1 — Frontend

- **Origem:** S3 bucket `vida-longa-flix-web-prod`
- **Domínio customizado:** `vidalongaflix.com` e `www.vidalongaflix.com`
- **Protocolo:** HTTPS forçado, HTTP redireciona para HTTPS
- **Uso:** entrega o app Angular (HTML, JS, CSS, assets) ao browser do usuário

### Distribuição 2 — API Backend

- **Origem:** Elastic Beanstalk (`Vidalongaflix-backend-env.eba-gteu4qmf.us-east-2.elasticbeanstalk.com`)
- **Domínio customizado:** `api.vidalongaflix.com`
- **Uso:** proxy reverso para a API Spring Boot — HTTPS externo, tráfego chega no EB via porta 443
- **Segurança:** o Security Group da EC2 só aceita conexões vindas do CloudFront (Prefix List `pl-b6a144df`)

### Distribuição 3 — Mídia (capas dos vídeos)

- **Origem:** S3 bucket `vidalongaflix-media` (privado)
- **ARN:** `arn:aws:cloudfront::359598898309:distribution/E3VXMJTM582YU8`
- **Acesso ao S3:** Origin Access Control (OAC) — o CloudFront autentica na AWS para acessar o bucket privado
- **WAF:** desabilitado (conteúdo estático público não exige proteção de WAF)
- **Cache policy:** `CachingOptimized` — imagens cacheadas por longo período
- **Price class:** North America and Europe (suficiente para usuários brasileiros com latência aceitável)
- **Implementado em:** 30/05/2026

---

## 2. S3 — Armazenamento de Objetos

### Conceito

S3 (Simple Storage Service) é o serviço de armazenamento de arquivos da AWS. Funciona como um HD na nuvem — você armazena qualquer arquivo (HTML, imagem, vídeo, CSV) e acessa via URL. Cada arquivo é um **objeto**, organizado em **buckets**.

**Buckets provisionados:**

### Bucket 1 — Frontend: `vida-longa-flix-web-prod`

- **Conteúdo:** build do Angular (`index.html`, `main.js`, `styles.css`, assets)
- **Acesso:** privado — só o CloudFront acessa via OAC
- **Deploy:** GitHub Actions faz `aws s3 sync` a cada push na main

### Bucket 2 — Mídia: `vidalongaflix-media`

- **Região:** `us-east-2`
- **Conteúdo:**
  - `covers/` — imagens de capa dos vídeos e menus (JPEG/PNG, 40–100KB cada)
  - `videos/` — arquivos de vídeo (MP4)
- **Acesso:** privado — Block Public Access ativado
- **Política de acesso:**

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowCloudFrontServicePrincipal",
            "Effect": "Allow",
            "Principal": { "Service": "cloudfront.amazonaws.com" },
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::vidalongaflix-media/*",
            "Condition": {
                "ArnLike": {
                    "AWS:SourceArn": "arn:aws:cloudfront::359598898309:distribution/E3VXMJTM582YU8"
                }
            }
        }
    ]
}
```

- **Upload via app:** `MediaStorageService.java` faz upload direto para S3 via AWS SDK e retorna URL do CloudFront quando `CDN_BASE_URL` está configurado
- **URL retornada:** `https://d27rhgr1cr7axs.cloudfront.net/covers/{filename}`

---

## 3. Elastic Beanstalk — Plataforma de Aplicação

### Conceito

Elastic Beanstalk (EB) é um serviço PaaS (Platform as a Service) da AWS. Você entrega o código (ou uma imagem Docker) e o Beanstalk gerencia automaticamente:

- Criação e configuração da instância EC2
- Health check contínuo da aplicação
- Rolling deploy — atualiza sem derrubar o serviço
- Rollback automático se o deploy falhar
- Reinício automático se a aplicação cair
- Escalabilidade (manual ou automática)

### Ambiente provisionado

| Campo | Valor |
|---|---|
| **Nome** | `Vidalongaflix-backend-env` |
| **Região** | `us-east-2` (Ohio) |
| **Instância EC2** | `t3.small` |
| **Plataforma** | Docker |
| **URL EB** | `Vidalongaflix-backend-env.eba-gteu4qmf.us-east-2.elasticbeanstalk.com` |
| **URL pública** | `https://api.vidalongaflix.com` (via CloudFront) |
| **Porta da app** | `8090` com context-path `/api` |

### Como o deploy funciona

O CI/CD (GitHub Actions) gera um arquivo `Dockerrun.aws.json` com a imagem Docker da versão exata do commit:

```json
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "fabricioengeroff/vidalongaflix:sha-abc1234",
    "Update": "true"
  },
  "Ports": [{ "ContainerPort": 8090 }]
}
```

O EB faz pull da imagem do Docker Hub e reinicia o container. Cada versão é identificada pelo SHA do commit — rastreável e reversível.

### Variáveis de ambiente configuradas no EB

| Variável | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (H2) ou `prod` (PostgreSQL) |
| `DB_URL` | JDBC URL do RDS PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo para assinar tokens JWT |
| `ADMIN_EMAIL` | Email do admin inicial |
| `ADMIN_PASSWORD` | Senha do admin inicial |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (domínios do frontend) |
| `AWS_S3_BUCKET` | `vidalongaflix-media` |
| `AWS_REGION` | `us-east-2` |
| `CDN_BASE_URL` | `https://d27rhgr1cr7axs.cloudfront.net` |
| `OTLP_ENDPOINT` | Endpoint Grafana Cloud (métricas/traces/logs) |
| `OTLP_AUTH_HEADER` | Credencial base64 para o Grafana Cloud |
| `WHATSAPP_ENABLED` | `true` em produção |

---

## 4. RDS — Banco de Dados Gerenciado

### Conceito

RDS (Relational Database Service) é o serviço de banco de dados gerenciado da AWS. A AWS cuida da máquina, backups automáticos, atualizações de segurança e alta disponibilidade — você só administra os dados.

### Instância provisionada

| Campo | Valor |
|---|---|
| **Engine** | PostgreSQL |
| **Host** | `vidalongaflix-prod.c3kuimg84np3.us-east-2.rds.amazonaws.com` |
| **Porta** | `5432` |
| **Database** | `vidalongaflix` |
| **Subnet** | Privada — sem acesso externo à internet |
| **SSL** | Obrigatório (`sslmode=require`) |
| **Conexão** | Somente interna à VPC — só o EB consegue se conectar |

```
JDBC URL:
jdbc:postgresql://vidalongaflix-prod.c3kuimg84np3.us-east-2.rds.amazonaws.com:5432/vidalongaflix?sslmode=require
```

**Em desenvolvimento:** substituído por H2 em memória (sem custo, sem configuração).

---

## 5. VPC e Segurança de Rede

### Conceito

VPC (Virtual Private Cloud) é uma rede privada isolada dentro da AWS. Dentro dela, **subnets** separam o que é público (acessível da internet) do que é privado. **Security Groups** funcionam como firewalls — controlam quais IPs e portas podem se comunicar com cada recurso.

### Topologia de rede

```
Internet
    │
    ▼
CloudFront (borda — IP público AWS)
    │  HTTPS apenas
    ▼
Application Load Balancer (subnet pública)
    │  porta 443 → 8090
    ▼
EC2 / Elastic Beanstalk (subnet pública, IP privado)
    │  JDBC interno (porta 5432)
    ▼
RDS PostgreSQL (subnet privada — sem acesso externo)
```

### Security Group da EC2

| Porta | Origem | Motivo |
|---|---|---|
| `22` (SSH) | IP específico (`128.201.150.233/32`) | Acesso restrito ao administrador |
| `80` (HTTP) | Fechado | CloudFront redireciona HTTP→HTTPS antes de chegar na EC2 |
| `443` (HTTPS) | CloudFront Prefix List (`pl-b6a144df`) | Só o CloudFront acessa o servidor |

A porta 443 usa a **Managed Prefix List** da AWS (`com.amazonaws.global.cloudfront.origin-facing`), que se atualiza automaticamente quando a AWS adiciona novos IPs ao CloudFront. Isso garante que apenas o CloudFront consiga alcançar o backend — qualquer acesso direto ao IP da EC2 é bloqueado.

---

## 6. IAM e OIDC — Autenticação do CI/CD

### Conceito

**Problema:** o CI/CD precisa de credenciais AWS para fazer deploy. A forma antiga era salvar `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` como segredos no GitHub — se vazassem, o acesso à AWS ficaria comprometido indefinidamente.

**Solução — OIDC:** o GitHub Actions solicita um token temporário à AWS em tempo real, sem credencial permanente armazenada. O token expira em 15 minutos.

```
GitHub Actions
    │ solicita token (OIDC)
    ▼
AWS verifica:
  - repositório correto? ✓
  - environment correto? ✓
    │ emite token temporário (15min)
    ▼
Deploy no Elastic Beanstalk
    │ token expira automaticamente
    ▼
Fim — nenhuma credencial permanente
```

### Recursos IAM provisionados

| Recurso | Valor |
|---|---|
| **OIDC Provider** | `token.actions.githubusercontent.com` |
| **IAM Role** | `github-actions-vidalongaflix` |
| **ARN da Role** | `arn:aws:iam::359598898309:role/github-actions-vidalongaflix` |
| **Policy** | `AdministratorAccess-AWSElasticBeanstalk` |
| **Condição** | `repo:FabricioMartinhoEngeroff/VidaLongaFlix:environment:production` |

Nenhuma credencial AWS permanente está armazenada no GitHub.

---

## 7. Fluxo Completo de uma Requisição

### Usuário acessa o site

```
1. Browser → CloudFront d2efnb2x9r22my
           → S3 vida-longa-flix-web-prod
           → entrega o Angular (HTML/JS/CSS)

2. Angular carrega no browser
           → POST api.vidalongaflix.com/api/auth/login
           → CloudFront d4faq9yal6gv7
           → Elastic Beanstalk (Spring Boot)
           → RDS PostgreSQL (valida credenciais)
           → retorna JWT

3. Angular recebe JWT
           → GET api.vidalongaflix.com/api/videos
           → Spring Boot busca vídeos no RDS
           → retorna lista com URLs das capas (CloudFront d27rhgr1cr7axs)

4. Angular exibe cards
           → imagens: GET d27rhgr1cr7axs.cloudfront.net/covers/nome.jpg
           → CloudFront verifica cache local (São Paulo)
           → se não tiver: busca no S3 vidalongaflix-media e cacheia
           → entrega imagem ao browser
```

### Admin faz upload de nova capa

```
1. Admin → POST api.vidalongaflix.com/api/admin/videos (multipart)
2. Spring Boot → MediaStorageService.store(file, "covers")
3. MediaStorageService → S3Client.putObject → s3://vidalongaflix-media/covers/uuid.jpg
4. Retorna URL: https://d27rhgr1cr7axs.cloudfront.net/covers/uuid.jpg
5. URL salva no banco de dados
```

---

## 8. Pipeline CI/CD — GitHub Actions

```
Push na main ou feat/*
         │
         ▼
  Rodar testes (JUnit 5 + Mockito)
         │  se falhar → pipeline para
         ▼
  Build da imagem Docker
         │
         ▼
  Push para Docker Hub
  (tag = SHA do commit)
         │
         ▼
  Deploy no Elastic Beanstalk
  (só na main, com aprovação manual)
         │
         ▼
  Aguarda health check (120s)
         │
         ▼
  Deploy concluído ✅
```

**Arquivo:** `.github/workflows/ci.yml`
**Autenticação AWS:** OIDC (token temporário 15min)
**Rollback:** Console AWS → Elastic Beanstalk → Application Versions → seleciona versão anterior → Deploy

---

## 9. Resumo dos Recursos AWS

| Serviço | Recurso | Finalidade |
|---|---|---|
| **CloudFront** | `d2efnb2x9r22my` | Frontend Angular |
| **CloudFront** | `d4faq9yal6gv7` | API Backend |
| **CloudFront** | `d27rhgr1cr7axs` | Capas dos vídeos/menus |
| **S3** | `vida-longa-flix-web-prod` | Arquivos do Angular |
| **S3** | `vidalongaflix-media` | Imagens e vídeos (privado, OAC) |
| **Elastic Beanstalk** | `Vidalongaflix-backend-env` | Spring Boot (Docker) |
| **EC2** | `t3.small` | Instância gerenciada pelo EB |
| **RDS** | `vidalongaflix-prod` | PostgreSQL (subnet privada) |
| **IAM Role** | `github-actions-vidalongaflix` | CI/CD via OIDC |
| **Managed Prefix List** | `pl-b6a144df` | IPs do CloudFront no Security Group |

---

## 10. Histórico de Implementação

| Data | Ação |
|---|---|
| Mar/2026 | Infraestrutura inicial: EB, RDS, S3 frontend, CloudFront frontend e API |
| Abr/2026 | OIDC para CI/CD, Security Group restrito ao CloudFront, `MediaStorageService` com suporte a S3 + CDN |
| Mai/2026 | CloudFront para mídia (`vidalongaflix-media`), bucket privado com OAC, CSVs atualizados com URLs CloudFront |

---

*Última atualização: 30/05/2026*