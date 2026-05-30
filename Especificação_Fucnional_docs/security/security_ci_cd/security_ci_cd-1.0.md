# Segurança CI/CD — SAST com SonarCloud no VidaLongaFlix

> Baseado no curso "Segurança CI/CD — SAST, SCA, DAST e Quality Gate".
> Este documento descreve: fundamentos do SAST e seu papel no SSDLC, vulnerabilidades reais mapeadas no projeto, cenários de teste com resultado esperado (positivos e negativos) para backend e frontend, estratégia de triagem e gestão de dívida técnica, customização de regras no SonarCloud e passo a passo completo de implementação.

---

## Parte 1 — Fundamentos do SAST no SSDLC

---

### O que é SAST

**SAST (Static Application Security Testing)** é a análise estática do código-fonte sem executar a aplicação. É considerado um teste de **caixa branca** (white box): o analista ou a ferramenta tem acesso ao código e consegue examinar sua estrutura completa — fluxo de dados, chamadas entre funções, uso de bibliotecas — identificando padrões que indicam vulnerabilidades.

O funcionamento técnico das ferramentas open source é baseado principalmente em **regex e análise de fluxo**:
1. A ferramenta mapeia todas as **entradas interativas** (parâmetros de request, inputs de formulário, campos de query)
2. Mapeia todas as **saídas** (respostas HTTP, logs, banco de dados)
3. Verifica se existe **sanitização adequada** entre entrada e saída
4. Flageia o caminho como vulnerável se não houver validação no fluxo

Exemplo do padrão Checkmarx para XSS (conceito do curso):
```
CxList inputs  = Find_Interactive_Inputs();
CxList outputs = Find_XSS_Outputs().Find_Console_Outputs();
CxList sanitized = Find_XSS_Sanitize();
result = inputs.InfluencingOnAndNotSanitized(outputs, sanitized);
```
Isso é o SAST funcionando como um grande regex contextual: busca entradas que influenciam saídas sem sanitização.

---

### Por que o SAST é importante (Shift Left)

O princípio **Shift Left** diz que quanto mais cedo uma vulnerabilidade é identificada no ciclo de desenvolvimento, menor o custo de correção:

```
Fase de desenvolvimento  →  custo 1x
Fase de testes           →  custo 10x
Após deploy em produção  →  custo 30x+
```

O SAST integrado ao CI/CD garante que o código vulnerável seja **bloqueado antes de chegar à produção**. Isso é possível com o conceito de **Quality Gate** (porta de qualidade) e **Security Gate** (porta de segurança):

- **Quality Gate**: conjunto de condições que o código deve satisfazer para avançar no pipeline. Exemplos: zero issues críticos novos, cobertura de testes > 80%, dívida técnica < 1 dia.
- **Security Gate**: subconjunto focado exclusivamente em segurança. Exemplos: zero vulnerabilidades Critical ou Blocker novas.

Se o gate falhar → o pipeline para. O deploy não acontece. A pessoa desenvolvedora recebe o feedback imediato no PR ou no output do pipeline.

---

### SAST vs DAST vs SCA — Quando usar cada um

| Técnica | Tipo | O que analisa | Quando atua no SSDLC | Ferramenta neste projeto |
|---|---|---|---|---|
| **SAST** | White box (estático) | Código-fonte sem executar | Development → CI/CD (pre-deploy) | **SonarCloud** |
| **DAST** | Black box (dinâmico) | Aplicação em execução | Staging → pré-produção | OWASP ZAP (Fase 3) |
| **SCA** | Dependências | Bibliotecas e CVEs | CI/CD (a cada build) | Trivy (já ativo) |
| **Secrets** | Estático | Tokens, senhas no código | Pre-commit + CI/CD | Gitleaks (Fase 1) |
| **IaC** | Estático | Terraform, Dockerfiles | CI/CD | KICS ou Checkov (Fase 2) |

O SAST **não substitui** o DAST: eles são complementares. O SAST encontra o problema no código (ex: consulta SQL sem parametrização). O DAST encontra o problema no comportamento (ex: injeta `' OR 1=1 --` no endpoint real e observa a resposta). Ambos são necessários para cobertura completa.

---

### Falsos Positivos vs Falsos Negativos

Este é o trade-off central de qualquer ferramenta SAST. O conceito do curso é direto:

**Falso Positivo**: a ferramenta sinaliza um problema, mas não é uma vulnerabilidade real.
- Exemplo: SonarCloud flageia um `new Random()` em Java como inseguro, mas o uso é para gerar IDs de log, não para criptografia.
- Impacto: ruído, frustração do desenvolvedor, desengajamento com a ferramenta.

**Falso Negativo**: existe uma vulnerabilidade real, mas a ferramenta não a encontrou.
- Exemplo: SQL Injection via concatenação de string em uma query HQL não mapeada pelas regras.
- Impacto: vulnerabilidade chega à produção silenciosamente — o risco mais grave.

**Regra de ouro do curso**: é sempre **melhor ter falso positivo do que falso negativo**. O falso positivo pode ser identificado e marcado na triagem. O falso negativo passa despercebido.

**Estratégia para reduzir ambos**:
1. Começar com poucos tipos de vulnerabilidade (ex: só XSS e Hardcoded Secrets) em vez de ativar todas as regras de uma vez
2. Conforme o time entende as regras, expandir gradualmente o escopo
3. Usar triagem humana para validar os primeiros alertas de cada categoria nova
4. Ferramentas enterprise com IA reduzem falsos positivos analisando contexto semântico; em open source, o refinamento é manual via customização de regras

