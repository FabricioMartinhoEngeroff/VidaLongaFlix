# Segurança de Aplicações Web — Headers HTTP, Upload de Arquivos e Proteções Client-Side

> Baseado no curso "Proteção de Vulnerabilidades — Módulo 4: Segurança no Client-Side".
> Este documento descreve: os conceitos, as vulnerabilidades reais identificadas no projeto, os cenários de teste com resultado esperado (positivos e negativos), e o fluxo de implementação para o backend e o frontend.

---

## Conceitos do Curso

### Headers de Segurança HTTP

Os headers de segurança são enviados pelo servidor na resposta HTTP e instruem o browser a aplicar regras de proteção. O servidor fornece a orientação — o browser a recebe e aplica enquanto o usuário navega. Eles são uma das camadas mais importantes da defesa em profundidade porque atuam independentemente do código da aplicação.

A maioria das vulnerabilidades client-side — XSS, clickjacking, MIME sniffing, CSRF — pode ser mitigada ou bloqueada inteiramente por um conjunto correto de headers, mesmo que o código da aplicação tenha falhas.

---

### MIME Sniffing e X-Content-Type-Options

**MIME (Multipurpose Internet Mail Extensions)** é o padrão de extensões que define o tipo de conteúdo na internet. Por muitos anos, os browsers implementaram um mecanismo de "sniffing" onde tentavam adivinhar o tipo do conteúdo pela extensão do arquivo, ignorando o `Content-Type` declarado.

O problema: se um atacante enviava um arquivo `.txt` contendo tags HTML ou JavaScript e o browser decidia interpretá-lo como HTML, um ataque XSS se tornava possível mesmo em conteúdo que deveria ser inofensivo.

**Solução: `X-Content-Type-Options: nosniff`** — força o browser a confiar exclusivamente no header `Content-Type`. O browser para de tentar adivinhar e usa apenas o tipo declarado pelo servidor. Se o servidor diz `text/plain`, o browser exibe como texto, nunca como HTML executável.

---

### Clickjacking e X-Frame-Options / CSP frame-ancestors

**Clickjacking** é o sequestro de cliques. O objetivo é fazer a vítima clicar em um elemento de uma página legítima sem saber, através de uma camada invisível. O mecanismo clássico:

1. O atacante cria uma página maliciosa com um `<iframe>` transparente apontando para a aplicação legítima
2. Posiciona um botão atrativo na página maliciosa, alinhado sobre um botão sensível no iframe (ex: "Confirmar transferência")
3. A vítima acredita estar clicando no botão da página maliciosa, mas está clicando no botão real dentro do iframe

Casos reais: páginas de transferência de criptomoedas, botões de confirmação de pagamento, formulários de autorização OAuth.

Em dispositivos móveis, a variante é chamada de **tapjacking** e segue a mesma lógica com interações de toque.

**Defesa principal:** impedir que a aplicação seja carregada dentro de um iframe.

**X-Frame-Options (XFO):** header mais antigo. Valores: `DENY` (bloqueia todos os iframes) e `SAMEORIGIN` (permite apenas o mesmo domínio). Considerado obsoleto porque não oferece granularidade suficiente.

**CSP `frame-ancestors`:** parte do Content Security Policy. É a forma recomendada e mais flexível. Permite listar domínios específicos que podem embutir a aplicação em iframes. `frame-ancestors 'none'` é o equivalente ao `DENY` do XFO, mas com mais possibilidade de customização por contexto.

---

### Referrer Policy

O header `Referer` é enviado pelo browser nas requisições e indica de qual página o usuário veio. Isso é útil para analytics, mas pode ser um vetor de vazamento de informações sensíveis.

**Problema:** imagine um usuário admin acessando `/usuarios/42/senhas`. Se esse admin clica em um link externo, o browser pode enviar `Referer: https://app.exemplo.com/usuarios/42/senhas` para o servidor externo. Esse servidor externo agora sabe que a URL de senhas existe e qual usuário está nela.

O header `Referrer-Policy` controla o que é enviado:

| Política | Quando enviar o Referer |
|---|---|
| `no-referrer` | Nunca envia |
| `origin` | Envia apenas o domínio (sem path) |
| `strict-origin` | Envia o domínio — somente se destino for HTTPS |
| `no-referrer-when-downgrade` | Não envia se destino for HTTP (padrão antigo) |
| `strict-origin-when-cross-origin` | Envia o path em same-origin, só o domínio em cross-origin |
| `same-origin` | Envia o path completo apenas para o mesmo domínio |
| `unsafe-url` | Sempre envia o path completo para qualquer domínio (perigoso) |

A política recomendada para a maioria das aplicações é **`strict-origin-when-cross-origin`**: envia o path completo apenas para requisições ao mesmo domínio, e apenas o domínio para requisições cross-origin. Nunca faz downgrade de HTTPS para HTTP.

---

### CDNs — Riscos e SRI

