# Segurança de Aplicações Web — Auditoria Real do VidaLongaFlix

> Auditoria realizada em 2026-04-18 com base no código real do backend (Spring Boot) e frontend Angular.
> Cada item tem evidência de código, arquivo e linha. Nada especulativo.

---

## Resumo Executivo

| Área | Status | Itens críticos |
|---|---|---|
| SQL Injection | ✅ Seguro | JPA usa Prepared Statements automaticamente |
| Command Injection | ✅ Seguro | Nenhuma chamada ao SO encontrada |
| SSTI | ✅ Seguro | API REST pura — sem template engine |
| XSS no Angular | ✅ Seguro | Zero usos de `[innerHTML]` ou `bypassSecurityTrust` |
| Secrets / Credenciais | ✅ Seguro | Todos externalizados via env vars |
| Dependências (CVEs) | ✅ Atualizado | 4 CVEs já corrigidos via override no pom.xml |
| CORS | ✅ Seguro | Whitelist explícita por ambiente |
| JWT Storage | ⚠️ Atenção | Token em `localStorage` — risco se houver XSS |
| Endpoints públicos | ⚠️ Atenção | 3 endpoints deveriam exigir autenticação |
| Actuator exposto | ⚠️ Atenção | `/actuator/**` sem autenticação em produção |
| Security Headers | ❌ Ausente | Headers HTTP desabilitados no Spring Security |
| Token expiry (frontend) | ⚠️ Atenção | `isAuthenticated()` não valida expiração do JWT |

---

## O que está protegido — confirmado no código

### SQL Injection — ✅ SAFE
Todos os repositórios usam Spring Data JPA com parâmetros nomeados ou métodos derivados. O JPA gera Prepared Statements automaticamente.

```java
// VideoRepository.java — JPQL parametrizado, sem concatenação
@Query("SELECT COUNT(v) FROM Video v WHERE v.category.id = :categoryId")
Long countViewsByCategoryId(@Param("categoryId") Long categoryId);

// UserRepository.java — parâmetro nomeado
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findByStatus(@Param("status") UserStatus status);

// AppConfigRepository.java — pessimistic lock para concorrência
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM AppConfig a WHERE a.configKey = :key")
Optional<AppConfig> findByKeyWithLock(@Param("key") String key);
```

Nenhum `nativeQuery = true` com input do usuário. Nenhum `createNativeQuery` com concatenação.

---

### Secrets — ✅ SAFE
Todos os valores sensíveis lidos de variáveis de ambiente, sem defaults perigosos em prod.

```java
// TokenService.java
@Value("${api.security.token.secret}")   // sem default → falha se não configurado
private String secret;

// WhatsAppService.java
@Value("${whatsapp.access-token}")       // token da API externo — env var
private String accessToken;
```

`application.properties` (dev) tem defaults apenas para desenvolvimento local:
- `api.security.token.secret` → sem default (obrigatório)
- `admin.password` → `Admin@123456` (apenas dev, sobrescrito em prod)

---

### CORS — ✅ SAFE
```java
// CorsConfig.java — lê origins do env var
@Value("${cors.allowed-origins:http://localhost:4200}")
private String allowedOrigins;

registry.addMapping("/**")
    .allowedOrigins(origins)  // whitelist explícita, não "*"
    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
    .allowCredentials(true);
```

Em produção: `CORS_ALLOWED_ORIGINS=https://vidalongaflix.com,https://www.vidalongaflix.com,https://d2efnb2x9r22my.cloudfront.net`

---

### Rate Limiting — ✅ SAFE
```java
// LoginRateLimitFilter.java
// 5 tentativas por IP por minuto, retorna HTTP 429
// Lê IP real do X-Forwarded-For (CloudFront/ALB)
// Localhost (127.0.0.1, ::1) é isento
```

---

### XSS Angular — ✅ SAFE
Grep em todo o repositório frontend: **zero ocorrências** de `[innerHTML]` ou `bypassSecurityTrust`. Todos os templates usam `{{ }}` (interpolação padrão Angular — escapa automaticamente).

---

### Dependências — ✅ ATUALIZADO
CVEs corrigidos via override no `pom.xml`:

| CVE | Dependência | Fix aplicado |
|---|---|---|
| CVE-2025-48734 | commons-beanutils (via opencsv) | override → 1.11.0 |
| CVE-2025-48924 | commons-lang3 | override → 3.18.0 |
| CVE-2025-53864 | nimbus-jose-jwt | override → 10.0.2 |
| GHSA-72hv | jackson-core | override → 2.21.1 |