---

### Triagem Humana e Estratégia de Engajamento

A **triagem** é o processo de classificar cada issue identificada pela ferramenta como:
- **Vulnerabilidade real** → corrigir com prioridade conforme severidade
- **Falso positivo** → marcar como `Won't Fix` ou `False Positive` no SonarCloud, com justificativa
- **Dívida técnica aceitável** → marcar como `Accepted Risk` com prazo definido

O curso enfatiza que a triagem é um **processo cultural**. Para engajar a equipe:
1. Comece pelo tipo de vulnerabilidade mais compreensível (ex: Hardcoded Secrets — todo desenvolvedor entende)
2. Não ative todas as regras de uma vez — o volume de alertas desmotiva
3. Celebre as correções — mostre a evolução do Security Score no dashboard
4. A triagem deve reduzir progressivamente a dependência do analista de segurança conforme a equipe amadurece

---

### OWASP Top 10 e CWE como Padrões de Referência

O SonarCloud mapeia todas as suas regras para padrões reconhecidos:

**OWASP Top 10 (2021)** — categorias de risco em aplicações web:
| # | Categoria | Exemplos |
|---|---|---|
| A01 | Broken Access Control | Endpoint sem autenticação, IDOR |
| A02 | Cryptographic Failures | MD5, SHA-1, secrets hardcoded |
| A03 | Injection | SQL Injection, Command Injection, XSS |
| A04 | Insecure Design | Ausência de rate limiting, falta de threat model |
| A05 | Security Misconfiguration | CORS wildcard, Actuator exposto |
| A06 | Vulnerable Components | Dependências com CVE (SCA) |
| A07 | Authentication Failures | JWT fraco, senha sem expiração |
| A08 | Software Integrity Failures | CI/CD sem verificação de integridade |
| A09 | Logging Failures | Dados sensíveis em logs, sem auditoria |
| A10 | SSRF | Requisições para endpoints internos |

**CWE (Common Weakness Enumeration)** — catálogo mais granular mantido pelo MITRE:
- CWE-89: SQL Injection
- CWE-79: XSS
- CWE-259: Hardcoded Password
- CWE-327: Broken Cryptographic Algorithm
- CWE-22: Path Traversal
- CWE-284: Improper Access Control

Ambos funcionam como vocabulário comum entre ferramentas e equipes. O SonarCloud exibe o mapeamento OWASP + CWE em cada issue, facilitando a comunicação com a gestão e a auditoria.

---

### IA no SAST — Open Source vs Enterprise

| Aspecto | Open Source (SonarCloud free) | Enterprise (SonarCloud Developer+) |
|---|---|---|
| Base das regras | Regex + análise de fluxo | Regex + análise semântica + IA |
| Falsos positivos | Mais frequentes — regras genéricas | Menos — contexto do projeto considerado |
| Customização | Manual (Quality Profiles, regras on/off) | Manual + IA + perfis por papel (admin/analista/dev) |
| Escala | Adequada para projetos até médio porte | Escala para milhares de projetos |
| Custo | Grátis para repos públicos | Pago por LOC |

Para o VidaLongaFlix (open source / repo público): o tier gratuito do SonarCloud cobre 100% das necessidades atuais. A customização manual de regras (Quality Profile) compensa a ausência de IA.

---

## Parte 2 — Vulnerabilidades Reais Identificadas no Projeto

### Tabela Mestre — Backend (Spring Boot / Java 17)

| ID | Arquivo | Linha | Vulnerabilidade | OWASP | CWE | Severidade SonarCloud | Detectável por SAST |
|---|---|---|---|---|---|---|---|
| B1 | `SecurityConfig.java` | 34 | `csrf.disable()` não documentado — risco ativado ao migrar para HttpOnly cookie | A05 | CWE-352 | Minor | ⚠️ Parcial (regra S4502) |
| B2 | `CorsConfig.java` | 19 | `allowedHeaders("*")` — aceita qualquer header customizado | A05 | CWE-346 | Major | ✅ Regra S5122 |
| B3 | `SecurityConfig.java` | 58 | `headers.disable()` — remove headers de segurança (Vary, X-Frame, etc.) | A05 | CWE-693 | Critical | ✅ Regra S4792 |
| B4 | `SecurityConfig.java` | 43 | `/actuator/**` sem autenticação — fingerprinting e SSRF interno | A01 | CWE-284 | Critical | ✅ Regra S4834 |
| B5 | `WhatsAppService.java` | 63 | `new RestTemplate()` sem timeout — DoS por thread blocking | A04 | CWE-400 | Major | ✅ Regra S2755 |
| B6 | `pom.xml` | 19 | Override de `jackson-core` para CVE — confirma que SCA é necessário | A06 | CWE-1395 | Info | ❌ SCA (Trivy) |
| B7 | Qualquer endpoint | — | Ausência de rate limiting em `/auth/login` | A04 | CWE-307 | Major | ⚠️ Parcial (hotspot) |
| B8 | `JwtService.java` | — | Algoritmo de assinatura JWT deve ser verificado (HS256 mínimo) | A02/A07 | CWE-347 | Blocker | ✅ Regra S5659 |
| B9 | Logs com dados sensíveis | — | Logging de tokens, senhas ou PII em stacks de erro | A09 | CWE-117 | Major | ✅ Regra S4792 |
| B10 | Upload de vídeo/capa | — | Path traversal em nome de arquivo no upload S3 | A03 | CWE-22 | Critical | ✅ Regra S6096 |

