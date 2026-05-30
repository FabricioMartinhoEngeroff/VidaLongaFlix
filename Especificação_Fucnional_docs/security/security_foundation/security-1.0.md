# Segurança de Aplicações — AppSec, SSDLC e DevSecOps no VidaLongaFlix

## Objetivos
- Compreender o SSDLC e a filosofia *Secure Shift Left*: trazer segurança desde o início do ciclo de desenvolvimento
- Aplicar modelagem de ameaças (STRIDE) para identificar riscos na arquitetura backend + frontend
- Utilizar o OWASP Top 10 (2021) como guia de vulnerabilidades críticas em APIs REST e SPAs Angular
- Implementar controles de segurança no pipeline CI/CD: SAST, SCA, DAST e IaC scanning
- Estabelecer um roadmap evolutivo de segurança para o VidaLongaFlix com base no OWASP SAMM

---

## Módulo 1 — SSDLC e Secure Shift Left

### SDLC vs SSDLC
- **SDLC** (Software Development Life Cycle): ciclo clássico — requisitos, design, desenvolvimento, testes, deploy. Segurança era tratada no fim.
- **SSDLC** (Secure SDLC): segurança integrada em **todas as fases** do desenvolvimento — não é uma fase separada, é uma camada transversal.
- **Shift Left**: quanto mais cedo uma vulnerabilidade é encontrada, menor o custo de correção. Achar um bug em produção é 30x mais caro que achar durante o desenvolvimento.

### O modelo em cascata e seus riscos
- Desenvolvimento completo → QA → deploy: vulnerabilidades identificadas tarde, correção cara e arriscada.
- DevOps com CI/CD permite testes contínuos — a segurança deve entrar no mesmo pipeline.

### As fases do SSDLC

```
Requirements → Design → Development → Test → Deployment
     |              |          |           |          |
Requisitos     Threat      Coding      SAST       Pentest
de Segurança   Modeling    Guidelines  DAST       Security
                           OWASP       Code       Hardening
                           Build       Review
```

| Fase | O que fazer |
|---|---|
| **Requirements** | Definir requisitos de segurança (ex: sessão JWT com expiração de 1h) |
| **Design** | Modelagem de ameaças, decisões de arquitetura segura |
| **Development** | Seguir guidelines OWASP, sanitização de input, sem hardcode de secrets |
| **Test** | SAST, SCA, DAST, Code Review com foco em segurança |
| **Deployment** | Pentest, Security Hardening, gestão de portas e patches |

> **Segurança é um requisito funcional**, não apenas não-funcional. Deve ser priorizada desde o início, não adicionada depois.

---

## Módulo 2 — Modelagem de Ameaças (Threat Modeling)

### O que é e por que importa
- Identifica pontos de ameaça na arquitetura **antes** de escrever código
- Permite implementar controles de segurança já na fase de design
- Processo: **decompor a aplicação → identificar vulnerabilidades → mitigar → validar**

### Ativos do VidaLongaFlix a proteger
| Ativo | Risco principal |
|---|---|
| API REST (Spring Boot) | Exposição de dados, injeção, broken access control |
| Angular SPA | XSS, token armazenado de forma insegura, rotas desprotegidas |
| RDS PostgreSQL | SQL Injection, acesso não autorizado, dados sensíveis sem criptografia |
| S3 (vídeos/imagens) | Bucket público, path traversal, upload malicioso |
| JWT / Secrets | Token longo prazo, secret exposto no código ou log |
| Pipeline CI/CD | Supply chain attack, secrets vazados em logs |

### Modelo STRIDE
Framework da Microsoft para categorizar ameaças sistematicamente:

