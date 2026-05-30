# Segurança de Aplicações Web — Autenticação, JWT e Gerenciamento de Sessão

> Baseado no curso "Proteção de Vulnerabilidades — Módulo 3: Autenticação".
> Este documento descreve: os conceitos, os cenários de teste com resultado esperado (positivos e negativos), e o fluxo de implementação para o backend e o frontend.

---

## Conceitos do Curso

### Tríade CIA e o papel da Autenticação

A segurança da informação é fundamentada em três pilares:

**Confidencialidade** — apenas quem tem autorização acessa os dados. A autenticação é a porta de entrada: sem ela qualquer pessoa pode requisitar recursos protegidos.

**Integridade** — os dados não são alterados por partes não autorizadas. Uma sessão comprometida permite que um atacante escreva dados como se fosse a vítima.

**Disponibilidade** — o sistema permanece acessível para usuários legítimos. Ataques de força bruta sem rate limiting podem esgotar recursos do servidor e derrubar o serviço.

---

### Métodos de Autenticação

**Email + senha** é o método mais comum. Os riscos são: senhas fracas, reutilização de senhas e armazenamento inseguro. O armazenamento deve ser sempre em hash, nunca em texto plano.

**Login social (OAuth 2.0)** delega a autenticação para um provedor confiável (Google, GitHub). O sistema não armazena senha — apenas o id do usuário no provedor. Isso elimina o risco de vazamento de senha, mas cria dependência do provedor.

**MFA / 2FA** adiciona um segundo fator além da senha. Mesmo com a senha comprometida, o atacante ainda precisa do segundo fator (código TOTP, SMS). É a forma mais eficaz de proteger contas de alto valor.

---

### Armazenamento de Senhas — Hash

Senhas nunca devem ser armazenadas em texto plano ou em hash reversível. As regras são:

**MD5** — proibido. Sua velocidade de processamento torna ataques de força bruta triviais com hardware moderno. Não usar nem para fins não relacionados à segurança em contextos sensíveis.

**SHA-256 puro** — insuficiente. É rápido demais. Sem salt, tabelas rainbow quebram a proteção em segundos para senhas comuns.

**BCrypt / Argon2** — recomendado. São algoritmos projetados para ser lentos (custo ajustável) e incluem salt automático. BCrypt está implementado no projeto via Spring Security.

O custo do BCrypt (work factor) deve ser alto o suficiente para que uma tentativa de login demore entre 100ms e 300ms. Abaixo de 100ms é fácil de atacar em escala.

---

### Rate Limiting — Defesa contra Força Bruta

Sem rate limiting, um atacante pode fazer milhares de tentativas de login por segundo testando senhas de um dicionário. Com rate limiting, o número de tentativas é limitado por IP e/ou por conta.

A estratégia ideal combina as duas dimensões:
- **Por IP**: bloqueia ataques distribuídos onde o atacante usa múltiplos IPs mas mira um conjunto de contas
- **Por conta**: bloqueia ataques onde um único IP testa a mesma conta com senhas diferentes

O projeto já possui rate limiting com Bucket4j (5 tentativas por minuto por IP) implementado no endpoint de login.

---

### OAuth 2.0 — Fluxo de Autorização

OAuth 2.0 é um protocolo de autorização delegada. Os atores são:

**Resource Owner** — o usuário que possui os dados.

**Resource Server** — o servidor que hospeda os dados protegidos (a API do projeto).

**Client** — a aplicação que quer acessar os dados em nome do usuário (o frontend Angular).

**Authorization Server** — o servidor que autentica o usuário e emite tokens (Google, GitHub, ou o próprio backend no caso de JWT).

**User Agent** — o browser.

O fluxo Authorization Code:
1. O frontend redireciona o usuário para o Authorization Server com `client_id` e um parâmetro `state` aleatório (CSRF token da OAuth)
2. O usuário autentica no provedor
3. O provedor redireciona de volta com um `code` de curta duração
4. O backend troca o `code` por um `access_token` (e opcionalmente um `refresh_token`) em uma chamada server-to-server com `client_secret`
5. O backend retorna o token ao frontend

O parâmetro `state` é crítico: ele deve ser um valor aleatório gerado pelo frontend e verificado quando o provedor redireciona de volta. Sem ele, um atacante pode forjar o redirecionamento OAuth (CSRF em OAuth).

---

### JWT — Estrutura e Validação