---

### Tabela Mestre — Frontend (Angular / TypeScript)

| ID | Arquivo | Vulnerabilidade | OWASP | CWE | Severidade SonarCloud | Detectável por SAST |
|---|---|---|---|---|---|---|
| F1 | Qualquer componente | `[innerHTML]` sem sanitização — XSS stored/reflected | A03 | CWE-79 | Blocker | ✅ Regra S5148 |
| F2 | `auth.interceptor.ts` | Interceptor sem filtro de URL — token vaza para terceiros | A07 | CWE-522 | Critical | ✅ Regra S4784 |
| F3 | `environment.ts` | API key ou secret hardcoded no ambiente de dev | A02 | CWE-259 | Blocker | ✅ Regra S2068 |
| F4 | Qualquer serviço | `eval()` ou `Function()` com dado externo — code injection | A03 | CWE-78 | Blocker | ✅ Regra S5334 |
| F5 | `localStorage` | JWT armazenado em localStorage — vulnerável a XSS | A02 | CWE-312 | Major | ⚠️ Hotspot (S5659) |
| F6 | `HttpClient` calls | URL hardcoded diferente da `environment.apiUrl` | A05 | CWE-1104 | Minor | ✅ Regra S1313 |
| F7 | `package.json` | Dependências Angular/npm com CVE conhecido | A06 | CWE-1395 | Variável | ❌ SCA (npm audit/Trivy) |
| F8 | Formulário de login | Ausência de proteção contra brute force no client | A07 | CWE-307 | Info | ⚠️ Hotspot |

---

### Mapa de Severidade — Referência para Quality Gate

| Severidade SonarCloud | Definição | Ação no Pipeline | Prazo de Correção |
|---|---|---|---|
| **Blocker** | Vulnerabilidade crítica — exploração direta provável | **Bloqueia o merge** imediatamente | Imediato (mesmo sprint) |
| **Critical** | Vulnerabilidade séria com alto impacto se explorada | **Bloqueia o merge** | 1 sprint (2 semanas) |
| **Major** | Vulnerabilidade moderada — exploração indireta ou com condições | Alerta no PR — não bloqueia | 2 sprints |
| **Minor** | Vulnerabilidade baixa ou code smell de segurança | Informativo no PR | Backlog |
| **Info / Hotspot** | Código que requer revisão humana — pode ou não ser vulnerabilidade | Triagem obrigatória antes de mergar | Triagem no mesmo sprint |

---

## Parte 3 — Cenários e Resultado Esperado — Backend (Spring Boot / Java)

> Positivo = comportamento seguro / correto. Negativo = comportamento inseguro que o SonarCloud deve detectar ou que um JUnit deve reprovar. Triagem = requer revisão humana no SonarCloud antes de mergar.

---

### B-SEC-01 — Hardcoded Secrets (CWE-259 / OWASP A02 / Blocker — S2068, S6437)

| # | Cenário | Esperado |
|---|---|---|
| 1 | JWT secret definido como string literal no JwtService | SonarCloud sinaliza Blocker — "Credentials should not be hard-coded" |
| 2 | Senha do banco hardcoded em application.properties commitado | SonarCloud sinaliza Blocker |
| 3 | API key de serviço externo escrita direto no código Java | SonarCloud sinaliza Blocker |
| 4 | @Value lendo JWT_SECRET de variável de ambiente | Nenhum alerta — padrão seguro |
| 5 | application-prod.properties usando placeholder sem valor real | Nenhum alerta |
| 6 | Token literal expirado em classe de teste unitário | SonarCloud pode sinalizar — avaliar e marcar Won't Fix com justificativa se for token inerte de mock |

---

### B-SEC-02 — SQL Injection (CWE-89 / OWASP A03 / Blocker — S3649)

| # | Cenário | Esperado |
|---|---|---|
| 7 | Query SQL montada com concatenação de string usando input do usuário | SonarCloud sinaliza Blocker S3649 |
| 8 | JPQL com concatenação de variável de usuário no createQuery | SonarCloud sinaliza — injeção funciona em JPQL também |
| 9 | Criteria API sem parâmetros nomeados recebendo input externo | SonarCloud pode sinalizar — avaliar contexto |
| 10 | @Query com parâmetro nomeado :title e @Param("title") | Nenhum alerta — parametrização correta |
| 11 | findByTitleContainingIgnoreCase() gerado pelo Spring Data JPA | Nenhum alerta — JPA gera query parametrizada automaticamente |
| 12 | Busca recebendo o valor "'; DROP TABLE videos; --" via findByTitle | Sem exceção, sem resultado indevido — query parametrizada isola o valor |

---

### B-SEC-03 — Path Traversal em Upload (CWE-22 / OWASP A03 / Critical — S6096)

| # | Cenário | Esperado |
|---|---|---|
| 13 | Caminho do arquivo montado com nome original enviado pelo usuário | SonarCloud sinaliza Critical S6096 |
| 14 | new File(baseDir, userFilename) sem verificar se o path resultante está dentro do baseDir | SonarCloud sinaliza — "../../../etc/passwd" atravessa o diretório |
| 15 | Path resolvido e verificado que está dentro do diretório base antes de usar | SonarCloud aceita — se alertar mesmo assim, marcar como Won't Fix com justificativa |
| 16 | Upload direto ao S3 com chave UUID sem usar o nome original como path local | Nenhum alerta — nome do arquivo não vira caminho de disco |
| 17 | Nome malicioso "../../../etc/passwd" enviado no upload | Arquivo salvo com UUID gerado — path traversal neutralizado |