---

## O que precisa ser corrigido

### 1. Security Headers HTTP — ❌ AUSENTE (Alta Prioridade)

**Evidência:**
```java
// SecurityConfig.java — linha 58
http.headers(headers -> headers.disable());  // ← desabilitado completamente
```

Isso remove proteções que o Spring Security ativaria por padrão: `X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`, etc.

**Correção em `SecurityConfig.java`:**
```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true))
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
);
```

---

### 2. `/actuator/**` sem autenticação — ⚠️ MÉDIO-ALTO

**Evidência:**
```java
// SecurityConfig.java — linha 42
.requestMatchers("/actuator/**").permitAll()
```

Em produção, `/actuator/prometheus` (métricas), `/actuator/health` (detalhes de infra) e `/actuator/info` ficam acessíveis sem autenticação.

**Correção em `SecurityConfig.java`:**
```java
// Só o health check fica público (necessário para o health check do ELB/EB)
.requestMatchers("/actuator/health").permitAll()
.requestMatchers("/actuator/**").hasRole("ADMIN")
```

---

### 3. Endpoints de usuário sem autenticação — ⚠️ MÉDIO

**Evidência — `UserController.java`:**

| Endpoint | Método | Status atual | Deveria ser |
|---|---|---|---|
| `/users/{id}` | GET | `permitAll()` | `authenticated()` — risco de enumeração |
| `/users/{id}` | PUT | `permitAll()` | `authenticated()` + validar que `id == token.sub` |
| `/users/{id}` | DELETE | `permitAll()` | `hasRole('ADMIN')` ou `id == token.sub` |

**Correc̃ao — adicionar em `SecurityConfig.java`:**
```java
// Antes do bloco anyRequest().authenticated()
.requestMatchers(HttpMethod.GET, "/users/{id}").authenticated()
.requestMatchers(HttpMethod.PUT, "/users/{id}").authenticated()
.requestMatchers(HttpMethod.DELETE, "/users/{id}").hasRole("ADMIN")
```

**E no controller, validar que o usuário só edita a si mesmo:**
```java
// UserController.java — PUT /users/{id}
@PutMapping("/{id}")
public ResponseEntity<UserResponseDTO> update(
        @PathVariable Long id,
        @RequestBody UserUpdateDTO dto,
        @AuthenticationPrincipal User currentUser) {
    // Validar que o usuário está editando apenas seu próprio perfil
    if (!currentUser.getId().equals(id) && !currentUser.hasRole("ROLE_ADMIN")) {
        throw new AccessDeniedException("Forbidden");
    }
    // ...
}
```

---

### 4. POST `/comments` público — ⚠️ MÉDIO

**Evidência — `SecurityConfig.java`:**
```java
// Linha atual — GET é público, mas POST também fica sem autenticação
// porque não há regra específica para POST /comments
.requestMatchers(HttpMethod.GET, "/comments/**").permitAll()
.requestMatchers(HttpMethod.DELETE, "/comments/**").hasRole("ADMIN")
```

Qualquer pessoa (sem conta) pode postar comentários. Sem auditoria de quem comentou.

**Correção em `SecurityConfig.java`:**
```java
.requestMatchers(HttpMethod.POST, "/comments").authenticated()
```

**E no `CommentController.java` adicionar o autor:**
```java
@PostMapping
public ResponseEntity<CommentResponseDTO> create(
        @RequestBody CommentRequestDTO dto,
        @AuthenticationPrincipal User currentUser) { // ← adicionar
    // passar currentUser para o service
}
```

---

### 5. JWT em `localStorage` — ⚠️ MÉDIO (Fase 2)

**Evidência — `auth.service.ts`:**
```typescript
private readonly TOKEN_KEY = 'token';
private readonly USER_KEY  = 'user';

// Login com "keepLoggedIn=true" (padrão) → localStorage
// Login com "keepLoggedIn=false" → sessionStorage
private saveSession(token: string, user: User, storage: 'local' | 'session') {
  const primary = storage === 'local' ? localStorage : sessionStorage;
  primary.setItem(this.TOKEN_KEY, token);
  primary.setItem(this.USER_KEY, JSON.stringify(user));  // ← objeto completo com roles, email, phone
}
```

O objeto `user` completo (incluindo `roles`, `email`, `phone`) fica serializado em `localStorage`. Se houver qualquer XSS (mesmo de uma dependência npm), o atacante obtém o token e todos os dados do usuário.

