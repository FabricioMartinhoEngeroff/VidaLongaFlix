# VidaLongaFlix - Documentacao do Sistema Backend

## 1. Historico de Revisoes

| Data       | Resumo da Revisao                                                            | Responsavel | Previsao de Desenvolvimento |
|------------|------------------------------------------------------------------------------|-------------|------------------------------|
| 28/02/2026 | Criacao do documento inicial                                                 | Fabricio    | -                            |
| 03/03/2026 | Correcoes: shape CommentResponseDTO, CORS configuravel, CategoryController   | Fabricio    | -                            |
| 07/03/2026 | CPF (taxId) passa a ser opcional no cadastro de usuario                      | Fabricio    | -                            |
| 08/03/2026 | Adicao de CD automatico: job deploy GitHub Actions -> Elastic Beanstalk      | Fabricio    | -                            |
| 08/03/2026 | GitHub Environment "production" com aprovacao manual antes do deploy         | Fabricio    | -                            |

---

## 2. Visao Geral do Sistema

O VidaLongaFlix e uma plataforma de streaming de videos e cardapios voltada para saude e longevidade. O sistema permite que usuarios autenticados assistam videos, curtam conteudos, comentem e recebam notificacoes de novos conteudos publicados. Administradores gerenciam todo o conteudo da plataforma.

**Tecnologias:** Spring Boot 3.x, Java 17, JWT, H2 (desenvolvimento), PostgreSQL (producao), WhatsApp Business API (Meta).

**URL base da API:** `/api`

**Porta padrao (desenvolvimento):** `8090`

---

## 3. Perfis de Acesso

| Perfil       | Descricao                                              |
|--------------|--------------------------------------------------------|
| ROLE_USER    | Usuario autenticado com acesso ao conteudo da plataforma |
| ROLE_ADMIN   | Administrador com acesso total, incluindo gerenciamento de conteudo |
| Anonimo      | Acesso limitado a leitura de videos, menus, comentarios e categorias |

---

## 4. Regras Gerais

**RG01** - A autenticacao utiliza JWT (JSON Web Token) com validade de 2 horas. O token deve ser enviado no cabecalho `Authorization: Bearer {token}` em todas as requisicoes que exigem autenticacao.

**RG02** - Senhas devem conter no minimo 8 caracteres, incluindo: letra maiuscula, letra minuscula, numero e caractere especial.

**RG03** - O usuario administrador inicial e criado automaticamente na inicializacao do sistema, com credenciais definidas por variaveis de ambiente (`ADMIN_EMAIL`, `ADMIN_PASSWORD`).

**RG04** - Todos os IDs de entidades são do tipo UUID gerado automaticamente.

**RG05** - Datas e horarios sao armazenados no fuso horario UTC.

**RG06** - Campos marcados como obrigatorios retornam erro HTTP 400 com lista de campos invalidos caso nao sejam preenchidos.

**RG07** - Requisicoes a recursos inexistentes retornam HTTP 404.

**RG08** - Requisicoes sem autenticacao a endpoints protegidos retornam HTTP 401. Requisicoes com perfil insuficiente retornam HTTP 403.

---

## 5. Modulo de Autenticacao

**Endpoint base:** `/auth`

**Acesso:** Publico

---

### 5.1 Login

**Endpoint:** `POST /auth/login`

**Descricao:** Autentica o usuario com e-mail e senha. Retorna token JWT e dados do usuario.

**Campos de entrada (LoginRequestDTO):**

| Campo    | Tipo   | Obrigatorio | Validacao               |
|----------|--------|-------------|--------------------------|
| email    | String | Sim         | Formato de e-mail valido |
| password | String | Sim         | Minimo 8 caracteres      |

**Retorno (AuthResponseDTO):**

| Campo | Tipo            | Descricao               |
|-------|-----------------|--------------------------|
| token | String          | Token JWT                |
| user  | UserResponseDTO | Dados do usuario logado  |

**Regras:**

**RG-AUTH-01** - O e-mail deve estar cadastrado no sistema. Caso nao exista, o sistema retorna HTTP 401 com mensagem de credenciais invalidas.