JSON Web Token é um formato compacto para transmitir informações verificáveis entre partes. Sua estrutura é `header.payload.signature`, cada parte codificada em Base64url.

**Header** — metadados do token: algoritmo de assinatura (`alg`) e tipo (`typ`). O algoritmo mais comum é `HS256` (HMAC com SHA-256). O ataque mais conhecido aqui é alterar `alg` para `none` para tentar bypass da validação — bibliotecas modernas rejetam isso.

**Payload** — as claims: dados sobre o usuário e o token. As claims padrão mais importantes:
- `sub` — subject, o identificador do usuário
- `exp` — expiration, timestamp Unix de expiração
- `iss` — issuer, quem emitiu o token
- `iat` — issued at, quando foi emitido

O payload é apenas codificado em Base64, **não é criptografado**. Qualquer pessoa com o token pode ler o payload. Nunca armazenar dados sensíveis (senha, CPF, informações médicas) no payload.

**Signature** — garante a integridade do token. É gerada com `HMAC(header + "." + payload, secret)`. Sem a chave secreta, é impossível gerar uma assinatura válida. Isso significa que mesmo que um atacante altere o payload, a assinatura não vai bater.

---

### Vulnerabilidades de JWT

**Manipulação de payload com algoritmo `none`:** o atacante decodifica o token, muda o `sub` para `"administrator"`, e recria o token com `alg: none` e sem assinatura. Bibliotecas antigas ou mal configuradas aceitavam isso. A defesa é sempre especificar explicitamente os algoritmos aceitos (`HS256`, `RS256`) e rejeitar `none`.

**Força bruta do secret:** se o secret do HMAC for fraco (ex: `"secret"`, `"123456"`), é possível quebrá-lo offline com ferramentas como `hashcat`. A defesa é um secret de pelo menos 256 bits (32 bytes) de entropia alta, armazenado em variável de ambiente — nunca hardcoded.

**Roubo de token armazenado em localStorage:** se um XSS for executado, o atacante pode chamar `localStorage.getItem('token')` e enviar o token para seu servidor. A defesa é mover o token para HttpOnly cookie.

---

### Storage de Token — localStorage vs HttpOnly Cookie

**localStorage**
- Acessível via JavaScript — vulnerável a qualquer XSS
- Persiste entre sessões do browser
- O frontend precisa gerenciar o envio manual (header `Authorization: Bearer`)
- Padrão atual do projeto

**HttpOnly Cookie**
- Inacessível via JavaScript — completamente imune a roubo por XSS
- O browser envia automaticamente a cada requisição
- Exige `SameSite=Strict` ou `Lax` para proteção CSRF
- Mais seguro mas exige mudanças no backend (emitir o cookie) e no frontend (usar `withCredentials: true`)

---

### Atributos de Cookie — Secure, HttpOnly, SameSite

**`Secure`** — o cookie só é enviado pelo browser em conexões HTTPS. Em HTTP, o cookie é silenciosamente omitido. Essencial em produção para prevenir que o token seja interceptado em tráfego não criptografado.

**`HttpOnly`** — o cookie não é acessível via `document.cookie` em JavaScript. Scripts maliciosos de XSS não conseguem lê-lo. Este é o atributo mais importante para proteger tokens de sessão.

**`SameSite`** — controla quando o browser envia o cookie em requisições cross-origin:
- `Strict` — o cookie só é enviado em navegações same-origin. Máxima proteção contra CSRF. Pode quebrar fluxos legítimos de redirect (ex: link externo que leva para área logada).
- `Lax` — o cookie é enviado em navegações top-level (ex: clicar em um link) mas não em requisições de fundo (imagens, scripts, fetch). Bom equilíbrio entre segurança e usabilidade. Padrão em browsers modernos.
- `None` — sem restrição cross-origin. Exige `Secure`. Usado em cenários de widgets embarcados — perigoso para tokens de sessão.

---

### Session Hijacking

O atacante rouba o token ou cookie de sessão de um usuário legítimo e usa para fazer requisições como se fosse ele. Vetores:
- XSS que lê o `localStorage`
- Sniffing de rede em HTTP (sem HTTPS)
- Acesso físico ao device ou ao browser

Defesa: HttpOnly cookie (inacessível via JS) + HTTPS obrigatório (`Secure`) + tokens com expiração curta.

---

### Session Fixation