---

### B-SEC-04 — Criptografia Fraca (CWE-327/328 / OWASP A02 / Critical — S4790, S5542, S2245)

| # | Cenário | Esperado |
|---|---|---|
| 18 | MessageDigest usando MD5 em qualquer classe | SonarCloud sinaliza Critical S4790 — MD5 é quebrado para uso criptográfico |
| 19 | MessageDigest usando SHA-1 para hash de senha | SonarCloud sinaliza — SHA-1 não é adequado para senhas |
| 20 | Cipher usando DES ou AES no modo ECB | SonarCloud sinaliza Critical S5542 — algoritmos fracos |
| 21 | new Random() para gerar token de reset de senha | SonarCloud sinaliza Critical S2245 — Random é previsível para fins de segurança |
| 22 | BCryptPasswordEncoder para hash de senhas | Nenhum alerta — BCrypt é o padrão recomendado |
| 23 | MessageDigest usando SHA-256 para hash de arquivo, não de senha | Nenhum alerta — uso não-criptográfico de autenticação aceito |
| 24 | new SecureRandom() para geração de tokens | Nenhum alerta — SecureRandom é criptograficamente seguro |

---

### B-SEC-05 — Controle de Acesso e Actuator (CWE-284 / OWASP A01 / Critical — S4834)

| # | Cenário | Esperado |
|---|---|---|
| 25 | requestMatchers("/actuator/**").permitAll() para todos os endpoints | SonarCloud sinaliza Security Hotspot S4834 — triagem obrigatória |
| 26 | management.endpoints.web.exposure.include=* em application.properties | SonarCloud sinaliza Hotspot — exposição total dos endpoints de management |
| 27 | Endpoint de admin sem verificação de role no SecurityConfig ou no método | SonarCloud pode sinalizar acesso sem controle explícito |
| 28 | /actuator/health com permitAll e demais endpoints com hasRole("ADMIN") | Nenhum alerta — restrição correta |
| 29 | @PreAuthorize("hasRole('ADMIN')") em todos os endpoints de administração | Nenhum alerta — autorização declarativa no nível do método |
| 30 | GET /actuator/env sem token de autenticação | HTTP 401 — somente admin pode acessar variáveis de ambiente |
| 31 | GET /actuator/health sem autenticação | HTTP 200 — health check permanece público para o load balancer |

---

### B-SEC-06 — Logging Inseguro (CWE-117 / OWASP A09 / Major — S4792)

| # | Cenário | Esperado |
|---|---|---|
| 32 | log.info concatenando o header Authorization completo na mensagem | SonarCloud sinaliza Major S4792 — dado sensível em log |
| 33 | log.error concatenando a senha recebida em texto claro | SonarCloud sinaliza Blocker — senha não pode aparecer em log |
| 34 | log.info com input do usuário sem sanitização da string | Possível log injection via CRLF — SonarCloud sinaliza |
| 35 | log.info logando userId com placeholder seguro, sem credencial | Nenhum alerta — dado não sensível |
| 36 | Input do usuário com quebras de linha removidas antes de entrar no log | Nenhum alerta — log injection neutralizado |

---

### B-SEC-07 — CORS e Headers de Segurança (CWE-346 / OWASP A05 / Major — S5122)

| # | Cenário | Esperado |
|---|---|---|
| 37 | addAllowedOrigin("*") combinado com allowCredentials(true) | SonarCloud sinaliza Blocker S5122 — combinação proibida pela spec CORS |
| 38 | addAllowedHeader("*") aceitando qualquer header customizado | SonarCloud sinaliza Hotspot — requer triagem |
| 39 | headers.disable() no SecurityConfig removendo todos os security headers | SonarCloud sinaliza S4792 — remove proteções como X-Frame-Options e HSTS |
| 40 | setAllowedOrigins() lendo lista da variável de ambiente CORS_ALLOWED_ORIGINS | Nenhum alerta — origens explícitas via configuração externa |
| 41 | setAllowedHeaders() com lista explícita: Authorization, Content-Type, Accept, X-Requested-With | Nenhum alerta — headers definidos explicitamente |

---

## Parte 4 — Cenários e Resultado Esperado — Frontend (Angular / TypeScript)

---

### F-SEC-01 — XSS via innerHTML (CWE-79 / OWASP A03 / Blocker — S5148)

| # | Cenário | Esperado |
|---|---|---|
| 42 | [innerHTML] binding com descrição de vídeo vinda da API | SonarCloud sinaliza Blocker S5148 — XSS por binding direto no DOM |
| 43 | element.nativeElement.innerHTML recebendo conteúdo externo | SonarCloud sinaliza — acesso direto ao DOM bypassa o sanitizador do Angular |
| 44 | bypassSecurityTrustHtml() com conteúdo de origem externa | SonarCloud sinaliza Hotspot — triagem obrigatória |
| 45 | Interpolação padrão do Angular {{ video.description }} no template | Nenhum alerta — Angular escapa o conteúdo automaticamente |
| 46 | Descrição de vídeo contendo payload com script malicioso | Conteúdo exibido como texto escapado — Angular não executa o script |

---

### F-SEC-02 — Hardcoded Secrets no Frontend (CWE-259 / OWASP A02 / Blocker — S2068, S6437)