**CDN (Content Delivery Network)** são redes de distribuição de conteúdo que servem arquivos estáticos de servidores próximos ao usuário para carregar mais rápido. A maioria das aplicações usa CDNs para carregar fonts, ícones, ou bibliotecas JavaScript.

**Risco:** se a CDN for comprometida, o arquivo servido pode ser substituído por um malicioso. Casos reais incluem: scripts para mineração de bitcoin nos navegadores dos visitantes, malware embutido em sites, defaces e conteúdo pirata.

**Solução: SRI (Subresource Integrity)** — o atributo `integrity` nas tags `<script>` e `<link>` contém um hash (sha384, sha256 ou sha512) do arquivo esperado. O browser baixa o arquivo, calcula o hash localmente e só executa se o hash bater. Se o arquivo foi adulterado, o hash não corresponde e o recurso é bloqueado antes da execução.

O hash é gerado com: `openssl dgst -sha384 -binary arquivo.js | openssl base64 -A`

Após o cálculo: `integrity="sha384-{hash}" crossorigin="anonymous"` na tag do HTML.

---

### Certificate Transparency

É um framework global para auditoria de certificados SSL/TLS. Foi criado porque, historicamente, cada autoridade certificadora emitia seus próprios certificados sem transparência entre elas, possibilitando ataques man-in-the-middle com certificados forjados.

Com o Certificate Transparency, todos os certificados emitidos ficam em logs públicos auditáveis. Se um certificado SSL válido for emitido para um domínio por um ator malicioso, pode ser detectado e revogado. A empresa dona do domínio original (ex: Google) pode solicitar a remoção do certificado fraudulento.

**Relevância prática:** certificate transparency não é algo que o desenvolvedor configura no código. É uma camada de infraestrutura. O que o desenvolvedor precisa saber: certificados expirados causam erros no browser (tela de aviso), e certificados de domínios similares (ex: vidalongaflíx.com.br vs vidalongaflix.com.br) podem ser usados em ataques de phishing.

---

### Permissions Policy

O header `Permissions-Policy` (antigo Feature Policy) controla quais recursos do browser a página pode acessar: câmera, microfone, geolocalização, fullscreen, etc. Ele se aplica tanto à página quanto aos iframes que ela contém.

**Caso de risco:** se um atacante injeta um XSS que habilita câmera e microfone via JavaScript, e a Permissions Policy não bloqueia isso, o atacante pode gravar a vítima. No contexto da LGPD, o acesso a câmera e microfone sem consentimento explícito é uma violação grave.

**Regra geral:** desabilitar todos os recursos que a aplicação não usa. Uma plataforma de streaming de vídeo não precisa de acesso à câmera do usuário — desabilitar explicitamente reduz a superfície de ataque.

Exemplo de política restritiva: `Permissions-Policy: camera=(), microphone=(), geolocation=(), fullscreen=(self)`

---

### COOP e COEP — Isolamento Cross-Origin

**COOP (Cross-Origin Opener Policy)** e **COEP (Cross-Origin Embedder Policy)** foram criados em resposta à vulnerabilidade **Spectre**, que afetou processadores Intel e causou grande impacto em 2018. O Spectre explorava falhas de memória para obter acesso quase total ao computador via browser.

Para mitigar, os browsers reduziram a resolução de `performance.now()` e bloquearam o `SharedArrayBuffer`. COOP e COEP habilitam o **Site Isolation** — o browser roda cada origem em um processo separado, como containers, impedindo que um site acesse a memória de outro.

**COOP:** controla o compartilhamento do contexto de browsing entre origens.
- `Cross-Origin-Opener-Policy: same-origin` isola a janela — scripts de outras origens não conseguem acessar `window.opener`

**COEP:** controla quais recursos cross-origin podem ser carregados.
- `Cross-Origin-Embedder-Policy: require-corp` só permite carregar recursos que explicitamente permitam o compartilhamento

Juntos, COOP + COEP habilitam o acesso ao `SharedArrayBuffer` de forma segura, mitigam Side Channel attacks e protegem contra Cross-Site Leaks — onde informações de um site vazam para outro pela exploração de recursos compartilhados.

---

### HSTS — HTTP Strict Transport Security

O header `Strict-Transport-Security` instrui o browser a acessar o domínio **exclusivamente via HTTPS**, mesmo que o usuário digite `http://` ou clique em um link HTTP. Após a primeira visita com HTTPS, o browser memoriza a regra pelo período definido em `max-age`.

`Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`

- `max-age=31536000` — 1 ano de validade
- `includeSubDomains` — aplica a regra para todos os subdomínios
- `preload` — solicita inclusão na lista de preloading dos browsers (hardcoded no Chrome, Firefox, Safari)

