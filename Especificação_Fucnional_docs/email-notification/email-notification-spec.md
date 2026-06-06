começa# Especificação Funcional — Notificações por Email

## E-01 — Boas-vindas (usuário cadastrado como ACTIVE)

### Quando dispara
Imediatamente após `userRepository.save(newUser)` em `RegistrationLimitService.register()`, quando `activeUsers < maxActiveUsers` — ou seja, o usuário entrou direto, sem fila.

### Quem envia
`WelcomeService.sendWelcomeMessage(String name, String email)`

### Dados disponíveis
| Campo   | Origem              |
|---------|---------------------|
| `name`  | `user.getName()`    |
| `email` | `user.getEmail()`   |

> **Nota:** O campo `phone` não é mais usado para notificação. Apenas `email`.

### Assunto
```
Bem-vindo(a) ao VidaLongaFlix, [Nome]!
```

### Corpo do email (texto simples)
```
Olá, [Nome]!

Seja bem-vindo(a) ao VidaLongaFlix — a plataforma de saúde, nutrição e bem-estar
criada pela nutricionista Amanda Fidelis Muraro.

Seu acesso está ativo. Acesse agora e explore os conteúdos exclusivos:
https://vidalongaflix.com

---
Quer uma consulta personalizada com a Dra. Amanda?

WhatsApp: (51) 97810-0460
E-mail:   Amandafidelismuraro@gmail.com

---
Qualquer dúvida, responda este email.

Equipe VidaLongaFlix
```

### Remetente
```
from: ${MAIL_USERNAME}   (configurado como variável de ambiente)
```

### Comportamento em caso de falha
- Exceção capturada em `sendWelcomeBestEffort()` no `RegistrationLimitService`
- Usuário **não** é afetado — cadastro foi concluído
- Erro logado via `logger.error(...)` (visível no Grafana/Loki em produção)

---

## E-02 — Fila de espera (usuário cadastrado como QUEUED)

### Quando dispara
Imediatamente após `userRepository.save(newUser)` em `RegistrationLimitService.register()`, quando `activeUsers >= maxActiveUsers`.

### Quem envia
`WaitlistNotificationService.notifyQueued(User user)`

### Assunto
```
VidaLongaFlix - Fila de espera
```

### Corpo do email
```
Olá, [Nome]. Seu cadastro foi adicionado à fila de espera do VidaLongaFlix.
Sua posição atual é #[posição].
Assim que novas vagas forem liberadas, você será avisado(a).
```

### Status atual
Já implementado e funcional. Nenhuma alteração necessária neste email.

---

## E-03 — Conta ativada (usuário promovido da fila)

### Quando dispara
- Quando um usuário é excluído (`deleteUser`) e o sistema promove automaticamente o próximo da fila
- Quando admin manualmente ativa um usuário em fila (`activateQueuedUser`)
- Quando admin aumenta o limite de usuários ativos (`updateMaxActiveUsers`)

### Quem envia
`WaitlistNotificationService.notifyActivated(User user)`

### Assunto
```
VidaLongaFlix - Conta ativada
```

### Corpo do email
```
Olá, [Nome]. Sua conta no VidaLongaFlix foi ativada.
Acesse https://vidalongaflix.com para fazer login.
```

### Alteração necessária
Remover a chamada ao WhatsApp na linha 40 do `WaitlistNotificationService`:
```java
// REMOVER esta linha:
whatsAppService.send(new Message(user.getPhone(), "account_activated_template"));
```

O email já está implementado e funciona. Apenas a chamada ao WhatsApp precisa ser removida.

---

## E-04 — Saída da fila de espera

### Quando dispara
- Quando o usuário cancela sua própria posição via `DELETE /auth/waitlist/me`
- Quando admin remove um usuário da fila via painel admin

### Quem envia
`WaitlistNotificationService.notifyRemoved(User user)`

### Assunto
```
VidaLongaFlix - Saída da fila de espera
```

### Corpo do email
```
Olá, [Nome]. Seu cadastro foi removido da fila de espera do VidaLongaFlix.
Se quiser, você pode realizar um novo cadastro quando houver interesse.
```

### Status atual
Já implementado e funcional. Nenhuma alteração necessária.

---

## Requisitos Técnicos

### Configuração SMTP — Local (dev)
```properties
# Nenhuma configuração necessária — LoggingEmailService apenas loga
# O email aparece nos logs do console durante desenvolvimento
```

### Configuração SMTP — Produção
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Variáveis de ambiente necessárias no Elastic Beanstalk
| Variável        | Descrição                                               |
|-----------------|---------------------------------------------------------|
| `MAIL_USERNAME` | Endereço Gmail usado como remetente (ex: noreply@gmail.com) |
| `MAIL_PASSWORD` | App Password gerada no Google (16 caracteres, sem espaços)  |

### Como gerar o App Password do Gmail
```
1. Acessar myaccount.google.com
2. Segurança → Verificação em duas etapas (deve estar ativa)
3. Segurança → Senhas de app
4. Criar: nome "VidaLongaFlix"
5. Google gera: xxxx xxxx xxxx xxxx
6. Remover os espaços antes de salvar no EB: xxxxxxxxxxxxxxxx
```

### Dependência Maven (a adicionar no pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### Seleção de implementação por profile
| Profile ativo | Implementação usada    | Comportamento               |
|---------------|------------------------|-----------------------------|
| `local`       | `LoggingEmailService`  | Loga no console, não envia  |
| `prod`        | `SmtpEmailService`     | Envia email real via Gmail  |

---

## Limitações conhecidas (fora do escopo desta implementação)

| Limitação | Descrição | Solução futura |
|---|---|---|
| Sem retry | Falha de SMTP não é retentada | Fila assíncrona com SQS |
| Sem histórico | Emails enviados não são persistidos | Tabela `email_log` + auditoria |
| Sem template HTML | Corpo em texto simples | Template HTML com Thymeleaf |
| Limite Gmail | ~500/dia conta pessoal, ~2000/dia Google Workspace | Migrar para SendGrid ou SES |
| Sem unsubscribe | Sem link de descadastro | Requisito legal (LGPD) — fase futura |

---

*Documento criado em: junho de 2026*