**Hoje o risco é baixo** porque não há `[innerHTML]` e as dependências estão atualizadas. Mas a postura ideal é `HttpOnly cookie`.

**Caminho para Fase 2 — backend:**
```java
// AuthController.java — retornar cookie HttpOnly em vez de body
ResponseCookie cookie = ResponseCookie.from("auth_token", token)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")
    .path("/")
    .maxAge(Duration.ofHours(2))
    .build();

return ResponseEntity.ok()
    .header(HttpHeaders.SET_COOKIE, cookie.toString())
    .body(new UserResponseDTO(user));  // retorna só user, sem token
```

**Caminho para Fase 2 — Angular:**
```typescript
// auth.interceptor.ts — remover setHeaders Authorization
// O cookie HttpOnly é enviado automaticamente pelo browser
// Adicionar withCredentials: true no HttpClient
```

> Esta mudança requer também configurar CSRF protection no Spring Security (voltaria a ser necessário com cookies).

---

### 6. `isAuthenticated()` não valida expiração do JWT — ⚠️ BAIXO

**Evidência — `auth.guard.ts` + `auth.service.ts`:**
```typescript
// auth.service.ts
isAuthenticated(): boolean {
  return !!this.getToken();  // ← só checa se existe, não se expirou
}

// auth.guard.ts
if (authService.isAuthenticated()) {
  return true;  // token expirado de 3 dias atrás passa aqui
}
```

Um token expirado ficará no `localStorage` e passará pelo guard. O backend vai rejeitar (401), mas a UX é ruim e o usuário pode tentar ações que falharão.

**Correção em `auth.service.ts`:**
```typescript
isAuthenticated(): boolean {
  const token = this.getToken();
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 > Date.now();  // valida expiração
  } catch {
    return false;
  }
}
```

---

### 7. `fake_token_123` — dead code para remover — ⚠️ BAIXO

**Evidência — `login-form.service.ts`:**
```typescript
// LoginFormService.register() — contém código morto de desenvolvimento
// fake_token_123 — nunca usado em produção, mas poluí o código
```

**Correção**: remover o dead code de `login-form.service.ts`.

---

### 8. Discrepância de URL no frontend — ⚠️ VERIFICAR

**Evidência — `environment.prod.ts`:**
```typescript
apiUrl: 'https://api.vidalongaflix.com/api',  // .com
```

Mas o domínio real da API é `https://api.vidalongaflix.com.br/api` (`.com.br`). Verificar qual é o correto e alinhar o arquivo de ambiente com o domínio real configurado no Route 53/CloudFront.

---

## Passo a Passo Prático

### Passo 1 — Security Headers (1h) — HOJE

**Arquivo:** `src/main/java/com/dvFabricio/VidaLongaFlix/infra/security/SecurityConfig.java`

Substituir `headers.disable()` pela configuração completa de headers (código no item 1 acima).

**Verificação pós-aplicação:**
```bash
curl -I https://api.vidalongaflix.com.br/api/videos | grep -E "X-Frame|X-Content|Strict-Transport|Content-Security"
```

---

### Passo 2 — Restringir Actuator (30min) — HOJE

**Arquivo:** `SecurityConfig.java`

Trocar `.requestMatchers("/actuator/**").permitAll()` pelas duas linhas do item 2.

**Verificação:**
```bash
# Deve retornar 200
curl https://api.vidalongaflix.com.br/api/actuator/health

# Deve retornar 401 ou 403
curl https://api.vidalongaflix.com.br/api/actuator/prometheus
```

---

### Passo 3 — Endpoints de Usuário com Auth (1h) — HOJE

**Arquivos:** `SecurityConfig.java` + `UserController.java`

1. Adicionar as regras de autorização em `SecurityConfig.java` (item 3)
2. Adicionar `@AuthenticationPrincipal User currentUser` nos métodos PUT e DELETE do `UserController.java`
3. Validar que `currentUser.getId().equals(id)` antes de permitir a operação

**Verificação:**
```bash
# Sem token — deve retornar 401
curl -X PUT https://api.vidalongaflix.com.br/api/users/1 -H "Content-Type: application/json" -d '{}'

# Com token de outro usuário — deve retornar 403
curl -X PUT https://api.vidalongaflix.com.br/api/users/1 \
  -H "Authorization: Bearer <token_de_usuario_2>" \
  -H "Content-Type: application/json" -d '{}'
```

