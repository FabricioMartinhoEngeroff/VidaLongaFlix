# Segurança de APIs — OWASP API Top 10 no VidaLongaFlix

> Baseado no curso "Segurança de APIs — OWASP API Top 10, BOLA, Autenticação e Controle de Autorização".
> Este documento descreve: fundamentos de segurança de APIs e arquiteturas, OWASP API Top 10 (2023) completo, vulnerabilidades reais mapeadas no projeto, cenários de teste com resultado esperado (positivos e negativos) para backend e frontend, e passo a passo completo de implementação por sprint.

---

## Parte 1 — Fundamentos de Segurança de APIs

---

### O que é uma API e por que segurança de APIs é um domínio próprio

Uma **API (Application Programming Interface)** é um contrato de comunicação entre sistemas. No contexto de aplicações web modernas, APIs REST expõem dados e operações de negócio diretamente via HTTP — o que as torna alvos de ataque tão relevantes quanto a interface do usuário.

A segurança de APIs é um domínio separado da segurança web tradicional porque:
- APIs expõem **lógica de negócio diretamente**, não apenas dados HTML
- Erros de autorização em APIs permitem acesso a **dados de outros usuários** sem explorar vulnerabilidades técnicas complexas
- APIs públicas são **documentadas e previsíveis** (Swagger, OpenAPI) — o atacante sabe exatamente o que chamar
- O volume de tráfego em APIs dificulta distinguir uso legítimo de abuso automatizado

O OWASP mantém uma lista separada para APIs (API Top 10) justamente porque as ameaças mais críticas em APIs **não aparecem com destaque** no Web Top 10 tradicional.

---

### Tipos de Exposição de APIs

O curso classifica as APIs em três tipos de acordo com quem pode consumi-las:

| Tipo | Quem acessa | Controle de acesso | Exemplo no projeto |
|---|---|---|---|
| **Privada** | Somente serviços internos da própria empresa | Sem exposição pública — rede interna | Comunicação entre microserviços (se houver) |
| **Parceira** | Clientes B2B com contrato (ex: parceiros de integração) | API Key + contrato formal | Eventual integração com gateway de pagamento |
| **Pública** | Qualquer pessoa na internet | Autenticação + rate limiting obrigatórios | `/api/auth/login`, `/api/videos` — VidaLongaFlix |

O VidaLongaFlix expõe APIs **públicas** via `https://api.vidalongaflix.com.br/api`. Isso exige que **todos** os controles de segurança de API sejam aplicados.

---

### Arquiteturas de API — REST, SOAP, GraphQL e gRPC

O curso apresenta as quatro principais arquiteturas e seus perfis de segurança:

#### REST (Representational State Transfer)
- **Protocolo**: HTTP/HTTPS
- **Formato**: JSON ou XML
- **Contrato**: OpenAPI / Swagger (opcional mas recomendado)
- **Vantagens**: Simples, stateless, amplamente suportado, fácil de testar
- **Riscos de segurança típicos**: BOLA/IDOR em IDs de recurso na URL, endpoint enumeration, falta de versionamento cria shadow APIs
- **Usado no VidaLongaFlix**: Sim — backend Spring Boot expõe REST

#### SOAP (Simple Object Access Protocol)
- **Protocolo**: HTTP ou outros, envelope XML obrigatório
- **Contrato**: WSDL — contrato formal e fortemente tipado
- **Vantagens**: Contrato explícito e rigoroso, suporte nativo a WS-Security
- **Riscos de segurança típicos**: XML injection, XXE (XML External Entity), WSDL enumeration expõe todas as operações
- **Usado no VidaLongaFlix**: Não — mas conceito relevante para integrações corporativas

#### GraphQL
- **Protocolo**: HTTP (único endpoint `/graphql`)
- **Formato**: JSON com linguagem de query própria
- **Vantagens**: Cliente escolhe exatamente os campos que quer (reduz overfetching)
- **Riscos de segurança típicos**: Introspection expõe o schema completo, queries aninhadas causam DoS por complexidade (N+1), ausência de rate limiting por query, campo `__schema` deve ser desativado em produção
- **Usado no VidaLongaFlix**: Não — potencial evolução futura

#### gRPC (Google Remote Procedure Call)
- **Protocolo**: HTTP/2
- **Formato**: Protocol Buffers (binário)
- **Vantagens**: Altíssima performance, tipagem forte, suporte nativo a streaming
- **Riscos de segurança típicos**: Difícil de inspecionar com proxies tradicionais (Burp Suite requer plugin), proto files expostos revelam a interface
- **Usado no VidaLongaFlix**: Não — relevante para comunicação entre microserviços se o projeto evoluir

---

### OWASP API Top 10 (2023) — Completo

O OWASP API Top 10 é o padrão de referência para vulnerabilidades em APIs REST. A versão 2023 é a mais atual e difere significativamente do Web Top 10 (2021):

| # | Categoria | Descrição resumida | CWE relacionado |
|---|---|---|---|
| **API1:2023** | Broken Object Level Authorization (BOLA) | Falta de verificação de propriedade do recurso na leitura/escrita | CWE-284, CWE-639 |
| **API2:2023** | Broken Authentication | Autenticação fraca, tokens previsíveis, sem proteção contra brute force | CWE-287, CWE-307 |
| **API3:2023** | Broken Object Property Level Authorization | Excesso de dados expostos na resposta (Excess Data Exposure) ou aceitos na entrada (Mass Assignment) | CWE-213, CWE-915 |
| **API4:2023** | Unrestricted Resource Consumption | Sem rate limiting, sem paginação, requisições ilimitadas causam DoS | CWE-400, CWE-770 |
| **API5:2023** | Broken Function Level Authorization | Funções administrativas acessíveis por usuários não-admin | CWE-285, CWE-732 |
| **API6:2023** | Unrestricted Access to Sensitive Business Flows | Abuso de fluxos de negócio (registro em massa, cancelamento de fila de outro usuário) | CWE-841 |
| **API7:2023** | Server Side Request Forgery (SSRF) | API faz requisição a URL fornecida pelo usuário, atingindo serviços internos | CWE-918 |
| **API8:2023** | Security Misconfiguration | CORS permissivo, Actuator exposto, headers de segurança ausentes, mensagens de erro verbose | CWE-16, CWE-693 |
| **API9:2023** | Improper Inventory Management | APIs legadas ou de debug expostas sem documentação ou controle | CWE-1059 |
| **API10:2023** | Unsafe Consumption of APIs | Backend consome API externa sem validar resposta — vulnerável a supply chain attacks | CWE-20, CWE-116 |