| Letra | Ameaça | Exemplo no VidaLongaFlix | Contramedida |
|---|---|---|---|
| **S** — Spoofing | Falsificação de identidade | JWT forjado | Assinatura com secret forte, validação em cada request |
| **T** — Tampering | Alteração não autorizada de dados | Modificar payload do token | Assinatura HMAC do JWT, SSL/TLS em trânsito |
| **R** — Repudiation | Negar ter realizado uma ação | Admin nega ter deletado um vídeo | Logs com timestamp, IP e usuário via Grafana/Loki |
| **I** — Information Disclosure | Exposição de dados sensíveis | Stack trace no response de erro | Custom error pages, sem dados técnicos no response |
| **D** — Denial of Service | Indisponibilidade do serviço | Flood de requisições na API | Rate limiting, ALB + Auto Scaling |
| **E** — Elevation of Privilege | Ganhar permissões maiores | USER acessar endpoint de ADMIN | `@PreAuthorize`, RBAC, deny by default |

### Ferramentas de modelagem
- **OWASP Threat Dragon**: ferramenta visual open source para desenhar diagramas de ameaças
- **Draw.io**: alternativa para diagramas de arquitetura com boundaries
- **Markdown Mermaid**: diagramas de fluxo com identificação de ameaças

### Etapas práticas
1. Desenhar a arquitetura com *boundaries* (quem acessa o quê)
2. Identificar ativos e vetores de ataque em cada componente
3. Classificar ameaças com STRIDE
4. Definir contramedidas para cada ameaça
5. Gerar relatório e validar implementação

---

## Módulo 3 — Princípios de Design Seguro

### Princípio do Menor Privilégio
- Cada usuário, serviço ou componente deve ter **somente as permissões necessárias** para sua função
- Se uma conta for comprometida, o impacto é limitado ao escopo mínimo dela
- Aplicado no VidaLongaFlix:
  - `ROLE_USER` não acessa endpoints admin
  - IAM role do EB só tem `s3:PutObject` no bucket `vidalongaflix-media`
  - RDS Security Group aceita conexão **somente** do Security Group do EB

### Defesa em Profundidade
- Segurança não é uma camada única — é uma sequência de barreiras
- Se uma camada falha, a próxima ainda protege

| Camada | Controle no VidaLongaFlix |
|---|---|
| Rede | Security Group + HTTPS obrigatório via CloudFront |
| API | Spring Security + JWT + CORS restrito |
| Aplicação | Validação de entrada + sanitização |
| Dados | RDS com `sslmode=require` + BCrypt para senhas |
| Pipeline | SAST + SCA + Gitleaks (fase 2) |
| Observabilidade | Logs de acesso no Grafana/Loki + alertas |

### Fail Safe (Falha Segura)
- Em caso de erro, a aplicação **não deve expor informações sensíveis**
- Stack trace no response revela tecnologias usadas, permitindo ataques direcionados
- Correto: página de erro genérica ("Algo deu errado") com log interno detalhado
- Errado: exibir `NullPointerException at com.vidalonga.service.UserService:45`

> **Regra**: erros são registrados em log interno (Loki) — nunca expostos ao usuário final.

---

## Módulo 4 — OWASP Top 10 (2021)

Evolução significativa: **Broken Access Control subiu para #1** em 2021.

| 2017 | 2021 | Mudança |
|---|---|---|
| A01 Injection | A01 Broken Access Control | ↑ subiu |
| A02 Broken Authentication | A02 Cryptographic Failures | ↑ subiu |
| A03 Sensitive Data Exposure | A03 Injection | ↓ caiu |
| A05 Broken Access Control | A04 Insecure Design | 🆕 novo |
| A06 Security Misconfiguration | A05 Security Misconfiguration | = manteve |

### A01 — Broken Access Control
- **Risco**: acessar dados de outro usuário, escalar para ADMIN
- **Backend**: `@PreAuthorize("hasRole('ADMIN')")` nos endpoints; validar `userId` do token contra o recurso
- **Frontend**: Angular route guards (`canActivate`) não bastam — o backend deve revalidar sempre

### A02 — Cryptographic Failures
- **Risco**: senhas em texto plano, conexão sem TLS, dados sensíveis em log
- **Backend**: BCrypt para senhas; RDS com SSL; HTTPS via CloudFront; logs sem dados sensíveis
- **Frontend**: nunca armazenar token em `localStorage` (vulnerável a XSS) — preferir `HttpOnly cookie`

