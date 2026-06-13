# Cenários e Resultados Esperados — Redefinição de Senha

> Abordagem TDD: os testes são escritos antes da implementação.
> Positivo = comportamento correto esperado.
> Negativo = comportamento incorreto que o sistema deve rejeitar ou tratar com segurança.

---

## Grupo 1 — PasswordResetService (lógica de negócio central)

### Contexto
`PasswordResetService` orquestra os três fluxos: solicitar reset, validar token e aplicar nova senha.
Depende de `UserRepository`, `PasswordResetTokenRepository`, `PasswordResetEmailService` e `PasswordEncoder`.

---

### PRS-01 — Deve gerar e salvar token ao receber email cadastrado

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `email = "joao@email.com"` (usuário ACTIVE existe no banco) |
| **Mock** | `userRepository.findByEmail()` retorna usuário válido; `tokenRepository.save()` com `doNothing()` |
| **Esperado** | `tokenRepository.save()` chamado 1 vez com token de 64 chars e `expiresAt` no futuro |
| **Verifica** | `ArgumentCaptor<PasswordResetToken>` — `token.length() == 64` e `expiresAt.isAfter(now())` |

---

### PRS-02 — Deve deletar token anterior ao gerar novo para o mesmo usuário

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `email = "joao@email.com"` (usuário já tem token ativo) |
| **Esperado** | `tokenRepository.deleteByUserId(userId)` chamado antes de `tokenRepository.save()` |
| **Verifica** | `InOrder` do Mockito — `deleteByUserId` precede `save` |

---

### PRS-03 — Deve retornar sem erro quando email não está cadastrado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (segurança — anti-enumeração) |
| **Entrada** | `email = "naoexiste@email.com"` |
| **Mock** | `userRepository.findByEmail()` retorna `Optional.empty()` |
| **Esperado** | Método retorna normalmente sem lançar exceção; `tokenRepository.save()` **não** é chamado; `emailService.send()` **não** é chamado |
| **Razão** | Não revelar se o email existe — resposta idêntica à do email cadastrado |

---

### PRS-04 — Deve retornar sem erro quando usuário está DISABLED

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (segurança) |
| **Entrada** | `email = "desativado@email.com"` (usuário com status DISABLED) |
| **Esperado** | Retorno normal sem enviar email nem salvar token |
| **Verifica** | `verify(tokenRepository, never()).save(any())` |

---

### PRS-05 — Deve retornar sem erro quando usuário está QUEUED

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (segurança) |
| **Entrada** | `email = "fila@email.com"` (usuário com status QUEUED) |
| **Esperado** | Retorno normal sem enviar email nem salvar token |
| **Verifica** | `verify(emailService, never()).send(any())` |

---

### PRS-06 — validateToken deve retornar true para token válido

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `token = "abc123..."` (existente, não expirado, `used = false`) |
| **Mock** | `tokenRepository.findByToken()` retorna `Optional` com token válido |
| **Esperado** | Método retorna `true` sem lançar exceção |

---

### PRS-07 — validateToken deve lançar exceção para token não encontrado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | `token = "tokeninexistente"` |
| **Mock** | `tokenRepository.findByToken()` retorna `Optional.empty()` |
| **Esperado** | Lança `ResourceNotFoundException` ou equivalente |
| **Verifica** | `assertThrows(ResourceNotFoundException.class, ...)` |

---

### PRS-08 — validateToken deve lançar exceção para token expirado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | Token com `expiresAt = now() - 1 minuto` |
| **Esperado** | Lança exceção indicando token expirado |
| **Verifica** | `assertThrows(TokenExpiredException.class, ...)` ou exceção customizada equivalente |

---

### PRS-09 — validateToken deve lançar exceção para token já utilizado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | Token com `used = true` |
| **Esperado** | Lança exceção indicando token já usado |
| **Verifica** | `assertThrows(...)` — mesmo tipo do PRS-08 |

---

### PRS-10 — resetPassword deve salvar nova senha codificada com BCrypt

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `token` válido + `newPassword = "NovaSenha@123"` |
| **Esperado** | `user.getPassword()` após save começa com `$2a$` (hash BCrypt) e não é igual a `"NovaSenha@123"` |
| **Verifica** | `ArgumentCaptor<User>` — `assertTrue(captured.getPassword().startsWith("$2a$"))` |

---

### PRS-11 — resetPassword deve marcar token como usado após reset

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | Token válido |
| **Esperado** | `tokenRepository.save()` chamado com `token.used == true` |
| **Verifica** | `ArgumentCaptor<PasswordResetToken>` — `assertTrue(captured.isUsed())` |

