# Video Flow - Backend, Upload and Nginx

## Objetivo

Este documento explica, de forma pragmatica, como o fluxo de videos funciona hoje no backend do VidaLongaFlix.

Ele cobre:

- a modelagem principal de video
- os controllers e services envolvidos
- o fluxo atual de cadastro via JSON e via multipart upload
- como a midia e servida
- o papel do Nginx no Elastic Beanstalk
- o que foi alterado recentemente para evitar videos quebrados

---

## Visao geral do fluxo atual

Hoje existem **dois caminhos** para cadastrar videos no admin:

1. **JSON com URL publica**
   - o frontend envia `title`, `description`, `url`, `cover`, `categoryId`
   - o backend valida se `url` e `cover` sao URLs publicas HTTP(S)
   - se for valido, persiste o registro

2. **Multipart com upload real**
   - o frontend envia os dados do formulario + `videoFile` + `coverFile`
   - o backend grava os arquivos localmente
   - gera URLs publicas em `/api/media/...`
   - monta um `VideoRequestDTO`
   - persiste o video usando essas URLs geradas

Se o frontend tentar enviar referencias invalidas como `blob:`, `data:`, caminho local ou `localhost`, o backend responde `422` e bloqueia o cadastro.

---

## Classes principais

### 1. Domain - `Video`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/domain/video/Video.java`

Responsabilidade:
- representar a entidade persistida em banco
- armazenar os dados principais do video

Campos relevantes:
- `title`
- `description`
- `url`
- `cover`
- `category`
- `views`
- `watchTime`
- `recipe`
- macros e calorias

Observacao importante:
- `url` e a URL final do video usada pela aplicacao publica
- `cover` e a URL final da capa usada pela aplicacao publica
- o backend nao faz transformacao posterior; a API publica devolve o que foi salvo

### 2. DTOs - `VideoRequestDTO` e `VideoDTO`

Arquivos:
- `src/main/java/com/dvFabricio/VidaLongaFlix/domain/video/VideoRequestDTO.java`
- `src/main/java/com/dvFabricio/VidaLongaFlix/domain/video/VideoDTO.java`

Responsabilidade:
- `VideoRequestDTO`: contrato de entrada para create/update
- `VideoDTO`: contrato de saida para leitura publica e admin

`VideoRequestDTO` exige:
- `title`
- `description`
- `url`
- `cover`
- `categoryId`

No fluxo multipart, o controller primeiro sobe os arquivos e depois monta esse mesmo DTO com as URLs geradas.

---

## Repository - `VideoRepository`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/repositories/VideoRepository.java`

Responsabilidade:
- persistencia JPA da entidade `Video`
- consultas para analytics e listagens

Consultas principais:
- mais vistos
- menos vistos
- views por categoria
- media de watch time
- videos com mais comentarios

Ponto importante:
- o repository nao conhece upload nem arquivos
- ele apenas persiste e consulta `url` e `cover` como texto

---

## Service - `VideoService`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/services/VideoService.java`

Responsabilidade:
- regra de negocio do video
- validacao de `url` e `cover`
- create, update, delete e leitura

### O que ele faz hoje

#### Create
- valida `url` e `cover`
- resolve a categoria por `categoryId`
- cria a entidade
- salva no banco
- dispara notificacao

#### Update
- valida `url` e `cover`
- busca o video por id
- atualiza apenas os campos enviados
- salva no banco

#### Leitura publica
- devolve `VideoDTO`
- sem transformar URLs

### Validacao de midia aplicada

O service bloqueia valores invalidos para `url` e `cover`, como:

- `blob:...`
- `data:...`
- caminho local
- `localhost`
- string sem host publico

Isso foi feito para impedir o fluxo antigo de "cadastra com 201 mas depois nao toca".

---

## Service de upload - `MediaStorageService`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/services/MediaStorageService.java`

Responsabilidade:
- receber `MultipartFile`
- validar tipo basico do arquivo
- gravar no filesystem local
- gerar URL publica final
- apagar arquivo em caso de rollback/logica de cleanup

### Regras aplicadas

Para `videoFile`:
- aceita `content-type` iniciando com `video/`

Para `coverFile`:
- aceita `content-type` iniciando com `image/`

### Onde os arquivos sao gravados

Por padrao:

`/tmp/vidalongaflix-media`

Subpastas:
- `videos/`
- `covers/`

Configuracao:

`app.media.storage-path=${MEDIA_STORAGE_PATH:/tmp/vidalongaflix-media}`

### Importante

Este storage e **local ao container**.