### A03 — Injection (SQL, XSS, Log, Command)
- **SQL Injection**: query concatenada com input do usuário
  ```java
  // ERRADO
  String query = "SELECT * FROM users WHERE id='" + request.getParameter("id") + "'";
  // CORRETO — Spring Data JPA com parâmetros
  userRepository.findById(id); // prepared statement automático
  ```
- **XSS**: input refletido sem sanitização no frontend
  ```html
  <!-- ERRADO -->
  <div [innerHTML]="userInput"></div>
  <!-- CORRETO -->
  <div>{{ userInput }}</div> <!-- Angular escapa automaticamente -->
  ```

### A04 — Insecure Design (novo em 2021)
- **Risco**: arquitetura sem requisitos de segurança desde o início
- Falta de *threat modeling*, falta de casos de abuso mapeados
- Sessão sem tempo de expiração, endpoints públicos desnecessários

### A05 — Security Misconfiguration
- **Risco**: portas abertas, headers de segurança ausentes, perfis de debug em produção
- **Backend**: `SPRING_PROFILES_ACTIVE=prod`; endpoints Actuator restritos (`health,info,prometheus`)
- **Frontend**: `environment.prod.ts` sem dados sensíveis; sem `console.log` em produção

### A06 — Vulnerable and Outdated Components
- **Risco**: bibliotecas com CVEs conhecidos (ex: Log4Shell — RCE via log4j 2021)
- Bibliotecas são adicionadas e nunca atualizadas — risco de supply chain attack
- **Mitigação**: SCA (Dependency Check) no CI; Dependabot no GitHub; Docker Scout

### A07 — Identification and Authentication Failures
- **Risco**: brute force, tokens sem expiração, credential stuffing
- **Backend**: JWT com expiração configurada; secret com alta entropia; refresh token (fase futura)
- **Frontend**: logout limpa token; sem armazenamento permanente de credenciais

### A08 — Software and Data Integrity Failures (novo em 2021)
- **Risco**: pipeline comprometido, atualização de software sem verificação de integridade
- Dependência pública pode injetar código malicioso em outra dependência
- **Mitigação**: SBOM (Software Bill of Materials), verificação de assinaturas

### A09 — Security Logging and Monitoring Failures
- **Risco**: falhas de segurança acontecem sem rastro — impossível investigar
- **VidaLongaFlix**: logs → Grafana Loki via OTLP; traces → Tempo; alertas configurados
- Log deve registrar: timestamp, IP, usuário, ação, resultado (sucesso/falha)

### A10 — Server-Side Request Forgery (novo em 2021)
- **Risco**: aplicação faz request para URL controlada pelo atacante → acesso a recursos internos
- Relevante se o backend consumir URLs enviadas pelo cliente (ex: webhook, avatar via URL)

---

## Módulo 5 — Validação, Sanitização e Controle de Acesso

### Validação de Input — Allow List vs Deny List
- **Deny list**: bloquear caracteres conhecidos como maliciosos — fraco, fácil de bypassar
- **Allow list**: definir exatamente o que é aceito — forte, correto
  ```java
  // Allow list com regex — aceitar apenas números
  if (!input.matches("^[0-9]+$")) throw new IllegalArgumentException("Input inválido");
  ```
- Angular: usar validadores nos formulários (`Validators.pattern(/^[a-zA-Z ]+$/)`)

### Controle de Acesso — RBAC e Deny by Default
- **RBAC** (Role-Based Access Control): permissões baseadas em roles (`ROLE_ADMIN`, `ROLE_USER`)
- **Deny by default**: tudo bloqueado por padrão, liberar explicitamente o necessário
  ```java
  // Spring Security — deny by default
  http.authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/public/**").permitAll()
      .anyRequest().authenticated() // tudo o mais requer auth
  );
  ```
- Segregação de funções: usuário comum não pode deletar conteúdo de outro usuário

