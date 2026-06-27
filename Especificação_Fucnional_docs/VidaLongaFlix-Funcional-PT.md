# VidaLongaFlix — Especificação Funcional do Sistema

> **Versão:** 1.0 — 17/01/2026
> **Escopo:** Sistema completo (backend Spring Boot + frontend Angular)
> **Complementa:** `VidaLongaFlix-Backend-PT.md` (especificação técnica de API)

---

## 1. Visão Geral do Sistema

O VidaLongaFlix é uma plataforma de streaming de conteúdo voltada para saúde e longevidade. Os usuários assistem vídeos de receitas saudáveis, acessam cardápios nutricionais, curtem conteúdos, comentam e recebem notificações de novidades.

### Arquitetura

```
Usuário (navegador)
    │
    ▼
Angular SPA (CloudFront / S3)
    │  HTTPS
    │  ├── Requisições autenticadas:  Authorization: Bearer {jwt}
    │  ├── Requisições sem Bearer:    cookie XSRF-TOKEN + header X-XSRF-TOKEN (CSRF)
    │  └── Sessão persistida:         cookie httpOnly "token" (JWT, inacessível ao JS)
    ▼
Spring Boot API (Elastic Beanstalk — porta 8090, context /api)
    │  Spring Security:
    │  ├── SecurityFilter: lê JWT do header Bearer ou do cookie "token"
    │  ├── CsrfFilter: valida Double Submit Cookie (isenta requisições Bearer)
    │  └── CsrfCookieFilter: escreve XSRF-TOKEN na resposta de cada request
    │
    ├── PostgreSQL (RDS — produção)
    ├── S3 (mídia — vídeos e imagens)
    └── SMTP (notificações de boas-vindas e recuperação de senha)
```

---

## 2. Perfis de Acesso

| Perfil        | Quem é                                      | Como obtém acesso           |
|---------------|---------------------------------------------|-----------------------------|
| **Anônimo**   | Visitante sem conta ou não autenticado      | Sem login                   |
| **ROLE_USER** | Usuário cadastrado e ativo                  | Cadastro + login com JWT    |
| **ROLE_ADMIN**| Administrador da plataforma                 | Criado pelo sistema no boot |

---

## 3. Matriz de Funcionalidades

| Funcionalidade                          | Anônimo | ROLE_USER | ROLE_ADMIN |
|-----------------------------------------|:-------:|:---------:|:----------:|
| Ver lista de vídeos                     | ✅      | ✅        | ✅         |
| Ver detalhes de um vídeo                | ✅      | ✅        | ✅         |
| Ver lista de cardápios                  | ✅      | ✅        | ✅         |
| Ver detalhes de um cardápio             | ✅      | ✅        | ✅         |
| Ver categorias                          | ✅      | ✅        | ✅         |
| Ver comentários de um vídeo             | ✅      | ✅        | ✅         |
| Registrar visualização em vídeo         | ✅      | ✅        | ✅         |
| Fazer login                             | ✅      | ✅        | ✅         |
| Cadastrar-se                            | ✅      | —         | —          |
| Verificar status de abertura de vagas   | ✅      | —         | —          |
| Sair da fila de espera                  | ✅      | —         | —          |
| Comentar em vídeos                      | ❌      | ✅        | ✅         |
| Adicionar/remover favoritos             | ❌      | ✅        | ✅         |
| Ver lista de favoritos                  | ❌      | ✅        | ✅         |
| Ver notificações                        | ❌      | ✅        | ✅         |
| Marcar notificações como lidas          | ❌      | ✅        | ✅         |
| Ver e editar próprio perfil             | ❌      | ✅        | ✅         |
| Criar vídeo                             | ❌      | ❌        | ✅         |
| Editar vídeo                            | ❌      | ❌        | ✅         |
| Excluir vídeo                           | ❌      | ❌        | ✅         |
| Criar cardápio                          | ❌      | ❌        | ✅         |
| Editar cardápio                         | ❌      | ❌        | ✅         |
| Excluir cardápio                        | ❌      | ❌        | ✅         |
| Criar/editar/excluir categorias         | ❌      | ❌        | ✅         |
| Excluir comentários                     | ❌      | ❌        | ✅         |
| Gerenciar usuários (criar/excluir)      | ❌      | ❌        | ✅         |
| Gerenciar fila de espera                | ❌      | ❌        | ✅         |
| Ver analytics                           | ❌      | ❌        | ✅         |
| Alterar limite de usuários ativos       | ❌      | ❌        | ✅         |

