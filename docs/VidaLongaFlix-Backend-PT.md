# VidaLongaFlix - Documentacao do Sistema Backend

## 1. Historico de Revisoes

| Data       | Resumo da Revisao                                                            | Responsavel | Previsao de Desenvolvimento |
|------------|------------------------------------------------------------------------------|-------------|------------------------------|
| 28/02/2026 | Criacao do documento inicial                                                 | Fabricio    | -                            |
| 03/03/2026 | Correcoes: shape CommentResponseDTO, CORS configuravel, CategoryController   | Fabricio    | -                            |
| 07/03/2026 | CPF (taxId) passa a ser opcional no cadastro de usuario                      | Fabricio    | -                            |
| 08/03/2026 | Adicao de CD automatico: job deploy GitHub Actions -> Elastic Beanstalk      | Fabricio    | -                            |
| 08/03/2026 | GitHub Environment "production" com aprovacao manual antes do deploy         | Fabricio    | -                            |
| 09/03/2026 | Limite de usuarios ativos, fila de espera (waitlist) e endpoints administrativos | Fabricio | Backend implementado; frontend deve tratar respostas `201/202` no cadastro |

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

**RG09** - O sistema possui um limite configuravel de usuarios com status `ACTIVE`, definido em `app_config.MAX_ACTIVE_USERS`. Quando o limite e atingido, novos cadastros entram na fila com status `QUEUED`.

**RG10** - Usuarios com status `QUEUED` nao recebem token JWT no cadastro e nao podem fazer login ate serem promovidos para `ACTIVE`.

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

**RG-AUTH-04** - Se o usuario estiver com status `QUEUED`, o sistema retorna HTTP 403 com `error = ACCOUNT_QUEUED`, mensagem explicativa e `queuePosition`.

**RG-AUTH-05** - Se o usuario estiver com status `DISABLED`, o sistema retorna HTTP 403 com `error = ACCOUNT_DISABLED`.

---

### 5.2 Cadastro

**Endpoint:** `POST /auth/register`

**Descricao:** Cadastra novo usuario com perfil ROLE_USER. Se houver vaga dentro do limite de usuarios ativos, o usuario entra como `ACTIVE` e recebe token. Se o limite estiver esgotado, o usuario entra na fila com status `QUEUED`, sem token JWT.

**Campos de entrada (RegisterRequestDTO):**

| Campo    | Tipo   | Obrigatorio | Validacao                                                        |
|----------|--------|-------------|------------------------------------------------------------------|
| name     | String | Sim         | Nao vazio                                                        |
| email    | String | Sim         | Formato de e-mail valido                                         |
| password | String | Sim         | Minimo 8 caracteres, com maiuscula, minuscula, numero e especial |
| phone    | String | Sim         | Formato (XX) XXXXX-XXXX                                         |

**Retorno:** `RegistrationResponseDTO`.

**Campos de retorno (RegistrationResponseDTO):**

| Campo         | Tipo            | Descricao |
|---------------|-----------------|-----------|
| token         | String          | Token JWT quando o usuario entra como `ACTIVE`; `null` quando entra na fila |
| user          | UserResponseDTO | Dados do usuario, incluindo `status` e `queuePosition` |
| queued        | Boolean         | Indica se o cadastro entrou na fila |
| queuePosition | Integer         | Posicao na fila quando `queued = true` |
| message       | String          | Mensagem explicativa para a UI |

**Regras:**

**RG-REG-01** - E-mails duplicados nao sao permitidos. O sistema retorna HTTP 409 caso o e-mail ja esteja cadastrado.

**RG-REG-02** - A senha nunca e armazenada em texto puro. E aplicado hash BCrypt antes de persistir.

**RG-REG-03** - Apos o cadastro, o sistema tenta enviar uma mensagem de boas-vindas via WhatsApp Business API (Meta) para o numero informado.

**RG-REG-04** - O numero de telefone e normalizado para o padrao internacional com codigo do pais 55 (Brasil) antes do envio.

**RG-REG-05** - Se `count(status = ACTIVE) < MAX_ACTIVE_USERS`, o sistema salva o usuario com status `ACTIVE`, gera o token JWT e retorna HTTP 201.

**RG-REG-06** - Se `count(status = ACTIVE) >= MAX_ACTIVE_USERS`, o sistema salva o usuario com status `QUEUED`, define `queuePosition`, nao gera token e retorna HTTP 202.