| # | Cenário | Esperado |
|---|---|---|
| 47 | Constante com API key real definida em arquivo TypeScript | SonarCloud sinaliza Blocker S2068 |
| 48 | environment.ts com campo jwtSecret contendo valor real | SonarCloud sinaliza — secrets não pertencem ao environment.ts |
| 49 | Arquivo .env com variáveis secretas commitado no repositório | SonarCloud e Gitleaks sinalizam Blocker |
| 50 | environment.ts contendo apenas apiUrl como URL pública sem credencial | Nenhum alerta — URL pública não é secret |
| 51 | Token expirado de mock em arquivo de teste | SonarCloud pode sinalizar — avaliar e marcar Won't Fix com justificativa se for token inerte |

---

### F-SEC-03 — JWT no Storage (CWE-312 / OWASP A02 / Major — Hotspot S5659)

| # | Cenário | Esperado |
|---|---|---|
| 52 | localStorage.setItem para persistir o token JWT | SonarCloud marca Hotspot — localStorage é acessível a qualquer JS na página |
| 53 | sessionStorage.setItem para guardar o token JWT | SonarCloud marca Hotspot — ainda acessível via XSS |
| 54 | Token em localStorage com arquitetura Bearer e XSS mitigado | Marcar como Acknowledged com justificativa: XSS mitigado por Angular sanitização e CSP |
| 55 | Token em HttpOnly Cookie com CSRF habilitado | Nenhum Hotspot de storage — acesso por JS impossível |

---

### F-SEC-04 — Code Injection via eval() (CWE-78 / OWASP A03 / Blocker — S5334, S1523)

| # | Cenário | Esperado |
|---|---|---|
| 56 | eval() recebendo string vinda de API ou input do usuário | SonarCloud sinaliza Blocker S5334 — eval executa qualquer string como código |
| 57 | new Function() com código dinâmico vindo de fonte externa | SonarCloud sinaliza Blocker S1523 |
| 58 | eval() usado com string literal fixa no código sem dado externo | SonarCloud sinaliza — mesmo sendo FP, eliminar o eval de toda forma |
| 59 | Lógica dinâmica implementada via switch ou mapa de funções em vez de eval | Nenhum alerta — abordagem correta e segura |

---

### F-SEC-05 — Interceptor e Vazamento de Token (CWE-522 / OWASP A07 / Critical — S4784)

| # | Cenário | Esperado |
|---|---|---|
| 60 | Interceptor adiciona Authorization header em todas as requisições sem filtro de URL | Token enviado para CDNs, analytics e serviços de terceiros — exfiltração |
| 61 | URL hardcoded como string literal no interceptor para decidir quando adicionar o header | SonarCloud sinaliza S1313 |
| 62 | req.url.startsWith(environment.apiUrl) antes de adicionar o Authorization header | Token enviado apenas para a própria API — comportamento correto |
| 63 | Requisição para fonte externa de fontes com interceptor sem filtro | Header Authorization ausente — token não vaza para terceiros |

---

### F-SEC-06 — Dependências npm com CVE (OWASP A06 / SCA — npm audit)

| # | Cenário | Esperado |
|---|---|---|
| 64 | package.json com versão de biblioteca com CVE crítico conhecido | npm audit --audit-level=critical falha no CI — merge bloqueado |
| 65 | Dependência transitiva vulnerável não atualizada | npm audit reporta moderate ou high |
| 66 | npm audit fix executado antes do merge com zero CVEs críticos | npm audit retorna sem issues críticas |
| 67 | Dependabot habilitado no repositório Angular | PRs automáticos criados ao publicar CVE em dependência usada |

---

## Parte 5 — Estratégia de Triagem, Dívida Técnica e Priorização

---

### O Processo de Triagem no SonarCloud

O SonarCloud classifica cada issue em:
- **Issues** (Vulnerabilidades detectadas automaticamente) — ficam abertas até ser corrigidas ou marcadas
- **Security Hotspots** — código que requer revisão humana. Podem ser marcados como:
  - `To Review` — padrão, ainda não analisado
  - `Acknowledged` — revisado, risco entendido e aceito (documentar justificativa)
  - `Fixed` — código foi corrigido
  - `Safe` — analisado e confirmado como não vulnerável neste contexto

**Fluxo de triagem recomendado para o VidaLongaFlix:**

```
1. SonarCloud roda no CI após cada push/PR
2. Se Quality Gate falhar → merge bloqueado
3. Developer analisa o issue:
   a. É vulnerabilidade real?    → Corrigir antes do merge
   b. É falso positivo?          → Marcar "Won't Fix" com comentário: "Falso positivo — [razão]"
   c. É risco aceito (Hotspot)?  → Marcar "Acknowledged" com comentário: "[Mitigação em vigor]"
4. Reabrir PR após resolução
5. Quality Gate passa → merge liberado
```

---

### Gestão de Dívida Técnica — Regras de Priorização

**Regra 1 — Por tipo de vulnerabilidade (recomendação do curso)**
Não comece pelas issues mais críticas em quantidade — comece pelos **tipos** que o time entende melhor:

```
Sprint 0 (setup):  Hardcoded Secrets → todos entendem que senha no código é problema
Sprint 1:          CORS misconfiguration → já mapeado em security-1.5
Sprint 2:          Actuator exposto → já mapeado em security-1.5
Sprint 3:          XSS (Angular) → [innerHTML] e interpolação
Sprint 4:          Criptografia fraca → algoritmos obsoletos
Sprint 5:          SQL Injection → parametrização de queries
Sprint 6+:         Issues de menor severidade e Security Hotspots
```

**Regra 2 — Pela matriz Severidade × Exploitabilidade**