---

## 4. Jornadas do Usuário Comum (ROLE_USER)

### 4.1 Cadastro e Primeiro Acesso

**Pré-condição:** Usuário ainda não possui conta.

**Fluxo principal — há vaga disponível:**

1. Usuário acessa a tela de cadastro e preenche: nome, e-mail, senha e telefone.
2. Sistema verifica disponibilidade de vagas (`count(ACTIVE) < MAX_ACTIVE_USERS`).
3. Backend cria o usuário com status `ACTIVE`, gera o token JWT e retorna HTTP 201.
4. Frontend armazena o token e redireciona para a tela inicial.
5. WhatsApp Business envia mensagem de boas-vindas para o número informado.

**Fluxo alternativo — limite de usuários atingido:**

1. Mesmos passos 1–2.
2. Backend cria o usuário com status `QUEUED`, atribui posição na fila e retorna HTTP 202 sem token.
3. Frontend exibe: *"Você está na posição #N da fila de espera."*
4. Usuário não consegue fazer login até ser promovido para `ACTIVE`.

**Regras de senha:** mínimo 8 caracteres com letra maiúscula, minúscula, número e caractere especial.

---

### 4.2 Login

**Fluxo principal:**

1. Usuário informa e-mail e senha.
2. Backend valida credenciais e retorna token JWT (validade: 2 horas) + dados do usuário.
3. Frontend armazena o token e redireciona para a home.

**Fluxos alternativos:**

| Situação               | Resposta do backend           | Comportamento do frontend                          |
|------------------------|-------------------------------|----------------------------------------------------|
| Credenciais inválidas  | 401                           | Exibe "E-mail ou senha incorretos"                 |
| Status `QUEUED`        | 403 `ACCOUNT_QUEUED`          | Exibe posição na fila e orienta a aguardar         |
| Status `DISABLED`      | 403 `ACCOUNT_DISABLED`        | Exibe mensagem de conta desativada                 |

---

### 4.3 Navegação no Catálogo de Vídeos

1. Tela inicial carrega as categorias de vídeo (`GET /categories?type=VIDEO`).
2. Para cada categoria, exibe um carrossel horizontal com os vídeos correspondentes.
3. Cada card exibe: imagem de capa, título, contador de visualizações e de curtidas.

**Categorias padrão (seed):**

| Categoria          | Qtd. de vídeos |
|--------------------|---------------|
| Bolos Clássicos    | 5             |
| Bolos Especiais    | 4             |
| Receitas Proteicas | 3             |

---

### 4.4 Assistir um Vídeo

1. Usuário clica em um card → tela de detalhes do vídeo (`GET /videos/{id}`).
2. Player carrega o vídeo. Ao iniciar reprodução → `PATCH /videos/{id}/view` (contador +1).
3. Tela exibe: player, título, descrição, receita, informações nutricionais.
4. Seção de comentários abaixo do player (leitura pública, escrita requer login).
5. Botão de favorito (❤️) visível apenas para usuários autenticados.

---

### 4.5 Comentar em um Vídeo

1. Usuário autenticado digita o comentário e clica em "Enviar".
2. Backend cria o comentário (`POST /comments`).
3. Comentário aparece imediatamente na lista.

**Proteção contra duplicatas:** mesmo usuário + mesmo texto + mesmo vídeo → backend retorna 409. Frontend exibe aviso de duplicata.

---

### 4.6 Favoritar Conteúdo