### Gerenciamento de Sessão com JWT
- Token composto por: `header.payload.signature`
- Claims importantes: `sub` (usuário), `roles`, `exp` (expiração)
- **Riscos no frontend**:
  - `localStorage`: vulnerável a XSS — qualquer script pode ler o token
  - `sessionStorage`: melhor que localStorage, mas ainda acessível via JS
  - `HttpOnly cookie`: ideal — inacessível via JS, protegido contra XSS
- Cookie seguro:
  ```
  Set-Cookie: token=...; HttpOnly; Secure; SameSite=Strict; Path=/
  ```
- Não expor o nome do framework no cookie (ex: `ASP.NET_SessionId` revela tecnologia)

### Cabeçalhos HTTP de Segurança
| Header | Valor recomendado | Proteção |
|---|---|---|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Força HTTPS |
| `X-Content-Type-Options` | `nosniff` | Previne MIME sniffing |
| `X-Frame-Options` | `DENY` | Previne clickjacking |
| `Content-Security-Policy` | Restrito ao necessário | Previne XSS |
| `Cache-Control` | `no-store` em endpoints autenticados | Evita cache de dados sensíveis |

```java
// Spring Security — headers de segurança
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true))
);
```

---

## Módulo 6 — ASVS (Application Security Verification Standard)

### O que é
- **ASVS** = Application Security Verification Standard (OWASP)
- Checklist padronizado de controles de segurança para auditar aplicações
- 3 níveis de maturidade:

| Nível | Descrição | Cobertura |
|---|---|---|
| **Nível 1** | Mínimo necessário para produção | ~20% dos controles |
| **Nível 2** | Aplicações com dados sensíveis | ~50-70% dos controles |
| **Nível 3** | Alta segurança (bancos, saúde) | >70% dos controles |

### Controles-chave relevantes para o projeto
- **Autenticação**: JWT com expiração, MFA (fase futura), sem brute force
- **Autorização**: RBAC implementado, verificação server-side em toda requisição
- **Upload de arquivos**: validação de `Content-Type`, nome gerado pelo backend (UUID), limite de tamanho
- **Dados sensíveis**: senhas com BCrypt, dados em trânsito via TLS, sem CPF/senha em log
- **Tratamento de erros**: sem stack trace no response, páginas de erro genéricas

---

## Módulo 7 — Ferramentas de Segurança no CI/CD

### Pipeline de segurança completo

```
push → SAST → SCA → IaC → Build → DAST (staging) → Deploy
         |      |     |              |
      código  deps  infra         testes
      estático CVEs  cloud        dinâmicos
```

### SAST — Static Application Security Testing
Analisa o **código-fonte** sem executar a aplicação. Ideal na fase de desenvolvimento.

| Ferramenta | Uso | Open Source |
|---|---|---|
| **SonarQube/SonarCloud** | Java, JavaScript, TypeScript — análise completa | Freemium |
| **Semgrep** | Regras customizáveis, multi-linguagem | ✅ |
| **Gitleaks** | Detecta secrets hardcodados no histórico Git | ✅ |
| **SpotBugs** | Java — detecta vulnerabilidades no bytecode | ✅ |

**O que o SAST detecta**:
- Hard Coded Secrets (senhas no código)
- SQL Injection e XSS
- Validação de input insuficiente
- Lógica de negócio insegura parcialmente

```yaml
# GitHub Actions — SAST com SonarCloud
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@master
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

### SCA — Software Composition Analysis
Analisa **dependências e bibliotecas** em busca de CVEs conhecidos.

**Exemplo real**: Log4Shell (2021) — CVE que permitia RCE (Remote Code Execution) via log4j. Uma biblioteca amplamente usada se tornou uma porta de entrada crítica.

| Ferramenta | Linguagem | Open Source |
|---|---|---|
| **OWASP Dependency Check** | Java, JavaScript, Python | ✅ |
| **Snyk** | Multi-linguagem | Freemium |
| **Docker Scout** | Imagens Docker | Freemium |
| **npm audit** | Node/Angular | ✅ |

```yaml
# GitHub Actions — SCA com OWASP Dependency Check
- name: Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'vidalongaflix'
    path: '.'
    format: 'HTML'
    out: 'reports'
    args: --nvdApiKey ${{ secrets.DC_KEY }}