**RG-AUTH-02** - A senha e verificada contra o hash BCrypt armazenado. Senhas incorretas retornam HTTP 401.

**RG-AUTH-03** - O token gerado tem validade de 2 horas a partir do momento do login.

---

### 5.2 Cadastro

**Endpoint:** `POST /auth/register`

**Descricao:** Cadastra novo usuario com perfil ROLE_USER. Apos o cadastro, envia mensagem de boas-vindas via WhatsApp.

**Campos de entrada (RegisterRequestDTO):**

| Campo    | Tipo   | Obrigatorio | Validacao                                                        |
|----------|--------|-------------|------------------------------------------------------------------|
| name     | String | Sim         | Nao vazio                                                        |
| email    | String | Sim         | Formato de e-mail valido                                         |
| password | String | Sim         | Minimo 8 caracteres, com maiuscula, minuscula, numero e especial |
| phone    | String | Sim         | Formato (XX) XXXXX-XXXX                                         |

**Retorno:** Mesmo que Login (AuthResponseDTO).

**Regras:**

**RG-REG-01** - E-mails duplicados nao sao permitidos. O sistema retorna HTTP 409 caso o e-mail ja esteja cadastrado.

**RG-REG-02** - A senha nunca e armazenada em texto puro. E aplicado hash BCrypt antes de persistir.

**RG-REG-03** - Apos o cadastro, o sistema tenta enviar uma mensagem de boas-vindas via WhatsApp Business API (Meta) para o numero informado.

**RG-REG-04** - O numero de telefone e normalizado para o padrao internacional com codigo do pais 55 (Brasil) antes do envio.

---

### 5.3 Dados do Usuario Autenticado

**Endpoint:** `GET /auth/me`

**Descricao:** Retorna os dados do usuario autenticado na sessao atual.

**Acesso:** Requer autenticacao (ROLE_USER ou ROLE_ADMIN).

**Retorno:** UserResponseDTO com id, name, email, taxId, phone, address, photo, profileComplete, roles.

---

## 6. Modulo de Usuarios

**Endpoint base:** `/users`

---

### 6.1 Buscar Usuario por ID

**Endpoint:** `GET /users/{id}`

**Acesso:** Publico.

**Retorno:** UserDTO com id, name, email, roles, taxId, phone, address.

---

### 6.2 Criar Usuario

**Endpoint:** `POST /users`

**Acesso:** Publico.

**Campos de entrada (UserRequestDTO):**

| Campo   | Tipo    | Obrigatorio | Validacao                              |
|---------|---------|-------------|----------------------------------------|
| name    | String  | Sim         | Nao vazio                              |
| email   | String  | Sim         | Formato de e-mail valido               |
| password| String  | Sim         | Minimo 8 caracteres, complexidade      |
| taxId   | String  | Nao         | Formato XXX.XXX.XXX-XX (CPF)          |
| phone   | String  | Sim         | Formato (XX) XXXXX-XXXX               |
| address | Address | Sim         | Rua, bairro, cidade, estado, CEP       |

**Regras:**

**RG-USR-01** - E-mail unico por usuario. Duplicatas retornam HTTP 409.

**RG-USR-02** - CPF (taxId) e opcional. Quando informado, deve ser unico por usuario. Duplicatas retornam HTTP 409.

**RG-USR-03** - Novo usuario recebe automaticamente o perfil ROLE_USER.

---

### 6.3 Atualizar Usuario

**Endpoint:** `PUT /users/{id}`

**Acesso:** Requer autenticacao.

**Regras:**

**RG-USR-04** - Somente campos informados sao atualizados (atualizacao parcial).

---

### 6.4 Excluir Usuario

**Endpoint:** `DELETE /users/{id}`

**Acesso:** Requer autenticacao.

---

## 7. Modulo de Videos

**Endpoint base:** `/videos` (leitura publica) | `/admin/videos` (escrita, ROLE_ADMIN)

---

### 7.1 Listar Videos

**Endpoint:** `GET /videos`

**Acesso:** Publico.

**Retorno:** Lista de VideoDTO com id, title, description, url, cover, category, comments, commentCount, views, watchTime, recipe, informacoes nutricionais (protein, carbs, fat, fiber, calories), likesCount, favorited.