1. Usuário clica no ícone ❤️ em um vídeo ou cardápio.
2. Sistema chama `POST /favorites/{type}/{itemId}`.
3. Comportamento toggle: se não favoritado → adiciona; se já favoritado → remove.
4. Ícone e contador de curtidas atualizam imediatamente na tela.

---

### 4.7 Notificações

1. Ícone de sino no cabeçalho exibe badge com a quantidade de não lidas (`GET /notifications/unread-count`).
2. Ao clicar → painel de notificações paginado (`GET /notifications?page=0&size=20`).
3. Notificações não lidas aparecem destacadas.
4. Ao abrir o painel → `POST /notifications/mark-all-read` → badge zera.

**Geração automática:** toda vez que um admin publica um vídeo ou cardápio, o sistema gera uma notificação para todos os usuários.

---

### 4.8 Gerenciar Perfil

1. Usuário acessa "Meu Perfil" → dados do próprio usuário (`GET /auth/me`).
2. Pode editar: nome, telefone, CPF (opcional), endereço, foto.
3. Salva (`PUT /users/{id}`) → somente o próprio usuário ou um ROLE_ADMIN pode alterar.
4. Dados atualizados refletem imediatamente.

---

### 4.9 Sair da Fila de Espera

1. Usuário com status `QUEUED` acessa a tela de cancelamento de fila.
2. Informa o e-mail e confirma.
3. Sistema chama `DELETE /auth/waitlist/me?email={email}`.
4. Conta é removida e as posições dos demais são recalculadas.

---

## 5. Jornadas do Administrador (ROLE_ADMIN)

### 5.1 Acesso Administrativo

1. Admin faz login com as credenciais configuradas no boot (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).
2. Frontend detecta `ROLE_ADMIN` e habilita funcionalidades exclusivas:
   - Ícone de lápis (✏️) em vídeos e cardápios.
   - Menu ou aba de administração.
   - Acesso às telas de analytics, gerenciamento de usuários e fila.

**Importante:** o ícone de edição e os botões de gerenciamento são exibidos **somente** para ROLE_ADMIN. Usuários comuns nunca veem esses controles.

---

### 5.2 Criar Vídeo

1. Admin acessa Gerenciar Vídeos → "Novo Vídeo".
2. Preenche o formulário:

| Campo                   | Obrigatório |
|-------------------------|:-----------:|
| Título                  | ✅          |
| Descrição               | ✅          |
| Categoria               | ✅          |
| Arquivo de vídeo ou URL | ✅          |
| Imagem de capa ou URL   | ✅          |
| Receita                 | ❌          |
| Informações nutricionais| ❌          |

3. Frontend envia `POST /admin/videos` (JSON ou `multipart/form-data` se houver upload de arquivo).
4. Backend armazena o arquivo no S3 (produção), salva o vídeo e **gera notificação para todos os usuários**.
5. Admin retorna à lista de vídeos.

---

### 5.3 Editar Vídeo

1. Admin clica no ícone ✏️ no card ou na tela de detalhes.
2. Formulário pré-preenchido com os dados atuais.
3. Admin altera apenas os campos desejados (atualização parcial).
4. Salva → `PUT /admin/videos/{id}`.

---

### 5.4 Excluir Vídeo

1. Admin clica em excluir → sistema exibe confirmação.
2. Admin confirma → `DELETE /admin/videos/{id}`.
3. Vídeo removido da lista.

---

### 5.5 Gerenciar Cardápios

Fluxo idêntico ao de vídeos (seções 5.2–5.4) usando os endpoints `/admin/menus`.

**Campo adicional exclusivo dos cardápios:** `nutritionistTips` (dicas do nutricionista, texto livre, opcional).

---

### 5.6 Gerenciar Categorias

1. Admin acessa Gerenciar Categorias.
2. Pode criar (nome + tipo: `VIDEO` ou `MENU`), renomear ou excluir categorias.
3. Restrição: duas categorias com mesmo nome e tipo retornam 409.

---

### 5.7 Gerenciar Fila de Espera

Tela exibe: limite atual, quantidade de usuários ativos e lista da fila por posição.

