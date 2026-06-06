# Cenários e Resultados Esperados — Notificações por Email

> Abordagem TDD: os testes são escritos antes da implementação.
> Positivo = comportamento correto esperado.
> Negativo = comportamento incorreto que o teste deve reprovar ou que o sistema deve tratar.

---

## Grupo 1 — WelcomeService (E-01: boas-vindas ao novo usuário ACTIVE)

### Contexto
`WelcomeService` recebe `name` e `email` do usuário recém-cadastrado como ACTIVE e delega para `EmailService`.

---

### WS-01 — Deve chamar EmailService ao enviar boas-vindas

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `name = "João Silva"`, `email = "joao@email.com"` |
| **Mock**     | `EmailService.send()` configurado com `doNothing()` |
| **Esperado** | `emailService.send()` chamado exatamente 1 vez |
| **Verifica** | `then(emailService).should().send(any(EmailMessage.class))` |

---

### WS-02 — Deve enviar para o email correto do usuário

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `name = "Maria"`, `email = "maria@teste.com"` |
| **Esperado** | `EmailMessage.to()` == `"maria@teste.com"` |
| **Verifica** | `ArgumentCaptor<EmailMessage>` captura o argumento e valida `to()` |

---

### WS-03 — Deve incluir o nome do usuário no assunto

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `name = "Carlos"`, `email = "carlos@email.com"` |
| **Esperado** | `EmailMessage.subject()` contém `"Carlos"` |
| **Verifica** | `assertTrue(captured.subject().contains("Carlos"))` |

---

### WS-04 — Deve incluir o nome do usuário no corpo do email

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `name = "Ana"`, `email = "ana@email.com"` |
| **Esperado** | `EmailMessage.body()` contém `"Ana"` |
| **Verifica** | `assertTrue(captured.body().contains("Ana"))` |

---

### WS-05 — Deve incluir o link da plataforma no corpo

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | qualquer nome e email válidos |
| **Esperado** | `EmailMessage.body()` contém `"vidalongaflix.com"` |
| **Verifica** | `assertTrue(captured.body().contains("vidalongaflix.com"))` |

---

### WS-06 — Deve incluir referência à nutricionista no corpo

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | qualquer nome e email válidos |
| **Esperado** | `EmailMessage.body()` contém `"Amanda"` |
| **Verifica** | `assertTrue(captured.body().contains("Amanda"))` |

---

### WS-07 — Não deve lançar exceção se EmailService falhar

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (resiliência) |
| **Entrada**  | `name = "João"`, `email = "joao@email.com"` |
| **Mock**     | `emailService.send()` configurado para lançar `RuntimeException` |
| **Esperado** | `sendWelcomeMessage()` **não** propaga a exceção |
| **Verifica** | `assertDoesNotThrow(() -> welcomeService.sendWelcomeMessage(...))` |
| **Razão**    | Falha no email não pode bloquear o cadastro do usuário |

---

### WS-08 — Não deve aceitar email nulo

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (validação) |
| **Entrada**  | `name = "João"`, `email = null` |
| **Esperado** | Exceção lançada antes de chamar `EmailService` |
| **Verifica** | `assertThrows(IllegalArgumentException.class, ...)` |

---

### WS-09 — Não deve aceitar nome em branco

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (validação) |
| **Entrada**  | `name = ""`, `email = "joao@email.com"` |
| **Esperado** | Exceção lançada antes de chamar `EmailService` |
| **Verifica** | `assertThrows(IllegalArgumentException.class, ...)` |

---

## Grupo 2 — SmtpEmailService (implementação real com JavaMailSender)

### Contexto
`SmtpEmailService` implementa `EmailService`. Usa `JavaMailSender` injetado para montar e enviar o email.

---

### SMTP-01 — Deve enviar para o destinatário correto

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo (com mock do JavaMailSender) |
| **Mock**     | `JavaMailSender` mockado, `MimeMessage` real criado pelo mock |
| **Entrada**  | `EmailMessage("dest@email.com", "Assunto", "Corpo")` |
| **Esperado** | `mailSender.send(mimeMessage)` chamado com `to = "dest@email.com"` |

---

### SMTP-02 — Deve usar o assunto correto

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `EmailMessage("dest@email.com", "Bem-vindo!", "Corpo")` |
| **Esperado** | `MimeMessage` configurado com `subject = "Bem-vindo!"` |

---