O atacante não rouba uma sessão existente — ele *cria* uma sessão antecipadamente. O fluxo:
1. Atacante acessa o sistema e obtém um session ID (ou cria um token com o `sub` que deseja controlar)
2. Atacante induz a vítima a usar aquele session ID (via link manipulado, cookie injetado)
3. A vítima faz login — o sistema autentica a vítima mas *mantém o mesmo session ID*
4. Atacante usa o session ID que conhece para acessar a sessão autenticada da vítima

Defesa principal: **regenerar o session ID / token após o login bem-sucedido**. Para JWT stateless: o token é sempre novo após cada login. O problema ocorre em sistemas com session ID fixo (ex: JSESSIONID que não muda após login).

---

### Session Hardening

Conjunto de práticas para dificultar o uso de sessões comprometidas:

**Expiração curta:** access tokens com TTL de 15 a 60 minutos limitam a janela de uso de um token roubado.

**Refresh token rotation:** em vez de tokens de longa duração, o sistema emite um `access_token` curto e um `refresh_token` de vida longa. Quando o access expira, o frontend usa o refresh token para obter um novo par. O refresh token é invalidado a cada uso (rotação). Se um refresh token for usado duas vezes, o sistema detecta comprometimento e invalida toda a sessão.

**Logout real:** invalidar o token no backend (blocklist) ou invalidar o refresh token. Com JWT puro e stateless, o logout do cliente não impede que o token seja usado até expirar. A solução completa exige uma blocklist de JTI (JWT ID).

**Regeneração após login:** fundamental contra Session Fixation. Sempre emitir um token novo após autenticação bem-sucedida.

---

## Vulnerabilidades Identificadas no Projeto

| # | Onde | O que falta | Tipo de risco |
|---|---|---|---|
| A1 | `AuthController.java` | Token retornado no body da resposta, não em HttpOnly cookie | Roubo de token via XSS |
| A2 | `auth.service.ts` | `isAuthenticated()` não valida o campo `exp` do JWT | Sessões expiradas permanecem ativas no frontend |
| A3 | `auth.service.ts` | Token armazenado em `localStorage` — acessível por qualquer JavaScript | Impacto máximo de XSS: token roubável |
| A4 | `auth.service.ts` | Logout apenas remove o token do `localStorage` — token ainda válido no backend até expirar | Sem invalidação real de sessão |
| A5 | `AuthController.java` | Sem mecanismo de refresh token | Usuário precisa relogar frequentemente ou token tem vida longa (maior risco) |
| A6 | `application-prod.properties` | JWT secret via env var — correto. Verificar entropia mínima de 256 bits | Força bruta do secret se fraco |

---

## Cenários de Teste e Resultado Esperado

### Backend — Autenticação e JWT

---

#### Cenário B1 — Login com credenciais válidas

**Contexto:** O endpoint de login deve autenticar o usuário e retornar um token com as informações corretas.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | POST `/auth/login` com email e senha corretos | HTTP 200 — token JWT presente na resposta |
| ✅ Positivo | Token retornado contém o campo `sub` com o id do usuário | Payload decodificado em Base64 confirma `sub` correto |
| ✅ Positivo | Token retornado contém o campo `exp` com timestamp futuro | `exp` está pelo menos 15 minutos à frente do momento da emissão |
| ❌ Negativo | POST `/auth/login` com senha errada | HTTP 401 — sem token na resposta |
| ❌ Negativo | POST `/auth/login` com email inexistente | HTTP 401 — mensagem genérica que não revela se o email existe |
| ❌ Negativo | POST `/auth/login` com body vazio | HTTP 400 — validação rejeita antes de consultar o banco |

---

#### Cenário B2 — Rate limiting no endpoint de login

**Contexto:** O rate limiter deve bloquear tentativas excessivas de login do mesmo IP para prevenir força bruta.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | 5 tentativas de login em 1 minuto do mesmo IP | Todas as 5 recebem resposta (200 ou 401 conforme credenciais) |
| ❌ Negativo | 6ª tentativa de login em menos de 1 minuto do mesmo IP | HTTP 429 — requisição bloqueada pelo Bucket4j |
| ❌ Negativo | 10ª tentativa sequencial sem aguardar reset | HTTP 429 — bloqueio continua |
| ✅ Positivo | Após 1 minuto do último bloqueio, nova tentativa com credenciais corretas | HTTP 200 — rate limit foi resetado, login funciona |

---