---

### API1:2023 em Detalhe — BOLA (Broken Object Level Authorization)

BOLA é a vulnerabilidade **número 1 em APIs** segundo o OWASP e é considerada a mais prevalente e fácil de explorar. Também é conhecida como **IDOR (Insecure Direct Object Reference)** no contexto do Web Top 10.

**Conceito central**: quando um endpoint recebe um ID de recurso como parâmetro, o backend deve verificar se o usuário autenticado tem permissão para operar sobre aquele recurso específico. Se não verificar, qualquer usuário pode usar o ID de outro para ler, modificar ou deletar seus dados.

**Exemplo do curso — lab prático**:
```
Usuário A:  DELETE /api/comments/550e8400-e29b-41d4-a716-446655440000
            Authorization: Bearer <token do Usuário B>
            → Resultado sem BOLA: HTTP 204 — comment do Usuário A deletado pelo Usuário B
            → Resultado correto:  HTTP 403 — "Você não tem permissão para deletar este comentário"
```

**Como o ataque acontece na prática**:
1. Usuário B faz login legítimo e obtém token JWT
2. Usuário B intercepta uma requisição sua com Burp Suite
3. Troca o ID do seu próprio recurso pelo ID de outro usuário
4. Se o backend não verificar ownership, a operação é executada com sucesso

**IDs inseguros vs UUIDs seguros**:

| Tipo de ID | Exemplo | Risco | Recomendação |
|---|---|---|---|
| Auto-increment numérico | `DELETE /comments/42` | Atacante enumera facilmente: 1, 2, 3... | **Não usar como ID de API** |
| GUID/UUID v4 aleatório | `DELETE /comments/550e8400-e29b-41d4-a716-446655440000` | Não enumerável — impossível de adivinhar | **Padrão recomendado** |
| Hash curta previsível | `DELETE /comments/a3f2` | Espaço pequeno — brute force viável | Não usar |

O VidaLongaFlix **já usa UUIDs** em todos os recursos (User, Video, Comment, Favorite) — isso é correto e reduz o risco de enumeração. Porém UUID não substitui a verificação de ownership: um atacante que conhece um UUID válido (via log, resposta pública, etc.) ainda pode executar BOLA.

**Correção padrão para BOLA**:
1. Recuperar o recurso pelo ID fornecido
2. Comparar o campo `owner` / `userId` do recurso com o usuário autenticado (`@AuthenticationPrincipal`)
3. Se não for o dono (e não for admin), retornar `403 Forbidden`

---

### API3:2023 — Mass Assignment e Excess Data Exposure

**Excess Data Exposure** ocorre quando a resposta da API inclui mais campos do que o necessário. O cliente filtra visualmente, mas um atacante interceptando a resposta obtém todos os dados.

**Mass Assignment** (o inverso) ocorre quando o backend aceita qualquer campo no corpo da requisição e mapeia diretamente para a entidade — permitindo que o cliente altere campos que não deveria, como `role`, `status` ou `isAdmin`.

**Exemplo de Mass Assignment**:
```
POST /api/users
{
  "email": "hacker@evil.com",
  "password": "123456",
  "role": "ADMIN"     ← campo que não deveria ser aceito via API pública
}
→ Se o backend mapeia direto para User.class, o atacante vira admin
```

**Correção**: usar DTOs de entrada (Request) separados das entidades de domínio. Os campos aceitos na API são apenas os do DTO. O VidaLongaFlix já usa `RegisterRequestDTO` e `UserRequestDTO` — mas é necessário verificar que nenhum campo sensível como `role` ou `status` está incluído neles.

---

### API4:2023 — Unrestricted Resource Consumption

Sem limites em endpoints de listagem, qualquer cliente pode:
- Solicitar `GET /videos?limit=999999` — retorna todo o catálogo em memória
- Fazer 10.000 requests/segundo para `/auth/login` — brute force ou DoS
- Encadear requests de analytics que fazem full table scan no banco

**Controles necessários**:
- Rate limiting por IP ou por usuário (Bucket4j — já presente no projeto)
- Limites de paginação obrigatórios com valor máximo definido (ex: max 100 itens por página)
- Timeout em chamadas externas (RestTemplate sem timeout — já mapeado em security-1.5)

---

### API5:2023 — Broken Function Level Authorization

Enquanto BOLA se refere a **qual objeto** o usuário acessa (nível de dado), BFLA se refere a **qual função** o usuário pode chamar (nível de endpoint).

**Exemplo**:
- `DELETE /api/users/{id}` — deveria exigir `ROLE_ADMIN`, mas `UserController` não declara `@PreAuthorize`
- `GET /api/admin/waitlist` — se acessível sem verificação de role

**Diferença chave**: BOLA = "usuário A acessa dados do usuário B" / BFLA = "usuário comum chama função de admin"

---

### Burp Suite como Ferramenta de Teste DAST para APIs

O curso apresenta o **Burp Suite Community Edition** como a ferramenta padrão para teste manual de APIs (DAST):

