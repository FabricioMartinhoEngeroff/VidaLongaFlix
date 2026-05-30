# Segurança de Aplicações Web — XSS, Output Encoding e CSP

> Baseado no curso "Proteção de Vulnerabilidades — Módulo 2".
> Este documento descreve: os conceitos, os cenários de teste com resultado esperado, e o fluxo TDD para colocar em prática no backend e no frontend.

---

## Conceitos do Curso

### XSS — Cross-Site Scripting

XSS é a injeção de código JavaScript malicioso no lado do cliente. O atacante faz o navegador da **vítima** executar um script que ele controla. O objetivo mais comum é roubar o token ou cookie de sessão e assumir a conta (ATO — Account Takeover).

Existem três variantes com comportamentos distintos:

**XSS Refletido** — o payload chega via URL ou parâmetro e é imediatamente refletido na resposta sem ser armazenado. Depende de engenharia social para funcionar: o atacante precisa convencer a vítima a clicar no link malicioso.

**XSS Armazenado** — o payload é salvo no banco de dados (ex: campo de comentário, título, nome de usuário) e executado automaticamente toda vez que qualquer usuário carrega aquela página. É o mais perigoso porque não depende de engenharia social — basta acessar a página normalmente.

**DOM-based XSS** — o payload manipula o DOM diretamente via JavaScript no navegador, sem passar pelo servidor. Ocorre quando o código usa funções como `document.write()`, `innerHTML` ou `decodeURIComponent()` com valores controlados pelo usuário.

---

### Output Encoding

É a prática de converter caracteres especiais em sua representação segura antes de exibi-los. Os caracteres `< > & " '` quando não são codificados permitem que o browser interprete o conteúdo como HTML ou JavaScript.

A resistência perfeita a XSS acontece quando todas as variáveis manipuláveis pelo usuário estão devidamente escapadas e sanitizadas antes de serem exibidas.

**Angular** já aplica Output Encoding automaticamente em templates com `{{ }}`. O risco existe apenas se o desenvolvedor usar `[innerHTML]` ou `bypassSecurityTrust*`, que desabilitam essa proteção.

**Backend REST** que retorna JSON não tem risco direto de XSS por Output Encoding, pois o Jackson serializa strings sem executar HTML. O risco está no front-end que renderiza esses valores.

---

### CSP — Content Security Policy

É um header HTTP que instrui o browser sobre quais origens de script são permitidas. Mesmo que um XSS seja injetado, o browser bloqueia a execução se a origem não estiver na whitelist.

Evolução histórica:
- **Nível 1 (2012)** — whitelist de domínios permitidos, bloqueio opcional de inline scripts
- **Nível 2 (2014)** — introduziu nonces e hashes para scripts inline, bloqueio da função `eval`
- **Nível 3 (atual, draft)** — combina `strict-dynamic` com nonces e hashes para contextos mais flexíveis

Limitação importante: CSP não protege implementações inseguras no código. Se o campo de comentário não sanitiza o input e o CSP está mal configurado, um bypass é possível. O caso do GitLab pagou $10.200 em bug bounty exatamente por isso — o CSP existia mas o campo não sanitizava o input.

---

### URL Encoding

Técnica de converter caracteres especiais em valores hexadecimais precedidos por `%`. A regra é codificar **apenas os parâmetros**, não a URL inteira. Codificar a URL completa pode abrir brechas no mecanismo de decodificação que permitem bypass.

---

### Defesa em Camadas

Nenhuma camada é suficiente sozinha. A segurança real é a combinação de todas:

| Camada | O que faz | Onde aplicar |
|---|---|---|
| Input Validation | Rejeita ou limita payloads na entrada | Backend — antes de salvar |
| Output Encoding | Converte caracteres perigosos antes de exibir | Frontend — Angular `{{ }}` faz automaticamente |
| CSP Header | Browser bloqueia scripts de origens não permitidas | Backend — header HTTP na resposta |
| HttpOnly Cookie | Token inacessível via JavaScript mesmo com XSS | Backend — `Set-Cookie: HttpOnly; Secure` |