```

### DAST — Dynamic Application Security Testing
Testa a **aplicação em execução** simulando um atacante. Executar em ambiente de **staging** — nunca em produção (risco de DoS).

| Ferramenta | Uso | Open Source |
|---|---|---|
| **OWASP ZAP** | Fuzzing de endpoints, XSS, auth bypass | ✅ |
| **Burp Suite** | Proxy, interceptação, análise manual | Freemium |

**O que o DAST encontra**:
- XSS refletido e armazenado
- Problemas de autenticação e autorização
- Headers de segurança ausentes
- CSP não configurado
- Exposição de informações no response

```yaml
# GitHub Actions — DAST com OWASP ZAP
- name: ZAP Scan
  uses: zaproxy/action-baseline@v0.9.0
  with:
    target: 'https://staging.vidalongaflix.com.br'
```

### IaC Security — Infrastructure as Code
Analisa arquivos de infraestrutura (Terraform, Dockerfile, docker-compose) em busca de misconfigurações.

**Problemas comuns detectados**:
- S3 bucket com ACL pública
- Container rodando como `root`
- Security Group com `0.0.0.0/0`
- Secrets em variáveis de ambiente no Dockerfile
- Recursos sem criptografia

| Ferramenta | O que analisa | Open Source |
|---|---|---|
| **KICS (Checkmarx)** | Dockerfile, Terraform, K8s, docker-compose | ✅ |
| **Checkov** | Terraform, CloudFormation | ✅ |
| **tfsec** | Terraform específico | ✅ |

```yaml
# GitHub Actions — IaC com KICS
- name: Run KICS Scan
  uses: checkmarx/kics-github-action@v2.1.4
  with:
    path: '.'
    output_path: reports/