A lista de preloading garante que mesmo na **primeira visita** o browser use HTTPS, sem depender do primeiro contato com o servidor. Para submeter, o domínio precisa ter HSTS ativo e pode ser enviado via [testpreload.org](https://testpreload.org).

---

### Upload de Arquivos — Riscos e Validação

O upload de arquivos é um dos vetores de ataque mais críticos. Um atacante pode enviar não uma imagem real, mas um payload malicioso que, dependendo de como o servidor processa, permite:

**Webshell / RCE (Remote Code Execution):** arquivo com código executável (PHP, JSP, Python) disfarçado como imagem. Se o servidor salva e serve o arquivo diretamente, o atacante acessa a URL e executa comandos no servidor.

**DoS por arquivo comprimido:** arquivos como bombshell ou zipshell parecem pequenos (ex: 1 MB) mas quando descomprimidos no servidor se expandem para centenas de gigabytes, sobrecarregando e derrubando o serviço.

**XSS via upload:** um arquivo SVG ou HTML enviado como imagem pode conter scripts que são executados quando acessado por outros usuários.

**Path traversal via nome de arquivo:** o nome do arquivo enviado contém `../../../etc/passwd`. Se o servidor usa o nome original para construir o path de destino sem sanitizar, o arquivo pode ser gravado fora do diretório permitido, sobrescrevendo arquivos críticos do sistema.

**Técnicas de bypass:** os atacantes tentam contornar validações simples com:
- Extensão dupla: `.php.jpg` — se o servidor valida apenas a extensão final, pode aceitar
- Troca de Content-Type: enviar `Content-Type: image/jpeg` enquanto o arquivo é um script PHP
- Validar apenas no frontend: inútil, o Burp Suite permite interceptar e modificar a requisição antes de enviar

**Validação correta no backend:**
- Verificar o Content-Type declarado no upload E o magic bytes (primeiros bytes do arquivo)
- Validar extensão contra uma whitelist explícita (ex: apenas `.mp4`, `.jpg`, `.png`, `.csv`)
- Renomear o arquivo no servidor com um UUID — nunca usar o nome original
- Verificar que o path resolvido começa dentro do diretório de upload permitido (`normalize().startsWith(UPLOAD_DIR)`)
- Definir limite de tamanho máximo
- Nunca servir arquivos de upload como arquivos executáveis

---

### Formulários como Vetores de Ataque

Formulários dependem de input do usuário e são vetores naturais para todos os ataques já discutidos: XSS, SQL Injection, upload malicioso, engenharia social.

A primeira e mais importante proteção é sanitização de input em todos os campos. As demais proteções já discutidas se aplicam diretamente: rate limiting (brute force via formulário de login), tamanho máximo (DoS por payload enorme), encoding antes de salvar (XSS armazenado).

**LGPD e minimização de dados:** formulários de cadastro só devem solicitar os dados estritamente necessários para o propósito da aplicação. Coletar CPF, renda, histórico de saúde em um formulário de streaming é uma violação do princípio de finalidade da LGPD.

---

### Dados Sensíveis em URLs

Tokens, senhas e informações sensíveis nunca devem ser transmitidos via query params na URL (GET). As URLs ficam visíveis em:
- Histórico do browser
- Logs de servidor e proxy
- Header `Referer` enviado para sites externos
- Capturas de tela acidentais

A regra é: dados sensíveis viajam via POST ou PUT no body da requisição — nunca na URL.

---

### Stack Trace — Vazamento de Informações em Erros

O stack trace é a mensagem de erro interna da aplicação. Por si só não é uma vulnerabilidade, mas pode funcionar como um vetor. Um stack trace exposto pode revelar:
- A versão exata do framework (ex: Spring Boot 3.5.12) — permite buscar CVEs específicas
- Paths internos de arquivos do servidor
- Nomes de classes e métodos internos — ajuda o atacante a mapear a aplicação
- Mensagens de banco de dados — pode revelar a estrutura das tabelas

A correção é simples: o usuário nunca precisa ver o erro interno. A resposta para o usuário deve ser uma mensagem genérica e amigável. O erro real vai para o log interno (Loki/Grafana Cloud), onde o time de desenvolvimento pode inspecioná-lo com segurança.

---

## Vulnerabilidades Reais Identificadas no Projeto

| # | Arquivo | Linha | Problema | Tipo de risco |
|---|---|---|---|---|
| C1 | `SecurityConfig.java` | 58 | `headers.disable()` — todos os headers de segurança desabilitados | Sem X-Content-Type-Options, X-Frame-Options, CSP frame-ancestors, HSTS, Referrer-Policy, Permissions-Policy |
| C2 | `GlobalExceptionHandler.java` | 131–133 | `globalError` expõe `e.getMessage()` na resposta HTTP pública | Stack trace / informação interna vaza para o cliente |
| C3 | `ImportController.java` | 26–28 | POST `/admin/import/videos` aceita qualquer `MultipartFile` sem validação de tipo, extensão ou tamanho | Upload malicioso de CSV adulterado ou arquivo não-CSV |
| C4 | `AdminVideoController.java` | 35–38 | Upload multipart chama `mediaStorageService.store(file, directoryName)` — sem validação de MIME type ou extensão no controller | Webshell, arquivo executável disfarçado de vídeo/imagem |
| C5 | `SecurityConfig.java` | 64–69 | `httpFirewall` permite semicolons e percent-encoded — abre possibilidade de path traversal bypass em operações de arquivo | Path traversal via URL encoded (`%2e%2e%2f`) |
| C6 | `index.html` (frontend) | 10–13 | Google Fonts e Material Icons carregados sem SRI (`integrity` attribute ausente) | CDN comprometida pode injetar CSS/scripts maliciosos |
| C7 | `GlobalExceptionHandler.java` | 55–57 | `typeMismatch` expõe o valor inválido recebido na mensagem de erro | Enumeração e fingerprinting da API |
| C8 | `view-history.service.ts` (frontend) | — | Histórico de visualização indexado pelo email do usuário no localStorage | Vazamento de dados de comportamento (LGPD — finalidade) |

---

## Cenários de Teste e Resultado Esperado

### Backend — Headers de Segurança

---

#### Cenário B1 — Headers obrigatórios em todas as respostas

**Contexto:** Qualquer endpoint da API deve retornar os headers que ativam as proteções do browser.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Qualquer endpoint retorna resposta | Header `X-Content-Type-Options: nosniff` presente |
| ✅ Positivo | Qualquer endpoint retorna resposta | Header `X-Frame-Options: DENY` **ou** CSP com `frame-ancestors 'none'` presente |
| ✅ Positivo | Qualquer endpoint retorna resposta | Header `Referrer-Policy: strict-origin-when-cross-origin` presente |
| ✅ Positivo | Qualquer endpoint retorna resposta | Header `Strict-Transport-Security: max-age=31536000; includeSubDomains` presente |
| ✅ Positivo | Qualquer endpoint retorna resposta | Header `Permissions-Policy: camera=(), microphone=(), geolocation=()` presente |
| ❌ Negativo | GET `/auth/login` sem os headers configurados | Resposta não contém `X-Content-Type-Options` — indica que `headers.disable()` ainda está ativo |
| ❌ Negativo | Aplicação carregada dentro de `<iframe>` por página externa | Browser bloqueia com base em `frame-ancestors 'none'` |

---

#### Cenário B2 — MIME sniffing bloqueado

**Contexto:** O backend serve JSON. O browser não deve interpretar a resposta como outro tipo.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | GET `/videos` — inspecionar headers da resposta | `Content-Type: application/json` + `X-Content-Type-Options: nosniff` |
| ✅ Positivo | Arquivo de imagem servido pelo backend com `Content-Type: image/jpeg` | Browser exibe como imagem — não tenta reinterpretar |
| ❌ Negativo | Resposta sem `X-Content-Type-Options` com conteúdo ambíguo | Browser pode tentar interpretar o conteúdo pelo seu conteúdo em vez do `Content-Type` |

---

#### Cenário B3 — Stack trace não exposto

**Contexto:** Erros internos não devem vazar detalhes técnicos para o cliente.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | GET `/videos/{id-inexistente}` | HTTP 404 com mensagem amigável — sem nome de classe, sem path interno, sem versão do framework |
| ✅ Positivo | POST com body JSON malformado | HTTP 400 com mensagem genérica — sem stack trace |
| ✅ Positivo | Erro interno inesperado no servidor | HTTP 500 com mensagem `"Ocorreu um erro interno. Tente novamente."` — sem `e.getMessage()` exposto |
| ❌ Negativo | `IllegalArgumentException` com mensagem interna | Atualmente retorna a mensagem exata da exceção — `e.getMessage()` visível no JSON de resposta |
| ❌ Negativo | Erro 500 genérico | Atualmente expõe a mensagem da exceção na resposta — versão do framework pode ser deduzida |

---

#### Cenário B4 — Validação de upload de arquivo (ImportController)

**Contexto:** O endpoint `/admin/import/videos` só deve aceitar arquivos CSV. Qualquer outro tipo deve ser rejeitado antes de ser processado.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | POST `/admin/import/videos` com arquivo `.csv` legítimo e `Content-Type: text/csv` | HTTP 200 — arquivo processado com o resultado de importação |
| ❌ Negativo | POST `/admin/import/videos` com arquivo `.php` e `Content-Type: text/csv` | HTTP 400 — extensão não é `.csv`, rejeitado |
| ❌ Negativo | POST com `Content-Type: application/octet-stream` e conteúdo binário | HTTP 400 — tipo de conteúdo não é CSV, rejeitado |
| ❌ Negativo | POST com arquivo de 500 MB (bomba CSV) | HTTP 413 ou HTTP 400 — tamanho excede o limite configurado |
| ❌ Negativo | POST com arquivo `.csv.php` (extensão dupla) | HTTP 400 — extensão final não é `.csv` |

---

#### Cenário B5 — Validação de upload de mídia (AdminVideoController)

**Contexto:** Upload de vídeo e thumbnail só deve aceitar formatos de mídia legítimos.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Upload de arquivo `.mp4` com `Content-Type: video/mp4` | HTTP 201 — vídeo salvo com nome UUID gerado pelo servidor |
| ✅ Positivo | Upload de imagem `.jpg` para thumbnail | HTTP 201 — imagem salva corretamente |
| ❌ Negativo | Upload de arquivo `.php` com `Content-Type: image/jpeg` | HTTP 400 — MIME type real do arquivo não é imagem |
| ❌ Negativo | Upload com nome de arquivo contendo `../` (path traversal) | HTTP 400 — ou o arquivo é salvo com UUID, ignorando o nome original |
| ❌ Negativo | Upload de arquivo `.php.jpg` (extensão dupla) | HTTP 400 — extensão não está na whitelist de tipos aceitos |

---

### Frontend Angular — Headers, SRI e Formulários

---

#### Cenário F1 — SRI nas dependências externas do index.html

**Contexto:** Google Fonts e Material Icons são carregados de CDN externa. Sem SRI, uma CDN comprometida pode injetar CSS malicioso.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Tag `<link>` para Google Fonts tem atributo `integrity="sha384-..."` e `crossorigin="anonymous"` | Browser verifica o hash antes de aplicar o CSS |
| ✅ Positivo | Hash no `integrity` corresponde ao arquivo servido pela CDN | Fonte carrega normalmente |
| ❌ Negativo | Hash no `integrity` não corresponde ao arquivo (CDN adulterada) | Browser bloqueia o carregamento — fonte não é aplicada, mas nenhum script malicioso executa |
| ❌ Negativo | Tag `<link>` sem `integrity` attribute | Browser carrega e aplica qualquer versão do arquivo, incluindo versões adulteradas |

---

#### Cenário F2 — Dados sensíveis não trafegam em URLs (Frontend)

**Contexto:** O Angular não deve construir URLs com tokens, senhas ou dados sensíveis como query params.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Login envia credenciais via POST no body | URL de login não contém `password=` ou `email=` como query param |
| ✅ Positivo | Token JWT enviado no header `Authorization` | Token não aparece na URL em nenhuma requisição |
| ❌ Negativo | Qualquer URL com `token=` ou `password=` como query param | Não deve existir nenhuma construção desse tipo no código Angular |

---

#### Cenário F3 — Mensagens de erro não expõem informação interna (Frontend)

**Contexto:** O frontend exibe mensagens de erro para o usuário. Detalhes técnicos da resposta do backend não devem ser mostrados.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Login com credenciais erradas | Mensagem amigável: "Email ou senha incorretos" — sem detalhe técnico |
| ✅ Positivo | Erro 500 do backend | Mensagem genérica exibida — sem `e.getMessage()` visível na tela |
| ❌ Negativo | Backend retorna stack trace no JSON e o frontend exibe o campo `message` raw | Stack trace não deve aparecer em nenhum elemento visível da interface |
| ❌ Negativo | `console.log(error)` em código de produção com objeto de erro completo | Nenhum `console.log` com erros completos em produção — log estruturado para observabilidade |

---

#### Cenário F4 — Formulários validados no backend (não só no frontend)

**Contexto:** As validações do Angular são úteis para UX, mas não são segurança. O backend deve rejeitar inputs inválidos independentemente do frontend.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Campo de email com formato válido no formulário Angular | Formulário habilita o submit |
| ✅ Positivo | Body inválido enviado diretamente via Postman (bypassando o frontend) | Backend retorna HTTP 400 com validação — não confia no frontend |
| ❌ Negativo | Script desabilita validação no browser e envia email sem `@` | Backend rejeita com 400 — não aceita o dado sem validar |
| ❌ Negativo | Upload de arquivo via Postman com extensão proibida, bypassando a validação do frontend | Backend rejeita — a validação existe no servidor |

---

#### Cenário F5 — Privacidade do histórico de visualização (Frontend)

**Contexto:** O `view-history.service.ts` usa o email do usuário como chave no localStorage. Isso cria um registro que associa o comportamento de navegação ao email — dado pessoal pela LGPD.

| Tipo | Situação | Resultado Esperado |
|---|---|---|
| ✅ Positivo | Usuário faz logout | Histórico de visualização é removido do localStorage junto com token e dados de sessão |
| ❌ Negativo | Chave do localStorage inspecionada após login | Atualmente expõe `vida-longa-flix:views:{email}` — email visível na chave |
| ❌ Negativo | Dois usuários no mesmo browser compartilhando histórico | Cada usuário tem seu histórico separado — correto, mas a chave ainda expõe o email |

---

## Fluxo de Implementação — Como Colocar os Conceitos em Prática

O fluxo segue o padrão TDD dos módulos anteriores: spec → Red → correção → Green → verificação manual.

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

**Backend:** confirmar que `./mvnw test` passa. Os novos specs de segurança serão criados em `src/test/java/.../security/` e `src/test/java/.../controllers/`.

**Frontend:** confirmar que `npm test -- --watch=false` passa. Os specs de SRI e URL serão verificações manuais de código e testes unitários simples.

---

### Sprint 1 — Headers de Segurança (Backend)

**Objetivo:** Substituir `headers.disable()` por um conjunto completo de headers de segurança.

**Passo a passo:**

1. Criar `SecurityHeadersTest.java` baseado no Cenário B1 — um teste por header
2. Rodar e confirmar que todos falham (a linha `headers.disable()` garante isso)
3. Abrir `SecurityConfig.java` e substituir `.headers(headers -> headers.disable())` pela configuração completa:
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY` — para CSP usar `frame-ancestors 'none'` junto ao CSP já planejado no Sprint 1 do Módulo 2
   - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - `Permissions-Policy: camera=(), microphone=(), geolocation=()`
4. Rodar os specs e confirmar verde
5. Subir localmente e usar `curl -I https://localhost:8090/api/videos` para inspecionar os headers visualmente
6. Verificar que o frontend Angular continua funcionando — alguns headers de CSP podem bloquear recursos se mal configurados

**Critério de conclusão:** spec verde + headers presentes no `curl` + frontend sem erros de console.

---

### Sprint 2 — Stack Trace e Mensagens de Erro (Backend)

**Objetivo:** O handler genérico de exceções não deve expor `e.getMessage()` na resposta pública.

**Passo a passo:**

1. Criar `GlobalExceptionHandlerTest.java` baseado no Cenário B3
2. Criar um endpoint de teste que lança `RuntimeException("Internal detail with stack info")` e verificar que a resposta não contém essa string
3. Rodar e confirmar que o caso falha — atualmente `globalError` passa `e.getMessage()` para `buildStandardError`
4. Editar `GlobalExceptionHandler.java` no método `globalError` (linha 131–133): substituir `e.getMessage()` por uma mensagem estática genérica como `"Ocorreu um erro interno. Tente novamente mais tarde."`
5. Verificar o mesmo para `illegalArgument` (linha 125–128) — a mensagem `e.getMessage()` de argumentos inválidos pode revelar informações internas da API
6. Garantir que o erro real ainda vai para o log (via `log.error()`) — o usuário não vê, mas o Loki/Grafana registra para análise
7. Rodar specs e confirmar verde

**Critério de conclusão:** spec verde + erro 500 retorna mensagem genérica + Loki recebe o log com o detalhe real.

---

### Sprint 3 — Validação de Upload de Arquivo (Backend)

**Objetivo:** Os endpoints de upload devem validar Content-Type, extensão e tamanho antes de processar.

**Passo a passo:**

1. Criar `ImportControllerSecurityTest.java` baseado no Cenário B4
2. Criar `AdminVideoControllerUploadTest.java` baseado no Cenário B5
3. Rodar e confirmar que upload de `.php` disfarçado de CSV passa atualmente (Red)
4. Criar um componente de validação de upload (ex: `FileUploadValidator.java`) que:
   - Verifica se a extensão está em uma whitelist (`csv`, `mp4`, `jpg`, `jpeg`, `png`, `webp`)
   - Verifica os primeiros bytes do arquivo (magic bytes) para confirmar o tipo real
   - Verifica que o tamanho não excede o limite configurado
5. Chamar o validador no início de `importVideos()` e `importMenus()` no `ImportService.java`
6. Confirmar que `MediaStorageService.store()` gera um UUID como nome de arquivo, nunca usa `file.getOriginalFilename()` como path
7. Rodar os specs e confirmar verde
8. Testar manualmente com `curl` enviando um arquivo `.php` com `Content-Type: text/csv` — deve retornar 400

**Critério de conclusão:** spec verde + arquivos com extensão/tipo inválido são rejeitados com 400 + nome UUID gerado pelo servidor.

---

### Sprint 4 — SRI nas Dependências Externas (Frontend)

**Objetivo:** Adicionar o atributo `integrity` com hash sha384 em todas as dependências carregadas de CDNs externas no `index.html`.

**Passo a passo:**

1. Abrir `/src/index.html` e identificar todas as tags `<link>` e `<script>` que apontam para domínios externos (`fonts.googleapis.com`, `fonts.gstatic.com`, CDNs)
2. Para cada URL externa, gerar o hash SRI: `curl -s {url} | openssl dgst -sha384 -binary | openssl base64 -A`
3. Adicionar o atributo `integrity="sha384-{hash}"` e `crossorigin="anonymous"` em cada tag
4. Verificar manualmente no browser: abrir o DevTools, aba Network, confirmar que os recursos são carregados sem erro de SRI
5. Se o hash calculado não bater (CDN usa versão diferente), buscar o hash oficial no site do provedor (Google Fonts gera o link com SRI automaticamente via URL parametrizada)
6. Criar um teste de snapshot do `index.html` que falha se uma tag de CDN for adicionada sem o atributo `integrity`

**Critério de conclusão:** todas as tags de CDN têm `integrity` + browser carrega os recursos sem erro.

---

### Sprint 5 — Privacidade do Histórico de Visualização (Frontend)

**Objetivo:** Substituir o email como chave do histórico de visualização por um identificador anônimo, e garantir que o histórico é limpo no logout.

**Passo a passo:**

1. Abrir `view-history.service.ts` e identificar onde a chave é construída com o email
2. Adicionar os casos do Cenário F5 no spec do serviço
3. Rodar e confirmar que a chave atual expõe o email (Red)
4. Trocar a chave de `vida-longa-flix:views:{email}` para `vida-longa-flix:views:{userId}` onde `userId` é o `sub` do JWT — ainda identifica o usuário no storage local, mas não expõe o email em texto claro
5. Verificar o método `clearSession()` do `auth.service.ts` e confirmar que remove o histórico de visualização junto com o token no logout. Se não faz, adicionar a chamada de limpeza
6. Rodar specs e confirmar verde

**Critério de conclusão:** spec verde + chave do localStorage não contém email + logout limpa o histórico.

---

### Sprint 6 — Mensagens de Erro no Frontend

**Objetivo:** O frontend não deve exibir mensagens de erro técnicas do backend na interface do usuário.

**Passo a passo:**

1. Mapear todos os componentes que exibem mensagens de erro vindas do backend (formulário de login, registro, comentários, upload de admin)
2. Verificar se algum exibe diretamente `error.message` ou `error.error.message` no template
3. Se sim, criar uma constante de mensagens amigáveis por código HTTP (401 → "Email ou senha incorretos", 400 → "Dados inválidos, verifique os campos", 500 → "Erro temporário, tente novamente")
4. Substituir a exibição do erro técnico pelo mapeamento de mensagem amigável
5. Buscar por `console.log(error)` ou `console.error(error)` no código — substituir por serviço de observabilidade (OpenTelemetry já configurado) ou remover em produção
6. Verificar com `ng build` (build de produção) que nenhum `console.log` de erro permanece

**Critério de conclusão:** nenhum erro técnico visível na interface + sem `console.log(error)` no build de produção.

---

### Verificação Final — Todos os Sprints

**Backend:** `./mvnw test` — suite completa verde, incluindo os novos specs de headers, stack trace e upload.

**Frontend:** `npm test -- --watch=false` — todos os specs incluindo SRI e histórico de visualização.

**Integração manual:**
1. Inspecionar os headers de qualquer resposta com `curl -I` ou DevTools → confirmar X-Content-Type-Options, X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy
2. Tentar carregar a aplicação em um `<iframe>` em outra página → deve ser bloqueado
3. Tentar upload de arquivo `.php` via Postman para `/admin/import/videos` → deve retornar 400
4. Forçar um erro 500 no backend → resposta JSON não deve conter informação técnica interna
5. Inspecionar o `index.html` no browser → todas as tags de CDN têm `integrity`
6. Fazer logout → inspecionar localStorage → não deve ter chave com email, não deve ter histórico de visualização

---

## Fluxos Específicos do Frontend Angular

---

### Fluxo 1 — SRI e a relação com o Angular Build

**Conceito aplicado:** O Angular é compilado em bundles estáticos servidos pelo CloudFront. Esses bundles são first-party (mesmo domínio), então não precisam de SRI. O risco de SRI está exclusivamente nas dependências carregadas de CDNs de terceiros no `index.html`.

**O que verificar no projeto:**
- `index.html` — identificar todas as tags com URLs de `fonts.googleapis.com` e `fonts.gstatic.com`
- Confirmar que os bundles gerados pelo `ng build` (arquivos `main.js`, `polyfills.js`, etc.) são servidos pelo próprio CloudFront — sem CDN de terceiros para o código Angular

**Regra para novos recursos:** toda vez que uma nova dependência externa for adicionada ao `index.html` (icon pack, font, analytics script), o atributo `integrity` com hash sha384 deve ser calculado e incluído na mesma linha.

**Arquivos afetados:** `/src/index.html`.

---

### Fluxo 2 — Clickjacking e a proteção pelo backend

**Conceito aplicado:** O Angular roda no browser. O browser só sabe que não pode ser embutido em iframe se o backend enviar o header correto. O frontend não tem como se proteger sozinho contra clickjacking.

**Situação atual:** `headers.disable()` no `SecurityConfig.java` remove o header `X-Frame-Options`. Qualquer site externo pode embutir a aplicação em um iframe invisível e fazer um ataque de clickjacking.

**Impacto no projeto:** a área de streaming tem vídeos e uma tela de perfil com dados do usuário. Um iframe invisível sobre uma página atrativa poderia capturar cliques em ações sensíveis (favoritar, postar comentário, editar perfil).

**O que verificar após o Sprint 1:**
Abrir o browser, criar um arquivo HTML local com `<iframe src="https://vidalongaflix.com.br">` e confirmar que o browser exibe um erro de carregamento em vez de renderizar a aplicação.

---

### Fluxo 3 — Referrer Policy e a privacidade do usuário

**Conceito aplicado:** O header `Referer` enviado pelo browser pode revelar a URL que o usuário estava visitando para o destino de um link. Em uma plataforma de saúde/nutrição, isso é especialmente sensível — um terceiro poderia deduzir o comportamento de saúde do usuário.

**Situação atual no projeto:** sem `Referrer-Policy`, o browser usa o padrão `no-referrer-when-downgrade`, que envia o path completo para destinos HTTPS. Se um vídeo sobre dieta específica tem um link externo, o destino vê a URL completa do vídeo.

**O que configurar:** `Referrer-Policy: strict-origin-when-cross-origin` — envia só o domínio (sem path) em requisições cross-origin. O path é visível apenas em requisições same-origin.

**Arquivos afetados:** `SecurityConfig.java` (backend) — o frontend se beneficia automaticamente.

---

### Fluxo 4 — Permissions Policy e a LGPD

**Conceito aplicado:** A plataforma é de streaming de vídeo e saúde/nutrição. Acesso à câmera ou microfone não é necessário para nenhuma funcionalidade. A Permissions Policy deve desabilitar explicitamente esses recursos.

**Risco sem a política:** um XSS bem-sucedido poderia habilitar a câmera via JavaScript. Mesmo que a probabilidade seja baixa com as demais proteções, o impacto (gravação do usuário) seria catastrófico em termos de LGPD.

**O que configurar no backend:** `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()` — lista vazia significa "nenhuma origem tem permissão".

**Arquivos afetados:** `SecurityConfig.java` (backend).

---

### Fluxo 5 — Formulários Angular e a validação em camadas

**Conceito aplicado:** A validação Angular (Reactive Forms) é a primeira camada — melhora a experiência do usuário, não é segurança. A segunda camada é o backend. Ambas são necessárias com responsabilidades diferentes.

**O que verificar no projeto:**
- `login.component.ts`: usa `Validators.required` e `Validators.email` — correto para UX
- `register.component.ts`: usa `strongPasswordValidator` — correto para guiar o usuário
- O backend deve rejeitar qualquer body inválido independentemente, via `@Valid` nos DTOs

**Fluxo de bypass a testar:**
Usar o DevTools do browser para remover o atributo `disabled` do botão de submit, ou usar Postman diretamente. O backend deve retornar 400 com os mesmos erros que o Angular mostraria. Se o backend aceitar, as validações existem apenas no frontend e são ineficazes como segurança.

**Arquivos relevantes (frontend):** `login.component.ts`, `register.component.ts`. **Backend:** `LoginRequestDTO.java`, `RegisterRequestDTO.java` — verificar se têm anotações `@NotBlank`, `@Email`, `@Size`.

---

### Fluxo 6 — Tratamento de Erros no Angular e informações técnicas

**Conceito aplicado:** O Angular recebe respostas de erro do backend. Se o frontend exibe `error.error.message` diretamente no template, ele pode exibir stack traces ou mensagens técnicas para o usuário — mesmo que o backend seja corrigido no Sprint 2, o frontend pode re-introduzir o problema.

**O que verificar no projeto:**
Buscar nos componentes Angular por uso de `error.message`, `error.error.message`, ou `error.error` sendo exibidos em templates com `{{ }}`. Isso é especialmente comum em interceptors e handlers de erro.

**Fluxo correto:**
O interceptor ou o serviço Angular captura o erro do backend, lê o `status` HTTP e mapeia para uma mensagem amigável. O `message` técnico do backend nunca chega ao template.

**Arquivos relevantes:** `auth.interceptor.ts`, qualquer serviço com `.pipe(catchError(...))`, componentes de login e registro que exibem mensagens de erro.

---

## Resumo — Estado do Projeto Após os Sprints

| Vulnerabilidade | Antes | Depois |
|---|---|---|
| `headers.disable()` | Zero headers de segurança — browser sem proteções | X-Content-Type-Options, X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy ativos |
| Stack trace exposto | `e.getMessage()` visível na resposta HTTP pública | Mensagem genérica para o usuário — detalhe real vai para Loki |
| Upload sem validação | Qualquer arquivo aceito no CSV import e no multipart video | Whitelist de extensão + validação de Content-Type + limite de tamanho |
| CDN sem SRI | Google Fonts e Material Icons sem integrity hash | Hash sha384 em todas as tags de CDN — adulteração detectada antes da execução |
| Email como chave do histórico | `vida-longa-flix:views:{email}` visível no localStorage | Chave usa userId (sub do JWT) — email não exposto |
| Mensagens de erro técnicas no frontend | Erro do backend pode ser exibido diretamente na UI | Mapeamento para mensagens amigáveis por código HTTP |