---

## Vulnerabilidades Reais Identificadas no Projeto

| # | Onde | O que falta | Tipo de risco |
|---|---|---|---|
| V1 | `SecurityConfig.java` | Headers de segurança desabilitados — sem CSP, sem X-Frame-Options | Amplifica todos os tipos de XSS |
| V2 | `SecurityConfig.java` | `/actuator/**` público sem autenticação | Exposição de dados internos |
| V3 | `SecurityConfig.java` | POST `/comments` sem autenticação obrigatória | Vetor de XSS Armazenado anônimo |
| V4 | `CreateCommentDTO.java` | Texto do comentário sem limite de tamanho e sem sanitização | XSS Armazenado |
| V5 | `UserController.java` | PUT e DELETE `/users/{id}` sem autenticação | Broken Access Control |
| V6 | `auth.service.ts` | `isAuthenticated()` não valida expiração do JWT | Session fixation com token expirado |
| V7 | `auth.service.ts` | Token armazenado em `localStorage` — acessível por JavaScript | Impacto de XSS: token roubável |
| V8 | `environment.prod.ts` | URL da API apontando para `.com` em vez de `.com.br` | Configuração incorreta |

---

## Cenários de Teste e Resultado Esperado

### Cenário 1 — Headers de segurança e CSP

**Contexto:** Toda resposta HTTP da API deve incluir headers que protejam o browser da vítima mesmo que um XSS seja injetado.

| Situação | Resultado Esperado |
|---|---|
| Qualquer endpoint recebe uma requisição | Resposta contém `Content-Security-Policy` com `script-src 'self'` |
| Qualquer endpoint recebe uma requisição | Resposta contém `X-Frame-Options: DENY` |
| Qualquer endpoint recebe uma requisição | Resposta contém `X-Content-Type-Options: nosniff` |
| Qualquer endpoint recebe uma requisição | Resposta contém `Strict-Transport-Security` com `max-age` de 1 ano |
| Atacante tenta carregar a aplicação dentro de um iframe | Browser bloqueia com base no `frame-ancestors 'none'` |

---

### Cenário 2 — Actuator restrito

**Contexto:** O endpoint `/actuator/health` precisa ser público para o health check do Elastic Beanstalk. Os demais endpoints do actuator expõem métricas internas e não devem ser acessíveis publicamente.

| Situação | Resultado Esperado |
|---|---|
| GET `/actuator/health` sem token | HTTP 200 — deve continuar público |
| GET `/actuator/info` sem token | HTTP 200 — informações básicas, pode ser público |
| GET `/actuator/prometheus` sem token | HTTP 401 — métricas são internas |
| GET `/actuator/prometheus` com token de ROLE_ADMIN | HTTP 200 |
| GET `/actuator/prometheus` com token de ROLE_USER | HTTP 403 |

---

### Cenário 3 — XSS Armazenado via comentários

**Contexto:** O campo de comentário é o principal vetor de XSS Armazenado no projeto. Um atacante autenticado pode tentar injetar um payload que será executado para todos os usuários que visualizarem o vídeo.

| Situação | Resultado Esperado |
|---|---|
| POST `/comments` sem token de autenticação | HTTP 401 — comentário anônimo não é permitido |
| POST `/comments` com token válido e texto normal | HTTP 201 — comentário criado |
| POST `/comments` com token válido e texto contendo `<script>alert(1)</script>` | HTTP 400 ou o texto é salvo com os caracteres escapados — nunca como HTML executável |
| POST `/comments` com texto com mais de 1000 caracteres | HTTP 400 com mensagem de validação |
| GET `/comments/video/{id}` após comentário com payload XSS ter sido salvo | O texto retornado não contém tags HTML funcionais — está encodado |

---

### Cenário 4 — Controle de acesso nos endpoints de usuário

**Contexto:** Qualquer pessoa que conheça o UUID de um usuário não pode alterar ou deletar aquela conta sem estar autenticada e autorizada.