**RG-REG-07** - Se o e-mail ja estiver cadastrado com status `QUEUED`, o sistema retorna HTTP 409 informando que o usuario ja esta na fila de espera e inclui a posicao atual na mensagem.

**RG-REG-08** - O backend atualmente registra em log as notificacoes por e-mail da fila, ativacao e remocao. O envio real de e-mail ainda nao esta implementado.

**Exemplo de resposta - cadastro ativo (201 Created):**

```json
{
  "token": "jwt-token",
  "user": {
    "id": "uuid",
    "name": "Maria Silva",
    "email": "maria@gmail.com",
    "phone": "(11) 98765-4321",
    "status": "ACTIVE",
    "queuePosition": null,
    "profileComplete": false,
    "roles": ["ROLE_USER"]
  },
  "queued": false,
  "queuePosition": null,
  "message": null
}
```

**Exemplo de resposta - cadastro na fila (202 Accepted):**

```json
{
  "token": null,
  "user": {
    "id": "uuid",
    "name": "Maria Silva",
    "email": "maria@gmail.com",
    "phone": "(11) 98765-4321",
    "status": "QUEUED",
    "queuePosition": 5,
    "profileComplete": false,
    "roles": ["ROLE_USER"]
  },
  "queued": true,
  "queuePosition": 5,
  "message": "Limite de usuarios atingido. Voce foi adicionado a fila de espera na posicao #5."
}
```

---

### 5.3 Status de Registro

**Endpoint:** `GET /auth/registration-status`

**Descricao:** Retorna o estado atual do cadastro publico, com quantidade de usuarios ativos, limite configurado e tamanho da fila.

**Retorno (RegistrationStatusDTO):**

| Campo       | Tipo    | Descricao |
|-------------|---------|-----------|
| open        | Boolean | `true` quando ainda existem vagas para `ACTIVE` |
| activeUsers | Long    | Quantidade atual de usuarios `ACTIVE` |
| limit       | Integer | Limite atual de usuarios ativos |
| queueSize   | Long    | Quantidade atual de usuarios na fila |

---

### 5.4 Cancelamento da Fila

**Endpoint:** `DELETE /auth/waitlist/me?email={email}`

**Descricao:** Remove da fila um usuario com status `QUEUED` a partir do e-mail informado.

**Retorno:** `WaitlistMessageDTO` com a mensagem `Voce foi removido da fila de espera.`

---

### 5.5 Dados do Usuario Autenticado

**Endpoint:** `GET /auth/me`

**Descricao:** Retorna os dados do usuario autenticado na sessao atual.

**Acesso:** Requer autenticacao (ROLE_USER ou ROLE_ADMIN).

**Retorno:** UserResponseDTO com id, name, email, taxId, phone, address, photo, profileComplete, status, queuePosition, roles.

---

## 5.6 Administracao da Fila de Espera

**Endpoint base:** `/admin`

**Acesso:** Requer `ROLE_ADMIN`

### 5.6.1 Listar fila

**Endpoint:** `GET /admin/waitlist`

**Descricao:** Retorna o limite atual, quantidade de usuarios ativos e a fila ordenada por posicao.

### 5.6.2 Ativar usuario da fila manualmente

**Endpoint:** `POST /admin/waitlist/{userId}/activate`

**Descricao:** Promove um usuario `QUEUED` para `ACTIVE` quando existir vaga disponivel.

### 5.6.3 Remover usuario da fila

**Endpoint:** `DELETE /admin/waitlist/{userId}`

**Descricao:** Remove um usuario da fila e recalcula as posicoes restantes.

### 5.6.4 Atualizar limite de usuarios ativos

**Endpoint:** `PUT /admin/config/max-users`

**Descricao:** Atualiza o limite de usuarios `ACTIVE`. Se o novo limite abrir vagas, o backend promove automaticamente usuarios da fila.

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
| deploy | apos docker (main)   | Gera o bundle EB com Dockerrun.aws.json + .platform, faz upload no S3 e atualiza o EB env |

### Deploy no Elastic Beanstalk

O EB utiliza `Dockerrun.aws.json` para saber qual imagem Docker executar. A cada deploy:

1. O job gera o arquivo com a imagem `{DOCKERHUB_USERNAME}/vidalongaflix:{SHA}`
2. O bundle `deploy.zip` e montado com `Dockerrun.aws.json` e `.platform/`
3. O hook `.platform/hooks/predeploy/01_create_db.sh` chega a instancia do EB e garante que o banco do `DB_URL` exista antes de subir o container
4. O bundle e enviado ao bucket S3 do EB
5. Uma nova "application version" e criada no EB
6. O environment e atualizado para usar a nova versao

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

---

## 18. Infraestrutura de Armazenamento e Upload de Midia

> **Adicionado: 04/04/2026**
> Esta secao documenta toda a infraestrutura de upload e armazenamento de midia introduzida para suportar videos e imagens de capa de forma persistente.

### 18.1 Contexto do Problema

A implementacao original permitia que o frontend enviasse uma URL `blob:` (gerada via `URL.createObjectURL()`) como a URL do video no corpo JSON da requisicao. URLs `blob:` sao **locais ao navegador** — existem apenas na sessao/aba que as criou e nao podem ser acessadas por nenhum outro cliente nem armazenadas de forma persistente. Como resultado, os videos pareciam ser salvos, mas nunca eram reproduzidos.

A correcao exigiu tres mudancas trabalhando juntas:
1. O **frontend** deve enviar o arquivo real via `multipart/form-data` em vez de uma URL `blob:`.
2. O **backend** deve aceitar `multipart/form-data` e armazenar o arquivo.
3. O **armazenamento deve ser persistente** — AWS S3 em producao, disco local como fallback de desenvolvimento.

---

### 18.2 Endpoint de Upload Multipart

`POST /admin/videos` e `PUT /admin/videos/{id}` agora suportam dois content-types:

| Content-Type | Handler | Uso |
|---|---|---|
| `application/json` | `create()` / `update()` | Fornece URLs publicas ja existentes para video e capa |
| `multipart/form-data` | `createMultipart()` / `updateMultipart()` | Envia os arquivos reais; o backend os armazena e retorna a URL |

**Campos aceitos no formulario:**

| Campo | Tipo | Obrigatorio (POST) | Descricao |
|---|---|---|---|
| `title` | texto | ✅ | Titulo do video |
| `description` | texto | ✅ | Descricao do video |
| `categoryId` | texto (UUID) | ✅ | UUID da categoria |
| `videoFile` / `video` / `url` / `file` | arquivo ou texto | ✅ | Arquivo de video ou URL publica existente |
| `coverFile` / `cover` / `thumbnail` / `image` | arquivo ou texto | ✅ | Imagem de capa ou URL publica existente |
| `recipe` | texto | ❌ | Texto da receita |
| `protein`, `carbs`, `fat`, `fiber`, `calories` | numero | ❌ | Informacoes nutricionais |

Quando um campo de arquivo e um campo de texto URL estao presentes para a mesma midia, a **URL de texto tem prioridade** (o arquivo e ignorado).

---

### 18.3 MediaStorageService

`src/main/java/com/dvFabricio/VidaLongaFlix/services/MediaStorageService.java`

Gerencia o armazenamento de arquivos em dois modos, selecionados na inicializacao:

**Modo S3** (producao) — ativo quando `aws.s3.bucket` nao esta em branco:
- Constroi um `S3Client` para a regiao configurada via `@PostConstruct`.
- Envia com `PutObjectRequest` usando `RequestBody.fromInputStream`.
- Retorna URL CloudFront (`CDN_BASE_URL/{key}`) se configurado, caso contrario URL direta do S3 (`https://{bucket}.s3.{region}.amazonaws.com/{key}`).

**Modo local** (dev/fallback) — ativo quando `aws.s3.bucket` esta em branco:
- Cria diretorios `{media.storage.path}/videos/` e `{media.storage.path}/covers/`.
- Copia o arquivo com um nome baseado em UUID.
- Retorna uma URL construida com `ServletUriComponentsBuilder.fromCurrentContextPath()` apontando para `/media/{dir}/{filename}`.

Os dois modos geram um nome de arquivo UUID aleatorio para evitar colisoes e ocultar o nome original do arquivo.

---

### 18.4 Servico de Midia Local (fallback dev)

`src/main/java/com/dvFabricio/VidaLongaFlix/infra/config/MediaResourceConfig.java`

Registra um resource handler do Spring MVC que mapeia `/api/media/**` para o diretorio de armazenamento local no disco.

```java
registry.addResourceHandler("/media/**")
    .addResourceLocations(localStorageRoot.toUri().toString());
```