#### Cenário B3 — Validação de token JWT em endpoints protegidos

**Contexto:** O backend deve aceitar apenas tokens válidos, não expirados, com assinatura correta e algoritmo esperado.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Requisição com token válido e não expirado | HTTP 200 — endpoint processa normalmente |
| ❌ Negativo | Requisição sem header `Authorization` para endpoint protegido | HTTP 401 |
| ❌ Negativo | Requisição com token expirado (campo `exp` no passado) | HTTP 401 — token rejeitado |
| ❌ Negativo | Requisição com token com assinatura adulterada (último segmento modificado) | HTTP 401 — assinatura inválida |
| ❌ Negativo | Requisição com token com payload modificado (ex: `sub` trocado) mas assinatura original | HTTP 401 — assinatura não bate com payload alterado |
| ❌ Negativo | Token com `alg: none` e sem assinatura | HTTP 401 — algoritmo `none` rejeitado explicitamente |
| ❌ Negativo | Token com `sub` de usuário inexistente no banco | HTTP 401 ou HTTP 403 |

---

#### Cenário B4 — Proteção da senha no banco

**Contexto:** A senha nunca deve aparecer em texto plano nem em hash reversível no banco de dados.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Novo usuário cadastrado — verificar campo `password` no banco | Começa com `$2a$` (prefixo BCrypt) — nunca texto plano |
| ✅ Positivo | Dois usuários com a mesma senha — verificar os hashes | Hashes diferentes — o salt do BCrypt é único por senha |
| ❌ Negativo | Hash da senha exposto em qualquer endpoint da API (GET `/users/{id}`, erros) | O campo `password` não aparece em nenhuma resposta JSON |

---

#### Cenário B5 — Controle de acesso baseado em role (RBAC)

**Contexto:** O token JWT carrega a role do usuário. Endpoints restritos a ROLE_ADMIN devem rejeitar tokens de ROLE_USER.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | GET endpoint público com token de ROLE_USER | HTTP 200 |
| ✅ Positivo | Endpoint ROLE_ADMIN com token de ROLE_ADMIN | HTTP 200 |
| ❌ Negativo | Endpoint ROLE_ADMIN com token de ROLE_USER | HTTP 403 — acesso negado pela role |
| ❌ Negativo | Token com campo `roles` adulterado de `ROLE_USER` para `ROLE_ADMIN` (sem chave secreta) | HTTP 401 — assinatura inválida rejeita o token |

---

### Frontend Angular — Autenticação e Sessão

---

#### Cenário F1 — Validação de expiração do token no frontend

**Contexto:** O método `isAuthenticated()` deve verificar o campo `exp` do JWT, não apenas a presença do token.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Token válido com `exp` no futuro está no storage | `isAuthenticated()` retorna `true` |
| ❌ Negativo | Token com `exp` no passado (expirado) está no storage | `isAuthenticated()` retorna `false` |
| ❌ Negativo | Token malformado (apenas 1 ou 2 segmentos, sem payload) está no storage | `isAuthenticated()` retorna `false` sem lançar exceção |
| ❌ Negativo | Storage vazio (sem token) | `isAuthenticated()` retorna `false` |
| ❌ Negativo | Token expirado e usuário tenta acessar rota protegida `/app` | Guard intercepta, redireciona para `/authorization` |

---

#### Cenário F2 — Fluxo de login e armazenamento do token

**Contexto:** Após o login bem-sucedido, o token deve ser armazenado e o usuário redirecionado. Falhas não devem vazar informações.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Login com credenciais corretas | Token armazenado no storage + redirecionamento para `/app` |
| ✅ Positivo | "Manter logado" marcado no login | Token armazenado em `localStorage` (persiste ao fechar browser) |
| ✅ Positivo | "Manter logado" desmarcado no login | Token armazenado em `sessionStorage` (apagado ao fechar aba) |
| ❌ Negativo | Login com credenciais erradas | Mensagem de erro exibida na tela — nenhum token armazenado |
| ❌ Negativo | Resposta de erro 401 do backend | Frontend não trava, não expõe detalhes técnicos do erro ao usuário |

---

#### Cenário F3 — Logout e limpeza de sessão