Isso significa:
- funciona para validar o fluxo de upload
- funciona para servir a midia no ambiente atual
- **nao e persistencia duravel entre redeploys/restarts**

Para producao robusta, o proximo passo natural e trocar esse service por S3 ou outro storage persistente, mantendo o mesmo contrato do controller.

---

## Controller admin - `AdminVideoController`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/controllers/AdminVideoController.java`

Responsabilidade:
- expor os endpoints de create/update/delete do admin

### Endpoints atuais

#### `POST /api/admin/videos` com JSON

Uso:
- quando o frontend ja tem URL publica para video e capa

Payload:
- `title`
- `description`
- `url`
- `cover`
- `categoryId`
- opcionais nutricionais

Fluxo:
1. recebe `VideoRequestDTO`
2. chama `videoService.create(...)`
3. retorna `201`

#### `POST /api/admin/videos` com multipart

Uso:
- quando o frontend seleciona arquivo local de video e capa

Campos esperados:
- `title`
- `description`
- `categoryId`
- opcionais: `recipe`, `protein`, `carbs`, `fat`, `fiber`, `calories`
- arquivos:
  - `videoFile`
  - `coverFile`

Fluxo:
1. recebe multipart
2. calcula `baseUrl` da aplicacao
3. grava `videoFile`
4. grava `coverFile`
5. monta `VideoRequestDTO` com as URLs publicas geradas
6. chama `videoService.create(...)`
7. se falhar apos gravar arquivo, tenta limpar os arquivos
8. retorna `201`

#### `PUT /api/admin/videos/{id}`
- atualiza dados do video
- usa validacao de URL publica no service

#### `DELETE /api/admin/videos/{id}`
- remove o registro

Observacao:
- hoje o delete remove o registro do banco
- ele **nao remove automaticamente a midia local** associada ao video
- isso pode ser melhorado depois, se necessario

---

## Controller publico - `VideoController`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/controllers/VideoController.java`

Responsabilidade:
- leitura publica dos videos
- registro de view
- endpoints de analytics publicos

Endpoints relevantes:
- `GET /api/videos`
- `GET /api/videos/{id}`
- `PATCH /api/videos/{id}/view`

Ponto importante:
- o frontend publico usa as URLs devolvidas por aqui
- se `url` ou `cover` estiverem erradas no banco, o player e a capa quebram

---

## Exposicao da midia - `MediaResourceConfig`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/infra/config/MediaResourceConfig.java`

Responsabilidade:
- expor os arquivos gravados localmente como recurso publico

Mapeamento:
- `/api/media/**`

Na pratica:
- arquivo salvo em `.../videos/abc.mp4`
- fica acessivel em `/api/media/videos/abc.mp4`

Isso permite que o backend gere uma URL publica imediatamente apos o upload.

---

## Seguranca - `SecurityConfig`

Arquivo:
- `src/main/java/com/dvFabricio/VidaLongaFlix/infra/security/SecurityConfig.java`

O que foi aplicado:
- `/media/**` foi liberado como publico
- `/admin/**` continua protegido com `ROLE_ADMIN`

Efeito:
- admin sobe o arquivo
- frontend publico consegue carregar a midia sem autenticacao

---

## Banco de dados

Migration principal:
- `src/main/resources/db/migration/V5__create-table-videos.sql`

Campos relevantes:
- `url`
- `thumbnail_url` (mapeado como `cover`)

Observacao:
- o banco guarda apenas as URLs finais
- ele nao guarda o arquivo em si

---

## Fluxo atual completo

### Fluxo A - JSON com URL publica

1. frontend ja possui URL publica do video
2. frontend ja possui URL publica da capa
3. envia `POST /api/admin/videos` em JSON
4. backend valida `url` e `cover`
5. backend persiste
6. `GET /api/videos` devolve o mesmo valor salvo

### Fluxo B - Upload multipart

1. usuario seleciona arquivo local de video
2. usuario seleciona arquivo local de capa
3. frontend envia `multipart/form-data` para `POST /api/admin/videos`
4. backend grava os arquivos localmente
5. backend gera:
   - `/api/media/videos/...`
   - `/api/media/covers/...`
6. backend monta `VideoRequestDTO`
7. backend valida as URLs geradas
8. backend persiste
9. `GET /api/videos` devolve essas URLs
10. frontend publico usa essas URLs para player/capa

---

## Onde o Nginx entra

No Elastic Beanstalk, o Nginx fica **na frente** da aplicacao Spring Boot.

Ele atua como reverse proxy:

```text
Browser
  -> Nginx (Elastic Beanstalk host)
  -> Spring Boot /api
```

### O papel do Nginx neste fluxo

Para requests comuns:
- recebe a requisicao
- encaminha para a app

Para upload multipart:
- precisa aceitar o tamanho do arquivo
- precisa manter a conexao aberta tempo suficiente
- pode precisar evitar buffering agressivo

Se o Nginx estiver mal configurado, o request pode falhar **antes de chegar no backend**.

Sintomas classicos:
- `413 Request Entity Too Large`
- `502`
- `504`
- upload interrompido

---

## O que foi aplicado no Nginx

Arquivo:
- `.platform/nginx/conf.d/upload.conf`

Configuracoes aplicadas:

```nginx
client_max_body_size 512M;
proxy_connect_timeout 60s;
proxy_read_timeout 300s;
proxy_send_timeout 300s;
send_timeout 300s;
proxy_request_buffering off;
```

### O que cada uma faz

#### `client_max_body_size 512M`
- permite uploads maiores no proxy
- evita `413` por tamanho pequeno demais

#### `proxy_connect_timeout 60s`
- tempo maximo para conectar no upstream

#### `proxy_read_timeout 300s`
- quanto tempo o Nginx espera lendo a resposta do backend

#### `proxy_send_timeout 300s`
- quanto tempo o Nginx tolera enviando dados ao backend

#### `send_timeout 300s`
- timeout de envio da resposta para o cliente

#### `proxy_request_buffering off`
- reduz o risco de o Nginx tentar bufferizar o upload inteiro antes
- melhora comportamento para upload de arquivo maior/lento

---

## O que foi aplicado no Spring multipart

Arquivo:
- `src/main/resources/application.properties`

Configuracoes aplicadas:

```properties
spring.servlet.multipart.max-file-size=${MAX_UPLOAD_FILE_SIZE:512MB}
spring.servlet.multipart.max-request-size=${MAX_UPLOAD_REQUEST_SIZE:512MB}
```

### Efeito

- o limite do Spring passou a ficar alinhado com o upload real
- agora os limites podem ser ajustados por ambiente sem alterar codigo

Variaveis disponiveis:
- `MAX_UPLOAD_FILE_SIZE`
- `MAX_UPLOAD_REQUEST_SIZE`

---

## O problema original e a correcao aplicada

### Problema original

O fluxo antigo de arrastar arquivo no frontend gerava referencias temporarias, como:
- `blob:...`
- caminho local
- outras referencias nao publicas

O backend aceitava o cadastro porque ele so recebia texto em `url` e `cover`.

Resultado:
- `POST /api/admin/videos` retornava `201`
- o video aparecia na lista
- mas depois o player nao tocava
- a capa nao carregava

### Correcao aplicada

1. o backend passou a validar `url` e `cover`
2. referencias invalidas agora retornam `422`
3. foi criado um caminho real de upload multipart
4. os arquivos sao gravados e expostos em `/api/media/...`
5. o cadastro final passa a usar URLs publicas reais

---

## Limitacoes atuais

Mesmo com o fluxo funcionando, ainda existem limitacoes:

1. **storage local**
   - arquivos podem se perder em redeploy/restart

2. **delete sem cleanup de midia**
   - apagar video do banco nao remove automaticamente o arquivo local

3. **sem CDN**
   - a midia esta sendo servida pela propria aplicacao/proxy

4. **sem persistencia externa**
   - para producao madura, o ideal e migrar para S3

---

## Proximo passo recomendado

Se o objetivo for consolidar a solucao para producao, o proximo passo certo e:

1. manter o endpoint multipart atual
2. trocar apenas o `MediaStorageService`
3. salvar em S3
4. gerar URL publica ou assinada conforme a regra do produto

Isso preserva:
- controller
- contrato do frontend
- fluxo de create

Ou seja: muda a persistencia da midia, sem mudar tudo.

---

## Resumo executivo

- `Video` guarda `url` e `cover` como URLs finais de consumo
- `VideoRepository` apenas persiste/consulta esses valores
- `VideoService` aplica a regra de validacao e CRUD
- `AdminVideoController` agora suporta JSON e multipart
- `MediaStorageService` grava video/capa localmente e gera URLs publicas
- `MediaResourceConfig` expone `/api/media/**`
- `SecurityConfig` libera a midia publicamente
- o Nginx do EB foi ajustado para suportar upload maior e mais lento
- o problema de `blob:`/arquivo local quebrado foi bloqueado e substituido por upload real
- a limitacao atual e a falta de storage duravel