### SMTP-03 — Deve delegar a falha de SMTP para o chamador

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (propagação de erro) |
| **Mock**     | `mailSender.send()` lança `MailException` |
| **Esperado** | `SmtpEmailService.send()` propaga a exceção |
| **Razão**    | É responsabilidade de quem chama (`WelcomeService`) tratar o erro com best-effort |

---

### SMTP-04 — Deve chamar mailSender.send() exatamente uma vez por email

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | 1 `EmailMessage` |
| **Esperado** | `mailSender.send()` chamado exatamente 1 vez |
| **Verifica** | `verify(mailSender, times(1)).send(any(MimeMessage.class))` |

---

## Grupo 3 — WaitlistNotificationService (E-02, E-03, E-04)

### Contexto
`WaitlistNotificationService` usa `EmailService`. O WhatsApp foi removido de `notifyActivated`.

---

### WN-01 — notifyQueued deve enviar email com posição na fila

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | Usuário com `queuePosition = 3` |
| **Esperado** | `EmailMessage.body()` contém `"#3"` |

---

### WN-02 — notifyActivated deve enviar email e NÃO chamar WhatsApp

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo (verificação de remoção) |
| **Mocks**    | `EmailService` mockado |
| **Esperado** | `emailService.send()` chamado 1 vez, **sem** referência a WhatsApp |
| **Verifica** | Não existe dependência de `WhatsAppService` na classe |

---

### WN-03 — notifyRemoved deve enviar email ao usuário removido

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | Usuário com `name = "Pedro"`, `email = "pedro@email.com"` |
| **Esperado** | `EmailMessage.to()` == `"pedro@email.com"` |

---

### WN-04 — notifyQueued não deve lançar exceção se EmailService falhar

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (resiliência) |
| **Mock**     | `emailService.send()` lança `RuntimeException` |
| **Esperado** | `notifyQueuedBestEffort()` não propaga — erro apenas logado |

---

### WN-05 — notifyActivated não deve lançar exceção se EmailService falhar

| Campo        | Valor |
|---|---|
| **Tipo**     | Negativo (resiliência) |
| **Mock**     | `emailService.send()` lança `RuntimeException` |
| **Esperado** | `notifyActivatedBestEffort()` não propaga — erro apenas logado |

---

## Grupo 4 — LoggingEmailService (stub para dev/local)

---

### LOG-01 — Deve logar to, subject e body ao ser chamado

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo |
| **Entrada**  | `EmailMessage("a@b.com", "Assunto", "Corpo")` |
| **Esperado** | Logger registra mensagem contendo `"a@b.com"`, `"Assunto"` e `"Corpo"` |
| **Verifica** | Via `TestAppender` ou inspecionando logs com `ListAppender` do Logback |

---

### LOG-02 — Não deve lançar exceção em nenhuma circunstância

| Campo        | Valor |
|---|---|
| **Tipo**     | Positivo (estabilidade do stub) |
| **Entrada**  | qualquer `EmailMessage` válido |
| **Esperado** | `assertDoesNotThrow(...)` — sempre retorna sem erro |

---

## Resumo — Mapa de testes por classe

| Classe sob teste             | Testes | Grupos  |
|---|---|---|
| `WelcomeService`             | 9      | WS-01 a WS-09 |
| `SmtpEmailService`           | 4      | SMTP-01 a SMTP-04 |
| `WaitlistNotificationService`| 5      | WN-01 a WN-05 |
| `LoggingEmailService`        | 2      | LOG-01 a LOG-02 |
| **Total**                    | **20** | |

---

## Ordem de implementação TDD

```
Etapa 1 — Testes do WelcomeService (WS-01 a WS-09)
          → todos vermelhos (WelcomeService ainda usa WhatsApp)
          → reescrever WelcomeService para verde

Etapa 2 — Testes do SmtpEmailService (SMTP-01 a SMTP-04)
          → todos vermelhos (classe não existe ainda)
          → criar SmtpEmailService para verde

Etapa 3 — Testes do WaitlistNotificationService (WN-01 a WN-05)
          → WN-02 falha (WhatsApp ainda presente)
          → remover linha WhatsApp para verde

Etapa 4 — Testes do LoggingEmailService (LOG-01 a LOG-02)
          → verificar que o stub existente já passa

Etapa 5 — Deletar WhatsAppService e HttpClientConfig
          → confirmar que nenhum teste quebrou
```

---

*Documento criado em: junho de 2026*