| Ação                          | Endpoint                                  | Efeito                                              |
|-------------------------------|-------------------------------------------|-----------------------------------------------------|
| Ativar usuário manualmente    | `POST /admin/waitlist/{userId}/activate`  | Promove QUEUED → ACTIVE (se houver vaga)            |
| Remover usuário da fila       | `DELETE /admin/waitlist/{userId}`         | Remove e recalcula posições                         |
| Alterar limite de ativos      | `PUT /admin/config/max-users`             | Se abrir vagas, backend promove automaticamente (FIFO)|

---

### 5.8 Gerenciar Usuários

| Ação                   | Endpoint              | Quem pode usar    |
|------------------------|-----------------------|-------------------|
| Buscar por ID          | `GET /users/{id}`     | ADMIN ou próprio usuário |
| Criar diretamente      | `POST /users`         | Somente ADMIN     |
| Editar qualquer perfil | `PUT /users/{id}`     | ADMIN ou próprio usuário |
| Excluir usuário        | `DELETE /users/{id}`  | Somente ADMIN     |

---

### 5.9 Excluir Comentários

1. Admin visualiza os comentários de qualquer vídeo.
2. Clica em excluir em um comentário inadequado → `DELETE /comments/{commentId}`.
3. Comentário removido permanentemente.

---

### 5.10 Analytics

| Seção        | Dado                                   | Endpoint                                              |
|--------------|----------------------------------------|-------------------------------------------------------|
| Vídeos       | Mais assistidos                        | `GET /admin/videos/most-viewed`                       |
| Vídeos       | Menos assistidos                       | `GET /admin/videos/least-viewed`                      |
| Vídeos       | Visualizações por categoria            | `GET /admin/videos/views-by-category`                 |
| Vídeos       | Tempo médio assistido                  | `GET /admin/videos/{id}/tempo-medio-assistido`        |
| Vídeos       | Com mais comentários                   | `GET /admin/videos/mais-comentados`                   |
| Comentários  | Total geral                            | `GET /admin/comentarios/total`                        |
| Comentários  | Total por vídeo                        | `GET /admin/comentarios/total-por-video`              |

---

## 6. Telas do Sistema

### 6.1 Telas Públicas

| Tela                    | Rota                    | Descrição                                        |
|-------------------------|-------------------------|--------------------------------------------------|
| Home / Catálogo         | `/`                     | Carrosséis de vídeos por categoria               |
| Detalhes do Vídeo       | `/videos/:id`           | Player + info + comentários                      |
| Catálogo de Cardápios   | `/menus`                | Lista de cardápios                               |
| Detalhes do Cardápio    | `/menus/:id`            | Receita + info nutricional + dicas               |
| Login                   | `/login`                | Formulário de login                              |
| Cadastro                | `/register`             | Formulário de cadastro + feedback de fila        |
| Status de Vagas         | `/registration-status`  | Indicador de abertura de vagas                   |

### 6.2 Telas Autenticadas

| Tela            | Rota              | Descrição                                     |
|-----------------|-------------------|-----------------------------------------------|
| Perfil          | `/profile`        | Dados do usuário + formulário de edição       |
| Favoritos       | `/favorites`      | Vídeos e cardápios favoritados                |
| Notificações    | `/notifications`  | Histórico com badge de não lidas              |

### 6.3 Telas Administrativas

| Tela                    | Rota                    | Descrição                                     |
|-------------------------|-------------------------|-----------------------------------------------|
| Gerenciar Vídeos        | `/admin/videos`         | CRUD de vídeos com upload de mídia            |
| Gerenciar Cardápios     | `/admin/menus`          | CRUD de cardápios                             |
| Gerenciar Categorias    | `/admin/categories`     | CRUD de categorias                            |
| Gerenciar Usuários/Fila | `/admin/waitlist`       | Fila de espera + promoção de usuários         |
| Analytics               | `/admin/analytics`      | Dashboards de visualizações e comentários     |

---

## 7. Segurança

### 7.1 Controles no Frontend