**Funcionalidades principais para teste de APIs**:
1. **Proxy Intercept**: intercepta e modifica requisições HTTP em tempo real antes de enviá-las ao servidor
2. **Repeater**: repete e modifica requisições isoladas — ideal para testar BOLA trocando IDs
3. **Intruder**: automação de payloads — testa múltiplos IDs ou senhas em sequência
4. **Scanner** (Pro): descobre automaticamente endpoints e vulnerabilidades
5. **Decoder**: codifica/decodifica Base64, URL encode — útil para analisar JWTs

**Fluxo de teste de BOLA com Burp Suite**:
1. Fazer login como Usuário A → capturar token JWT no Burp
2. Criar recurso (ex: comentário) como Usuário A → capturar o ID na resposta
3. Fazer login como Usuário B → capturar token JWT do Usuário B
4. No Repeater: enviar `DELETE /comments/{ID do comentário do Usuário A}` com token do Usuário B
5. Se retornar `204 No Content` → BOLA confirmada. Se retornar `403 Forbidden` → controle funciona

**Posição no SSDLC**: Burp Suite é uma ferramenta DAST — atua na fase de **Staging / Pré-produção**, após o código estar rodando. Complementa o SAST (SonarCloud) que atua no código estático.

---

### Resumo — OWASP API Top 10 vs Web Top 10

| Categoria | Presente no Web Top 10 (2021) | Presente no API Top 10 (2023) | Observação |
|---|---|---|---|
| Broken Access Control | ✅ A01 (genérico) | ✅ API1 (BOLA), API5 (BFLA) | APIs especializam em dois subtipos distintos |
| Injection (SQL, XSS) | ✅ A03 | ⚠️ Implícito (API8) | Web foca mais — APIs menos expostas a XSS |
| Cryptographic Failures | ✅ A02 | ⚠️ Implícito (API2) | JWT fraco cobre parcialmente |
| Security Misconfiguration | ✅ A05 | ✅ API8 | Em APIs: CORS, headers, Actuator, verbose errors |
| Vulnerable Components | ✅ A06 | ⚠️ Implícito (API10) | API10 foca consumo inseguro de APIs externas |
| **Mass Assignment** | ❌ Não listado | ✅ API3 | Crítico em APIs — não aparece no Web Top 10 |
| **BOLA/IDOR** | ⚠️ Mencionado em A01 | ✅ API1 — número 1 | Em APIs é a vulnerabilidade mais frequente |
| **Unrestricted Consumption** | ❌ Não listado | ✅ API4 | Rate limiting e paginação — específico de APIs |
| **Business Flow Abuse** | ❌ Não listado | ✅ API6 | Registro em massa, scraping de catálogo |
| SSRF | ✅ A10 | ✅ API7 | Presente em ambos |

---

## Parte 2 — Vulnerabilidades Reais Identificadas no Projeto

### Tabela Mestre — Backend (Spring Boot / Java 17)

| ID | Arquivo / Endpoint | Vulnerabilidade | OWASP API | CWE | Severidade | Detectável por SAST |
|---|---|---|---|---|---|---|
| BA1 | `CommentController.java:41` | `DELETE /comments/{commentId}` sem verificação de ownership — qualquer usuário deleta comentário alheio | API1:2023 BOLA | CWE-639 | Critical | ⚠️ Parcial (hotspot S4834) |
| BA2 | `UserController.java:40` | `PUT /users/{id}` sem verificação de ownership — qualquer usuário atualiza dados alheios | API1:2023 BOLA | CWE-284 | Critical | ⚠️ Parcial |
| BA3 | `UserController.java:47` | `DELETE /users/{id}` sem role check — qualquer usuário autenticado deleta qualquer conta | API5:2023 BFLA | CWE-285 | Critical | ✅ Regra S4834 |
| BA4 | `UserController.java:28` | `GET /users/{id}` expõe perfil completo de qualquer usuário — sem filtro de campos sensíveis | API3:2023 Excess Data | CWE-213 | Major | ⚠️ Hotspot |
| BA5 | `AuthController.java:48` | `/auth/login` sem rate limiting — brute force de senhas | API2:2023 / API4:2023 | CWE-307 | Critical | ⚠️ Hotspot (já mapeado) |
| BA6 | `VideoController.java:42` | `/videos/most-viewed?limit=N` sem valor máximo — `limit=999999` retorna tudo em memória | API4:2023 | CWE-770 | Major | ⚠️ Hotspot |
| BA7 | `CommentService.java:70` | `getCommentsByUser(userId)` acessível sem verificação se o caller é o próprio usuário | API1:2023 BOLA | CWE-284 | Major | ⚠️ Parcial |
| BA8 | `AuthController.java:86` | `DELETE /auth/waitlist/me?email=X` — qualquer pessoa cancela fila de qualquer usuário por email | API6:2023 | CWE-841 | Major | ❌ Lógico |
| BA9 | `UserController.java` | Ausência de `@PreAuthorize` em endpoints sensíveis — autorização só no SecurityConfig | API5:2023 BFLA | CWE-732 | Major | ✅ Regra S4834 |
| BA10 | `GlobalExceptionHandler.java` | Stack trace completo retornado em exceções não tratadas — revela estrutura interna da API | API8:2023 | CWE-209 | Major | ✅ Regra S4792 |
| BA11 | `RegisterRequestDTO.java` | Verificar se `role` ou `status` são campos aceitos no DTO de registro (Mass Assignment) | API3:2023 Mass Assign | CWE-915 | Critical | ⚠️ Parcial |
| BA12 | Todos os endpoints de listagem | Ausência de paginação obrigatória — `/videos`, `/categories`, `/menus` retornam coleção completa | API4:2023 | CWE-400 | Major | ❌ Lógico |

---

### Tabela Mestre — Frontend (Angular / TypeScript)

