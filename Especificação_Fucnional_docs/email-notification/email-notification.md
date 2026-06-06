# Notificações por Email — VidaLongaFlix

## Contexto e Motivação

O sistema utilizava a API do WhatsApp Business (Facebook Graph API) para enviar mensagens de boas-vindas aos usuários cadastrados. A integração nunca funcionou corretamente em produção por dificuldades com aprovação de templates no Meta.

A decisão foi substituir completamente o WhatsApp por **email transacional via Gmail SMTP**, que é:
- Gratuito (até ~500 emails/dia com conta pessoal)
- Confiável e sem dependência de aprovação externa
- Mais adequado para o contexto de boas-vindas e notificações de fila

---

## Arquitetura

```
Evento de domínio (cadastro, ativação, remoção)
        │
        ▼
  Service (WelcomeService / WaitlistNotificationService)
        │
        ▼
  EmailService (interface)
        ├── LoggingEmailService   @Profile("!prod")   → loga no console (dev/test)
        └── SmtpEmailService      @Profile("prod")    → envia email real via Gmail SMTP
        │
        ▼
  JavaMailSender (Spring Mail)
        │
        ▼
  smtp.gmail.com:587 (STARTTLS)
        │
        ▼
  Caixa de entrada do usuário
```

O `EmailService` é uma interface — quem chama não sabe se está logando ou enviando de verdade. O Spring escolhe a implementação pelo profile ativo (`local` vs `prod`).

---

## Gatilhos de Email

| ID    | Evento                          | Destinatário   | Serviço responsável            |
|-------|---------------------------------|----------------|--------------------------------|
| E-01  | Usuário cadastrado como ACTIVE  | Novo usuário   | `WelcomeService`               |
| E-02  | Usuário cadastrado como QUEUED  | Novo usuário   | `WaitlistNotificationService`  |
| E-03  | Usuário promovido da fila       | Usuário ativo  | `WaitlistNotificationService`  |
| E-04  | Usuário removido da fila        | Usuário        | `WaitlistNotificationService`  |

> **E-01 é o foco principal desta funcionalidade.** Os demais (E-02, E-03, E-04) já existem no `WaitlistNotificationService` usando `EmailService` — apenas a chamada ao WhatsApp em E-03 será removida.

---

## Comportamento esperado (todos os gatilhos)

- **Best-effort**: falha no envio de email **não** interrompe o fluxo principal (cadastro, ativação, remoção)
- **Non-blocking**: o email é enviado de forma síncrona, mas erros são capturados e logados
- **Sem retry**: se o SMTP falhar, o email não é reenviado automaticamente (fase futura: fila assíncrona com SQS)
- **Sem persistência**: o sistema não armazena histórico de emails enviados (fase futura)

---

## Remoção do WhatsApp

### Arquivos a deletar
- `src/main/java/.../services/WhatsAppService.java`
- `src/main/java/.../infra/config/HttpClientConfig.java`

### Arquivos a modificar
- `WelcomeService.java` — trocar `WhatsAppService` por `EmailService`
- `WaitlistNotificationService.java` — remover linha com `whatsAppService.send(...)` em `notifyActivated`
- `SecurityConfig.java` — remover `.requestMatchers("/whatsapp/webhook").permitAll()`
- `application.properties` — remover todas as props `whatsapp.*`, adicionar `spring.mail.*`
- `application-prod.properties` — idem

### Teste a reescrever
- `src/test/.../welcome/WelcomeService/WelcomeService.java` — mockar `EmailService` em vez de `WhatsAppService`

---

## Ordem de implementação (TDD)

```
1. Escrever testes de WelcomeService   (mock EmailService)
2. Reescrever WelcomeService           (verde nos testes)
3. Criar SmtpEmailService              (implementação real)
4. Deletar WhatsAppService             (limpar código morto)
5. Limpar SecurityConfig + properties  (remover referências WhatsApp)
6. Verificar WaitlistNotificationService (remover linha WhatsApp)
```

---

## Sub-documentos

- [Especificação Funcional](./email-notification-spec.md) — conteúdo de cada email, regras de negócio, requisitos técnicos
- [Cenários e Resultados Esperados](./email-notification-scenarios.md) — casos de teste com TDD, positivos e negativos

---

*Documento criado em: junho de 2026*