---

### 7.2 Buscar Video por ID

**Endpoint:** `GET /videos/{id}`

**Acesso:** Publico.

---

### 7.3 Registrar Visualizacao

**Endpoint:** `PATCH /videos/{id}/view`

**Acesso:** Requer autenticacao.

**Regras:**

**RG-VID-01** - Cada chamada incrementa o contador de visualizacoes do video em 1.

---

### 7.4 Videos Mais Assistidos

**Endpoint:** `GET /videos/most-viewed?limit=10`

**Acesso:** Publico.

**Retorno:** Lista de VideoDTO ordenada por views decrescente, limitada pelo parametro `limit` (padrao 10).

---

### 7.5 Videos Menos Assistidos

**Endpoint:** `GET /videos/least-viewed?limit=10`

**Acesso:** Publico.

---

### 7.6 Visualizacoes por Categoria

**Endpoint:** `GET /videos/views-by-category`

**Acesso:** Publico.

**Retorno:** Mapa com nome da categoria e total de visualizacoes.

---

### 7.7 Criar Video (Admin)

**Endpoint:** `POST /admin/videos`

**Acesso:** ROLE_ADMIN.

**Campos de entrada (VideoRequestDTO):**

| Campo      | Tipo   | Obrigatorio | Descricao                      |
|------------|--------|-------------|--------------------------------|
| title      | String | Sim         | Titulo do video                |
| description| String | Sim         | Descricao completa             |
| url        | String | Sim         | URL do arquivo de video        |
| cover      | String | Sim         | URL da imagem de capa          |
| categoryId | UUID   | Sim         | ID da categoria associada      |
| recipe     | String | Nao         | Receita relacionada ao video   |
| protein    | Double | Nao         | Proteinas (g)                  |
| carbs      | Double | Nao         | Carboidratos (g)               |
| fat        | Double | Nao         | Gorduras (g)                   |
| fiber      | Double | Nao         | Fibras (g)                     |
| calories   | Double | Nao         | Calorias (kcal)                |

**Regras:**

**RG-VID-02** - Ao criar um video, o sistema gera automaticamente uma notificacao do tipo VIDEO para todos os usuarios.

**RG-VID-03** - A categoria informada deve existir previamente no sistema.

---

### 7.8 Atualizar Video (Admin)

**Endpoint:** `PUT /admin/videos/{id}`

**Acesso:** ROLE_ADMIN.

**Regras:**

**RG-VID-04** - Somente campos informados sao atualizados (atualizacao parcial).

---

### 7.9 Excluir Video (Admin)

**Endpoint:** `DELETE /admin/videos/{id}`

**Acesso:** ROLE_ADMIN.

---

## 8. Modulo de Cardapios (Menus)

**Endpoint base:** `/menus` (leitura publica) | `/admin/menus` (escrita, ROLE_ADMIN)

---

### 8.1 Listar Cardapios

**Endpoint:** `GET /menus`

**Acesso:** Publico.

**Retorno:** Lista de MenuDTO com id, title, description, cover, category, recipe, nutritionistTips, informacoes nutricionais.

---

### 8.2 Buscar Cardapio por ID

**Endpoint:** `GET /menus/{id}`

**Acesso:** Publico.

---

### 8.3 Criar Cardapio (Admin)

**Endpoint:** `POST /admin/menus`

**Acesso:** ROLE_ADMIN.

**Campos de entrada (MenuRequestDTO):**

| Campo           | Tipo   | Obrigatorio | Descricao                        |
|-----------------|--------|-------------|----------------------------------|
| title           | String | Sim         | Titulo do cardapio               |
| description     | String | Sim         | Descricao completa               |
| cover           | String | Nao         | URL da imagem de capa            |
| categoryId      | UUID   | Sim         | ID da categoria associada        |
| recipe          | String | Nao         | Receita do cardapio              |
| nutritionistTips| String | Nao         | Dicas do nutricionista           |
| protein         | Double | Nao         | Proteinas (g)                    |
| carbs           | Double | Nao         | Carboidratos (g)                 |
| fat             | Double | Nao         | Gorduras (g)                     |
| fiber           | Double | Nao         | Fibras (g)                       |
| calories        | Double | Nao         | Calorias (kcal)                  |

