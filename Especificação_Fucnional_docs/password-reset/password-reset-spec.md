# Especificação Funcional — Redefinição de Senha via Email

## Visão Geral

O fluxo de redefinição de senha permite que um usuário que esqueceu sua senha receba
um token temporário por email e o utilize para cadastrar uma nova senha. O sistema nunca
deve expor ou transmitir a senha atual e deve invalidar o token após o uso.

---

## PR-01 — Solicitar redefinição de senha

### Quando dispara
Quando o usuário clica em "Esqueci minha senha" na tela de login, informa o email
e envia o formulário do modal de recuperação (`PasswordRecoveryComponent`).

### Chamada do frontend
`POST /auth/password-recovery`
```json
{ "email": "usuario@exemplo.com" }
```

### Quem trata (backend)
`PasswordResetController.requestReset(String email)`
→ delega para `PasswordResetService.requestReset(email)`

### Regras de negócio

| Regra | Detalhe |
|---|---|
| Email não encontrado | Retornar **200 OK** com mensagem genérica — não revelar se o email existe (evita enumeração de usuários) |
| Usuário DISABLED | Mesmo 200 genérico — não elegível para reset, mas não informar o motivo |
| Usuário QUEUED | Mesmo 200 genérico — não elegível para reset, mas não informar o motivo |
| Token anterior existe | Deletar o token antigo antes de criar o novo (um token ativo por usuário) |
| Geração do token | `SecureRandom` — 64 caracteres hexadecimais (32 bytes) |
| Expiração do token | 30 minutos a partir da criação (`expires_at = now() + 30min`) |
| Armazenamento | Tabela `password_reset_tokens` — ver modelo de dados abaixo |

### Email enviado
`PasswordResetEmailService.send(String name, String email, String token)`

#### Assunto
```
VidaLongaFlix — Solicitação de redefinição de senha
```

#### Corpo (texto simples)
```
Olá, [Nome].

Recebemos uma solicitação para redefinir a senha da sua conta no VidaLongaFlix.

Clique no link abaixo para criar uma nova senha (válido por 30 minutos):

https://vidalongaflix.com/password-change?token=[TOKEN]

Se você não solicitou a redefinição, pode ignorar este email com segurança.
Sua senha atual permanecerá sem alterações.

---
Equipe VidaLongaFlix
```

### Resposta ao frontend (sempre a mesma)
```json
{ "message": "Se o seu email estiver cadastrado, você receberá um link em breve." }
```

### Comportamento em caso de falha
- Erro de SMTP é capturado e logado — não afeta o retorno HTTP 200
- O token **não** é salvo se o envio do email falhar (atômico: ou os dois ocorrem, ou nenhum)

---

## PR-02 — Validar token

### Quando dispara
Quando `PasswordChangeComponent.ngOnInit()` lê o token da query string da URL
e consulta o backend para verificar se o link ainda é válido antes de exibir o formulário.

### Chamada do frontend
`GET /auth/validate-token?token=<valor>`

### Quem trata (backend)
`PasswordResetController.validateToken(String token)`
→ delega para `PasswordResetService.validateToken(token)`

### Regras de negócio

| Condição | Resposta HTTP |
|---|---|
| Token encontrado, não expirado, `used = false` | 200 OK |
| Token não encontrado | 404 Not Found |
| Token expirado (`expires_at < now()`) | 410 Gone |
| Token já utilizado (`used = true`) | 410 Gone |

### Comportamento do frontend em caso de não-200
`PasswordChangeComponent` redireciona para `/login` e exibe mensagem de erro.

---

## PR-03 — Redefinir senha

### Quando dispara
Quando o usuário submete o formulário com a nova senha dentro do `PasswordChangeComponent`.

### Chamada do frontend
`POST /auth/reset-password`
```json
{ "token": "<valor>", "newPassword": "NovaSenha@123" }
```

### Quem trata (backend)
`PasswordResetController.resetPassword(ResetPasswordRequestDTO body)`
→ delega para `PasswordResetService.resetPassword(token, newPassword)`

### Regras de negócio

| Regra | Detalhe |
|---|---|
| Revalidação do token | Mesmo processo do PR-02 — revalidar a cada tentativa de reset |
| Codificação da senha | `BCryptPasswordEncoder.encode(newPassword)` |
| Salvar | `userRepository.save(user)` com a nova senha codificada |
| Invalidar token | Setar `used = true` e salvar — token não pode ser reutilizado |
| Senha mínima | `@Size(min = 8)` no DTO; frontend também valida com `strongPasswordValidator` |
| Mesma senha | Não bloqueado no servidor — tradeoff de UX aceitável |