**Contexto:** O logout deve remover o token de todos os storages e redirecionar para a tela de login.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Usuário clica em logout | Token removido do `localStorage` e do `sessionStorage` |
| ✅ Positivo | Após logout, usuário tenta navegar para `/app` | Guard detecta ausência de token, redireciona para `/authorization` |
| ❌ Negativo | Após logout, usuário usa o botão "voltar" do browser e tenta acessar rota protegida | Guard bloqueia — não há token válido |
| ❌ Negativo | Após logout, token anterior (ainda não expirado) é reinserido manualmente no localStorage | Fluxo de negócio funciona até o token expirar (limitação do JWT stateless — resolvida com blocklist na Fase 2) |

---

#### Cenário F4 — Interceptor não vaza o token para serviços externos

**Contexto:** O header `Authorization: Bearer` deve ser enviado apenas para a API do projeto.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Requisição para URL que começa com `environment.apiUrl` | Header `Authorization: Bearer {token}` presente |
| ❌ Negativo | Requisição para URL externa (CDN, Google Fonts, serviço de analytics) | Header `Authorization` ausente |
| ❌ Negativo | Token malformado (não tem 3 segmentos) mas presente no storage | Nenhum header `Authorization` é enviado |

---

#### Cenário F5 — Proteção de rotas (Route Guard)

**Contexto:** Rotas da área logada não devem ser acessíveis para usuários sem token válido.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Usuário autenticado com token válido acessa `/app` | Página carrega normalmente |
| ✅ Positivo | Usuário não autenticado acessa `/` | Página pública carrega normalmente |
| ❌ Negativo | Usuário não autenticado tenta acessar diretamente `/app/catalog` pela URL | Guard redireciona para `/authorization` |
| ❌ Negativo | Usuário com token expirado tenta acessar `/app` | Guard valida `exp`, redireciona para `/authorization` |

---

#### Cenário F6 — Exposição do payload JWT no frontend

**Contexto:** O payload JWT é encodado mas não criptografado. Dados sensíveis nunca devem estar no payload.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Token decodificado em Base64 — verificar campos do payload | Contém `sub`, `exp`, `iss`, `roles` — sem senha, CPF ou dados sensíveis |
| ✅ Positivo | Backend retorna apenas dados necessários no payload | Payload mínimo — só o que o frontend precisa para funcionamento |
| ❌ Negativo | Payload do token exposto em log de console no frontend | Nenhum `console.log(token)` em código de produção |

---

## Fluxo de Implementação — Como Colocar os Conceitos em Prática

O fluxo segue o mesmo padrão TDD dos módulos anteriores: descrever o comportamento esperado em um spec, confirmar que o spec falha com o código atual (Red), corrigir o código (Green), verificar manualmente.

---

### O ciclo para cada vulnerabilidade

```
1. Escolher o cenário de teste deste documento
2. Criar o spec descrevendo o resultado esperado
3. Rodar o spec → ele vai FALHAR (Red) — confirma que o problema existe
4. Fazer a correção mínima necessária no código
5. Rodar o spec novamente → deve PASSAR (Green)
6. Verificar manualmente o fluxo principal (login, navegação, logout)
7. Passar para o próximo cenário
```

---

### Preparação — Antes de começar

**Backend:** confirmar que `./mvnw test` passa sem erros. O `JwtService.java` ou equivalente é o arquivo central — localizar antes de começar.

**Frontend:** confirmar que `npm test -- --watch=false` roda sem erros fatais. Os specs serão adicionados em `auth.service.spec.ts` e `auth.guard.spec.ts`.

---

### Sprint 1 — Validação completa do JWT no backend

**Objetivo:** O backend deve rejeitar qualquer token inválido: expirado, assinatura errada, algoritmo `none`, ou payload adulterado.

**Passo a passo:**

1. Criar `JwtValidationTest.java` em `src/test/java/.../security/`
2. Escrever os casos baseados no Cenário B3 — um método de teste por situação negativa
3. Rodar e identificar se algum caso negativo retorna 200 (indicaria falha na validação)
4. Abrir o serviço de JWT do projeto e verificar: se a biblioteca usada está configurada para rejeitar `alg: none` explicitamente (JJWT e Nimbus JOSE fazem isso por padrão em versões modernas — confirmar a versão)
5. Confirmar que o campo `exp` é validado automaticamente pela biblioteca — a maioria das libs faz isso por padrão
6. Rodar os specs e confirmar verde
7. Testar manualmente com `curl` enviando um token expirado e confirmando HTTP 401

**Critério de conclusão:** todos os casos negativos do Cenário B3 resultam em HTTP 401.