| | Alta Exploitabilidade | Baixa Exploitabilidade |
|---|---|---|
| **Alta Severidade** | 🔴 Corrigir imediatamente | 🟠 Próximo sprint |
| **Baixa Severidade** | 🟡 2 sprints | 🟢 Backlog |

**Regra 3 — SLA por severidade (definição para o Quality Gate)**

| Severidade | SLA de Correção | Bloqueia merge? |
|---|---|---|
| Blocker | Mesmo dia | Sim |
| Critical | 5 dias úteis | Sim |
| Major | 30 dias | Não (alerta) |
| Minor | 90 dias (backlog) | Não |
| Info/Hotspot | Triagem em 15 dias | Não (até ser classificado) |

---

### Customizando Regras no SonarCloud para Reduzir Falsos Positivos

**Problema principal em open source**: SonarCloud free não tem IA para contexto — as regras são genéricas. Isso gera mais falsos positivos que ferramentas enterprise.

**Estratégia para o VidaLongaFlix:**

#### 1. Quality Profile customizado (Java)

No SonarCloud > Quality Profiles > Java > criar "VidaLongaFlix Java Security":
- Herdar do perfil base "Sonar way"
- **Desativar** regras que geram FP sistemático no projeto:
  - S2245 (`Random` usage) — se a aplicação usa Random apenas para IDs não-criptográficos, documentar e desativar
  - S1313 (hardcoded IP) — se `127.0.0.1` ou `0.0.0.0` é usado em configurações de bind local, não em lógica de negócio
- **Ativar** regras adicionais específicas:
  - S5659 (JWT não verificado) — verificar se o algoritmo JWT é validado
  - S5693 (upload sem limite de tamanho) — já houve problema no projeto (ver commit de nginx)
  - S4792 (logging inseguro) — crítico para o projeto com Grafana/Loki

#### 2. Quality Profile customizado (TypeScript/Angular)

No SonarCloud > Quality Profiles > TypeScript > criar "VidaLongaFlix TypeScript Security":
- **Ativar** S5148 (innerHTML) como Blocker — padrão é Major, mas para SPA Angular é Blocker
- **Ativar** S5334 (eval) como Blocker
- **Considerar desativar** S1186 (empty function) — gera muito ruído em componentes Angular com lifecycle hooks vazios

#### 3. `.sonarcloud.properties` — exclusões por arquivo

Excluir arquivos que geram FP sistemático sem impacto de segurança:
```properties
sonar.exclusions=**/*.spec.ts,**/*.test.ts,**/environments/environment.ts,**/mock/**
sonar.cpd.exclusions=**/*.spec.ts
```

> **Atenção:** não excluir arquivos de negócio real para "suprimir alertas". Exclusões devem ser apenas para arquivos genuinamente fora do escopo de segurança (mocks de teste, configuração de ambiente sem credenciais reais).

---

### Lidando com Falsos Positivos no SonarCloud — Prática

**Para Issues (não Hotspots):**
```
SonarCloud > Issue > "..." > "Won't Fix"
Comentário obrigatório: "[Data] FP — [razão técnica]. [Quem revisou]"

Exemplo:
"2026-04-20 FP — new Random() usado para gerar correlationId de log, não para criptografia.
 Confirmado por revisão de código. @fabricio"
```

**Para Security Hotspots:**
```
SonarCloud > Security Hotspots > Issue > "Safe" ou "Acknowledged"
Comentário: "[Data] Analisado — [mitigação em vigor]"

Exemplo para localStorage:
"2026-04-20 Acknowledged — JWT em localStorage. Risco mitigado por:
 1. Angular sanitização automática de templates (XSS prevenido)
 2. CSP header bloqueando scripts inline
 3. Auth token expira em 24h
 Revisão pendente para migração para HttpOnly Cookie (security-1.5 Sprint 7)"
```

**Métricas para monitorar evolução da triagem:**
- `Issues abertos` (total e por severidade) → deve diminuir sprint a sprint
- `Security Hotspots revisados / total` → deve aumentar para > 90%
- `Technical Debt` (dias estimados) → deve ser < 1 dia para issues novas por sprint
- `Security Rating` → meta: **A** (zero Blocker/Critical abertos)

---

## Parte 6 — Passo a Passo de Implementação

---

### Pré-requisitos

- [ ] Conta GitHub com acesso admin ao repo backend (`VidaLongaFlix`) e frontend (`vida-longa-flix`)
- [ ] Conta em `sonarcloud.io` criada com login pelo GitHub
- [ ] `./mvnw test` passando localmente no backend
- [ ] `npm test -- --watch=false` passando localmente no frontend

---

### Fase 1 — Configurar SonarCloud (conta e organização)

**Passo 1.1 — Criar conta e organização**
1. Acessar `https://sonarcloud.io` → "Log in with GitHub"
2. Autorizar o SonarCloud a acessar os repositórios
3. Criar organização: "Import an organization from GitHub" → selecionar seu perfil GitHub
4. O nome da organização será `fabriciomartinhoengeroff` (slug do GitHub)
5. Selecionar o plano "Free" → confirmar

**Passo 1.2 — Adicionar o projeto backend**
1. SonarCloud Dashboard → "+" → "Analyze new project"
2. Selecionar `FabricioMartinhoEngeroff/VidaLongaFlix`
3. Escolher "Previous version" ou "Number of days" como reference branch (recomendado: "Previous version")
4. Anotar o `Project Key` gerado: tipicamente `FabricioMartinhoEngeroff_VidaLongaFlix`