| Situação                                          | Comportamento esperado                              |
|---------------------------------------------------|-----------------------------------------------------|
| Token JWT expirado (2h)                           | Redirect automático para `/login`                   |
| Usuário sem token acessa rota privada             | Redirect para `/login`                              |
| ROLE_USER tenta acessar rota `/admin/**`          | Redirect para home ou página 403                    |
| Ícone de edição (✏️) em vídeos e cardápios        | Visível **somente** para ROLE_ADMIN                 |
| Botão de excluir comentário                       | Visível **somente** para ROLE_ADMIN                 |
| Botão de favorito (❤️)                            | Visível **somente** para usuários autenticados      |
| Campo de comentário                               | Habilitado **somente** para usuários autenticados   |
| Requisições autenticadas                          | Header `Authorization: Bearer {token}` via `auth.interceptor.ts` |
| POST / PATCH / DELETE sem Bearer                  | Angular inclui header `X-XSRF-TOKEN` automaticamente via `withXsrfConfiguration()` |
| Abertura do app com token legado no localStorage  | `loadSession()` apaga o token imediatamente (proteção contra XSS pré-migração) |
| Logout                                            | Chama `POST /auth/logout` (backend apaga cookie httpOnly) + limpa localStorage |

---

### 7.2 Controles no Backend (Spring Security)

| Camada                   | Mecanismo                                                              |
|--------------------------|------------------------------------------------------------------------|
| Autenticação             | JWT validado pelo `SecurityFilter` — lê do header `Authorization: Bearer` ou do cookie `token` |
| Proteção CSRF            | `CookieCsrfTokenRepository` — padrão Double Submit Cookie              |
| Isenção CSRF             | Requisições com header `Authorization: Bearer` são automaticamente isentas |
| Cookie de sessão         | `token` — `httpOnly=true`, `secure=true`, `sameSite=None`, `path=/`   |
| Cookie CSRF              | `XSRF-TOKEN` — `httpOnly=false` (Angular precisa ler via JS), `sameSite=Strict` |
| Senhas                   | BCrypt                                                                 |
| CORS                     | Origens restritas às configuradas em `CORS_ALLOWED_ORIGINS`            |
| Headers HTTP             | `X-Frame-Options: DENY`, `X-Content-Type-Options`, `HSTS 1 ano`, `Referrer-Policy` |
| Actuator                 | `/health` e `/info` públicos; demais endpoints exigem `ROLE_ADMIN`     |

---

### 7.3 Proteção CSRF — Como Funciona (Double Submit Cookie)

**O problema que o CSRF resolve:**

Sem proteção CSRF, um site malicioso pode forçar o navegador da vítima a disparar uma requisição para a API enquanto o cookie de sessão é enviado automaticamente pelo browser:

```
site-malicioso.com → <form action="https://api.vidalongaflix.com.br/api/auth/logout" method="POST">
navegador da vítima envia o cookie token automaticamente → logout executado sem o usuário saber
```

**A solução implementada — Double Submit Cookie:**

```
1. Angular faz qualquer GET  →  backend seta cookie XSRF-TOKEN (JS pode ler)
2. Angular faz POST/PATCH/DELETE  →  envia:
      cookie:  XSRF-TOKEN=abc123   (browser envia automaticamente)
      header:  X-XSRF-TOKEN: abc123  (Angular lê o cookie e copia no header)
3. Backend compara cookie == header
      ✅ iguais  → requisição autorizada
      ❌ diferentes ou header ausente → 403 Forbidden
```

O site malicioso consegue forçar o browser a enviar o cookie, mas **não consegue ler o cookie XSRF-TOKEN** (política Same-Origin do browser) e portanto não consegue incluir o header `X-XSRF-TOKEN` com o valor correto.