---

### Sprint 2 — Validação de expiração do JWT no frontend

**Objetivo:** `isAuthenticated()` deve decodificar o payload e verificar o campo `exp` antes de retornar `true`.

**Passo a passo:**

1. Abrir `auth.service.spec.ts` e adicionar os casos do Cenário F1
2. Criar tokens de teste: um com `exp` no futuro (timestamp Unix + 3600) e um com `exp` no passado
3. Rodar os specs e confirmar que o caso de token expirado falha (atualmente retorna `true`)
4. Abrir `auth.service.ts` e reescrever `isAuthenticated()`:
   - Pegar o token do storage
   - Separar os 3 segmentos por `.` — se não houver 3 segmentos, retornar `false`
   - Decodificar o segundo segmento com `atob()` e fazer `JSON.parse()`
   - Comparar `payload.exp` com `Math.floor(Date.now() / 1000)` (exp é em segundos, Date.now() em ms)
   - Envolver tudo em `try/catch` — retornar `false` se qualquer passo lançar exceção
5. Rodar os specs e confirmar verde
6. Testar manualmente: fazer login, editar manualmente o `exp` no localStorage para `1` (passado distante), recarregar a página — deve redirecionar

**Critério de conclusão:** spec verde + redirecionamento automático com token expirado.

---

### Sprint 3 — Segurança do logout

**Objetivo:** O logout deve limpar o token de todos os storages e o guard deve bloquear acesso após o logout.

**Passo a passo:**

1. Abrir `auth.service.spec.ts` e adicionar os casos do Cenário F3
2. Confirmar que o método de logout remove token de `localStorage` **e** de `sessionStorage` — verificar se ambos são limpos ou apenas um
3. Se apenas um storage for limpo, abrir `auth.service.ts` e editar o método de logout para limpar ambos
4. Verificar se após logout o guard bloqueia navegação para rotas protegidas — testar com o caso de "botão voltar"
5. Rodar specs e confirmar verde
6. Testar manualmente o fluxo completo: login → navegação → logout → botão voltar → deve cair na tela de login

**Critério de conclusão:** spec verde + logout limpa todos os storages + guard bloqueia após logout.

---

### Sprint 4 — Verificação do secret JWT

**Objetivo:** Confirmar que o `JWT_SECRET` em produção tem entropia suficiente para resistir a força bruta offline.

**Passo a passo:**

1. Verificar no Elastic Beanstalk (painel AWS → Configuração → Variáveis de ambiente) o valor de `JWT_SECRET`
2. Confirmar que o valor tem pelo menos 32 caracteres aleatórios (256 bits de entropia)
3. Se o secret for uma palavra simples (ex: `"secret"`, nome do projeto, data), gerar um novo com `openssl rand -base64 32` e atualizar no painel EB
4. Criar um teste unitário que verifica que o `JwtService` rejeita um token assinado com um secret diferente — isso confirma que o secret está sendo usado corretamente na validação
5. Documentar no `MEMORY.md` que o secret foi revisado e atende o mínimo de 256 bits

**Critério de conclusão:** secret com 256+ bits confirmado + teste de validação de assinatura passa.

---

### Sprint 5 — Proteção de rotas com guard atualizado

**Objetivo:** Com `isAuthenticated()` corrigido no Sprint 2, confirmar que o guard bloqueia todas as rotas protegidas com token expirado.

**Passo a passo:**

1. Abrir `auth.guard.spec.ts` e adicionar os casos do Cenário F5
2. Confirmar que o guard usa `isAuthenticated()` do `AuthService` — se sim, os casos do Sprint 2 já cobrem indiretamente
3. Testar os casos de redirecionamento: token expirado → rota protegida → deve redirecionar para `/authorization`
4. Confirmar que usuário não autenticado acessando `/` (rota pública) não é redirecionado
5. Rodar os specs e confirmar verde

**Critério de conclusão:** spec verde + guard redireciona apenas nas rotas protegidas.

---

### Sprint 6 — Auditoria do payload JWT

**Objetivo:** Confirmar que o payload do token não expõe dados sensíveis e tem o mínimo necessário.

**Passo a passo:**

