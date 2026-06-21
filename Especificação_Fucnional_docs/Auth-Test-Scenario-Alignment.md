# Auth Test Scenario Alignment

Este repositório implementa apenas o backend Spring Boot da autenticação.
O documento de 290 cenários mistura:

- backend real deste projeto
- frontend Angular que não está neste repositório
- uma feature de waitlist que ainda não existe no sistema

## Coberto pelo backend atual

Os cenários abaixo existem no sistema e possuem cobertura automatizada relevante:

- `B6` login com sucesso (inclui verificação de `Set-Cookie: token` com `HttpOnly`, `Secure`, `Max-Age`)
- `B8` erros de login (credenciais inválidas, usuário QUEUED, usuário DISABLED)
- `B12` renderização não se aplica, mas os contratos de registro existem no backend
- `B13` validações de `RegisterRequestDTO`
- `B18` registro com sucesso (inclui cookie para usuário ACTIVE; sem cookie para QUEUED)
- `B20` erros de registro
- `B34` proteção de rotas autenticadas no backend
- `B35` proteção de rotas admin no backend
- `B36` autenticação por cookie httpOnly (SecurityFilter aceita token no cookie `token` em vez de header Bearer)
- `B37` autenticação por Bearer header (SecurityFilter aceita ambas as formas)
- `B38` logout via `POST /auth/logout` expira o cookie no servidor (`Max-Age=0`)
- `B39` `keepLoggedIn=true` → cookie persistente com `Max-Age`; `keepLoggedIn=false` → cookie de sessão sem `Max-Age`
- `B50` conversão do telefone para envio WhatsApp

Arquivos principais de teste:

- `src/test/java/com/dvFabricio/VidaLongaFlix/userTest/controller/AuthControllerTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/infra/security/SecurityFilterTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/integration/auth/AuthIntegrationTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/integration/security/SecurityAccessIntegrationTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/welcome/whatsappTest/whatsappTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/userTest/service/UserServiceTest.java`
- `src/test/java/com/dvFabricio/VidaLongaFlix/welcome/WelcomeService/WelcomeService.java`

## Não aplicável neste repositório

Estas seções dependem de frontend Angular e não podem ser cobertas aqui:

- `B1` a `B5`
- `B7`
- `B9` a `B17`
- `B21`
- `B39` a `B42`

Motivo:

- não há `*.component.ts`
- não há `*.service.ts` de frontend
- não há guards/interceptor Angular
- não há utilitários de máscara no frontend neste repositório

## Não implementado no sistema atual

Estas seções exigiriam código novo no backend antes de qualquer teste útil:

- `B22` a `B33`
- `B43` a `B49`

Motivo:

- não existem endpoints de password recovery/password change
- não existe modelo de waitlist
- não existem endpoints admin de fila
- não existe `registration-status`
- não existe promoção automática de fila

## Ajustes feitos nesta rodada

Para alinhar os testes ao comportamento real do backend atual, foram adicionados cenários para:

- erro detalhado de validação no `POST /auth/login`
- erro detalhado de validação no `POST /auth/register`
- JSON malformado em login e registro
- falha de registro quando `ROLE_USER` não existe
- comportamento real de `/auth/me` com token inválido

**Rodada 2 (21/06/2026) — migração para cookie httpOnly:**

- cookie `token` presente na resposta de login com `HttpOnly`, `Secure`, `SameSite=None`
- `keepLoggedIn=false` gera cookie de sessão (`Max-Age=-1`); `keepLoggedIn=true` (padrão) gera cookie persistente
- registro de usuário ACTIVE inclui cookie; usuário QUEUED não recebe cookie
- `POST /auth/logout` expira o cookie com `Max-Age=0`
- `SecurityFilter` autentica via cookie (sem header Authorization) — `shouldAuthenticateUserWhenCookieTokenIsValid`
- `SecurityFilter` ainda aceita Bearer header para compatibilidade

## Próximo passo recomendado

Se você quiser alinhar o documento inteiro ao sistema de verdade, há dois caminhos:

1. reduzir o documento para o escopo real do backend atual
2. implementar as features faltantes, principalmente:
   - password recovery / password change
   - waitlist / limite de usuários
   - frontend Angular correspondente