**Regras:**

**RG-MNU-01** - Ao criar um cardapio, o sistema gera automaticamente uma notificacao do tipo MENU para todos os usuarios.

---

### 8.4 Atualizar Cardapio (Admin)

**Endpoint:** `PUT /admin/menus/{id}`

**Acesso:** ROLE_ADMIN.

---

### 8.5 Excluir Cardapio (Admin)

**Endpoint:** `DELETE /admin/menus/{id}`

**Acesso:** ROLE_ADMIN.

---

## 9. Modulo de Categorias

**Endpoint base:** `/categories`

---

### 9.1 Listar Categorias

**Endpoint:** `GET /categories?type=VIDEO|MENU`

**Acesso:** Publico.

**Parametros:**

| Parametro | Tipo         | Descricao                                |
|-----------|--------------|------------------------------------------|
| type      | CategoryType | Filtra por VIDEO ou MENU (obrigatorio)   |

---

### 9.2 Criar, Atualizar e Excluir Categorias

**Endpoints:** `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}`

**Acesso:** ROLE_ADMIN.

**Regras:**

**RG-CAT-01** - Nao e permitido cadastrar duas categorias com o mesmo nome e tipo.

---

## 10. Modulo de Comentarios

**Endpoint base:** `/comments`

---

### 10.1 Listar Comentarios por Video

**Endpoint:** `GET /comments/video/{videoId}`

**Acesso:** Publico.

**Retorno:** Lista de CommentResponseDTO com id, text, date (ISO 8601), user: { id, name }.

---

### 10.2 Listar Comentarios por Usuario

**Endpoint:** `GET /comments/user/{userId}`

**Acesso:** Publico.

---

### 10.3 Criar Comentario

**Endpoint:** `POST /comments`

**Acesso:** Requer autenticacao (ROLE_USER).

**Campos de entrada (CreateCommentDTO):**

| Campo   | Tipo   | Obrigatorio | Validacao                          |
|---------|--------|-------------|-------------------------------------|
| text    | String | Sim         | Nao vazio. Mensagem: "Comment text is required." |
| videoId | UUID   | Sim         | ID do video existente              |

**Regras:**

**RG-COM-01** - Nao e permitido o mesmo usuario postar comentarios com texto identico no mesmo video (protecao contra duplicatas).

---

### 10.4 Excluir Comentario

**Endpoint:** `DELETE /comments/{commentId}`

**Acesso:** ROLE_ADMIN.

---

## 11. Modulo de Favoritos

**Endpoint base:** `/favorites`

**Acesso:** Requer autenticacao para todos os endpoints.

---

### 11.1 Alternar Favorito (Toggle)

**Endpoint:** `POST /favorites/{type}/{itemId}`

**Descricao:** Adiciona o item aos favoritos se ainda nao estiver. Remove se ja estiver. Funciona como um botao de curtir.

**Parametros de rota:**

| Parametro | Tipo                | Descricao              |
|-----------|---------------------|------------------------|
| type      | FavoriteContentType | VIDEO ou MENU          |
| itemId    | String              | UUID do item           |

**Retorno:** `{ favorited: boolean, itemId: string, itemType: string }`

**Regras:**

**RG-FAV-01** - Um usuario nao pode favoritar o mesmo item duas vezes. A segunda chamada remove o favorito.

---

### 11.2 Listar Todos os Favoritos

**Endpoint:** `GET /favorites`

**Retorno:** Lista de FavoriteDTO com itemId, itemType, createdAt.

---

### 11.3 Listar Favoritos por Tipo

**Endpoint:** `GET /favorites/{type}`

---

### 11.4 Verificar Status de Favorito

**Endpoint:** `GET /favorites/{type}/{itemId}/status`

**Retorno:** `{ favorited: boolean, likesCount: long }`

---

## 12. Modulo de Notificacoes

**Endpoint base:** `/notifications`

**Acesso:** Requer autenticacao para todos os endpoints.

---

### 12.1 Listar Notificacoes

**Endpoint:** `GET /notifications?page=0&size=20`