---

### PRS-12 — resetPassword deve enviar email de confirmação após reset

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | Token válido + nova senha |
| **Esperado** | `emailService.sendChangeConfirmation()` chamado 1 vez |
| **Verifica** | `verify(emailService, times(1)).sendChangeConfirmation(anyString(), anyString())` |

---

### PRS-13 — resetPassword não deve desfazer a senha se o email de confirmação falhar

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (resiliência) |
| **Mock** | `emailService.sendChangeConfirmation()` lança `RuntimeException` |
| **Esperado** | `userRepository.save()` já foi chamado; exceção do email é capturada e logada; a senha do usuário permanece alterada |
| **Razão** | Email de confirmação é best-effort — não pode reverter uma operação já concluída |

---

## Grupo 2 — PasswordResetController (camada HTTP)

### Contexto
`PasswordResetController` recebe as requisições HTTP, delega para `PasswordResetService`
e devolve os status codes corretos.

---

### PRC-01 — POST /auth/password-recovery deve retornar 200 para email cadastrado

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `{ "email": "joao@email.com" }` |
| **Mock** | `passwordResetService.requestReset()` com `doNothing()` |
| **Esperado** | HTTP 200 com body `{ "message": "Se o seu email estiver cadastrado..." }` |

---

### PRC-02 — POST /auth/password-recovery deve retornar 200 mesmo para email não cadastrado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (segurança — anti-enumeração) |
| **Entrada** | `{ "email": "naoexiste@email.com" }` |
| **Esperado** | HTTP 200 com **a mesma** mensagem genérica do PRC-01 |
| **Razão** | Resposta idêntica impede que atacante descubra quais emails estão cadastrados |

---

### PRC-03 — POST /auth/password-recovery deve retornar 400 para email inválido

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (validação) |
| **Entrada** | `{ "email": "nao-e-um-email" }` |
| **Esperado** | HTTP 400 Bad Request |
| **Verifica** | Bean Validation `@Email` no DTO rejeita antes de chegar no service |

---

### PRC-04 — GET /auth/validate-token deve retornar 200 para token válido

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `?token=tokenvalido` |
| **Mock** | `passwordResetService.validateToken()` retorna `true` |
| **Esperado** | HTTP 200 OK |

---

### PRC-05 — GET /auth/validate-token deve retornar 410 para token expirado ou usado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | `?token=tokenexpirado` |
| **Mock** | `passwordResetService.validateToken()` lança exceção de token inválido |
| **Esperado** | HTTP 410 Gone |

---

### PRC-06 — GET /auth/validate-token deve retornar 404 para token não encontrado

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | `?token=tokeninexistente` |
| **Mock** | `passwordResetService.validateToken()` lança `ResourceNotFoundException` |
| **Esperado** | HTTP 404 Not Found |

---

### PRC-07 — POST /auth/reset-password deve retornar 204 em caso de sucesso

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `{ "token": "tokenvalido", "newPassword": "NovaSenha@123" }` |
| **Mock** | `passwordResetService.resetPassword()` com `doNothing()` |
| **Esperado** | HTTP 204 No Content |

---

### PRC-08 — POST /auth/reset-password deve retornar 410 para token inválido

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | `{ "token": "tokenexpirado", "newPassword": "NovaSenha@123" }` |
| **Mock** | `passwordResetService.resetPassword()` lança exceção de token inválido |
| **Esperado** | HTTP 410 Gone com `{ "error": "Token expirado ou já utilizado." }` |

---

### PRC-09 — POST /auth/reset-password deve retornar 400 para senha fraca

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (validação) |
| **Entrada** | `{ "token": "tokenvalido", "newPassword": "123" }` |
| **Esperado** | HTTP 400 Bad Request — `@Size(min = 8)` no DTO rejeita antes do service |

---

## Grupo 3 — PasswordResetEmailService (composição e envio)

### Contexto
`PasswordResetEmailService` monta o email de reset e o de confirmação, delegando
o envio para `EmailService` (implementado por `SmtpEmailService` em prod e `LoggingEmailService` em dev).

---

### PRE-01 — Deve enviar email de reset para o destinatário correto

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `name = "João"`, `email = "joao@email.com"`, `token = "abc123"` |
| **Esperado** | `EmailMessage.to()` == `"joao@email.com"` |
| **Verifica** | `ArgumentCaptor<EmailMessage>` |

---