---

### Passo 4 — Autenticar POST /comments (30min) — HOJE

**Arquivos:** `SecurityConfig.java` + `CommentController.java`

1. Adicionar regra `.requestMatchers(HttpMethod.POST, "/comments").authenticated()` em `SecurityConfig.java`
2. Adicionar `@AuthenticationPrincipal User currentUser` no método `create` do `CommentController.java`

**Verificação:**
```bash
# Sem token — deve retornar 401
curl -X POST https://api.vidalongaflix.com.br/api/comments \
  -H "Content-Type: application/json" \
  -d '{"videoId":1,"content":"test"}'
```

---

### Passo 5 — Corrigir `isAuthenticated()` no Angular (30min) — HOJE

**Arquivo:** `src/app/auth/services/auth.service.ts`

Substituir o método `isAuthenticated()` pela versão que decodifica o payload JWT e valida `exp` (código no item 6 acima).

**Verificação manual**: fazer login, aguardar o token expirar (2h), recarregar a página — deve redirecionar para `/authorization` automaticamente.

---

### Passo 6 — Remover dead code `fake_token_123` (10min) — HOJE

**Arquivo:** `src/app/auth/services/login-form.service.ts`

Remover qualquer bloco que contenha `fake_token_123`.

---

### Passo 7 — Verificar URL da API em produção (15min) — HOJE

**Arquivo:** `src/environments/environment.prod.ts`

Confirmar se o domínio correto é `.com` ou `.com.br` e corrigir se necessário:
```typescript
apiUrl: 'https://api.vidalongaflix.com.br/api',  // confirmar domínio real
```

---

### Passo 8 — OWASP Dependency Check no CI (2h) — PRÓXIMO SPRINT

Adicionar no `.github/workflows/ci.yml`:

```yaml
- name: OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'VidaLongaFlix'
    path: '.'
    format: 'HTML'
    args: '--failBuildOnCVSS 7 --suppression suppression.xml'

- name: Upload Dependency Check Report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: dependency-check-report
    path: reports/
```

---

### Passo 9 — JWT em HttpOnly Cookie (4h) — FASE 2

Migração completa descrita nos itens 5. Envolve:
- Backend: `AuthController.java` retorna `Set-Cookie` em vez de `token` no body
- Frontend: `auth.service.ts` remove armazenamento local do token; `auth.interceptor.ts` usa `withCredentials: true`
- Spring Security: reativar CSRF (necessário com cookies)
- CORS: ajustar `allowCredentials(true)` (já configurado)

---

## Ordem de execução recomendada

```
Hoje (2-3h total):
  ├── Passo 1 → Security Headers           (1h)
  ├── Passo 2 → Restringir Actuator        (30min)
  ├── Passo 3 → Endpoints User com Auth    (1h)
  ├── Passo 4 → POST /comments autenticado (30min)
  ├── Passo 5 → isAuthenticated() com exp  (30min)  ← frontend
  ├── Passo 6 → Remover fake_token_123     (10min)  ← frontend
  └── Passo 7 → Verificar URL da API       (15min)  ← frontend

Próximo sprint:
  └── Passo 8 → OWASP Dep. Check CI       (2h)

Fase 2:
  └── Passo 9 → JWT HttpOnly Cookie        (4h)
```

---

## Status de conformidade após aplicar todos os passos

| Vulnerabilidade do Curso | Aplicável? | Status Final |
|---|---|---|
| SQL Injection via ORM | Sim | ✅ Já seguro (JPA PS automático) |
| NoSQL Injection | Não | — PostgreSQL relacional |
| Command Injection | Não | ✅ Sem exec/ProcessBuilder |
| SSTI | Não | ✅ API REST pura |
| XSS (Angular) | Sim | ✅ Sem `[innerHTML]` — seguro |
| Broken Access Control (SPA) | Sim | ✅ authGuard + adminGuard + Passo 3 |
| Token exposto browser | Sim | ⚠️ Passo 5 + Fase 2 (Passo 9) |
| IAM excessivo (FaaS/cloud) | Sim | ⚠️ Revisar IAM role do EB (sprint) |
| Credenciais hardcoded | Sim | ✅ Todos em env vars |
| Supply chain | Sim | ✅ CVEs corrigidos + Passo 8 (CI) |
| Security Headers ausentes | Sim | ❌ → Passo 1 |
| Actuator exposto | Sim | ❌ → Passo 2 |