**Descricao:** Retorna notificacoes paginadas. Cada notificacao indica se foi lida com base na data em que o usuario acessou as notificacoes pela ultima vez.

**Retorno (NotificationsPageDTO):**

| Campo  | Tipo                      | Descricao                           |
|--------|---------------------------|-------------------------------------|
| items  | List<NotificationItemDTO> | Lista de notificacoes da pagina     |
| hasMore| boolean                   | Indica se ha mais paginas           |

**NotificationItemDTO:**

| Campo     | Tipo             | Descricao                              |
|-----------|------------------|----------------------------------------|
| id        | UUID             | Identificador da notificacao           |
| type      | NotificationType | VIDEO ou MENU                          |
| title     | String           | Titulo da notificacao                  |
| contentId | UUID             | ID do conteudo relacionado             |
| createdAt | Instant          | Data e hora de criacao                 |
| read      | boolean          | True se criada antes do ultimo acesso  |

**Regras:**

**RG-NOT-01** - Uma notificacao e considerada lida se foi criada antes do campo `notificationsLastReadAt` do usuario.

**RG-NOT-02** - Notificacoes sao criadas automaticamente quando um video ou cardapio novo e publicado.

---

### 12.2 Contador de Nao Lidas

**Endpoint:** `GET /notifications/unread-count`

**Retorno:** `{ unreadCount: long }`

---

### 12.3 Marcar Todas como Lidas

**Endpoint:** `POST /notifications/mark-all-read`

**Descricao:** Atualiza o campo `notificationsLastReadAt` do usuario para o momento atual. A proxima consulta de notificacoes tera `unreadCount = 0`.

---

## 13. Modulo de Analytics (Admin)

**Acesso:** ROLE_ADMIN para todos os endpoints.

---

### 13.1 Analytics de Videos

**Endpoint base:** `/admin/videos`

| Endpoint                                      | Descricao                                        |
|-----------------------------------------------|--------------------------------------------------|
| `GET /admin/videos/most-viewed?limit=10`      | Videos mais assistidos                           |
| `GET /admin/videos/least-viewed?limit=10`     | Videos menos assistidos                          |
| `GET /admin/videos/views-by-category`         | Total de visualizacoes agrupadas por categoria   |
| `GET /admin/videos/{videoId}/tempo-medio-assistido` | Tempo medio de assistencia de um video    |
| `GET /admin/videos/mais-comentados?limit=10`  | Videos com mais comentarios                      |

---

### 13.2 Analytics de Comentarios

**Endpoint base:** `/admin/comentarios`

| Endpoint                                              | Descricao                                      |
|-------------------------------------------------------|------------------------------------------------|
| `GET /admin/comentarios/quantidade/video/{videoId}`   | Quantidade de comentarios em um video          |
| `GET /admin/comentarios/usuarios/video/{videoId}`     | Nomes dos usuarios que comentaram em um video  |
| `GET /admin/comentarios/total`                        | Total de comentarios em toda a plataforma      |
| `GET /admin/comentarios/total-por-video`              | Total de comentarios agrupados por video       |

---

## 14. Integracao WhatsApp Business API

**Provedor:** Meta (Facebook) Graph API v22.0

**Endpoint de webhook:** `GET /whatsapp/webhook`, `POST /whatsapp/webhook` (publico, verificacao Meta)

**Regras:**

**RG-WPP-01** - O envio de mensagens so ocorre quando a propriedade `whatsapp.enabled=true`. Em desenvolvimento, as mensagens sao simuladas no console.

**RG-WPP-02** - O numero de destino e normalizado: caracteres nao numericos sao removidos e o codigo do pais 55 (Brasil) e adicionado caso nao esteja presente.

**RG-WPP-03** - O conteudo da mensagem e definido por template aprovado no WhatsApp Manager da Meta, nao pelo codigo da aplicacao.

**RG-WPP-04** - Em caso de falha no envio (erro HTTP da API Meta), o status da mensagem e atualizado para `SEND_ERROR` e o erro e registrado em log. O cadastro do usuario nao e afetado.

---

## 15. Modelo de Dados

### Entidade: User