### PRE-02 — Deve incluir o link com o token no corpo do email de reset

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `token = "abc123"` |
| **Esperado** | `EmailMessage.body()` contém `"password-change?token=abc123"` |
| **Verifica** | `assertTrue(captured.body().contains("password-change?token=abc123"))` |

---

### PRE-03 — Deve incluir o nome do usuário no corpo do email de reset

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `name = "Maria"` |
| **Esperado** | `EmailMessage.body()` contém `"Maria"` |

---

### PRE-04 — Não deve lançar exceção se EmailService falhar ao enviar reset

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (resiliência) |
| **Mock** | `emailService.send()` lança `RuntimeException` |
| **Esperado** | `send()` do `PasswordResetEmailService` **não** propaga a exceção |
| **Razão** | Falha no email não deve impedir que o token seja salvo e a requisição conclua |

---

### PRE-05 — Deve enviar email de confirmação para o destinatário correto

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | `name = "Carlos"`, `email = "carlos@email.com"` |
| **Esperado** | `EmailMessage.to()` == `"carlos@email.com"` |

---

### PRE-06 — Email de confirmação não deve conter o token nem a nova senha

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (segurança) |
| **Esperado** | `EmailMessage.body()` **não** contém a string do token nem qualquer senha |
| **Verifica** | `assertFalse(captured.body().contains(token))` |

---

### PRE-07 — Não deve lançar exceção se EmailService falhar ao enviar confirmação

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (resiliência) |
| **Mock** | `emailService.send()` lança `RuntimeException` |
| **Esperado** | `sendChangeConfirmation()` não propaga — erro apenas logado |

---

## Grupo 4 — PasswordResetTokenRepository (acesso ao banco)

### Contexto
Testes de integração com banco H2 em memória via `@DataJpaTest`.

---

### PRTR-01 — findByToken deve retornar o token correto

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | Token salvo previamente com valor `"meutoken"` |
| **Esperado** | `Optional` presente com `token.getToken() == "meutoken"` |

---

### PRTR-02 — findByToken deve retornar Optional vazio para token inexistente

| Campo | Valor |
|---|---|
| **Tipo** | Negativo |
| **Entrada** | `"tokenqueñaoexiste"` |
| **Esperado** | `Optional.empty()` |

---

### PRTR-03 — deleteByUserId deve remover todos os tokens do usuário

| Campo | Valor |
|---|---|
| **Tipo** | Positivo |
| **Entrada** | Usuário com 2 tokens salvos |
| **Esperado** | Após `deleteByUserId(userId)`, `findByToken()` de ambos retorna `Optional.empty()` |

---

### PRTR-04 — Token deve ter constraint UNIQUE no campo token

| Campo | Valor |
|---|---|
| **Tipo** | Negativo (integridade) |
| **Entrada** | Dois tokens com o mesmo valor de `token` para usuários diferentes |
| **Esperado** | Segunda inserção lança `DataIntegrityViolationException` |

---

## Resumo — Mapa de testes por classe

| Classe sob teste | Testes | Grupos |
|---|---|---|
| `PasswordResetService` | 13 | PRS-01 a PRS-13 |
| `PasswordResetController` | 9 | PRC-01 a PRC-09 |
| `PasswordResetEmailService` | 7 | PRE-01 a PRE-07 |
| `PasswordResetTokenRepository` | 4 | PRTR-01 a PRTR-04 |
| **Total** | **33** | |

---

## Ordem de implementação TDD

```
Etapa 1 — Testes do PasswordResetTokenRepository (PRTR-01 a PRTR-04)
          → todos vermelhos (entidade e repositório não existem)
          → criar PasswordResetToken + migration Flyway + repository para verde

Etapa 2 — Testes do PasswordResetService (PRS-01 a PRS-13)
          → todos vermelhos (service não existe)
          → criar PasswordResetService para verde

Etapa 3 — Testes do PasswordResetEmailService (PRE-01 a PRE-07)
          → todos vermelhos (classe não existe)
          → criar PasswordResetEmailService para verde

Etapa 4 — Testes do PasswordResetController (PRC-01 a PRC-09)
          → todos vermelhos (endpoints não existem)
          → criar PasswordResetController para verde

Etapa 5 — Substituir mocks no frontend (service-password-recovery.ts)
          → validar manualmente o fluxo completo em dev (perfil local)

Etapa 6 — Deploy em prod-h2 e testar com email real
          → confirmar recebimento do email e redefinição de senha
```

---

*Documento criado em: junho de 2026*