| Situação | Resultado Esperado |
|---|---|
| GET `/users/{id}` sem token | HTTP 401 — previne enumeração de usuários |
| PUT `/users/{id}` sem token | HTTP 401 |
| PUT `/users/{id}` com token de outro usuário (não admin) | HTTP 403 — usuário só pode editar a si mesmo |
| PUT `/users/{id}` com token do próprio usuário | HTTP 200 |
| PUT `/users/{id}` com token de ROLE_ADMIN | HTTP 200 — admin pode editar qualquer usuário |
| DELETE `/users/{id}` sem token | HTTP 401 |
| DELETE `/users/{id}` com token de ROLE_USER | HTTP 403 — somente admin pode deletar |
| DELETE `/users/{id}` com token de ROLE_ADMIN | HTTP 204 |

---

### Cenário 5 — Validade do token JWT no frontend

**Contexto:** O guard de autenticação não deve deixar o usuário navegar em rotas protegidas usando um token expirado, mesmo que o token ainda exista no storage.

| Situação | Resultado Esperado |
|---|---|
| Token válido e não expirado está no `localStorage` | `isAuthenticated()` retorna `true` |
| Token expirado está no `localStorage` | `isAuthenticated()` retorna `false` |
| Token malformado (sem os 3 segmentos JWT) está no `localStorage` | `isAuthenticated()` retorna `false` sem lançar exceção |
| Nenhum token está em `localStorage` nem em `sessionStorage` | `isAuthenticated()` retorna `false` |
| Token expirado e usuário tenta acessar `/app` | Guard redireciona para `/authorization` |

---

### Cenário 6 — Token não vaza para serviços externos (Frontend)

**Contexto:** O interceptor Angular adiciona o header `Authorization: Bearer` nas requisições. Isso só deve acontecer para chamadas à própria API, nunca para serviços externos como Google Fonts ou CDNs.

| Situação | Resultado Esperado |
|---|---|
| Requisição para `environment.apiUrl` com token válido | Header `Authorization: Bearer {token}` está presente |
| Requisição para URL externa (ex: Google Fonts) com token válido | Header `Authorization` está **ausente** |
| Requisição com token malformado (não tem 3 segmentos) | Nenhum token é enviado — requisição passa sem `Authorization` |

---

### Cenário 7 — XSS Refletido via parâmetros de rota (Frontend)

**Contexto:** O Angular usa `{{ }}` para interpolação e escapa automaticamente. O risco surge se algum componente usar `[innerHTML]` ou `bypassSecurityTrust*` com valores vindos de parâmetros de rota ou query params.

| Situação | Resultado Esperado |
|---|---|
| Parâmetro de rota contém `<script>alert(1)</script>` e é exibido com `{{ }}` | O texto é exibido literalmente, script **não é executado** |
| Parâmetro de rota contém payload XSS e é usado em `[innerHTML]` | **Não deve existir nenhum `[innerHTML]` com valor de parâmetro** — confirmado na auditoria |
| Input de formulário (login, registro, comentário) contém tags HTML | O Angular exibe como texto, não renderiza HTML |

---

### Cenário 8 — Configuração de produção (Frontend)

**Contexto:** A URL da API em produção deve apontar para o domínio correto.

| Situação | Resultado Esperado |
|---|---|
| Build de produção (`ng build`) | `environment.prod.ts` usa `https://api.vidalongaflix.com.br/api` |
| Requisição de login em produção | Vai para `https://api.vidalongaflix.com.br/api/auth/login` |

---

## Fluxo de Implementação — Como Colocar os Conceitos em Prática

O fluxo é baseado em TDD (Test Driven Development) aplicado à segurança. A lógica é simples: primeiro você descreve o comportamento seguro esperado em forma de teste, depois corrige o código até o teste passar. Isso garante que a correção realmente funciona e que ninguém vai reverter sem perceber.

---

### O ciclo para cada vulnerabilidade