| Campo                    | Tipo          | Obrigatorio | Descricao                           |
|--------------------------|---------------|-------------|--------------------------------------|
| id                       | UUID          | Sim         | Identificador unico (auto-gerado)    |
| name                     | String        | Sim         | Nome completo (unico)                |
| email                    | String        | Sim         | E-mail (unico)                       |
| password                 | String        | Sim         | Senha criptografada (BCrypt)         |
| taxId                    | String (14)   | Nao         | CPF no formato XXX.XXX.XXX-XX (unico)|
| phone                    | String (15)   | Sim         | Telefone no formato (XX) XXXXX-XXXX  |
| address                  | Address       | Nao         | Endereco embutido                    |
| photo                    | String        | Nao         | URL da foto de perfil                |
| profileComplete          | boolean       | -           | Indica se o perfil esta completo     |
| createdAt                | LocalDateTime | -           | Data de criacao (auto-preenchida)    |
| updatedAt                | LocalDateTime | -           | Data de atualizacao (auto-preenchida)|
| notificationsLastReadAt  | Instant       | Nao         | Ultima vez que leu as notificacoes   |
| roles                    | List<Role>    | -           | Perfis de acesso (ManyToMany)        |

### Entidade: Video

| Campo       | Tipo          | Obrigatorio | Descricao                      |
|-------------|---------------|-------------|--------------------------------|
| id          | UUID          | Sim         | Identificador unico            |
| title       | String (150)  | Sim         | Titulo                         |
| description | String (text) | Sim         | Descricao                      |
| url         | String        | Sim         | URL do video                   |
| cover       | String        | Nao         | URL da imagem de capa          |
| category    | Category      | Sim         | Categoria (ManyToOne)          |
| views       | int           | -           | Contador de visualizacoes      |
| watchTime   | double        | -           | Tempo total assistido          |
| recipe      | String (text) | Nao         | Receita                        |
| protein     | Double        | Nao         | Proteinas (g)                  |
| carbs       | Double        | Nao         | Carboidratos (g)               |
| fat         | Double        | Nao         | Gorduras (g)                   |
| fiber       | Double        | Nao         | Fibras (g)                     |
| calories    | Double        | Nao         | Calorias (kcal)                |
| likesCount  | int           | -           | Total de curtidas              |

### Entidade: Notification

| Campo     | Tipo             | Descricao                                  |
|-----------|------------------|--------------------------------------------|
| id        | UUID             | Identificador unico (auto-gerado se nulo)  |
| type      | NotificationType | VIDEO ou MENU                              |
| title     | String (255)     | Titulo da notificacao                      |
| contentId | UUID             | ID do conteudo relacionado                 |
| createdAt | Instant          | Data de criacao (auto-preenchida)          |

---

## 16. Pipeline de CI/CD

O projeto utiliza GitHub Actions para integracao e entrega continuas. O pipeline e ativado a cada push na branch `main` ou em branches `feat/*`.

### Fluxo

```
push main → test → docker (build/push Docker Hub) → deploy (Elastic Beanstalk)
```

### Jobs