**Passo 1.3 — Gerar o token de autenticação**
1. SonarCloud → My Account (canto superior direito) → Security
2. "Generate Token" → nome: `github-actions-vidalongaflix-backend`
3. Copiar o token gerado (só aparece uma vez)
4. GitHub → repo `VidaLongaFlix` → Settings → Secrets and variables → Actions
5. "New repository secret" → nome: `SONAR_TOKEN` → valor: o token copiado

**Passo 1.4 — Desativar análise automática (importante)**
1. SonarCloud → projeto backend → Administration → Analysis Method
2. Desativar "Automatic Analysis" — usaremos GitHub Actions para controle total
3. Confirmar a desativação

---

### Fase 2 — Integrar SonarCloud no Backend (ci.yml)

**Passo 2.1 — Adicionar `sonar-project.properties` na raiz do projeto backend**

Criar o arquivo `/home/fabricio/IdeaProjects/VidaLongaFlix/sonar-project.properties`:
```properties
sonar.projectKey=FabricioMartinhoEngeroff_VidaLongaFlix
sonar.organization=fabriciomartinhoengeroff
sonar.projectName=VidaLongaFlix Backend

# Diretórios de código-fonte e testes
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.source=17

# Exclusões — arquivos sem impacto de segurança
sonar.exclusions=**/generated/**,**/*Generated*.java,**/config/SwaggerConfig.java

# Relatório de cobertura (gerado pelo Maven Surefire + JaCoCo)
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Encoding
sonar.sourceEncoding=UTF-8
```

**Passo 2.2 — Adicionar plugin JaCoCo ao `pom.xml`**

No `pom.xml`, dentro de `<build><plugins>`, adicionar:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

**Passo 2.3 — Adicionar step de SonarCloud no `ci.yml`**

No job `test` do `.github/workflows/ci.yml`, após o step "Executar testes", adicionar:

```yaml
      # 5. Análise de segurança e qualidade com SonarCloud
      - name: Análise SonarCloud
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          ./mvnw -B verify sonar:sonar \
            -Dsonar.projectKey=FabricioMartinhoEngeroff_VidaLongaFlix \
            -Dsonar.organization=fabriciomartinhoengeroff \
            -Dsonar.host.url=https://sonarcloud.io \
            -Dsonar.qualitygate.wait=true
```

> **Nota sobre `-Dsonar.qualitygate.wait=true`:** faz o step aguardar o resultado do Quality Gate e falhar o CI se o gate falhar. Sem isso, o scan é assíncrono e o CI passa independente do resultado.

**Passo 2.4 — Verificar o Quality Gate padrão**

1. SonarCloud → projeto → Quality Gate → verificar que usa "Sonar way" (padrão)
2. As condições padrão incluem:
   - No new bugs
   - No new vulnerabilities (Critical ou Blocker)
   - No new Security Hotspots unreviewed
   - Coverage on new code > 80% (ajustar conforme cobertura atual)
3. Para começar sem bloquear o pipeline por cobertura: Administration → Quality Gates → criar gate customizado "VidaLongaFlix Gate" com apenas condições de segurança:
   - `No new vulnerabilities with severity Blocker or Critical`
   - `No new Security Hotspots unreviewed`

---

### Fase 3 — Integrar SonarCloud no Frontend Angular

**Passo 3.1 — Acessar o repo frontend e adicionar o projeto no SonarCloud**

1. SonarCloud → "+" → "Analyze new project"
2. Selecionar `FabricioMartinhoEngeroff/vida-longa-flix`
3. Anotar o `Project Key`: `FabricioMartinhoEngeroff_vida-longa-flix`
4. Desativar Automatic Analysis (igual ao backend)

**Passo 3.2 — Gerar token para o frontend**

1. SonarCloud → My Account → Security
2. "Generate Token" → nome: `github-actions-vidalongaflix-frontend`
3. GitHub → repo `vida-longa-flix` → Settings → Secrets → `SONAR_TOKEN`

**Passo 3.3 — Criar `sonar-project.properties` na raiz do repo Angular**

```properties
sonar.projectKey=FabricioMartinhoEngeroff_vida-longa-flix
sonar.organization=fabriciomartinhoengeroff
sonar.projectName=VidaLongaFlix Frontend

# Código-fonte Angular
sonar.sources=src
sonar.tests=src
sonar.test.inclusions=**/*.spec.ts
sonar.exclusions=**/*.spec.ts,node_modules/**,dist/**,coverage/**,.angular/**

# Relatório de cobertura (gerado pelo Angular test runner com Istanbul)
sonar.typescript.lcov.reportPaths=coverage/vida-longa-flix/lcov.info
sonar.javascript.lcov.reportPaths=coverage/vida-longa-flix/lcov.info

sonar.sourceEncoding=UTF-8
```

**Passo 3.4 — Configurar cobertura no Angular**

No `angular.json`, dentro de `"test"."options"`, adicionar:
```json
"codeCoverage": true,
"codeCoverageExclude": [
  "src/environments/**",
  "src/main.ts",
  "src/polyfills.ts"
]
```

**Passo 3.5 — Criar/atualizar CI do repo Angular**