**Implementação no backend (`SecurityConfig.java`):**

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // XSRF-TOKEN legível pelo JS
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
    .ignoringRequestMatchers(request -> {
        String auth = request.getHeader("Authorization");
        return auth != null && auth.startsWith("Bearer ");  // Bearer já é seguro, isenta CSRF
    })
)
.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class) // força escrita do cookie na resposta
```

**Implementação no frontend (`app.config.ts`):**

```typescript
provideHttpClient(
  withInterceptors([authInterceptor]),
  withXsrfConfiguration({
    cookieName: 'XSRF-TOKEN',    // mesmo nome configurado no backend
    headerName: 'X-XSRF-TOKEN'  // mesmo header que o backend valida
  })
)
```

O Angular lê o cookie `XSRF-TOKEN` e inclui o header `X-XSRF-TOKEN` automaticamente em todo POST/PATCH/PUT/DELETE.

---

### 7.4 Proteção XSS — Migração do localStorage para Cookie httpOnly

**O problema que existia:**

Antes da migração, o token JWT ficava em `localStorage`. Qualquer script rodando na página conseguia lê-lo:

```javascript
// Ataque XSS: script injetado na página
localStorage.getItem('token') // → 'eyJhbGci...' (token real do usuário)
// atacante envia para seu servidor e assume a sessão da vítima
```

**A solução: cookie httpOnly**

O backend (`AuthController.java`) passa a retornar o token em cookie marcado como `httpOnly`:

```java
ResponseCookie.from("token", token)
    .httpOnly(true)   // JavaScript não consegue ler — XSS não roubar o token
    .secure(true)     // apenas HTTPS
    .sameSite("None") // necessário para cross-origin (frontend no CloudFront, backend no EB)
    .path("/")
    .build()
```

O backend ainda retorna o `token` no corpo JSON para manter compatibilidade com o fluxo Bearer existente. O `SecurityFilter` aceita JWT tanto do header `Authorization: Bearer` quanto do cookie `token`.

**Limpeza de token legado no startup (`auth.service.ts`):**

Usuários que fizeram login antes da migração podiam ainda ter o JWT no `localStorage`. A correção garante que ao abrir o app, qualquer token legado é removido imediatamente — sem esperar login ou logout:

```typescript
private loadSession() {
    // Remove tokens legados do localStorage (migração para cookie httpOnly)
    localStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.TOKEN_KEY);
    // ... restante da lógica de sessão
}
```

**Logout real (`auth.service.ts`):**

O logout agora chama o endpoint do backend para que o servidor apague o cookie httpOnly (JavaScript não consegue apagar um cookie `httpOnly` diretamente):

```typescript
logout() {
    await firstValueFrom(this.http.post(`${this.api.baseURL}/auth/logout`, {}));
    this.clearSession();
    this.router.navigate(['/authorization']);
}
```

O backend (`AuthController.java`) apaga o cookie setando `maxAge(0)`:

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletResponse response) {
    ResponseCookie expired = ResponseCookie.from("token", "")
        .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
    response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
    return ResponseEntity.ok().build();
}
```

---

### 7.5 Ciclo TDD Aplicado às Mudanças de Segurança

As mudanças de segurança foram desenvolvidas seguindo TDD (Test-Driven Development):

**Sequência:**
1. Escrever teste → teste **falha** (vermelho)
2. Escrever o mínimo de código para passar → teste **passa** (verde)
3. Refatorar se necessário

**Testes criados para CSRF (`CsrfProtectionIntegrationTest.java` — backend):**

| Teste | O que verifica |
|-------|---------------|
| `shouldSetCsrfCookieOnGetRequest` | GET retorna cookie `XSRF-TOKEN` sem `httpOnly` |
| `shouldAllowGetRequestWithoutCsrfToken` | GET não precisa de CSRF |
| `shouldRejectPostWithoutCsrfToken` | POST sem token → 403 |
| `shouldRejectPostWithMismatchedCsrfToken` | Cookie ≠ header → 403 |
| `shouldAllowPostWithValidCsrfToken` | Cookie = header → 200 |

> `@DirtiesContext(BEFORE_CLASS)` foi necessário porque `with(csrf())` do Spring Security Test usa reflexão para substituir o `CsrfTokenRepository` no contexto compartilhado — o contexto fresco garante o `CookieCsrfTokenRepository` original.