```
1. Escolher o cenário de teste deste documento
2. Criar o spec descrevendo o resultado esperado
3. Rodar o spec → ele vai FALHAR (Red) — isso é esperado e correto
4. Fazer a menor correção possível no código para o spec passar
5. Rodar o spec novamente → ele deve PASSAR (Green)
6. Verificar manualmente que nada quebrou no fluxo principal
7. Passar para o próximo cenário
```

O passo 3 é importante: se o spec passar logo de cara sem nenhuma correção, o teste está errado ou o problema já foi resolvido antes.

---

### Preparação — Antes de começar

**Backend:**
Confirmar que o projeto sobe localmente com `./mvnw spring-boot:run -Dspring-boot.run.profiles=test` e que a suite de testes existente passa com `./mvnw test`. Isso é a linha de base — nenhuma correção de segurança deve quebrar o que já funciona.

**Frontend:**
Confirmar que `npm test -- --watch=false` roda sem erros fatais no repositório local do frontend. Os specs de segurança serão adicionados aos arquivos `.spec.ts` existentes.

---

### Sprint 1 — Headers de Segurança e CSP (Backend)

**Objetivo:** Toda resposta da API deve incluir os headers que ativam as proteções do browser.

**Passo a passo:**

1. Criar o arquivo `SecurityHeadersTest.java` em `src/test/java/.../security/`
2. Escrever os casos de teste baseados no Cenário 1 deste documento — um caso por header
3. Rodar apenas esse arquivo de teste e confirmar que falha (a linha `headers.disable()` garante a falha)
4. Abrir `SecurityConfig.java` e substituir `headers.disable()` pela configuração completa de headers, incluindo CSP com `script-src 'self'`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` e `Strict-Transport-Security`
5. Rodar o spec novamente e confirmar que passa
6. Subir a aplicação localmente e usar `curl -I` para inspecionar os headers visualmente
7. Verificar o console do browser com o frontend rodando — confirmar que nenhum recurso Angular está sendo bloqueado pelo CSP. Se estiver, ajustar as diretivas (`style-src 'unsafe-inline'` pode ser necessário para o Angular)

**Critério de conclusão:** spec verde + nenhum erro de CSP no console do browser em fluxo normal de uso.

---

### Sprint 2 — Actuator Restrito (Backend)

**Objetivo:** Somente `/actuator/health` e `/actuator/info` ficam públicos. Todo o restante exige ROLE_ADMIN.

**Passo a passo:**

1. Criar o arquivo `ActuatorSecurityTest.java` baseado no Cenário 2
2. Rodar e confirmar que o caso de `/actuator/prometheus` sem token falha (atualmente retorna 200)
3. Editar `SecurityConfig.java` — trocar a linha `.requestMatchers("/actuator/**").permitAll()` por duas linhas: uma que libera só `/health` e `/info`, e outra que restringe o restante a ROLE_ADMIN
4. Rodar o spec e confirmar verde
5. Testar manualmente que o health check do Elastic Beanstalk continua funcionando — o EB faz GET para `/actuator/health` sem token

**Critério de conclusão:** spec verde + health check do EB continua acessível.

---

### Sprint 3 — XSS Armazenado em Comentários (Backend + Frontend)

**Objetivo:** Comentários só podem ser criados por usuários autenticados, o texto tem tamanho limitado e caracteres HTML são escapados antes de salvar.

**Passo a passo:**

1. Criar `CommentSecurityTest.java` baseado no Cenário 3
2. Rodar e confirmar que o caso de POST sem token falha (atualmente retorna 201)
3. Editar `SecurityConfig.java` para exigir `authenticated()` no `POST /comments`
4. Rodar o spec do passo 2 e confirmar verde
5. Abrir `CreateCommentDTO.java` e adicionar a anotação `@Size(max = 1000)` no campo `text`
6. Abrir `CommentService.java` e adicionar output encoding nos caracteres `< > & " '` antes de persistir o texto — substituindo cada um pelo seu equivalente em entidade HTML
7. Criar um teste unitário para o `CommentService` que confirma que um texto com `<script>` é salvo com `&lt;script&gt;`
8. **No frontend:** abrir o template do componente de listagem de comentários e confirmar que o campo de texto do comentário usa `{{ comentario.text }}` e não `[innerHTML]`. Documentar a confirmação como comentário no componente

