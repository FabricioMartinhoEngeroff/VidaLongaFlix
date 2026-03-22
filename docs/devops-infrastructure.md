# DevOps e Infraestrutura — VidaLongaFlix

## Contexto

Este documento registra os conceitos de DevOps, infraestrutura e segurança aplicados no VidaLongaFlix em produção na AWS. O objetivo é documentar as decisões de implementação, o raciocínio por trás de cada escolha e servir como referência futura.

---

## Infraestrutura na AWS

### Máquina Virtual — Elastic Beanstalk

**Conceito:** uma máquina virtual é um computador simulado rodando dentro de um servidor físico. Na AWS, isso é feito pelo EC2 (Elastic Compute Cloud).

**Como está implementado:** o sistema usa Elastic Beanstalk, que gerencia a EC2 automaticamente. Em vez de criar e configurar a máquina manualmente, o Beanstalk cuida de:

- Criação e configuração da instância EC2
- Health check contínuo da aplicação
- Rolling deploy — atualiza sem derrubar o serviço
- Rollback automático se o deploy falhar
- Reinício automático se a aplicação cair

A aplicação roda em container Docker dentro da EC2, definido pelo `Dockerrun.aws.json`.

---

### Banco de Dados — RDS PostgreSQL

**Conceito:** banco de dados gerenciado — a AWS cuida da máquina, atualizações, backups e alta disponibilidade.

**Como está implementado:**