**Testes criados para CSRF (`auth.interceptor.spec.ts` — frontend):**

| Teste | O que verifica |
|-------|---------------|
| `#195 POST para API com cookie CSRF` | Angular inclui `X-XSRF-TOKEN` automaticamente |
| `#196 GET para API` | GET não inclui header CSRF |

**Testes criados para limpeza de token legado (`auth.service.spec.ts` — frontend):**

| Teste | O que verifica |
|-------|---------------|
| `#305 loadSession remove token legado do localStorage` | Token antigo apagado no startup |
| `#306 loadSession remove token legado do sessionStorage` | Idem para sessionStorage |

---

## 8. Estados do Usuário

```
[Cadastro]
    │
    ├── há vaga ──────► ACTIVE ◄──── Admin ativa da fila
    │                     │
    └── sem vaga ──► QUEUED ──────── Admin remove ──► (excluído)
                          │
                          └── Admin ativa ──► ACTIVE

ACTIVE   → pode fazer login, assistir, comentar, favoritar
QUEUED   → não consegue fazer login; aguarda promoção
DISABLED → não consegue fazer login; conta desativada
```

---

## 9. Regras de Negócio Consolidadas

| Código | Regra                                                                                               |
|--------|-----------------------------------------------------------------------------------------------------|
| RN-01  | Token JWT tem validade de 2 horas. Após expirar, o usuário precisa fazer login novamente.           |
| RN-02  | Senhas são armazenadas com hash BCrypt. Nunca em texto puro.                                        |
| RN-03  | O limite de usuários ativos é configurável pelo admin sem redeploy.                                 |
| RN-04  | Quando o limite sobe, o backend promove automaticamente os primeiros da fila (FIFO).                |
| RN-05  | Um usuário não pode comentar o mesmo texto duas vezes no mesmo vídeo.                               |
| RN-06  | Favorito é toggle: segunda chamada remove; não existe estado duplicado.                             |
| RN-07  | Notificações são globais: todos os usuários recebem quando um novo conteúdo é publicado.            |
| RN-08  | Notificação é "lida" se foi criada antes do campo `notificationsLastReadAt` do usuário.             |
| RN-09  | Admin criado no boot usa credenciais de variáveis de ambiente (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).   |
| RN-10  | ROLE_USER só edita e consulta o próprio perfil. ROLE_ADMIN gerencia qualquer usuário.               |
| RN-11  | Upload de vídeo/capa vai para S3 em produção; URL definitiva é retornada pelo backend.              |
| RN-12  | Falha no envio de WhatsApp não bloqueia o cadastro do usuário.                                      |
| RN-13  | CSRF protege todos os POST/PATCH/PUT/DELETE sem Bearer token via padrão Double Submit Cookie.        |
| RN-14  | O token JWT é retornado no corpo JSON **e** em cookie `httpOnly` — backend aceita ambos.            |
| RN-15  | Ao inicializar o app, tokens legados no localStorage/sessionStorage são apagados imediatamente.     |
| RN-16  | Logout chama `POST /auth/logout` — somente o servidor pode apagar um cookie `httpOnly`.             |
| RN-17  | O cookie `XSRF-TOKEN` deve ser `httpOnly=false` — Angular precisa lê-lo via JavaScript.            |
| RN-18  | O cookie `token` (JWT) deve ser `httpOnly=true` — JavaScript nunca deve ter acesso direto.         |

---

## 10. Estrutura de Pacotes

O projeto segue uma organização por **domínio funcional**, espelhando a mesma estrutura nos testes. Cada camada (`controllers`, `services`, `domain`) é subdividida pelos mesmos nomes de domínio — facilitando navegar, adicionar e testar funcionalidades.