| ID | Arquivo / Componente | Vulnerabilidade | OWASP API | CWE | Severidade | Detectável por SAST |
|---|---|---|---|---|---|---|
| FA1 | Qualquer service Angular | Frontend passando ID de outro usuário na URL para explorar BOLA do backend | API1:2023 BOLA | CWE-284 | Critical | ❌ Comportamental |
| FA2 | `user.service.ts` / componentes | Exibindo todos os campos da resposta sem filtrar dados sensíveis do backend | API3:2023 Excess Data | CWE-213 | Major | ⚠️ Hotspot |
| FA3 | Formulários de criação/edição | Enviando campos extras no body (ex: `role`, `isAdmin`) além do necessário | API3:2023 Mass Assign | CWE-915 | Major | ⚠️ Parcial |
| FA4 | `auth.interceptor.ts` | Token enviado para todas as URLs sem filtro — vaza para CDN, analytics, terceiros | API2:2023 | CWE-522 | Critical | ✅ Regra S4784 |
| FA5 | Componentes de lista | Sem paginação implementada — carrega todo o catálogo em uma única chamada | API4:2023 | CWE-400 | Major | ❌ Lógico |
| FA6 | Tratamento de erros HTTP | Sem handler específico para 401 — token expirado não é tratado, usuário fica em loop | API2:2023 | CWE-287 | Major | ⚠️ Hotspot |
| FA7 | `environment.ts` | URL da API hardcoded em múltiplos serviços em vez de centralizado em `environment.apiUrl` | API8:2023 | CWE-1104 | Minor | ✅ Regra S1313 |
| FA8 | Componente de cancelamento de fila | Envia email de outro usuário no param da requisição — explora API6:2023 no backend | API6:2023 | CWE-841 | Major | ❌ Comportamental |

---

### Mapa de Severidade para APIs — Referência para Security Gate

| Severidade | Definição no contexto de APIs | Ação | Prazo |
|---|---|---|---|
| **Critical** | BOLA confirmado, BFLA sem role check, brute force sem proteção | Bloqueia merge imediatamente | Mesmo sprint |
| **Major** | Excess data exposure, rate limiting ausente, stack trace em erro, sem paginação | Alerta no PR — não bloqueia | 1-2 sprints |
| **Minor** | URL hardcoded, log verbose, documentação de API desatualizada | Informativo | Backlog |
| **Hotspot** | Código que pode ser BOLA dependendo do contexto — requer triagem humana | Triagem obrigatória antes de mergar | Mesmo sprint |

---

## Parte 3 — Cenários e Resultado Esperado — Backend (Spring Boot / Java)

> Positivo = comportamento seguro e correto. Negativo = comportamento inseguro que deve ser detectado por SAST, DAST (Burp Suite) ou teste automatizado. Triagem = requer revisão humana antes de mergar.

---

### B-API-01 — BOLA em Comentários (CWE-639 / API1:2023 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 1 | Usuário B envia `DELETE /comments/{id do comentário do Usuário A}` com token válido | HTTP 403 — backend verifica ownership antes de deletar |
| 2 | `CommentService.delete()` não compara `comment.getUser().getId()` com o usuário autenticado | BOLA confirmada — deletar funciona sem ser dono |
| 3 | Usuário B envia `DELETE /comments/{commentId}` com token expirado | HTTP 401 — token inválido rejeitado antes da verificação de ownership |
| 4 | Usuário A deleta seu próprio comentário com token válido | HTTP 204 — comportamento correto |
| 5 | Admin deleta comentário de qualquer usuário com role ADMIN | HTTP 204 — admin tem permissão ampla |
| 6 | `CommentController.deleteComment` recebe `@AuthenticationPrincipal User user` e passa para o service | Nenhum alerta de BOLA — ownership verificado no service |
| 7 | Burp Suite: intercepta `DELETE /comments/UUID-A`, troca UUID pelo de outro comentário | HTTP 403 se ownership implementado; HTTP 204 se BOLA presente |

---

### B-API-02 — BOLA em Usuários (CWE-284 / API1:2023 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 8 | Usuário B envia `PUT /users/{id do Usuário A}` com seus próprios dados | HTTP 403 — usuário comum só atualiza a si mesmo |
| 9 | Usuário B envia `DELETE /users/{id do Usuário A}` com token válido | HTTP 403 — deleção de outra conta não permitida sem ser admin |
| 10 | Usuário A envia `PUT /users/{próprio id}` com dados válidos | HTTP 200 — owner pode atualizar sua própria conta |
| 11 | Admin envia `DELETE /users/{qualquer id}` com role ADMIN | HTTP 204 — admin pode remover qualquer conta |
| 12 | `UserController.updateUser` sem verificação de ownership | SonarCloud marca Hotspot — verificação manual necessária |
| 13 | `UserController.updateUser` recebe `@AuthenticationPrincipal` e verifica `id.equals(user.getId()) || isAdmin` | Nenhum alerta de BOLA — controle correto |
| 14 | `GET /users/{id de outro usuário}` retorna todos os campos incluindo role e status | Excess Data Exposure — API3:2023 — retornar DTO com campos filtrados conforme role do caller |

---

### B-API-03 — Broken Function Level Authorization (CWE-285 / API5:2023 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 15 | `DELETE /users/{id}` acessível por usuário com role USER sem `@PreAuthorize` | SonarCloud sinaliza Hotspot S4834 — ausência de declaração de autorização |
| 16 | `GET /admin/waitlist` acessível sem role check declarado no controller | HTTP 403 se SecurityConfig cobre; HTTP 200 se não cobre — BFLA |
| 17 | `@PreAuthorize("hasRole('ADMIN')")` em todos os endpoints de deleção de usuário | Nenhum alerta — autorização declarativa explícita |
| 18 | `requestMatchers("/admin/**").hasRole("ADMIN")` no SecurityConfig | Proteção presente — mas requer verificação de que todos os endpoints admin usam esse prefixo |
| 19 | Endpoint de import de CSV (`/import`) acessível sem role check | HTTP 403 deveria ser retornado para usuários comuns — BFLA |
| 20 | `ImportController` sem `@PreAuthorize` ou mapeamento no SecurityConfig | SonarCloud sinaliza Hotspot — revisar autorização do endpoint |