1. Fazer login na aplicação em ambiente local e copiar o token da resposta
2. Decodificar o payload com qualquer decodificador Base64 (ex: `echo "payload_base64" | base64 -d | python3 -m json.tool`)
3. Verificar os campos presentes: confirmar que não há `password`, `cpf`, `credit_card`, ou dados médicos
4. Verificar que os campos presentes são apenas os necessários para o funcionamento: `sub` (user id), `exp`, `iat`, `iss`, e a role
5. Se houver campos desnecessários, localizar o `JwtService.java` e remover da construção do token
6. Buscar no código frontend por `console.log(token)` ou `console.log(this.getToken())` e remover
7. Documentar os campos presentes no token como referência para novos desenvolvedores

**Critério de conclusão:** payload mínimo confirmado + sem logs de token em produção.

---

### Sprint 7 — Planejamento do HttpOnly Cookie (Fase 2)

**Objetivo:** Documentar e preparar a migração do token de `localStorage` para `HttpOnly cookie`. Esta sprint não implementa — apenas planeja e cria os specs que vão falhar até a Fase 2 ser executada.

**Passo a passo:**

1. Criar `auth.cookie.spec.ts` no frontend com um caso que verifica: após login, o token **não** está no `localStorage` (vai falhar agora — é o Red esperado da Fase 2)
2. Criar `AuthControllerCookieTest.java` no backend com um caso que verifica: a resposta do login contém um header `Set-Cookie` com `HttpOnly` (vai falhar agora)
3. Esses specs documentam o comportamento futuro esperado e servem como ponto de partida para a Fase 2
4. Listar os arquivos afetados na migração: `AuthController.java` (emitir `Set-Cookie`), `SecurityConfig.java` (reativar CSRF com token cookie), `auth.service.ts` (parar de armazenar token), `auth.interceptor.ts` (usar `withCredentials: true` em vez de header), `app.config.ts` (configurar `withCredentials` global)
5. Documentar que a migração exige CSRF habilitado no backend, pois cookies são enviados automaticamente (diferente de Bearer token que requer JavaScript explícito)

**Critério de conclusão:** specs criados e documentados como "Fase 2 pendente" + lista de arquivos afetados registrada.

---

### Verificação Final — Todos os Sprints

**Backend:** `./mvnw test` — suite completa deve passar, incluindo os novos specs de autenticação.

**Frontend:** `npm test -- --watch=false` — todos os specs incluindo os do guard e do `isAuthenticated()`.

**Integração manual:**
1. Fazer login com credenciais corretas → token armazenado, redirecionamento para `/app`
2. Navegar pelas rotas protegidas → todas funcionam
3. Fazer logout → token removido, redirecionamento para login
4. Tentar botão "voltar" após logout → guard bloqueia
5. Manipular `exp` no localStorage para valor no passado → recarregar página → redirecionamento automático
6. Testar 6 logins incorretos em sequência → 6ª deve receber 429

---

## Fluxos Específicos do Frontend Angular

---

### Fluxo 1 — Decodificação segura do JWT no cliente

**Conceito aplicado:** O payload JWT é apenas Base64 — não é criptografado. Pode ser lido no frontend para extrair `exp` e tomar decisões de expiração, sem nenhuma chamada extra ao backend.

**O que implementar no projeto:**
Reescrever `isAuthenticated()` em `auth.service.ts` para decodificar o segundo segmento do token com `atob()`, extrair `exp` e comparar com `Date.now() / 1000`. Usar `try/catch` para tratar tokens malformados sem exceção visível.

**Regra importante:** decodificar é diferente de validar a assinatura. O frontend só decodifica para saber a expiração. A validação da assinatura é responsabilidade exclusiva do backend.

**Arquivos afetados:** `auth.service.ts`.

---

### Fluxo 2 — Route Guard com expiração real

**Conceito aplicado:** O `AuthGuard` é a defesa do frontend contra acesso a rotas protegidas. Ele deve ser a primeira camada, não a única.

**Situação atual:** o guard chama `isAuthenticated()` que apenas verifica a presença do token. Após o Sprint 2, `isAuthenticated()` valida o `exp` — o guard não precisa mudar, apenas se beneficia da melhoria no serviço.

**Fluxo completo após correção:**
Usuário com token expirado acessa `/app/catalog` → guard chama `isAuthenticated()` → método lê o payload, detecta `exp` no passado → retorna `false` → guard redireciona para `/authorization`.

**Arquivos afetados:** `auth.service.ts` (único arquivo com mudança), `auth.guard.ts` (sem mudança).

---

### Fluxo 3 — Sessão e o conceito de "manter logado"