**Critério de conclusão:** spec verde + comentário com payload XSS é salvo encodado e exibido como texto literal no browser.

---

### Sprint 4 — Controle de Acesso nos Endpoints de Usuário (Backend)

**Objetivo:** Nenhuma operação sensível em `/users/{id}` funciona sem autenticação e ownership.

**Passo a passo:**

1. Criar `UserControllerSecurityTest.java` baseado no Cenário 4
2. Rodar e confirmar que GET, PUT e DELETE sem token falham (atualmente retornam 200/204)
3. Editar `SecurityConfig.java` para exigir `authenticated()` em GET e PUT, e `ROLE_ADMIN` no DELETE
4. Editar `UserController.java` no método `updateUser` para injetar o `@AuthenticationPrincipal` e comparar o id do token com o id da URL — retornar 403 se forem diferentes e o usuário não for admin
5. Rodar o spec e confirmar verde
6. Testar o fluxo de edição de perfil no frontend para confirmar que um usuário logado ainda consegue editar seus próprios dados

**Critério de conclusão:** spec verde + fluxo de edição do próprio perfil continua funcionando.

---

### Sprint 5 — Validação de Token Expirado (Frontend)

**Objetivo:** O guard não deve deixar passar tokens expirados ou malformados.

**Passo a passo:**

1. Abrir `auth.service.spec.ts` e adicionar os casos baseados no Cenário 5
2. Rodar os novos casos e confirmar que o caso de token expirado falha (atualmente `isAuthenticated()` retorna `true` para qualquer token presente)
3. Editar `auth.service.ts` — reescrever `isAuthenticated()` para decodificar o payload base64 do JWT, ler o campo `exp` e comparar com `Date.now()`. Envolver em try/catch para tokens malformados
4. Rodar os specs e confirmar verde
5. Testar manualmente: fazer login, esperar o token expirar (ou editar o `exp` manualmente no localStorage para uma data no passado), recarregar a página — deve redirecionar para `/authorization`

**Critério de conclusão:** spec verde + redirecionamento automático ao expirar o token.

---

### Sprint 6 — Limpeza e Configuração (Frontend)

**Objetivo:** Remover código morto e corrigir a URL de produção.

**Passo a passo:**

1. Abrir `login-form.service.ts`, localizar e deletar as linhas com `fake_token_123`
2. Rodar `npm test` para confirmar que nada quebrou com a remoção
3. Abrir `environment.prod.ts` e corrigir `apiUrl` de `api.vidalongaflix.com` para `api.vidalongaflix.com.br`
4. Fazer um build de produção (`ng build`) e confirmar que compila sem erros
5. Testar o fluxo de login apontando para o domínio correto

**Critério de conclusão:** sem referência a `fake_token_123` no código + URL correta no build de produção.

---

### Verificação Final — Todos os Sprints

Após todos os sprints concluídos, fazer uma rodada completa:

**Backend:** rodar `./mvnw test` e confirmar que a suite inteira passa, incluindo os novos specs de segurança e os testes existentes.

**Frontend:** rodar `npm test -- --watch=false` e confirmar que todos os specs passam.

**Integração:** subir backend e frontend localmente, fazer o fluxo completo de login → navegação → comentário → logout e confirmar que tudo funciona normalmente. Inspecionar os headers no DevTools do browser e confirmar CSP, X-Frame-Options e HSTS presentes.

**Fase 2 — HttpOnly Cookie:** após todos os sprints acima estarem verdes, planejar a migração do token de `localStorage` para `HttpOnly cookie`. Esse item elimina o risco de roubo de token via XSS Armazenado mesmo que todas as outras proteções falhem. Os arquivos afetados são `AuthController.java`, `SecurityConfig.java` (reativar CSRF), `auth.service.ts`, `auth.interceptor.ts` e `app.config.ts`.

---

## Fluxos Específicos do Frontend Angular