---

### B-API-04 — Mass Assignment (CWE-915 / API3:2023 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 21 | `POST /auth/register` com body incluindo `"role": "ADMIN"` | HTTP 201 com role USER — campo role ignorado pelo RegisterRequestDTO |
| 22 | `RegisterRequestDTO` contém campo `role` como record component | SonarCloud pode sinalizar — campo sensível em DTO de entrada público |
| 23 | `PUT /users/{id}` com body incluindo `"status": "DISABLED"` | Status ignorado — UserRequestDTO não aceita campo status |
| 24 | Backend usa `UserRequestDTO` sem campos role ou status para aceitar entrada da API | Nenhum alerta — separação correta de DTOs |
| 25 | Controller mapeia direto da entidade User para aceitar input da API sem DTO | Risco de Mass Assignment — qualquer campo da entidade pode ser sobrescrito |

---

### B-API-05 — Unrestricted Resource Consumption (CWE-770 / API4:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 26 | `GET /videos/most-viewed?limit=999999` sem validação de valor máximo | Consulta retorna todos os vídeos — potencial estouro de memória |
| 27 | `@RequestParam(defaultValue = "10") int limit` sem `@Max(100)` ou similar | SonarCloud pode sinalizar Hotspot — parâmetro ilimitado |
| 28 | `@RequestParam @Max(100) int limit` validando o valor máximo | Nenhum alerta — limite definido explicitamente |
| 29 | `POST /auth/login` sem rate limiting — 1000 tentativas por segundo | Brute force possível — Bucket4j deve estar configurado neste endpoint |
| 30 | Bucket4j configurado para limitar `/auth/login` a 5 tentativas por minuto por IP | HTTP 429 Too Many Requests na 6ª tentativa — comportamento correto |
| 31 | `GET /videos` retorna catálogo completo sem paginação obrigatória | Sem Page/Pageable — toda a coleção em memória para requisições automatizadas |
| 32 | `GET /videos` com `Pageable` obrigatório e tamanho máximo de página definido | Nenhum alerta — controle de consumo implementado |

---

### B-API-06 — Unrestricted Access to Sensitive Business Flows (CWE-841 / API6:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 33 | `DELETE /auth/waitlist/me?email=victim@vidalongaflix.com.br` por qualquer pessoa sem autenticação | Cancelamento da fila da vítima — fluxo de negócio sem proteção de ownership |
| 34 | `DELETE /auth/waitlist/me` exige autenticação e cancela apenas a fila do usuário autenticado | HTTP 403 se email não corresponde ao usuário logado — comportamento correto |
| 35 | `POST /auth/register` sem CAPTCHA ou rate limiting — criação de 10.000 contas em loop | Contas falsas criadas em massa — necessidade de rate limiting no registro |
| 36 | `POST /auth/register` com rate limiting de 3 tentativas por IP por hora | HTTP 429 na 4ª tentativa — fluxo protegido contra abuso |
| 37 | Script automatizado fazendo scraping de todos os vídeos via `GET /videos` em loop | Sem rate limiting global — API consumida ilimitadamente por bots |

---

### B-API-07 — Excess Data Exposure nas Respostas (CWE-213 / API3:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 38 | `GET /users/{id}` retorna `UserDTO` com campos `role`, `status`, `createdAt` visíveis para qualquer usuário autenticado | Dados sensíveis expostos — filtrar campos conforme role do caller |
| 39 | `UserDTO` retornado para usuário comum inclui campo `role: "ROLE_USER"` | Funcional mas recomendado ocultar — usuário não precisa ver sua própria role via DTO público |
| 40 | `GET /auth/me` retorna dados do próprio usuário com todos os campos do UserResponseDTO | Aceitável para o próprio usuário — mas não deve incluir hash de senha |
| 41 | `UserResponseDTO` ou `UserDTO` não incluem campo `password` (hash BCrypt) | Nenhum alerta — hash de senha nunca sai do backend |
| 42 | Stack trace de NullPointerException retornado no body da resposta HTTP 500 | Informação interna exposta — GlobalExceptionHandler deve retornar mensagem genérica sem stack trace |
| 43 | `GlobalExceptionHandler` retorna `{"error": "Internal server error", "traceId": "..."}` sem stack trace | Nenhum alerta — resposta de erro não revela estrutura interna |

---

### B-API-08 — Security Misconfiguration em APIs (CWE-16 / API8:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 44 | `management.endpoints.web.exposure.include=*` expõe `/actuator/env` com variáveis de ambiente | Credenciais de banco e JWT secret visíveis — Critical |
| 45 | `/actuator/health` público, demais endpoints do actuator protegidos por `ROLE_ADMIN` | Nenhum alerta — configuração correta de exposição mínima |
| 46 | CORS configurado com `addAllowedHeader("*")` aceitando qualquer header | SonarCloud sinaliza Hotspot S5122 — headers devem ser explícitos |
| 47 | Ausência de versionamento na API — `/api/videos` sem prefixo de versão `/api/v1/videos` | API9:2023 — endpoint legado pode ficar exposto sem inventário |
| 48 | Swagger/OpenAPI habilitado em produção sem autenticação — schema completo acessível publicamente | Exposição do inventário da API — endpoint `/v3/api-docs` deve exigir auth em prod |
| 49 | `springdoc.api-docs.enabled=false` ou protegido por role em application-prod.properties | Nenhum alerta — documentação protegida em produção |

---