**Conceito aplicado:** A diferença entre `localStorage` (persiste) e `sessionStorage` (apagado ao fechar aba) implementa de forma simples o conceito de sessões temporárias versus persistentes.

**Situação atual no projeto:** `auth.service.ts` já implementa essa lógica com base no checkbox "manter logado". Isso é um padrão correto para aplicações sem HttpOnly cookie.

**Limitação de segurança:** mesmo com `sessionStorage`, o token ainda é acessível via JavaScript enquanto a aba está aberta. A proteção real é o HttpOnly cookie — nenhum JavaScript consegue ler, independente de qual storage.

**O que documentar:** a decisão arquitetural atual (localStorage/sessionStorage com Bearer token) e o caminho para o HttpOnly cookie (Fase 2).

---

### Fluxo 4 — Logout real vs logout aparente

**Conceito aplicado:** Em arquiteturas JWT stateless, o logout do cliente (remover o token do storage) não invalida o token no servidor. O token continua tecnicamente válido até expirar.

**Impacto no projeto:** se um token for roubado antes do logout, o atacante pode continuar usando-o até o `exp`. Com tokens de 1 hora, a janela de exploração é de até 1 hora mesmo após o usuário ter feito logout.

**Mitigações disponíveis:**
- **Curta duração do token:** tokens de 15 minutos limitam a janela sem implementação extra
- **Refresh token rotation:** o access token curto + refresh token de uso único. Ao detectar reuso do refresh token, toda a sessão é invalidada
- **JTI blocklist:** cada token tem um ID único (`jti`). No logout, o ID é salvo em cache (Redis). O backend verifica o cache antes de aceitar o token. Eficaz mas adiciona latência e dependência de estado

**O que implementar agora:** verificar a duração atual do token em `application.properties` e confirmar que é no máximo 60 minutos. Documentar a estratégia de blocklist como item da Fase 2.

---

### Fluxo 5 — Proteção contra Session Fixation no contexto SPA

**Conceito aplicado:** Session Fixation clássica afeta frameworks de sessão server-side (JSESSIONID). Para JWT stateless, o risco análogo seria o frontend reutilizar um token antigo após um novo login.

**Situação no projeto:** ao fazer login, `auth.service.ts` sobrescreve qualquer token existente com o novo token recebido. Isso já é o comportamento correto — cada login gera um token novo.

**O que verificar:** confirmar que o método de login sempre chama o método de armazenamento com o token novo, nunca reutiliza um token existente. Buscar por qualquer lógica que cheque "já tem token, não precisa fazer login" — isso seria problemático.

**Arquivos relevantes:** `auth.service.ts`, `login-form.service.ts`.

---

### Fluxo 6 — Não expor dados sensíveis via JWT

**Conceito aplicado:** O payload JWT é visível por qualquer pessoa com o token. Não é o lugar para dados sensíveis.

**O que verificar no projeto:**
Decodificar um token gerado em desenvolvimento e confirmar os campos. O payload deve conter apenas: `sub` (user ID, não email), `exp`, `iat`, `iss`, e a role. O email pode estar presente se usado como identificador, mas deve-se avaliar se é necessário.

**O que nunca deve estar no payload:** senha (mesmo que hashed), CPF, data de nascimento, endereço, número de cartão, dados médicos.

**Arquivos relevantes:** `JwtService.java` (onde o token é construído no backend).

---

## Resumo — Estado do Projeto Após os Sprints

| Vulnerabilidade | Antes | Depois |
|---|---|---|
| `isAuthenticated()` sem validação de `exp` | Tokens expirados permanecem válidos no frontend | Tokens expirados redirecionam para login automaticamente |
| Logout deixa token válido no servidor | Token roubado usável até expirar (ex: 1h) | Ainda limitado pela expiração — blocklist é Fase 2 |
| Sem validação de algoritmo `alg: none` no backend | Risco teórico de bypass se biblioteca desatualizada | Biblioteca moderna validada + teste unitário confirmando rejeição |
| Token em localStorage | Vulnerável a roubo via XSS | Mitigado pelas defesas XSS do Módulo 2 — HttpOnly cookie é Fase 2 |
| JWT secret sem verificação de entropia | Risco de força bruta se secret fraco | Secret verificado com mínimo de 256 bits |
| Payload JWT com possíveis dados extras | Não auditado | Payload mínimo confirmado — apenas sub, exp, iat, iss, roles |