```

---

## Módulo 8 — Gestão de Segredos e Security Hardening

### Gestão de Segredos

**O problema**: secrets commitados no Git ficam no histórico **para sempre** — mesmo se removidos depois. Bots escaneiam repositórios públicos continuamente.

**Estratégia correta**:
- Injetar credentials via variáveis de ambiente em runtime (EB vars, GitHub Secrets)
- Nunca usar valores hardcodados em `application.properties` ou código
- Usar cofres de segredos para organizar e rotacionar

| Ferramenta | Uso | Custo |
|---|---|---|
| **HashiCorp Vault** | Cofre centralizado, rotação automática | Open Source / Enterprise |
| **AWS Secrets Manager** | Integrado com EB, Lambda, RDS | Pay per secret |
| **GitHub Secrets** | Secrets para CI/CD pipeline | ✅ Grátis |

**Rotação de segredos no VidaLongaFlix**:
| Secret | Impacto da rotação | Estratégia |
|---|---|---|
| `JWT_SECRET` | Invalida todos tokens ativos | Janela de manutenção planejada |
| `OTLP_AUTH_HEADER` | Perde telemetria até atualizar | Atualizar EB vars antes de expirar |
| `DB_PASSWORD` | Downtime se não coordenado | Rolling deploy: atualizar RDS → EB vars |

### Security Hardening

**Gestão de portas**:
- Fechar portas desnecessárias — cada porta aberta é uma superfície de ataque
- Porta 22 (SSH) aberta indica serviço SSH explorável por scanners automatizados
- Preferir portas não-padrão para dificultar reconhecimento

**Segregação de serviços**:
- Banco de dados e aplicação nunca no mesmo servidor
- RDS em subnet privada — sem acesso direto pela internet
- Se o app for comprometido, o atacante não tem acesso direto ao banco

**Patch management**:
- Manter bibliotecas, OS e serviços atualizados
- Automatizar alertas de novas CVEs (Dependabot, Docker Scout alerts)
- Testar updates em staging antes de produção
- Manter inventário de versões em uso

---

## Módulo 9 — Logs, Monitoramento e Resposta a Incidentes

### Logging para Segurança e Auditabilidade
- Log deve registrar: **timestamp + IP + usuário + ação + resultado**
- Essencial para provar o que aconteceu em uma auditoria (non-repudiation do STRIDE)
- Logs de acesso indevido devem gerar **alertas automáticos**

**O que logar**:
- Tentativas de login (sucesso e falha)
- Acessos a endpoints sensíveis (`/admin/**`)
- Erros de autorização (403)
- Operações de criação/deleção de dados críticos

**O que NÃO logar**:
- Senhas, tokens JWT, dados de cartão
- Dados pessoais sensíveis (CPF, endereço completo)

### SIEM e Ferramentas de Monitoramento
- **SIEM** (Security Information and Event Management): centraliza logs e gera alertas de segurança
- Ferramentas: Datadog, LogZ.io, Grafana + Loki (já implementado no VidaLongaFlix)
- Alerta quando: múltiplos 401/403 do mesmo IP, acesso fora do horário normal, volume anormal de requisições

### Resposta a Incidentes
Ciclo quando um incidente de segurança é detectado:

```
Detecção → Contenção → Mitigação → Remediação → Pós-mortem
    |            |           |           |            |
  alerta     isolar      corrigir    deploy      lições
  Grafana    serviço     hotfix      fix        aprendidas
```

1. **Detecção**: alerta via Grafana/Loki ou report externo
2. **Contenção**: isolar o componente comprometido, revogar tokens, bloquear IP
3. **Mitigação**: patch temporário para parar o vazamento
4. **Remediação**: correção definitiva com testes
5. **Pós-mortem**: documentar causa raiz, criar tarefas no backlog, evitar recorrência

---

## Módulo 10 — Maturidade de Segurança e DevSecOps

### OWASP SAMM (Software Assurance Maturity Model)
Framework para avaliar e evoluir a maturidade de segurança de uma organização.

**5 domínios principais**:
| Domínio | O que avalia |
|---|---|
| **Governance** | Estratégia, políticas, treinamentos |
| **Design** | Threat modeling, requisitos de segurança |
| **Implementation** | Secure coding, SAST, SCA, gestão de dependências |
| **Verification** | DAST, pentest, code review de segurança |
| **Operations** | Monitoramento, gestão de incidentes, gestão de segredos |

**3 níveis de maturidade por domínio** (1 = básico → 3 = avançado). Avaliação periódica a cada 6-12 meses com scorecard.

### Security Champion
- Proporção real em empresas: ~100 devs para 1 profissional de segurança
- **Security Champion**: desenvolvedor engajado com segurança que dissemina a cultura para o time
- Responsabilidades: code review com foco em segurança, integração de ferramentas, evangelização

### DevSecOps
Integração de segurança dentro do pipeline de desenvolvimento:

```
Dev → Security Gate → Ops
         |
    SAST + SCA + IaC + DAST
    Bloqueia se severity CRITICAL
    Alerta se severity HIGH
```

- Feedback rápido: o desenvolvedor recebe o alerta de vulnerabilidade no mesmo pipeline de build
- **Security Gate**: quebra o pipeline se encontrar vulnerabilidade crítica
- Dashboard de segurança com métricas: número de vulnerabilidades abertas, MTTR (tempo médio de correção)

### Ferramentas de Maturidade
| Framework | Foco | Uso |
|---|---|---|
| **OWASP SAMM** | Avaliação de maturidade geral | Mais comum em empresas |
| **BSIMM** | Benchmark com mercado | Empresas grandes |
| **OpenCRE** | Mapeamento técnico de controles | Referência técnica aberta |
| **NIST SSDF** | Padrão do governo dos EUA | Compliance e auditoria |

---

## Roadmap de Segurança — VidaLongaFlix (Backend + Frontend)

### Arquitetura atual e superfície de ataque

```
Internet
    │
CloudFront (HTTPS) ─── S3 (Angular SPA)
    │
ALB (AWS)
    │
Elastic Beanstalk (Spring Boot :8090)
    │
RDS PostgreSQL (subnet privada)
    │
Grafana Cloud (OTLP: métricas, logs, traces)
```

**Frontend Angular**:
- `auth.interceptor.ts` — Bearer token JWT em cada requisição
- `environment.prod.ts` → `https://api.vidalongaflix.com.br/api`
- Route guards para proteção de rotas por role

---

### Estado Atual — Controles Implementados

| Controle | Backend (Spring Boot) | Frontend (Angular) | Status |
|---|---|---|---|
| HTTPS | CloudFront forçado | CloudFront + S3 | ✅ |
| Autenticação JWT | `JwtAuthenticationFilter` | `auth.interceptor.ts` | ✅ |
| Autorização RBAC | `@PreAuthorize` + roles | Route guards | ✅ |
| CORS restrito | `CORS_ALLOWED_ORIGINS` via EB | N/A (upstream) | ✅ |
| Senhas com BCrypt | `PasswordEncoder` bean | N/A | ✅ |
| RDS com SSL | `sslmode=require` | N/A | ✅ |
| Secrets via env vars | EB environment vars | GitHub Secrets | ✅ |
| Imagem multi-stage | `eclipse-temurin:17-jre` | Build Angular → Nginx | ✅ |
| Logs de segurança | Grafana Loki via OTLP | N/A | ✅ |
| Traces por requisição | Grafana Tempo | — | ✅ |
| Actuator restrito | `health,info,prometheus` | N/A | ✅ |

---

### Fase 1 — Fundação de Segurança no CI/CD *(prioridade alta)*

**1.1 — Gitleaks: bloquear secrets no histórico Git**
- Impede que tokens, senhas e API keys sejam commitados
- Aplica-se ao: backend e frontend

```yaml
# .github/workflows/ci.yml
- name: Gitleaks — scan de secrets
  uses: gitleaks/gitleaks-action@v2
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**1.2 — SonarCloud: análise estática (SAST)**
- Detecta vulnerabilidades de código, code smells e cobertura de testes
- Backend: Java (Spring Boot) — detecta SQL injection, hardcoded secrets, NPE
- Frontend: TypeScript/Angular — detecta XSS, uso de `any`, funções inseguras

```yaml
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@master
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**1.3 — OWASP Dependency Check: SCA para Java**
- Verifica CVEs nas dependências do `pom.xml`
- Bloquear deploy se encontrar vulnerabilidade CRITICAL

```yaml
- name: OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'vidalongaflix-backend'
    path: '.'
    format: 'HTML'
    args: --failOnCVSS 9 --nvdApiKey ${{ secrets.DC_KEY }}
```

**1.4 — npm audit: SCA para Angular**
- `npm audit --audit-level=high` no pipeline do frontend
- Bloquear se encontrar vulnerabilidade HIGH ou CRITICAL

**1.5 — Docker Scout: CVEs na imagem Docker**
```yaml
- name: Docker Scout CVEs
  uses: docker/scout-action@v1
  with:
    command: cves
    image: ${{ env.DOCKER_IMAGE }}
    exit-code: true
    severity: critical,high
```

---

### Fase 2 — Hardening de Código e Infraestrutura *(prioridade média)*

**2.1 — Cabeçalhos HTTP de segurança (Backend)**
```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true))
);
```

**2.2 — Content Security Policy no Angular (Frontend)**
- Configurar CSP no `nginx.conf` do container Angular (fase de migração do frontend)
- Ou via CloudFront Response Headers Policy

**2.3 — Rate limiting nos endpoints de autenticação (Backend)**
- Limitar tentativas de login: máx. 5 tentativas em 5 minutos por IP
- Opções: Spring Cloud Gateway, Bucket4j, ou AWS WAF

**2.4 — KICS: IaC Security Scanning**
- Detectar misconfigurações no `Dockerrun.aws.json`, `docker-compose.yml`, Terraform (fase 3)
```yaml
- name: KICS IaC Scan
  uses: checkmarx/kics-github-action@v2.1.4
  with:
    path: '.'
    output_path: reports/
```

**2.5 — Fail Safe: páginas de erro customizadas (Backend)**
```java
// Sem stack trace no response
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleGenericError(Exception e) {
    log.error("Erro interno", e); // log interno com detalhe
    return ResponseEntity.status(500)
        .body(Map.of("error", "Ocorreu um erro interno. Tente novamente.")); // sem detalhe ao usuário
}
```

**2.6 — Revisão de IAM roles (Menor Privilégio)**
- Auditar a IAM role do EB: remover permissões não utilizadas
- Separar IAM user do CI (`elasticbeanstalk:*`, `s3:PutObject` apenas no bucket de deploy)

**2.7 — Threat Modeling documentado**
- Criar diagrama STRIDE da arquitetura atual no OWASP Threat Dragon
- Revisar a cada release major ou mudança arquitetural

---

### Fase 3 — Testes Dinâmicos e Evolução *(prioridade média-baixa)*

**3.1 — DAST com OWASP ZAP em staging**
- Criar ambiente de staging no EB (ou localmente via Docker Compose)
- Rodar ZAP baseline scan após cada deploy em staging
- Bloquear deploy em produção se encontrar vulnerabilidade HIGH

**3.2 — AWS Secrets Manager (substituir EB env vars)**
- Migrar `JWT_SECRET`, `DB_PASSWORD`, `OTLP_AUTH_HEADER` para AWS Secrets Manager
- Rotação automática configurada
- Aplicação lê secrets via SDK em runtime (sem restart necessário)

**3.3 — S3 Signed URLs para acesso a mídia**
- Substituir URLs públicas do S3 por URLs pré-assinadas com expiração
- Controle de acesso ao conteúdo baseado em autenticação do usuário

**3.4 — Refresh Token + Revogação JWT**
- Implementar refresh token para renovação sem re-login
- Token de curta duração (15 min) + refresh de longa duração (7 dias)
- Blacklist de tokens revogados (Redis)

**3.5 — OWASP SAMM Assessment**
- Aplicar questionário OWASP SAMM para medir maturidade atual
- Criar scorecard com os 5 domínios
- Repetir anualmente para medir evolução

---

### Fase 4 — Maturidade Avançada *(evolução futura)*

| Controle | Descrição | Tecnologia |
|---|---|---|
| WAF (Web Application Firewall) | Proteção na frente do CloudFront | AWS WAF |
| MFA (Multi-Factor Authentication) | Segunda camada de autenticação | TOTP / Authenticator |
| Pentest externo | Simulação de ataque real por Red Team | Empresa especializada |
| SBOM | Inventário de todas as dependências | CycloneDX / Syft |
| IAST | Teste interativo em runtime | Checkmarx IAST |
| Security Hub AWS | Visão centralizada de postura de segurança | AWS Security Hub |
| Defects Dojo | Gestão centralizada de vulnerabilidades | DefectDojo (open source) |

---

### Resumo do Roadmap por Fase

| Fase | Itens | Onde aplica | Prioridade |
|---|---|---|---|
| **1 — CI/CD** | Gitleaks, SonarCloud, Dep. Check, npm audit, Docker Scout | Backend + Frontend | 🔴 Alta |
| **2 — Hardening** | HTTP Headers, Rate Limiting, KICS, Fail Safe, IAM review, Threat Model | Backend + Infra | 🟠 Média |
| **3 — Dinâmico** | DAST/ZAP, AWS Secrets Manager, Signed URLs, Refresh Token, SAMM | Backend + AWS | 🟡 Média-baixa |
| **4 — Maturidade** | WAF, MFA, Pentest, SBOM, IAST, Security Hub | Full stack | 🟢 Futura |