### B-API-09 — Improper Inventory Management (CWE-1059 / API9:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 50 | `/api/import` endpoint disponível em produção sem documentação formal no Swagger | Shadow API — endpoint de admin não inventariado |
| 51 | Endpoint de importação CSV acessível sem restrição de ambiente (não bloqueado em prod) | Risco operacional — importação de dados em produção sem controle |
| 52 | `@Profile("dev")` aplicado nos endpoints de debug e import | Nenhum alerta — endpoint não existe no artefato de produção |
| 53 | Ausência de header `X-API-Version` nas respostas — cliente não sabe qual versão está consumindo | API9:2023 — necessidade de gestão explícita de versões |
| 54 | Resposta inclui header `X-API-Version: 1.0` e documentação de deprecação para versões antigas | Nenhum alerta — inventário controlado |

---

### B-API-10 — Unsafe Consumption of APIs Externas (CWE-20 / API10:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 55 | `WhatsAppService` usa `RestTemplate` sem timeout — API externa com delay de 60s bloqueia thread | DoS por thread starvation — SonarCloud sinaliza Major S2755 (já mapeado) |
| 56 | Resposta da API do WhatsApp retorna payload JSON sem validação de campos obrigatórios antes de usar | NullPointerException em produção se API externa muda o schema |
| 57 | `RestTemplate` substituído por `RestClient` (Spring Boot 3.2+) com `connectTimeout` e `readTimeout` configurados | Nenhum alerta — timeout evita thread blocking |
| 58 | Payload da API externa validado com `@Valid` ou verificação explícita de campos nulos antes de processar | Nenhum alerta — consumo defensivo |
| 59 | Circuit breaker (Resilience4j) configurado para a chamada ao WhatsApp — falha na API externa não cascateia | Nenhum alerta — padrão de resiliência correto |

---

## Parte 4 — Cenários e Resultado Esperado — Frontend (Angular / TypeScript)

> Positivo = comportamento seguro. Negativo = falha de segurança que deve ser detectada ou corrigida. Triagem = depende do contexto.

---

### F-API-01 — BOLA no Cliente (CWE-284 / API1:2023 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 60 | Componente de perfil busca dados com `GET /users/${route.params.id}` — ID vem da URL | Frontend não deve confiar em ID da URL para operações sensíveis — usar `/users/me` |
| 61 | Componente de comentário envia `DELETE /comments/${comment.id}` sem confirmar que o comentário pertence ao usuário logado | Solicitação enviada — proteção deve existir no backend; frontend deve checar também |
| 62 | Frontend esconde botão "deletar comentário" para comentários de outros usuários | Proteção visual apenas — backend deve ser a fonte de verdade para autorização |
| 63 | Frontend usa `/users/me` para carregar os dados do próprio usuário em vez de `/users/{id}` | Nenhum risco de BOLA — endpoint retorna dados do usuário autenticado sempre |
| 64 | Componente de edição de perfil envia `PUT /users/${currentUser.id}` onde `currentUser.id` vem do token decodificado | Correto se token é lido via decode local e não via parâmetro de URL |

---

### F-API-02 — Excess Data Exposure no Cliente (CWE-213 / API3:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 65 | Componente exibe `user.role` diretamente para o usuário final na UI | Exposição desnecessária de dados internos — ocultar role da UI do usuário comum |
| 66 | Service armazena resposta completa da API no estado local incluindo campos sensíveis | Dados não necessários na UI presentes em memória — filtrar antes de salvar no store |
| 67 | Componente de lista de usuários (admin) exibe apenas campos name, email, status para admin | Correto — dados sensíveis visíveis apenas em contexto admin com role verificada |
| 68 | Response da API inclui campo `passwordHash` por bug no backend — frontend exibe na UI | Nunca exibir campo — frontend deve ignorar campos não mapeados no modelo TypeScript |
| 69 | Interface TypeScript do modelo User inclui apenas os campos necessários para a UI | Correto — tipagem TypeScript como documentação viva dos campos consumidos |

---

### F-API-03 — Mass Assignment no Cliente (CWE-915 / API3:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 70 | Formulário de registro envia objeto completo do form incluindo campos não visíveis como `role: 'ADMIN'` | Risco de Mass Assignment — enviar apenas os campos do formulário explicitamente definidos |
| 71 | Service de registro envia `{ email, password, name }` extraídos explicitamente do form value | Correto — apenas campos necessários enviados |
| 72 | Formulário de edição de perfil inclui campo `status` oculto no HTML | Campo oculto pode ser manipulado pelo usuário via DevTools — não incluir campos que não devem ser editados |
| 73 | Interface TypeScript `RegisterRequest` define apenas os campos aceitos pela API | Correto — tipagem evita envio acidental de campos extras |

---

### F-API-04 — Autenticação e Tratamento de Token (CWE-287 / API2:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 74 | Token JWT expirado não é tratado — API retorna 401, Angular mostra erro genérico sem redirecionar para login | Usuário preso sem feedback claro — interceptor deve capturar 401 e redirecionar para /login |
| 75 | Interceptor Angular captura respostas HTTP 401 e chama `authService.logout()` seguido de `router.navigate(['/login'])` | Correto — tratamento automático de sessão expirada |
| 76 | `auth.interceptor.ts` adiciona header Authorization em toda requisição sem checar se URL é da própria API | Token vaza para CDN, Google Analytics, serviços de terceiros — filtrar por `environment.apiUrl` |
| 77 | `req.url.startsWith(environment.apiUrl)` antes de adicionar Authorization header | Correto — token só vai para a própria API |
| 78 | Token JWT armazenado em localStorage — acessível por qualquer script na página (XSS) | Hotspot de segurança — idealmente usar HttpOnly cookie ou ao menos mitigar XSS com CSP |
| 79 | Tempo de expiração do token exibido ao usuário como aviso de "sessão expira em X minutos" | Boa UX de segurança — usuário sabe quando fazer login novamente |

