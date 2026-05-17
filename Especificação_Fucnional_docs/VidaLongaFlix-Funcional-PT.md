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
    │  HTTP + JWT Bearer
    ▼
Spring Boot API (Elastic Beanstalk — porta 8090, context /api)
    │
    ├── PostgreSQL (RDS — produção)
    ├── S3 (mídia — vídeos e imagens)
    └── WhatsApp Business API (Meta — notificações de boas-vindas)
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

## 7. Segurança no Frontend

| Situação                                          | Comportamento esperado                              |
|---------------------------------------------------|-----------------------------------------------------|
| Token JWT expirado (2h)                           | Redirect automático para `/login`                   |
| Usuário sem token acessa rota privada             | Redirect para `/login`                              |
| ROLE_USER tenta acessar rota `/admin/**`          | Redirect para home ou página 403                    |
| Ícone de edição (✏️) em vídeos e cardápios        | Visível **somente** para ROLE_ADMIN                 |
| Botão de excluir comentário                       | Visível **somente** para ROLE_ADMIN                 |
| Botão de favorito (❤️)                            | Visível **somente** para usuários autenticados      |
| Campo de comentário                               | Habilitado **somente** para usuários autenticados   |
| Todas as requisições autenticadas                 | Header `Authorization: Bearer {token}` via interceptor Angular |

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

---

## 10. Histórico de Revisões

| Data       | Descrição                                            | Responsável |
|------------|------------------------------------------------------|-------------|
| 17/05/2026 | Criação do documento — cobertura completa do sistema | Fabricio    |