O `SecurityConfig` libera `/media/**` publicamente para que clientes nao autenticados possam fazer streaming de video.

---

### 18.5 Limite de Tamanho de Upload no nginx

`.platform/nginx/conf.d/upload.conf`

O AWS Elastic Beanstalk usa nginx como proxy reverso. Por padrao, o nginx bloqueia corpos de requisicao maiores que 1 MB. Sem este arquivo, o upload de um video retorna **413 Content Too Large** antes de a requisicao chegar ao Spring Boot.

```nginx
client_max_body_size 512M;
proxy_connect_timeout   60s;
proxy_read_timeout     300s;
proxy_send_timeout     300s;
send_timeout           300s;
proxy_request_buffering off;
```

`proxy_request_buffering off` transmite o upload diretamente para a aplicacao em vez de armazenar o arquivo inteiro na memoria do nginx, essencial para arquivos grandes.

---

### 18.6 Infraestrutura AWS

**Bucket S3**

| Configuracao | Valor |
|---|---|
| Nome do bucket | `vidalongaflix-media` |
| Regiao | `us-east-2` |
| Acesso publico | Block Public Access: **desabilitado** |
| Politica do bucket | Leitura publica para todos os objetos (veja abaixo) |

Politica de bucket aplicada:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::vidalongaflix-media/*"
    }
  ]
}
```

**Politica IAM para EC2 → S3 upload**

Politica inline `vidalongaflix_media_upload` adicionada ao role `aws-elasticbeanstalk-ec2-role`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::vidalongaflix-media/*"
    }
  ]
}
```

Isso permite que as instancias EC2 do EB facade upload e delete de objetos sem precisar de credenciais AWS de longo prazo na aplicacao.

---

### 18.7 Variaveis de Ambiente

Novas variaveis necessarias no Elastic Beanstalk:

| Variavel | Obrigatoria | Exemplo | Descricao |
|---|---|---|---|
| `AWS_S3_BUCKET` | Sim (para S3) | `vidalongaflix-media` | Habilita modo S3; deixar em branco para armazenamento local |
| `AWS_REGION` | Nao | `us-east-2` | Regiao do bucket S3 (padrao: `us-east-2`) |
| `CDN_BASE_URL` | Nao | `https://xxxx.cloudfront.net` | Se definido, as URLs retornadas usam CloudFront em vez do S3 direto |
| `MEDIA_STORAGE_PATH` | Nao | `/tmp/vidalongaflix-media` | Caminho no disco para o modo fallback local |
| `MAX_UPLOAD_FILE_SIZE` | Nao | `512MB` | Limite de tamanho por arquivo do multipart do Spring |
| `MAX_UPLOAD_REQUEST_SIZE` | Nao | `512MB` | Limite de tamanho total da requisicao multipart do Spring |

Propriedades da aplicacao (`application-prod.properties`):

```properties
aws.s3.bucket=${AWS_S3_BUCKET:}
aws.s3.region=${AWS_REGION:us-east-2}
aws.cdn.base-url=${CDN_BASE_URL:}
media.storage.path=${MEDIA_STORAGE_PATH:/tmp/vidalongaflix-media}
spring.servlet.multipart.max-file-size=${MAX_UPLOAD_FILE_SIZE:512MB}
spring.servlet.multipart.max-request-size=${MAX_UPLOAD_REQUEST_SIZE:512MB}
server.forward-headers-strategy=FRAMEWORK
```

`server.forward-headers-strategy=FRAMEWORK` e necessario para que o `ServletUriComponentsBuilder` use o header `X-Forwarded-Host` do nginx ao construir URLs de midia local, em vez do hostname interno da EC2.

---

### 18.8 Fluxo de Upload Resumido

```
Navegador (FormData)
    │
    ▼
nginx (.platform/nginx/conf.d/upload.conf)
    │  client_max_body_size 512M — permite a requisicao passar
    ▼
Spring Boot — AdminVideoController.createMultipart()
    │  le campos de arquivo do MultipartHttpServletRequest
    ▼
MediaStorageService.store(file, "videos" | "covers")
    │
    ├─ AWS_S3_BUCKET definido? ──► S3Client.putObject() → retorna URL S3/CDN
    │
    └─ em branco ───────────────► Files.copy() para disco local → retorna URL /media/...
    ▼
VideoService.create(VideoRequestDTO com URL resolvida)
    ▼
Banco de dados — linha do video salva com URL permanente e acessivel
```