Os itens abaixo descrevem como os conceitos do curso se aplicam diretamente no código Angular do projeto. Cada fluxo termina com os arquivos a verificar ou corrigir.

---

### Fluxo 1 — Output Encoding automático do Angular

**Conceito aplicado:** Angular escapa automaticamente qualquer valor exibido com `{{ }}`. Isso previne XSS Refletido e Armazenado por padrão.

**O que verificar no projeto:**
Confirmar que nenhum componente usa `[innerHTML]` para exibir dados vindos da API (comentários, nomes de usuário, títulos de vídeo). A auditoria confirmou zero ocorrências, mas ao adicionar novos componentes essa regra deve ser mantida.

**Regra a seguir para novos componentes:** sempre usar `{{ dado }}` para exibir texto. Se precisar renderizar HTML por motivo legítimo, usar `DomSanitizer.sanitize()` do próprio Angular, nunca `bypassSecurityTrustHtml()`.

**Arquivos relevantes:** qualquer `*.component.html` que exiba dados da API (video, comments, user profile).

---

### Fluxo 2 — XSS Armazenado via campo de comentário

**Conceito aplicado:** O campo de comentário é o vetor clássico de XSS Armazenado. O payload é digitado pelo usuário, enviado ao backend, salvo no banco e depois retornado para todos os usuários que visualizarem o vídeo.

**O que verificar no projeto:**
O componente que exibe os comentários deve usar `{{ comentario.text }}` e nunca `[innerHTML]`. O backend deve sanitizar o texto antes de salvar (output encoding nos caracteres `< > & " '`).

**Fluxo do ataque que o projeto precisa bloquear:**
Um usuário autenticado envia um comentário com payload XSS → backend salva sem sanitizar → outro usuário acessa o vídeo → Angular exibe o comentário → se usar `{{ }}` o Angular escapa e o script não executa.

**Arquivos a verificar:** componente de listagem de comentários (arquivo de template `.html`) e o `CommentService.java` no backend.

---

### Fluxo 3 — DOM-based XSS e uso seguro do DOM

**Conceito aplicado:** Funções como `document.write()`, `innerHTML` e `decodeURIComponent()` com valores controlados pelo usuário criam vetores de XSS DOM-based. No Angular, `textContent` e `{{ }}` são as alternativas seguras.

**O que verificar no projeto:**
Buscar no código do frontend por usos de `document.write`, `innerHTML` atribuído diretamente em TypeScript (fora de bindings Angular), e `eval`. Confirmar que o resultado é zero.

**Regra a seguir:** nunca construir HTML por concatenação de strings em TypeScript. Usar os mecanismos do Angular (bindings, diretivas estruturais) para renderizar conteúdo dinâmico.

**Arquivos relevantes:** todos os arquivos `*.ts` dos componentes e serviços do frontend.

---

### Fluxo 4 — CSP e a relação com o Angular

**Conceito aplicado:** O CSP é configurado no backend como header HTTP e afeta como o browser executa o JavaScript do Angular no frontend.

**Impacto do CSP `style-src 'unsafe-inline'` no Angular:** O Angular injeta estilos inline nos componentes. Por isso o CSP do backend precisa permitir `style-src 'unsafe-inline'` temporariamente. A forma mais segura no futuro é configurar o Angular com hash-based CSP, mas isso exige mudança no processo de build.

**Impacto do CSP `script-src 'self'` no Angular:** O Angular é servido como bundle estático pelo CloudFront com o mesmo domínio da aplicação. O `script-src 'self'` é suficiente — não há scripts carregados de domínios externos.

**O que verificar no projeto:** após aplicar o header CSP no backend, abrir o frontend em produção e verificar no console do browser se algum recurso está sendo bloqueado. Ajustar as diretivas do CSP se necessário sem enfraquecer a política.

---

### Fluxo 5 — Segurança do token JWT no storage

**Conceito aplicado:** O objetivo do XSS Armazenado é roubar o token de sessão. Se o token está em `localStorage`, qualquer script malicioso pode lê-lo com `localStorage.getItem('token')`. O `HttpOnly cookie` torna o token completamente inacessível via JavaScript.