Se não existir `.github/workflows/ci.yml` no repo Angular, criar:
```yaml
name: CI — Frontend

on:
  push:
    branches: [ "main", "feat/*" ]
  pull_request:
    branches: [ "main" ]

jobs:
  test-and-scan:
    name: Testes + SonarCloud
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # necessário para SonarCloud analisar histórico completo

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Instalar dependências
        run: npm ci

      - name: Executar testes com cobertura
        run: npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage

      - name: Análise SonarCloud
        uses: SonarSource/sonarcloud-github-action@master
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

> **Nota sobre `fetch-depth: 0`:** o SonarCloud precisa do histórico completo do git para calcular "new code" (issues em código novo vs código antigo). Sem isso, o Quality Gate não funciona corretamente.

---

### Fase 4 — Verificar e Ajustar após Primeiro Scan

**Passo 4.1 — Analisar o resultado do primeiro scan**

Após o primeiro push com as mudanças acima:
1. Acessar SonarCloud → projeto backend → Issues
2. Filtrar por: Severity = Blocker, Critical → corrigir imediatamente
3. Filtrar por: Severity = Major → planejar para próximo sprint
4. Acessar Security Hotspots → triar todos os pendentes

**Passo 4.2 — Configurar PR Decoration**

O SonarCloud já decora PRs automaticamente se:
- O `GITHUB_TOKEN` está presente no step (já configurado acima)
- A análise é disparada por `pull_request` event (já no trigger)

Resultado: cada PR terá um comentário do SonarCloud com issues encontradas no código novo do PR.

**Passo 4.3 — Configurar notificações**

SonarCloud → My Account → Notifications:
- "New issues" → ativar para ser notificado por email de issues novas
- Ou integrar com Slack via Webhook em Administration → Webhooks

---

### Fase 5 — Manutenção Contínua e Evolução

**Checklist semanal (manutenção mínima):**
- [ ] Verificar novos Security Hotspots não triados
- [ ] Verificar se Security Rating ainda está em A
- [ ] Revisar issues Major abertas há mais de 15 dias

**Checklist por sprint:**
- [ ] Revisar tendência de Technical Debt (subindo ou descendo?)
- [ ] Atualizar regras do Quality Profile se houver novos FP sistemáticos
- [ ] Verificar se há regras novas publicadas pelo SonarCloud que se aplicam ao projeto

**Checklist por trimestre:**
- [ ] Revisar todas as issues marcadas como `Won't Fix` — ainda fazem sentido?
- [ ] Revisar Security Hotspots marcados como `Acknowledged` — o contexto mudou?
- [ ] Avaliar upgrade do Quality Gate (adicionar condição de cobertura mínima)

---

## Apêndice — Referência Rápida de Regras SonarCloud

### Java (Backend)

| Regra | Categoria | Severidade | Descrição |
|---|---|---|---|
| S2068 | Hardcoded credentials | Blocker | Senha ou secret literal no código |
| S6437 | Hardcoded secrets | Blocker | API key, token ou credencial no código |
| S3649 | SQL Injection | Blocker | Query SQL com concatenação de string |
| S4790 | Weak hash | Critical | MD5, SHA-1 usados para criptografia |
| S5542 | Weak cipher | Critical | DES, AES/ECB ou RC4 |
| S2245 | Insecure random | Critical | `new Random()` para contextos de segurança |
| S5659 | JWT not verified | Blocker | JWT sem verificação de assinatura |
| S5122 | CORS misconfiguration | Blocker | `allowedOrigin("*")` com credentials |
| S4834 | Access control | Critical | Endpoint sem verificação de autorização |
| S6096 | Path traversal | Critical | Caminho de arquivo construído com input do usuário |
| S4792 | Logging sensitive data | Major | Dado sensível escrito em log |
| S5693 | File size limit | Major | Upload sem verificação de tamanho |

### TypeScript/Angular (Frontend)

| Regra | Categoria | Severidade | Descrição |
|---|---|---|---|
| S2068 | Hardcoded credentials | Blocker | Secret literal em TypeScript |
| S6437 | Hardcoded secrets | Blocker | API key ou token no código |
| S5148 | Angular innerHTML | Blocker | `[innerHTML]` com dado dinâmico sem sanitização |
| S5334 | eval() usage | Blocker | `eval()` com dado externo |
| S1523 | Dynamic code execution | Blocker | `new Function()` dinâmico |
| S4784 | Hard-coded URI | Major | URL literal fora de `environment.ts` |
| S1313 | Hard-coded IP | Minor | IP literal no código de negócio |
| S5659 | Insecure JWT | Major | JWT sem verificação adequada |

---

## Resumo dos Sprints de Implementação

| Sprint | O que fazer | Onde | Resultado |
|---|---|---|---|
| **Sprint 0** | Criar conta SonarCloud + configurar projetos | SonarCloud + GitHub Secrets | Projetos criados, tokens configurados |
| **Sprint 1** | `sonar-project.properties` + JaCoCo no backend | Backend repo | Primeiro scan rodando |
| **Sprint 2** | step SonarCloud no `ci.yml` backend com `qualitygate.wait=true` | `.github/workflows/ci.yml` | Quality Gate ativo no pipeline |
| **Sprint 3** | `sonar-project.properties` + CI no repo Angular | Frontend repo | Frontend sendo analisado |
| **Sprint 4** | Triar todos os Blockers e Criticals do primeiro scan | SonarCloud dashboard | Security Rating chegando a A |
| **Sprint 5** | Quality Profile customizado — desativar FP sistemáticos | SonarCloud Quality Profiles | Ruído reduzido |
| **Sprint 6** | Quality Gate customizado — adicionar condição de cobertura | SonarCloud Quality Gates | Gate completo: segurança + qualidade |
| **Sprint 7** | Corrigir issues identificadas nos grupos B-SEC-01 a B-SEC-07 | Código backend + frontend | Zero Blocker/Critical em produção |