### Resposta em caso de sucesso
```
204 No Content
```

### Resposta em caso de token inválido
```
410 Gone  { "error": "Token expirado ou já utilizado." }
```

---

## PR-04 — Email de confirmação após redefinição

### Quando dispara
Imediatamente após um `resetPassword()` bem-sucedido.

### Quem envia
`PasswordResetEmailService.sendChangeConfirmation(String name, String email)`

#### Assunto
```
VidaLongaFlix — Sua senha foi alterada
```

#### Corpo (texto simples)
```
Olá, [Nome].

Sua senha do VidaLongaFlix foi alterada com sucesso.

Se você não realizou essa alteração, entre em contato conosco imediatamente
respondendo este email.

---
Equipe VidaLongaFlix
```

### Comportamento em caso de falha
- Erro de SMTP é capturado e logado
- O reset **não** é desfeito — a alteração de senha já foi concluída

---

## Modelo de Dados

### Tabela: `password_reset_tokens`

```sql
CREATE TABLE password_reset_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token   ON password_reset_tokens(token);
CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);
```

### Entidade: `PasswordResetToken.java`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Chave primária |
| `user` | `User` (ManyToOne) | Dono do token |
| `token` | `String` | 64 chars hex, único |
| `expiresAt` | `LocalDateTime` | `now() + 30 min` |
| `used` | `boolean` | `false` até ser consumido |
| `createdAt` | `LocalDateTime` | Auditoria |

---

## Resumo dos novos endpoints

| Método | Caminho | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/auth/password-recovery` | Não | Solicitar reset — enviar email |
| `GET` | `/auth/validate-token` | Não | Verificar validade do token |
| `POST` | `/auth/reset-password` | Não | Aplicar nova senha |

Os três devem ser adicionados ao `SecurityConfig` sob `.requestMatchers("/auth/**").permitAll()` — já coberto pela regra existente.

---

## Alterações necessárias no frontend

### `service-password-recovery.ts` — substituir mocks por chamadas reais

```typescript
// validateToken — substituir mock com setTimeout
async validateToken(token: string): Promise<boolean> {
  return firstValueFrom(
    this.http.get<boolean>(`${this.api.baseURL}/auth/validate-token?token=${token}`)
  ).catch(() => false);
}

// changePassword — substituir mock com setTimeout
async changePassword(token: string, newPassword: string): Promise<void> {
  await firstValueFrom(
    this.http.post<void>(`${this.api.baseURL}/auth/reset-password`, { token, newPassword })
  );
}
```

> Nenhuma alteração necessária em `PasswordRecoveryComponent` ou `PasswordChangeComponent` — eles já chamam o service corretamente.

---

## Novas classes no backend

| Classe | Pacote | Responsabilidade |
|---|---|---|
| `PasswordResetToken` | `domain.passwordreset` | Entidade JPA |
| `PasswordResetTokenRepository` | `repositories` | `findByToken`, `deleteByUserId` |
| `PasswordResetService` | `services` | Lógica de negócio dos três fluxos |
| `PasswordResetController` | `controllers` | Handlers HTTP |
| `PasswordResetRequestDTO` | `domain.passwordreset` | `{ email }` |
| `ResetPasswordRequestDTO` | `domain.passwordreset` | `{ token, newPassword }` |
| `PasswordResetEmailService` | `services` | Composição e envio dos emails |
| `VXX__create_password_reset_tokens.sql` | `resources/db/migration` | Migration Flyway |

---

## Limitações conhecidas (fora do escopo desta implementação)

| Limitação | Descrição | Solução futura |
|---|---|---|
| Sem retry em falha SMTP | Se o email falhar, o usuário precisa solicitar novamente | Fila assíncrona com SQS |
| Sem throttle por email | O mesmo email poderia receber muitos links | Rate limit por email (além do IP) |
| Token armazenado no banco | Aumenta levemente a carga no banco | JWT assinado como token stateless (fase 2) |
| Sem email HTML | Corpo em texto simples | Template HTML com Thymeleaf (fase 2) |
| Sem link de descadastro | Emails de reset sem opt-out | Requisito legal LGPD — fase futura |

---

*Documento criado em: junho de 2026*