---

### F-API-05 — Consumo Responsável de APIs e Paginação (CWE-400 / API4:2023 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 80 | Componente de catálogo carrega todos os vídeos em uma única chamada sem parâmetros de página | Sem paginação — requisição cresce com o catálogo, impacto em performance e memória |
| 81 | Service de vídeos usa `GET /videos?page=0&size=20` com parâmetros de paginação | Correto — carregamento sob demanda com tamanho definido |
| 82 | URL da API hardcoded em múltiplos services como string literal em vez de `environment.apiUrl` | SonarCloud sinaliza S1313 — URL deve vir do environment |
| 83 | Todos os services Angular usam `environment.apiUrl` como base da URL | Nenhum alerta — centralização correta da URL da API |
| 84 | Componente de lista de comentários carrega todos de uma vez para vídeos com 500+ comentários | Performance degradada — implementar carregamento paginado ou scroll infinito |
| 85 | `DELETE /auth/waitlist/me?email=X` chamado pelo frontend com email vindo de input do usuário sem validação | Qualquer email pode ser submetido — risco de API6:2023 no backend |
| 86 | Frontend envia `DELETE /auth/waitlist/me` usando o email do usuário autenticado (do token) em vez de input livre | Correto — email fixado ao usuário da sessão, não editável |

---

## Parte 5 — Passo a Passo de Implementação por Sprint

---

### Sprint 0 — Preparação e Análise (sem código novo)

**Objetivo**: mapear o estado atual antes de corrigir.

1. Executar o backend localmente e acessar `http://localhost:8090/v3/api-docs` — documentar todos os endpoints expostos
2. Instalar Burp Suite Community Edition — configurar o proxy no browser
3. Fazer login como usuário comum e testar `DELETE /comments/{id de outro usuário}` — confirmar se BOLA está presente
4. Fazer login como usuário comum e testar `DELETE /users/{id de outro usuário}` — confirmar se BFLA está presente
5. Verificar se `RegisterRequestDTO` aceita campo `role` via Postman: enviar registro com `"role": "ADMIN"` e verificar o role atribuído
6. Verificar `GET /videos/most-viewed?limit=99999` — observar tempo de resposta e tamanho da resposta
7. Registrar os resultados — cada vulnerabilidade confirmada vira uma issue no GitHub com label `security`

---

### Sprint 1 — Corrigir BOLA em Comentários (Backend) — Prioridade: Critical

**Objetivo**: garantir que só o dono ou admin pode deletar um comentário.

1. Abrir `CommentController.java` — modificar `deleteComment` para receber `@AuthenticationPrincipal User user`
2. Abrir `CommentService.java` — modificar `delete(UUID commentId)` para receber também `UUID callerId`
3. Na `CommentService.delete`: após buscar o comentário, verificar `comment.getUser().getId().equals(callerId) || caller.hasRole("ADMIN")`
4. Se não for dono nem admin: lançar `ForbiddenException` que o `ResourceExceptionHandler` mapeará para HTTP 403
5. Criar teste: `deleteComment_shouldReturn403_whenCallerIsNotOwner` — simular dois usuários, tentar deletar comentário do primeiro com credenciais do segundo
6. Criar teste: `deleteComment_shouldReturn204_whenCallerIsOwner` — cenário positivo
7. Criar teste: `deleteComment_shouldReturn204_whenCallerIsAdmin` — admin pode deletar qualquer comentário
8. Rodar `mvn test` — garantir todos os testes passando
9. Abrir PR com label `security` — incluir descrição do BOLA corrigido

---

### Sprint 2 — Corrigir BOLA e BFLA em Usuários (Backend) — Prioridade: Critical

**Objetivo**: garantir ownership em `PUT /users/{id}` e role check em `DELETE /users/{id}`.

1. Abrir `UserController.java`
2. Adicionar `@AuthenticationPrincipal User caller` em `updateUser` e `deleteUser`
3. Em `updateUser`: verificar `id.equals(caller.getId()) || caller.hasRole("ADMIN")` antes de chamar o service
4. Em `deleteUser`: verificar `caller.hasRole("ADMIN")` — apenas admin deleta qualquer conta; usuário pode deletar apenas a própria
5. Abrir `UserService.java` — revisar `updateUser` e `deleteUser` para aceitar e validar o caller
6. Criar testes cobrindo: owner atualiza própria conta (204), usuário tenta atualizar conta alheia (403), admin deleta qualquer conta (204), usuário tenta deletar conta alheia (403)
7. Verificar também `GET /users/{id}` — definir se usuário comum pode ver dados de outros: se não, restringir ao próprio ID ou exigir role ADMIN
8. Rodar `mvn test` — garantir cobertura

---

### Sprint 3 — Corrigir Mass Assignment (Backend) — Prioridade: Critical

**Objetivo**: garantir que campos sensíveis não são aceitos via API pública.

1. Abrir `RegisterRequestDTO.java` — confirmar que não há campos `role`, `status`, `isAdmin`, `createdAt`
2. Abrir `UserRequestDTO.java` — confirmar ausência dos mesmos campos
3. Se algum campo sensível estiver presente: remover do record/DTO
4. No `UserService.createUser` e `updateUser`: mapear campos explicitamente do DTO para a entidade — nunca usar `BeanUtils.copyProperties` sem filtro de campos
5. Criar teste: `register_shouldIgnoreRoleField_whenProvidedInBody` — enviar registro com role ADMIN no body, verificar que role atribuída é USER
6. Criar teste: `updateUser_shouldIgnoreStatusField_whenProvidedInBody` — verificar que status não muda via PUT

---

### Sprint 4 — Rate Limiting e Paginação (Backend) — Prioridade: Major

**Objetivo**: proteger endpoints contra consumo abusivo.