- RDS PostgreSQL em **subnet privada** — sem acesso externo à internet
- Credenciais via variáveis de ambiente (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`)
- Backup automático gerenciado pela AWS
- Conexão interna à VPC via JDBC — nunca exposta publicamente
- Em desenvolvimento: H2 em memória (sem custo, sem configuração)

---

### Rede — VPC, Subnets e Security Groups

**Conceito:** a rede na AWS é isolada dentro de uma VPC (Virtual Private Cloud). Dentro dela, subnets públicas e privadas separam o que pode ser acessado da internet do que não pode. Security Groups funcionam como ACLs — controlam quais IPs e portas podem se comunicar com cada recurso.

**Como está implementado:**

```
Internet
    │
    ▼
CloudFront (borda — IP público AWS)
    │  HTTPS apenas
    ▼
Application Load Balancer (subnet pública)
    │  porta 8090
    ▼
EC2 / Elastic Beanstalk (subnet pública, IP privado)
    │  JDBC interno
    ▼
RDS PostgreSQL (subnet privada — sem acesso externo)
```

**Security Group da EC2:**

| Porta | Origem | Motivo |
|---|---|---|
| 22 (SSH) | IP específico (`128.201.150.233/32`) | acesso restrito ao administrador |
| 80 (HTTP) | fechado | CloudFront faz redirect HTTP→HTTPS antes de chegar na EC2 |
| 443 (HTTPS) | CloudFront Prefix List (`pl-b6a144df`) | só o CloudFront chega no servidor |

A porta 443 usa a **Managed Prefix List** da AWS (`com.amazonaws.global.cloudfront.origin-facing`), que se atualiza automaticamente quando a AWS adiciona novos IPs ao CloudFront.

**Comandos para replicar a configuração:**

```bash
# Buscar o ID da prefix list do CloudFront na região us-east-2
aws ec2 describe-managed-prefix-lists \
  --region us-east-2 \
  --filters "Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing" \
  --query 'PrefixLists[0].PrefixListId' \
  --output text

# Adicionar HTTPS restrito ao CloudFront
echo '[{"IpProtocol":"tcp","FromPort":443,"ToPort":443,"PrefixListIds":[{"PrefixListId":"pl-b6a144df","Description":"CloudFront only"}]}]' > /tmp/sg443.json
aws ec2 authorize-security-group-ingress \
  --group-id <GROUP_ID> \
  --region us-east-2 \
  --ip-permissions file:///tmp/sg443.json
```

---

### CDN e Frontend — CloudFront + S3

**Conceito:** CDN (Content Delivery Network) distribui o conteúdo em servidores próximos ao usuário, reduzindo latência.

**Como está implementado:**

- Frontend Angular hospedado no S3
- CloudFront distribui o frontend globalmente
- CloudFront também atua como proxy reverso para a API (`/api/*`)
- HTTPS forçado em toda a comunicação
- Latência medida: ~28ms do Brasil para o CloudFront da AWS

---

## Pipeline de CI/CD

### Integração Contínua

**Conceito:** a cada push no repositório, verificações automáticas garantem que o código está correto antes de chegar em produção.

**Como está implementado — `.github/workflows/ci.yml`:**

```
Push na main ou feat/*
    │
    ▼
Rodar todos os testes (JUnit 5 + Mockito)
    │  se falhar → pipeline para, deploy não acontece
    ▼
Build da imagem Docker
    │
    ▼
Push da imagem para o Docker Hub (tag = SHA do commit)
    │
    ▼
Deploy no Elastic Beanstalk (só na main, com aprovação manual)
    │
    ▼
wait_for_environment_recovery: 120s
```

Cada versão deployada tem tag única com o SHA do commit — rastreável e reversível.

---

### Entrega Contínua — Deploy Automatizado

**Conceito:** o processo de colocar a aplicação no ar é totalmente automatizado, sem intervenção manual além da aprovação.

**Como está implementado:**

1. GitHub Actions gera o `Dockerrun.aws.json` com a imagem do commit exato
2. Envia o pacote para o Elastic Beanstalk via AWS CLI
3. Beanstalk faz pull da imagem do Docker Hub e reinicia o container
4. Aguarda 120s monitorando a saúde antes de concluir
5. Se o health check falhar, o deploy não conclui e a versão anterior continua rodando

---

### Rollback

**Conceito:** capacidade de voltar para uma versão anterior da aplicação quando algo dá errado.

**Como está implementado:**

O Beanstalk mantém histórico completo de todas as versões deployadas, identificadas pelo SHA do commit. Para fazer rollback:

```
Console AWS → Elastic Beanstalk → Application Versions
→ seleciona versão anterior → Deploy
```

Não é necessário nenhuma lógica extra no CI/CD — o Beanstalk gerencia isso nativamente.

---

### Segurança no CI/CD — OIDC

**Conceito:** autenticação baseada em identidade verificada em tempo real, sem credencial permanente armazenada.

**Problema anterior:** `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` salvos como GitHub Secrets — se vazarem, o acesso à AWS fica comprometido indefinidamente.

**Como está implementado:**

```
GitHub Actions → solicita token à AWS via OIDC
               → AWS verifica: repositório + environment corretos?
               → AWS emite token temporário (válido por 15 minutos)
               → deploy acontece com esse token
               → token expira automaticamente
```

**Recursos AWS:**
```
OIDC Provider: token.actions.githubusercontent.com
IAM Role: github-actions-vidalongaflix
  ARN: arn:aws:iam::359598898309:role/github-actions-vidalongaflix
  Policy: AdministratorAccess-AWSElasticBeanstalk
  Condição: repo:FabricioMartinhoEngeroff/VidaLongaFlix:environment:production
```

Nenhuma credencial AWS permanente armazenada no GitHub.

---

## Segurança da Aplicação

### Rate Limiting — proteção contra força bruta

**Conceito:** limitar tentativas de uma ação por IP em um intervalo de tempo. Equivalente a uma ACL no nível de aplicação — analisa origem (IP) e destino (endpoint) antes de permitir ou bloquear.

**Como está implementado — `LoginRateLimitFilter.java`:**

```
Regra aplicada no POST /auth/login:
  - Máximo de 5 tentativas por IP a cada 1 minuto
  - Se ultrapassar: HTTP 429 Too Many Requests
  - Após 1 minuto: balde de tokens recarrega
  - Localhost liberado (testes de carga locais)
  - X-Forwarded-For respeitado (IP real do usuário atrás do CloudFront/ALB)
```

Biblioteca: Bucket4j 8.10.1 — algoritmo de token bucket.
Cobertura: 6 testes unitários cobrindo todos os cenários.

---

### Variáveis de Ambiente — sem segredos no código

**Conceito:** credenciais e configurações sensíveis nunca ficam no código-fonte.

**Como está implementado:**

Todas as propriedades sensíveis em `application-prod.properties` usam `${VARIAVEL}`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
api.security.token.secret=${JWT_SECRET}
cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
```

Os valores ficam no Elastic Beanstalk → Configuration → Environment Properties. Nunca passam pelo GitHub Actions.

---

## Monitoramento

### Spring Actuator

**Conceito:** monitoramento interno da aplicação — visão de dentro do sistema em tempo real.

**Como está implementado:**

| Endpoint | Ambiente | O que mostra |
|---|---|---|
| `/api/actuator/health` | dev + prod | saúde: banco, disco, memória |
| `/api/actuator/metrics` | dev apenas | métricas de requisições por endpoint |
| `/api/actuator/info` | dev + prod | versão e nome da aplicação |

Em produção: `show-details=never` e apenas `health` e `info` expostos.

**Resultado observado após teste de carga:**
- 6.251 requisições processadas
- Tempo médio de processamento interno: 44ms
- Zero exceptions lançadas
- Zero erros internos

---

## Testes de Carga

### k6

**Conceito:** simular múltiplos usuários simultâneos para medir como o sistema responde sob carga real.

**Como está implementado — `load-tests/login-flow.js`:**

```
25 usuários virtuais simultâneos, 2 minutos:
  1. POST /auth/login     → autentica e obtém JWT
  2. GET /videos          → carrega o catálogo
  3. GET /favorites       → verifica favoritos do usuário
  4. GET /comments/video  → carrega comentários (se existir vídeo)
```

**Resultado validado:**

| Métrica | Resultado | Limite |
|---|---|---|
| p95 (todas as requisições) | 152ms | < 2000ms ✅ |
| p95 (login específico) | 292ms | < 1500ms ✅ |
| Taxa de erro | 0.00% | < 5% ✅ |
| Total de requisições | 1716 | — |

**Quando rodar:** manualmente antes de merges que alterem endpoints, queries ao banco ou configurações de pool de conexões.

---

## SLIs e SLOs — Metas de Confiabilidade

**Conceito:**
- **SLI** (Service Level Indicator) — métrica que mede o comportamento real do sistema
- **SLO** (Service Level Objective) — meta que o sistema deve atingir para essa métrica

SLO bom = baseado em dados reais do usuário + monitoramento contínuo + revisão periódica. Uma meta de 100% é irrealista — qualquer manutenção a viola. O objetivo é definir o mínimo aceitável que garante boa experiência.

**SLIs e SLOs definidos para o VidaLongaFlix:**

| SLI (o que medimos) | Valor medido | SLO (meta) | Status |
|---|---|---|---|
| p95 de latência — todas as requisições | 152ms | < 2000ms | ✅ |
| p95 de latência — login específico | 292ms | < 1500ms | ✅ |
| Taxa de erro geral | 0.00% | < 5% | ✅ |
| Taxa de erro no login | 0.00% | < 2% | ✅ |
| Health check da aplicação | UP | UP contínuo | ✅ |
| Tempo médio de processamento interno | 44ms | < 500ms | ✅ |

**Por que o login tem SLO separado:** o login usa bcrypt (hash de senha), que é intencionalmente lento por segurança. Por isso a meta é mais generosa (1500ms) do que o restante (2000ms). Um p95 de 292ms com bcrypt é excelente.

**Como os SLIs são coletados:**

```bash
# Durante ou após um teste de carga (k6)
k6 run load-tests/login-flow.js

# Em tempo real enquanto a aplicação está rodando
curl -s "http://localhost:8090/api/actuator/metrics/http.server.requests" | jq '.measurements'
curl -s http://localhost:8090/api/actuator/health | jq
```

**Quando revisar os SLOs:**
- Se o número de usuários dobrar (hoje: 100, meta: 200+)
- Se novos endpoints pesados forem adicionados (ex: upload de vídeo)
- Se o banco de dados migrar para uma instância maior

---

## Resumo geral

| Conceito | Implementação |
|---|---|
| Máquina virtual | EC2 gerenciada pelo Elastic Beanstalk |
| Banco de dados gerenciado | RDS PostgreSQL em subnet privada |
| Containerização | Docker — imagem versionada por SHA do commit |
| Registro de imagens | Docker Hub |
| Rede isolada | VPC com subnets públicas e privadas |
| Controle de acesso à rede | Security Groups (CloudFront only na porta 443) |
| CDN e borda | CloudFront + S3 |
| Integração contínua | GitHub Actions — testes a cada push |
| Entrega contínua | Deploy automático no Beanstalk com aprovação |
| Rollback | Histórico de versões no Beanstalk |
| Credenciais no CI/CD | OIDC — token temporário de 15 minutos |
| Rate limiting | Bucket4j — 5 tentativas/min por IP no login |
| Segredos da aplicação | Variáveis de ambiente no Beanstalk |
| Monitoramento interno | Spring Actuator |
| Teste de carga | k6 — 25 VUs, p95 152ms, 0% erro |
| Testes automatizados | JUnit 5 + Mockito |
| SLIs / SLOs | latência p95 < 2000ms, erro < 5%, health UP |

---

*Documento criado em: março de 2026*