```
src/main/java/.../VidaLongaFlix/
│
├── controllers/
│   ├── auth/          Autenticação, registro, logout, recuperação de senha
│   ├── admin/         CRUD de vídeos e cardápios, gestão da fila de espera
│   ├── analytics/     Relatórios de visualizações e comentários (somente ADMIN)
│   ├── content/       Endpoints públicos de vídeos, cardápios e categorias
│   ├── user/          Perfil do usuário autenticado
│   ├── interaction/   Comentários, favoritos e notificações
│   └── imports/       Importação em massa de conteúdo via JSON
│
├── services/
│   ├── auth/          Regras de negócio de usuário e controle de limite de vagas
│   ├── content/       Lógica de vídeos, cardápios e categorias
│   ├── interaction/   Lógica de comentários, favoritos e notificações
│   ├── email/         Interface EmailService + implementações (SMTP, log, boas-vindas, fila)
│   ├── password/      Geração de token, validação e envio de e-mail de reset
│   └── media/         Upload para S3 e importação de conteúdo
│
├── repositories/      Interfaces JPA — mantidas planas (10 repositórios, sem subpastas)
│
├── domain/            Entidades JPA e DTOs organizados por contexto:
│   ├── address/       Endereço e enum de Estado
│   ├── auth/          DTOs de login, registro e resposta de autenticação
│   ├── category/      Entidade Category, enum CategoryType e DTOs
│   ├── comment/       Entidade Comment e DTOs
│   ├── config/        AppConfig (configurações dinâmicas, ex.: limite de usuários)
│   ├── email/         EmailMessage (modelo de e-mail transacional)
│   ├── favorite/      Entidade UserFavorite, enum FavoriteContentType e DTOs
│   ├── imports/       DTO de resultado de importação em massa
│   ├── menu/          Entidade Menu e DTOs
│   ├── message/       Message e enum DeliveryStatus (histórico de envios)
│   ├── notification/  Entidade Notification, enum NotificationType e DTOs
│   ├── passwordreset/ Entidade PasswordResetToken e DTOs de reset
│   ├── payment/       Entidade Payment e enum PaymentStatus (reservado para fase futura)
│   ├── user/          Entidade User, enum Role, enum UserStatus e DTOs
│   ├── video/         Entidade Video e DTOs
│   └── waitlist/      DTOs de fila de espera e configuração de limite
│
└── infra/
    ├── config/        DataInitializer, FlywayConfig, MediaResourceConfig, ObservabilityConfig
    ├── exception/     Exceções customizadas e handlers globais (por tipo: auth, comment, db, resource)
    └── security/      SecurityConfig, SecurityFilter, CsrfCookieFilter, TokenService, CorsConfig
```

### Regra de correspondência testes ↔ main

Os testes unitários espelham exatamente o mesmo domínio:

| Pacote de teste          | Corresponde a                              |
|--------------------------|--------------------------------------------|
| `userTest/controller`    | `controllers/auth/` + `controllers/user/`  |
| `userTest/service`       | `services/auth/`                           |
| `videoTest/controller`   | `controllers/content/` (vídeos)            |
| `videoTest/service`      | `services/content/` (vídeos)              |
| `categoryTest/*`         | `controllers/content/` + `services/content/` (categorias) |
| `commentTest/*`          | `controllers/interaction/` + `services/interaction/` (comentários) |
| `favoriteTest/*`         | `controllers/interaction/` + `services/interaction/` (favoritos) |
| `menuTest/*`             | `controllers/content/` + `controllers/admin/` + `services/content/` |
| `emailTest/service`      | `services/email/`                          |
| `passwordResetTest/*`    | `controllers/auth/` + `services/password/` |
| `importTest/*`           | `controllers/imports/` + `services/media/` |
| `integration/**`         | Testes de ponta a ponta cobrindo todos os domínios |

---

## 11. Histórico de Revisões

| Data       | Descrição                                            | Responsável |
|------------|------------------------------------------------------|-------------|
| 17/05/2026 | Criação do documento — cobertura completa do sistema                                                   | Fabricio    |
| 27/06/2026 | Seção 7 expandida: CSRF, cookie httpOnly, XSS, TDD. RN-13 a RN-18 adicionadas                         | Fabricio    |
| 27/06/2026 | Seção 10 adicionada: estrutura de pacotes por domínio com tabela de correspondência testes ↔ main      | Fabricio    |