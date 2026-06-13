# Redefinição de Senha — VidaLongaFlix

## Contexto e Motivação

O sistema possuía apenas um mecanismo de atualização de senha via endpoint administrativo
(`PUT /users/{id}`), que exige que um `ADMIN` realize a operação. Não havia fluxo
self-service para o próprio usuário redefinir a senha em caso de esquecimento.

A decisão foi implementar um fluxo completo de **redefinição de senha via email**, que:
- Permite ao usuário recuperar o acesso de forma autônoma, sem depender do administrador
- É seguro: token temporário de uso único com expiração de 30 minutos
- Reutiliza a infraestrutura de email SMTP já existente (`SmtpEmailService`)
- Não requer autenticação — o próprio token é a prova de identidade temporária

---

## Arquitetura

```
Usuário clica "Esqueci minha senha"
        │
        ▼
PasswordRecoveryComponent (Angular)
        │  POST /auth/password-recovery
        ▼
PasswordResetController
        │
        ▼
PasswordResetService.requestReset(email)
        ├── UserRepository.findByEmail()
        ├── PasswordResetTokenRepository.deleteByUserId()   ← limpa token anterior
        ├── PasswordResetTokenRepository.save()             ← salva novo token
        └── PasswordResetEmailService.send()
                │
                ▼
          EmailService (interface)
                ├── LoggingEmailService   @Profile("!prod")  → loga no console
                └── SmtpEmailService      @Profile("prod")   → envia email real
                        │
                        ▼
                  smtp.gmail.com:587 (STARTTLS)
                        │
                        ▼
                  Caixa de entrada do usuário
                        │
                        ▼
        Link: https://vidalongaflix.com/password-change?token=xxx
                        │
                        ▼
        PasswordChangeComponent (Angular)
                │  GET /auth/validate-token?token=xxx
                ▼
        PasswordResetService.validateToken(token)
                │
                ▼ (token válido — exibe formulário)
        Usuário digita nova senha
                │  POST /auth/reset-password
                ▼
        PasswordResetService.resetPassword(token, novaSenha)
                ├── BCryptPasswordEncoder.encode()
                ├── UserRepository.save()
                ├── PasswordResetToken.used = true → save()
                └── PasswordResetEmailService.sendChangeConfirmation()
```

---

## Gatilhos de Email

| ID | Evento | Destinatário | Serviço responsável |
|---|---|---|---|
| PR-01 | Usuário solicita reset de senha | Usuário solicitante | `PasswordResetEmailService.send()` |
| PR-04 | Senha redefinida com sucesso | Usuário que realizou o reset | `PasswordResetEmailService.sendChangeConfirmation()` |

---

## Comportamento esperado

- **Anti-enumeração**: a resposta do `POST /auth/password-recovery` é sempre 200 OK com a mesma mensagem genérica, independente de o email existir ou não no banco
- **Token de uso único**: após ser consumido em `POST /auth/reset-password`, o token é marcado como `used = true` e não pode ser reutilizado
- **Expiração**: token válido por 30 minutos — após isso retorna 410 Gone
- **Um token por usuário**: ao solicitar um novo reset, o token anterior é deletado automaticamente
- **Best-effort no email**: falha no envio de email não interrompe o fluxo (cadastro do token já foi feito; usuário pode solicitar novamente)
- **Confirmação best-effort**: falha no email de confirmação pós-reset não desfaz a alteração de senha

---

## Estado atual do frontend

O frontend Angular já possui toda a estrutura necessária, porém com chamadas mockadas:

| Componente / Serviço | Estado |
|---|---|
| `PasswordRecoveryComponent` — modal "Esqueci minha senha" | Pronto — chama `POST /auth/password-recovery` |
| `PasswordChangeComponent` — página `/password-change?token=` | Pronto — lê token da URL |
| Rota `/password-change` e `/redefinir-senha` | Pronta no router |
| Tipos `ResetPassword { token, newPassword }` | Definidos em `user.types.ts` |
| `service-password-recovery.validateToken()` | **Mock** — retorna `true` após 500ms |
| `service-password-recovery.changePassword()` | **Mock** — retorna após 1s sem chamar API |

A única alteração necessária no frontend é substituir os dois mocks por chamadas reais.

---

## O que precisa ser criado no backend

| Artefato | Tipo |
|---|---|
| `VXX__create_password_reset_tokens.sql` | Migration Flyway |
| `PasswordResetToken.java` | Entidade JPA |
| `PasswordResetTokenRepository.java` | Repository |
| `PasswordResetService.java` | Service (lógica de negócio) |
| `PasswordResetEmailService.java` | Service (composição dos emails) |
| `PasswordResetController.java` | Controller (endpoints HTTP) |
| `PasswordResetRequestDTO.java` | DTO `{ email }` |
| `ResetPasswordRequestDTO.java` | DTO `{ token, newPassword }` |

---

## Novos endpoints

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| `POST` | `/auth/password-recovery` | Não | Solicitar reset — gera token e envia email |
| `GET` | `/auth/validate-token` | Não | Verificar se token ainda é válido |
| `POST` | `/auth/reset-password` | Não | Aplicar nova senha com o token |

Todos cobertos pela regra existente `.requestMatchers("/auth/**").permitAll()` no `SecurityConfig`.

---

## Ordem de implementação (TDD)

```
1. Criar migration + entidade + repository    → testes PRTR verdes
2. Criar PasswordResetService                 → testes PRS verdes
3. Criar PasswordResetEmailService            → testes PRE verdes
4. Criar PasswordResetController              → testes PRC verdes
5. Substituir mocks no frontend               → validação manual em dev
6. Deploy em prod-h2 e testar com email real  → fluxo completo validado
```

---

## Sub-documentos

- [Especificação Funcional](./password-reset-spec.md) — conteúdo de cada email, regras de negócio, modelo de dados, endpoints e alterações necessárias
- [Cenários e Resultados Esperados](./password-reset-scenarios.md) — 33 casos de teste TDD, positivos e negativos, por classe

---

*Documento criado em: junho de 2026*