| Job    | Trigger              | Descricao                                                      |
|--------|----------------------|----------------------------------------------------------------|
| test   | push main / feat/*   | Executa todos os testes com Maven (./mvnw test)                |
| docker | apos test (main)     | Builda imagem Docker e faz push no Docker Hub com tag SHA      |
| deploy | apos docker (main)   | Gera Dockerrun.aws.json, faz upload no S3 e atualiza o EB env  |

### Deploy no Elastic Beanstalk

O EB utiliza `Dockerrun.aws.json` para saber qual imagem Docker executar. A cada deploy:

1. O job gera o arquivo com a imagem `{DOCKERHUB_USERNAME}/vidalongaflix:{SHA}`
2. O arquivo e zipado e enviado ao bucket S3 do EB
3. Uma nova "application version" e criada no EB
4. O environment e atualizado para usar a nova versao

**Configuracoes necessarias (GitHub Secrets):**

| Secret                | Descricao                                   |
|-----------------------|---------------------------------------------|
| `DOCKERHUB_USERNAME`  | Usuario do Docker Hub                       |
| `DOCKERHUB_TOKEN`     | Token de acesso ao Docker Hub               |
| `AWS_ACCESS_KEY_ID`   | Chave de acesso IAM do usuario vidalongaflix-ci |
| `AWS_SECRET_ACCESS_KEY`| Chave secreta IAM                           |

**Configuracoes do Elastic Beanstalk:**

| Parametro            | Valor                                      |
|----------------------|--------------------------------------------|
| Application name     | vidalongaflix-backend                      |
| Environment name     | Vidalongaflix-backend-env                  |
| Region               | us-east-2                                  |
| Porta do container   | 8090                                       |

### Racional da Ordem dos Jobs

A ordem `test → docker → deploy` pode parecer invertida em relacao ao padrao da industria, onde o build ocorre antes dos testes. Neste projeto, os testes unitarios (Maven) nao dependem da aplicacao em execucao nem de imagem Docker — eles simulam a aplicacao em memoria (H2). Por isso, faz sentido validar o codigo primeiro e so entao construir e publicar a imagem.

Em projetos com testes end-to-end (que acessam a aplicacao real rodando), o fluxo seria:

```
build imagem → subir container → rodar testes e2e → push Docker Hub → deploy
```

### Permissoes IAM (usuario vidalongaflix-ci)

Criar no AWS Console → IAM → Users → `vidalongaflix-ci` → Attach policies → JSON:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:CreateApplicationVersion",
        "elasticbeanstalk:UpdateEnvironment",
        "elasticbeanstalk:DescribeEnvironments",
        "elasticbeanstalk:DescribeApplicationVersions",
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": "*"
    }
  ]
}
```

### Passos Operacionais para Ativar o CD

| # | Passo | Status |
|---|---|---|
| 1 | Criar usuario IAM `vidalongaflix-ci` no AWS Console com a policy acima | ✅ Concluido |
| 2 | Gerar Access Key para o usuario (IAM → Security credentials → Create access key) | ✅ Concluido |
| 3 | Cadastrar `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` no GitHub Environment `production` | ✅ Concluido |
| 4 | Confirmar nomes no console AWS: `application_name` e `environment_name` no `ci.yml` | ✅ Concluido |
| 5 | Criar o GitHub Environment `production` com reviewer obrigatorio | ✅ Concluido |

### GitHub Environment: production

O job `deploy` referencia o environment `production` no `ci.yml`. Isso adiciona uma porta de aprovacao manual antes de qualquer deploy ir para producao.

**Como configurar (uma unica vez):**

```
GitHub → Settings → Environments → New environment
Nome: production
```

Em seguida, dentro do environment `production`:

- Marcar **"Required reviewers"** e adicionar seu usuario → exige aprovacao manual antes do deploy executar
- Mover os secrets AWS para o nivel do environment (opcional, mas recomendado):
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`

**Fluxo apos a configuracao:**

```
push main → test ✅ → docker ✅ → deploy ⏸ aguarda aprovacao
                                         ↓ voce recebe notificacao por email
                                         ↓ voce clica "Approve" no GitHub
                                         ✅ deploy executa no Elastic Beanstalk
```

**Beneficios:**

| Beneficio | Descricao |
|---|---|
| Controle de deploy | Nenhum codigo vai para producao sem aprovacao consciente |
| Historico auditavel | GitHub registra quem aprovou, quando e qual commit foi deployado |
| Secrets isolados | Credenciais AWS ficam no escopo do environment, nao misturadas com outros secrets |
| Protecao contra deploy acidental | Evita deployar codigo quebrado que passou nos testes mas tem problema logico |

---

## 17. Codigos de Retorno HTTP

| Codigo | Descricao                                     |
|--------|-----------------------------------------------|
| 200    | Requisicao processada com sucesso             |
| 201    | Recurso criado com sucesso                    |
| 204    | Operacao concluida sem conteudo de retorno    |
| 400    | Dados de entrada invalidos ou campos faltando |
| 401    | Nao autenticado ou credenciais invalidas      |
| 403    | Sem permissao para o recurso solicitado       |
| 404    | Recurso nao encontrado                        |
| 409    | Conflito: recurso duplicado                   |
| 500    | Erro interno do servidor                      |