**Situação atual no projeto:** o token está em `localStorage` ou `sessionStorage` dependendo da opção "manter logado". Isso é o padrão para SPAs que usam Bearer token, e funciona enquanto o XSS for prevenido nas camadas anteriores.

**Fluxo de risco atual:**
Se um XSS Armazenado passar por todas as proteções → o script malicioso executa no browser da vítima → chama `localStorage.getItem('token')` → envia o token para servidor do atacante → atacante usa o token para fazer requisições como a vítima.

**O que reduz o risco hoje:** Angular escapa `{{ }}` automaticamente, não há `[innerHTML]` no projeto, o backend vai exigir autenticação nos comentários, e o CSP vai bloquear scripts externos.

**Fluxo para Fase 2 — migração para HttpOnly cookie:**
Backend passa a retornar um `Set-Cookie: HttpOnly; Secure; SameSite=Strict` no login em vez de retornar o token no body. O Angular para de armazenar o token manualmente e passa a usar `withCredentials: true` no `HttpClient`. O browser envia o cookie automaticamente em cada requisição. O token nunca fica acessível via JavaScript.

**Arquivos afetados na migração:** `AuthController.java`, `SecurityConfig.java` (reativar CSRF), `auth.service.ts`, `auth.interceptor.ts`, `app.config.ts`.

---

### Fluxo 6 — URL Encoding em links e redirecionamentos

**Conceito aplicado:** Ao construir URLs dinamicamente com parâmetros vindos de variáveis, os parâmetros devem ser codificados com `encodeURIComponent()` para prevenir injeção via manipulação de URL.

**O que verificar no projeto:**
Buscar nos serviços Angular por construção manual de URLs com concatenação de strings onde o valor vem de input do usuário ou de parâmetros de rota. Substituir por `encodeURIComponent()` nos parâmetros.

**Exemplo de padrão inseguro a evitar:** construir uma URL de busca ou filtro concatenando diretamente o termo digitado pelo usuário sem encoding.

**Arquivos relevantes:** arquivos `*.service.ts` que fazem chamadas HTTP com parâmetros dinâmicos, especialmente `api.service.ts`.

---

### Fluxo 7 — Route Guards e validação de expiração

**Conceito aplicado:** O guard de autenticação é a primeira linha de defesa no frontend. Se ele não valida a expiração do token, um usuário com token expirado continua navegando nas rotas protegidas até o backend rejeitar a primeira requisição, criando uma experiência inconsistente.

**Situação atual no projeto:** `auth.guard.ts` chama `isAuthenticated()` que retorna apenas `!!this.getToken()` — verifica só se existe, não se está válido. Tokens expirados passam pelo guard.

**Fluxo corrigido:**
`isAuthenticated()` decodifica o payload base64 do JWT, lê o campo `exp` (timestamp em segundos) e compara com `Date.now() / 1000`. Se expirado, retorna `false` e o guard redireciona para `/authorization`. Se o JWT estiver malformado, o bloco tenta/captura retorna `false` sem lançar exceção.

**Arquivos afetados:** `auth.service.ts` (método `isAuthenticated`), `auth.guard.ts` (sem mudança necessária — já chama `isAuthenticated()`).

---

## Resumo — O que o Projeto Fica com os Fluxos Implementados

| Tipo de XSS | Defesa no Backend | Defesa no Frontend |
|---|---|---|
| XSS Armazenado (comentários) | Autenticação obrigatória + limite de tamanho + output encoding antes de salvar | Angular `{{ }}` não executa scripts — exibe como texto |
| XSS Refletido (parâmetros) | CSP header bloqueia scripts externos | Angular `{{ }}` escapa automaticamente |
| DOM-based XSS | — (é puramente client-side) | Sem `document.write` nem `[innerHTML]` no projeto |
| Roubo de token via XSS | CSP reduz superfície de execução | `isAuthenticated()` expira tokens; HttpOnly cookie elimina risco (Fase 2) |