1. Verificar se `Bucket4j` está configurado para `/auth/login` — se não: adicionar `RateLimitFilter` ou `@RateLimiter` neste endpoint
2. Adicionar `@RequestParam @Max(100) @Min(1) int limit` nos endpoints de listagem com parâmetro de tamanho (`most-viewed`, `least-viewed`)
3. Implementar paginação em `GET /videos` usando `Pageable` do Spring Data — retornar `Page<VideoDTO>` com metadados de página
4. Configurar `spring.data.web.pageable.max-page-size=100` em application.properties — limite global de paginação
5. Criar teste: `mostViewed_shouldReturn400_whenLimitExceeds100` — limite máximo respeitado
6. Criar teste: `login_shouldReturn429_afterFiveAttempts` — rate limiting funciona

---

### Sprint 5 — Corrigir Fluxo de Negócio e Inventário de APIs (Backend) — Prioridade: Major

**Objetivo**: proteger fluxos de negócio e documentar APIs.

1. Abrir `AuthController.java` — `DELETE /auth/waitlist/me`: modificar para usar `@AuthenticationPrincipal` e cancelar apenas a fila do usuário autenticado, ignorando o param `email` da query
2. Adicionar `@PreAuthorize("hasRole('ADMIN')")` em `ImportController` — endpoint de importação deve ser exclusivo de admin
3. Revisar `application-prod.properties` — confirmar que `springdoc.api-docs.enabled=false` ou endpoint protegido por role em produção
4. Adicionar `@Profile("dev")` no `ImportController` se importação não for necessária em produção
5. Criar teste: `cancelWaitlist_shouldReturn403_whenEmailDoesNotMatchAuthenticatedUser` — proteção de fluxo de negócio

---

### Sprint 6 — Tratamento de Erros e Headers (Backend) — Prioridade: Major

**Objetivo**: evitar exposição de informações internas nas respostas.

1. Abrir `GlobalExceptionHandler.java` — garantir que exceções genéricas retornam `{"error": "Internal server error"}` sem stack trace
2. Verificar se `server.error.include-stacktrace=never` está em `application-prod.properties`
3. Adicionar headers de segurança no `SecurityConfig`:
   - `headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))`
   - `headers.frameOptions(frame -> frame.deny())`
   - `headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))`
4. Remover `headers.disable()` se presente no SecurityConfig (já mapeado como Critical em security_ci_cd-1.0)
5. Criar teste: `genericError_shouldNotExposeStackTrace_inResponseBody`

---

### Sprint 7 — Correções no Frontend Angular — Prioridade: Critical e Major

**Objetivo**: corrigir interceptor, paginação e tratamento de token expirado.

1. Abrir `auth.interceptor.ts` — adicionar verificação `req.url.startsWith(environment.apiUrl)` antes de injetar o header Authorization
2. Criar handler para HTTP 401 no interceptor: chamar `authService.logout()` e `router.navigate(['/login'])`
3. Abrir serviço de vídeos — adicionar parâmetros de paginação `page` e `size` nas chamadas ao catálogo
4. Centralizar todas as URLs de API nos services para usar `environment.apiUrl` como base — eliminar strings hardcoded
5. Abrir service de registro — garantir que o objeto enviado ao backend inclui apenas `{ email, password, name }` — sem campos extras
6. No componente de cancelamento de fila: usar email do usuário autenticado (do `authService.currentUser`) em vez de input livre
7. Criar testes Jasmine:
   - `interceptor should not add Authorization header for external URLs`
   - `interceptor should redirect to login on 401 response`
   - `register service should send only allowed fields`

---

### Sprint 8 — Verificação com Burp Suite (DAST Manual) — Prioridade: Major

**Objetivo**: confirmar que as correções dos sprints anteriores eliminaram as vulnerabilidades.

1. Deploy da versão corrigida em ambiente de staging (ou rodar localmente em modo prod)
2. Abrir Burp Suite — configurar proxy no browser
3. **Teste de BOLA em comentários**: login como Usuário A, criar comentário, copiar UUID. Login como Usuário B, tentar `DELETE /comments/{UUID do A}` — verificar HTTP 403
4. **Teste de BOLA em usuários**: login como Usuário B, tentar `PUT /users/{id do A}` — verificar HTTP 403
5. **Teste de BFLA**: login como usuário comum, tentar `DELETE /users/{qualquer id}` sem role ADMIN — verificar HTTP 403
6. **Teste de Mass Assignment**: `POST /auth/register` com body `{"email":"test@t.com","password":"123456","role":"ADMIN"}` — verificar que role atribuída é USER
7. **Teste de Rate Limiting**: automatizar 10 chamadas a `/auth/login` em sequência — verificar HTTP 429 após o limite
8. **Teste de Stack Trace**: provocar erro 500 (ex: ID malformado) — verificar que resposta não inclui stack trace Java
9. Documentar resultado de cada teste como evidência no GitHub Issue da sprint

---

### Referências de Ferramentas

| Ferramenta | Fase | Uso no Projeto | Instalação |
|---|---|---|---|
| **Burp Suite Community** | DAST manual | Teste de BOLA, BFLA, Mass Assignment | `https://portswigger.net/burp/communitydownload` |
| **OWASP ZAP** | DAST automatizado | Fase 3 — pipeline de staging | Docker: `owasp/zap2docker-stable` |
| **Postman / Insomnia** | Teste manual de APIs | Documentar e reproduzir cenários | `https://www.postman.com` |
| **SonarCloud** | SAST | Detectar hotspots de autorização (Fase 1) | GitHub Actions (ver security_ci_cd-1.0.md) |
| **jwt.io** | Debug de tokens | Decodificar JWTs em testes | Browser: `https://jwt.io` |
| **Spring Security Test** | Testes unitários | `@WithMockUser`, `@WithUserDetails` para cenários de RBAC | Já no pom.xml |