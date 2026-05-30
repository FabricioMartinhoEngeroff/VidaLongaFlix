# Gerenciamento de Segredos — Segurança, Auditoria e Automação no VidaLongaFlix

> Baseado no curso "Gerenciamento de Segredos: segurança, auditoria e automação com Vault".
> Este documento descreve: fundamentos do gerenciamento de segredos, ciclo de vida completo, ferramentas (Vault, AWS Secrets Manager, Gitleaks), vulnerabilidades reais no projeto, cenários de teste com resultado esperado para backend e frontend, e passo a passo completo de implementação por sprint.

---

## Parte 1 — Fundamentos de Gerenciamento de Segredos

---

### Tipos Comuns de Segredos em Aplicações

Segredos são qualquer informação que, se exposta, permite acesso não autorizado a sistemas ou dados. Os tipos mais comuns em aplicações modernas são:

- **Senhas de banco de dados** — credenciais de acesso ao RDS, PostgreSQL, MySQL
- **Tokens JWT e chaves de API** — segredos de assinatura JWT, chaves do Google Maps, AWS, Grafana Cloud
- **Credenciais de acesso a serviços externos** — chaves do S3, SMTP, WhatsApp API, Stripe
- **Certificados e chaves SSH** — chaves privadas de criptografia e autenticação
- **Variáveis de configuração sensíveis** — Secret Key, Salt, passphrase de HSM
- **Arquivos `.env`** — formato comum em aplicações com múltiplos ambientes

No VidaLongaFlix, os segredos ativos em produção são: `JWT_SECRET`, `DB_URL` (contém senha do RDS), `OTLP_AUTH_HEADER` (token Grafana Cloud), `ADMIN_EMAIL`, credenciais AWS S3 e chave da API do WhatsApp.

---

### Como Segredos Aparecem na Aplicação

Os segredos chegam até o runtime da aplicação por diferentes caminhos, cada um com perfil de risco distinto:

| Canal | Risco | Uso no VidaLongaFlix |
|---|---|---|
| **Hardcoded no código** | Crítico — exposto no repositório e no histórico Git | Não deve existir — mas exige varredura para confirmar |
| **Arquivo `.env` commitado** | Crítico — mesmo removido, permanece no histórico Git | `.gitignore` deve bloquear — confirmar na sessão de sprint |
| **Variáveis de ambiente** | Moderado — sem audit trail, sem rotação automática | Situação atual no Elastic Beanstalk |
| **Secret Manager (Vault / AWS SM)** | Baixo — criptografado, auditável, com rotação | Meta da Fase 1 de segurança |
| **Containers e imagens Docker** | Alto — segredos em `ENV` do Dockerfile vão para a imagem | `Dockerfile` do projeto precisa ser revisado |
| **Arquivos de IaC** | Alto — Terraform/Cloudformation com secrets em texto | `observability/` e `terraform/` precisam de varredura |

**Risco invisível dos logs**: logs de erro enviados para suporte técnico ou para ferramentas externas (Power BI, Grafana, ferramentas de APM) podem conter tokens JWT, connection strings ou stack traces com dados de configuração. Quem compartilha um log pode não perceber que ele contém credenciais.

---

### Exemplos Reais de Comprometimento (Casos do Curso)

**Uber (2016)**: Desenvolvedor deixou credenciais AWS em repositório privado do GitHub. Resultado: 57 milhões de dados de usuários e motoristas expostos. Uber multada em US$ 148 milhões e tentou negociar com os hackers sem sucesso.

**Toyota (2022)**: Chaves de acesso no código-fonte do GitHub por cinco anos sem rotação. Resultado: grande perda de confiança e prejuízo de marketing — a Toyota não sabia que as chaves estavam expostas por tanto tempo exatamente porque não havia auditoria.

**Microsoft/Azure (2023)**: Chave privilegiada do Azure exposta acidentalmente em repositório público. Permitia acesso às caixas de e-mail do Outlook de diversas organizações. Descoberta por pesquisadores externos — a própria Microsoft não detectou antes deles.

**Padrão comum nos três casos**: ausência de rotação, ausência de auditoria e ausência de detecção automática no código-fonte.

---

### Riscos do Gerenciamento Inadequado

| Risco | Impacto |
|---|---|
| Hardcoded secrets em repositório público | Bots escaneiam o GitHub continuamente — credencial é coletada em minutos |
| Senha sem rotação | Vazamento antigo permanece válido indefinidamente |
| Sem audit log | Impossível saber quem usou a credencial e quando |
| Token em log de erro | Log exportado para terceiros expõe credenciais sem intenção |
| `.env` no Docker Hub | Imagem pública com variáveis sensíveis acessível por qualquer pessoa |

**Consequências legais e financeiras**: multas por violação de LGPD/GDPR, danos à reputação, custos de resposta a incidentes, ações judiciais de clientes prejudicados, compra de dados na dark web por concorrentes.

---

### Ciclo de Vida Completo dos Segredos

O curso define 5 fases no ciclo de vida de qualquer segredo:

#### Fase 1 — Criação
- Nunca criar senhas fracas manualmente — usar geradores com alta entropia
- **Entropia**: medida de aleatoriedade de uma string. `Token2024` tem baixa entropia (padrão previsível). Uma chave gerada com caracteres aleatórios tem alta entropia e resiste a brute force
- Ferramentas recomendadas: geradores de senha, KMS (Key Management Service), HSM (Hardware Security Module)
- Documentar: quem pode criar segredos e quais ferramentas são aprovadas
- Usar prefixos customizados (ex: `VLF_JWT_`) para facilitar detecção por scanners como Gitleaks e TruffleHog

#### Fase 2 — Armazenamento
- **Nunca versionado em repositórios** — Git representa risco permanente (o histórico não apaga)
- Locais proibidos: repositórios Git, planilhas, dentro do código-fonte, e-mail
- Locais aprovados: Vault, AWS Secrets Manager, Azure Key Vault, GCP Secret Manager
- Criptografia em repouso é obrigatória
- Criptografia em trânsito (TLS/HTTPS) é obrigatória
- Estabelecer regras de acesso por papel (RBAC)

#### Fase 3 — Uso
- Acesso mínimo necessário (Princípio do Menor Privilégio)
- Evitar exposição em logs, mensagens de erro ou capturas de tela
- Preferir segredos efêmeros de curta duração (TTL definido)
- Documentar como os segredos são injetados nas aplicações
- Nunca passar segredos como argumentos de linha de comando (ficam visíveis no `ps aux`)

#### Fase 4 — Rotação
- Substituição periódica ou imediata em caso de suspeita de vazamento
- Deve ser automatizada sempre que possível
- Referência prática: site **HowToRotate** — playbooks passo a passo para AWS, Azure, GitLab
- Criar documentação interna com capturas de tela para cada tipo de segredo do projeto

#### Fase 5 — Descarte e Revogação
- Quando o segredo não é mais necessário: revogar imediatamente
- Ex: colaborador que saiu da equipe — credenciais devem ser revogadas no mesmo dia
- Criar checklist de desligamento que inclua revogação de acessos
- Documentar o procedimento de revogação emergencial (para incidentes)

---

### O Problema do Hardcoding

**O que é**: inserir senhas, tokens ou chaves diretamente no código-fonte.

**Por que acontece**:
- Comodidade e pressão por entrega
- Falta de cultura de segurança
- Desconhecimento de que é uma má prática
- Desconhecimento de ferramentas de gerenciamento

**Consequências específicas do hardcoding**:
1. O segredo fica armazenado no **histórico do Git para sempre** — mesmo que o arquivo seja editado para remover a credencial, ela continua acessível via `git log` ou `git show <commit>`
2. Qualquer colaborador ou invasor com acesso ao repositório pode extraí-la
3. Servidor mal configurado pode expor os arquivos via HTTP e o conteúdo é baixável
4. Dificuldade de rotação — exige alterar o código, recompilar e fazer deploy completo

---

### Exposição Acidental de Segredos

**Como ocorre**:
- Arquivo `.env` ou `configs.json` commitado por engano
- Arquivo `.env.example` com valores reais (deveria ter valores fictícios)
- `.env` não adicionado ao `.gitignore`
- Prática inadequada de debug (imprimir segredos no log)
- Falta de revisão de código antes do merge
- Falta de automação para detectar segredos no CI/CD

**Google Dorks — o scanner passivo do atacante**: usando busca avançada no Google, é possível encontrar arquivos `.env` públicos indexados com credenciais reais de Laravel, conexões MySQL, chaves AWS. Bots fazem isso de forma automatizada 24/7.

**Persistência no histórico Git**: mesmo que o arquivo seja editado e o segredo removido no próximo commit, o valor ainda aparece em `git diff <commit-anterior>`. Para remover definitivamente, é necessário usar `git filter-branch` ou BFG Repo-Cleaner — e revogar o segredo imediatamente, pois qualquer pessoa que clonou o repositório antes da limpeza ainda tem acesso.

---

### Variáveis de Ambiente — Vantagens, Desvantagens e Riscos

#### Vantagens
- Previne hardcoding de segredos no código
- Rotação e alteração facilitadas sem alterar o código
- Controle de ambientes: desenvolvimento, staging, produção com valores distintos
- CI/CD pode injetar variáveis sem modificar o artefato

#### Desvantagens e Riscos
- Qualquer usuário do sistema pode ler as variáveis se não houver controle de acesso adequado
- **Não são versionadas nem auditáveis** — impossível saber quem alterou e quando
- Variáveis podem ser acidentalmente registradas em logs
- Em ambientes compartilhados, outros serviços ou usuários podem acessar variáveis de outros containers
- **O Elastic Beanstalk mostra as variáveis em texto claro no console AWS** — qualquer pessoa com acesso ao console consegue ler

#### Quando variáveis de ambiente são suficientes
- Desenvolvimento local com dados não sensíveis
- CI/CD simples com tokens temporários de curta duração
- Ambientes onde o controle de acesso ao console é rigoroso e auditado

#### Quando variáveis de ambiente não são suficientes
- Produção com segredos críticos (JWT_SECRET, DB_URL)
- Ambientes com múltiplos colaboradores com acesso ao console
- Cenários que exigem auditoria de quem acessou cada segredo
- Políticas de compliance que exigem rotação automática

---

### HashiCorp Vault — Conceitos e Arquitetura

O **HashiCorp Vault** é a principal ferramenta open source para gerenciamento centralizado de segredos. Centraliza armazenamento, controle de acesso, audit log e rotação automática.

#### Componentes do Vault

**Storage Backend**: onde os dados residem de forma persistente.
- Arquivo local (desenvolvimento/testes)
- AWS S3 com versionamento (produção)
- O backend não define como os segredos são gerenciados — apenas onde são guardados

**Secret Engines (Motores de Segredos)**: mecanismos para gerar e fornecer segredos.
- **KV (Key-Value)**: armazenamento simples de chave-valor para segredos estáticos
- **Database**: cria credenciais temporárias para bancos de dados automaticamente
- **AWS**: gera chaves de acesso temporárias para a AWS com TTL definido

**Métodos de Autenticação**: como usuários e serviços se autenticam no Vault.
- **Token**: chave única fornecida manualmente — útil para testes e provas de conceito
- **Userpass**: usuário e senha — útil para testes locais
- **AppRole**: autenticação para máquinas e aplicações — consiste em RoleID + SecretID; a aplicação troca os dois por um token dinâmico com TTL

**Políticas de Acesso (Policies)**: escritas em HCL ou JSON, definem quem pode acessar o quê.
- Capabilities disponíveis: `create`, `read`, `update`, `delete`, `list`, `deny`
- São o coração do Princípio do Menor Privilégio no Vault

---

### AppRole — Autenticação para Aplicações e CI/CD

O AppRole é o método de autenticação recomendado para aplicações e pipelines automatizados. Funciona com dois elementos:

| Elemento | Equivalente | Como proteger |
|---|---|---|
| **RoleID** | Identificador da aplicação (semi-público) | Pode estar no arquivo de configuração da app |
| **SecretID** | Senha da aplicação (privado) | Nunca commitar — injetar via variável de ambiente ou Vault Agent |

**Fluxo**: Aplicação envia RoleID + SecretID → Vault retorna token dinâmico com TTL → Token é usado para acessar segredos conforme a policy configurada.

**Casos de uso**: pipelines CI/CD (GitHub Actions), infraestrutura como código, aplicações Spring Boot em produção.

---

### Segredos Estáticos vs Segredos Dinâmicos

| Aspecto | Estáticos | Dinâmicos |
|---|---|---|
| Exemplos | JWT_SECRET fixo, senha do banco permanente | Usuário de banco temporário, chave AWS com TTL |
| Gestão | Manual | Automática via Vault |
| Validade | Indefinida até rotação manual | Limitada pelo TTL configurado |
| Risco de vazamento | Alto — válido para sempre | Baixo — expira rapidamente |
| Rotação | Exige intervenção humana | Automática via Vault Database Engine |

**Exemplo prático de segredo dinâmico com MySQL**: o Vault cria um usuário temporário no banco (`v-app-read-xYz123`) com senha aleatória e TTL de 1 hora. Após o TTL expirar, o usuário é automaticamente removido do banco. Se a credencial vazar, o atacante tem no máximo 1 hora de janela.

---

### Vault Agent — Sem Mudanças no Código da Aplicação

O **Vault Agent** é um daemon que realiza toda a autenticação, renovação de token e entrega de segredos sem necessidade de modificar o código da aplicação. Útil para:
- Aplicações legadas sem suporte a SDKs do Vault
- Cenários onde o time de segurança não quer depender do time de desenvolvimento para integrar o Vault

**Funcionamento**: o Agent autentica via AppRole, obtém o token, lê os segredos e os escreve em um arquivo local no formato configurado (JSON, env, etc.). Quando o segredo é atualizado no Vault, o Agent detecta e atualiza o arquivo automaticamente sem reiniciar a aplicação.

---

### Root Token — Gestão Segura

O Root Token é gerado ao inicializar o Vault e possui privilégios totais. Age como chave mestra sem limitações.

**Regras de uso**:
- Usar apenas para configuração inicial e emergências
- Após definir as policies adequadas: criar tokens específicos com privilégios restritos
- Armazenar em cofre físico ou gerenciador de segredos confiável — nunca em arquivo de texto ou repositório
- Monitorar e auditar qualquer uso do Root Token
- Revogar imediatamente quando não for mais necessário

---

### Rotação e Revogação de Segredos

**Por que rotacionar**:
- Segredos podem ter sido comprometidos sem que a empresa saiba
- Rotação periódica limita a janela de exploração mesmo em caso de vazamento silencioso
- Auditorias de compliance exigem evidência de rotação (SLA: 60, 90 ou 180 dias dependendo da criticidade)

**Conceito de Lease no Vault**: cada segredo dinâmico gerado tem um `lease` — um identificador único + TTL. Permite `renew` (estender) ou `revoke` (revogar) individualmente sem afetar outros segredos.

**Exemplo de impacto da rotação**:
- Sem rotação: senha do banco válida por um ano; se vazar, o atacante tem acesso por todo esse período
- Com rotação horária: credencial expira em 1 hora; atacante precisa agir antes da expiração e gerar nova credencial — janela de risco drasticamente reduzida

---

### Audit Log — Rastreabilidade Completa

O Vault registra em log auditável todas as operações:
- Autenticações: logins, logouts, renovações de token
- Operações de segredos: leitura, escrita, exclusão
- Gerenciamento de policies
- Rotação e revogação de credenciais
- Eventos de lease

**Tipos de destino de log**: arquivo JSON (`file`), syslog local, socket de rede para SIEM.

**Por que é crítico**: sem audit log, em caso de vazamento, é impossível saber qual aplicação ou usuário acessou o segredo. Obriga verificar dezenas de servidores e clusters manualmente — inviável em produção.

---

### Detecção de Segredos no Código-Fonte

#### Gitleaks
- Detecta segredos em commits recentes, branches e histórico
- Leve e rápido — fácil integração com GitHub Actions
- Suporte a regras personalizadas com prefixos do projeto
- Gera relatório JSON com: regra detectada, arquivo, linha, commit hash, autor, data e mensagem do commit

#### TruffleHog
- Escaneia o repositório Git em busca de segredos
- **Detecta até segredos removidos do histórico** — não apenas o código atual
- Mais profundo que o Gitleaks para análise forense de histórico

**Métodos de detecção**:
1. **Local**: executar manualmente na máquina
2. **Pre-commit (GitHooks)**: executa antes do commit e bloqueia se encontrar credencial
3. **CI/CD**: etapa no pipeline como camada adicional de segurança

#### Entropia como Técnica de Detecção
Ferramentas como Gitleaks calculam a entropia de cada string no código. Strings com alta entropia (padrão aleatório) são candidatas a segredos. Combinado com expressões regulares para formatos conhecidos (padrão de chave AWS, formato de JWT), forma uma abordagem híbrida que reduz falsos positivos.

---

### Resposta a Incidentes de Segredos Expostos

**Primeiros passos ao detectar um segredo exposto**:
1. Não ignorar — quanto mais rápido, mais seguro
2. Avaliar o alcance: verificar commits, branches e repositórios afetados
3. Notificar responsáveis: time de DevOps, segurança ou infraestrutura
4. Documentar o que aconteceu (registrar como incidente)
5. Revogar ou rotacionar imediatamente o segredo comprometido
6. Substituir por variável de ambiente ou Secret Manager
7. Testar a aplicação para garantir que a mudança não interrompeu o serviço
8. Registrar no sistema de tickets (JIRA/GitHub Issues): tipo de segredo, repositório, branch, data, ação tomada

**Abordagem cultural**: ao identificar um incidente, focar em corrigir e treinar, não em punir. Postura punitiva impede que a equipe reporte problemas futuros. Uma pessoa corrigida com empatia se torna um agente ativo da cultura de segurança.

---

### Ferramentas de Gerenciamento por Plataforma

| Ferramenta | Plataforma | Pontos Fortes | Uso no Projeto |
|---|---|---|---|
| **HashiCorp Vault** | On-premise / Cloud | Open source, granular, audit log completo, segredos dinâmicos | Referência de mercado — ideal para múltiplos serviços |
| **AWS Secrets Manager** | AWS | Integração nativa com RDS, rotação automática, IAM policies | Melhor opção para VidaLongaFlix (já na AWS) |
| **Azure Key Vault** | Azure | Integração com AD, certificados gerenciados | Não aplicável ao projeto |
| **GCP Secret Manager** | GCP | Simples e integrado | Não aplicável ao projeto |
| **GitLab Variables** | CI/CD GitLab | Variáveis de pipeline — não é vault dedicado | Não aplicável (GitHub Actions) |

---

## Parte 2 — Vulnerabilidades Reais Identificadas no Projeto

### Tabela Mestre — Backend (Spring Boot / Java 17)

| ID | Arquivo / Local | Vulnerabilidade | Risco | Severidade | Detectável |
|---|---|---|---|---|---|
| BMS1 | Elastic Beanstalk console | `JWT_SECRET`, `DB_URL`, `OTLP_AUTH_HEADER` como env vars em texto claro — visíveis no console AWS para qualquer usuário com acesso | Sem audit trail, sem rotação automática, expostos no console | Critical | ❌ Processo |
| BMS2 | `.github/workflows/ci.yml` | `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` como long-lived credentials nos GitHub Secrets — sem expiração, sem rotação | Vazamento do secret do GitHub = acesso permanente à AWS | Critical | ⚠️ Gitleaks (se vazar para código) |
| BMS3 | `pom.xml` / histórico Git | Verificar se algum commit histórico contém credencial hardcoded — não há varredura automatizada do histórico | Credencial antiga no histórico pode estar válida | Critical | ✅ TruffleHog / Gitleaks |
| BMS4 | `application.properties` | Usa `${OTLP_HTTP_ENDPOINT}` e `${JWT_SECRET}` — padrão correto. Confirmar que nenhum valor real aparece no arquivo | Bom padrão — mas sem rotação dos valores | Info | ✅ Gitleaks (deteta se valor vazar) |
| BMS5 | `application-prod.properties` | `OTLP_ENDPOINT` e `OTLP_AUTH_HEADER` configurados via variáveis — sem rotação documentada | Token Grafana sem prazo de expiração | Major | ❌ Processo |
| BMS6 | `Dockerfile` | Verificar se `ENV` no Dockerfile contém valores hardcoded de secrets | Segredos em `ENV` do Dockerfile ficam na camada da imagem Docker | Critical | ✅ Gitleaks / Docker Scout |
| BMS7 | `.gitignore` | Verificar se `.env`, `*.properties` com valores reais e arquivos de configuração sensíveis estão listados | Arquivo sensível commitado por engano | Critical | ✅ Gitleaks (pre-commit) |
| BMS8 | `TokenService.java` | `JWT_SECRET` não tem política de rotação definida — se vazar, permanece válido indefinidamente | Janela de exploração indefinida | Major | ❌ Processo |
| BMS9 | Logs no Loki | Verificar se `log.info/error` expõe tokens, headers Authorization ou connection strings em mensagens de log | Logs exportados para Grafana podem conter credenciais | Major | ✅ SonarCloud (S4792) |
| BMS10 | Histórico Git | Ausência de Gitleaks no pipeline — nenhum scan automático de segredos em commits | Credencial commitada passa despercebida até ser explorada | Critical | ✅ Gitleaks no ci.yml |

---

### Tabela Mestre — Frontend (Angular / TypeScript)

| ID | Arquivo / Local | Vulnerabilidade | Risco | Severidade | Detectável |
|---|---|---|---|---|---|
| FMS1 | `environment.ts` / `environment.prod.ts` | Verificar se contém chaves de API, secrets ou tokens — deve ter apenas `apiUrl` pública | Segredo exposto no código JavaScript bundle | Critical | ✅ Gitleaks / SonarCloud S2068 |
| FMS2 | `package.json` / histórico Git | Verificar se algum commit histórico contém token ou secret no repositório Angular | Credencial antiga no histórico Git público | Critical | ✅ TruffleHog / Gitleaks |
| FMS3 | `.gitignore` do repositório Angular | Verificar se `.env`, arquivos de configuração local e `environment.local.ts` estão bloqueados | Arquivo com secrets commitado por engano | Critical | ✅ Gitleaks (pre-commit) |
| FMS4 | `localStorage` / `sessionStorage` | Token JWT armazenado no browser — acessível por JavaScript (XSS) | Exfiltração do token se XSS estiver presente | Major | ⚠️ Hotspot SonarCloud |
| FMS5 | Qualquer `.ts` ou `.json` | API keys de terceiros (Google Maps, analytics, etc.) hardcoded no código Angular | Bundle JS é público — qualquer usuário pode extrair as chaves | Critical | ✅ Gitleaks / SonarCloud S2068 |
| FMS6 | CI/CD do repositório Angular | Ausência de `npm audit` e Gitleaks no pipeline do repositório Angular | Secrets e CVEs no frontend passam sem detecção | Major | ✅ Adicionar no ci.yml do Angular |

---

### Referência de Severidade — Gestão de Segredos

| Severidade | Definição neste contexto | Ação | Prazo |
|---|---|---|---|
| **Critical** | Segredo ativo exposto ou sem auditoria em produção | Revogar e migrar imediatamente | Mesma sprint |
| **Major** | Segredo sem rotação ou sem audit trail | Documentar e planejar rotação | 1–2 sprints |
| **Minor** | Processo não documentado ou ferramenta de detecção ausente | Adicionar ao backlog de segurança | Próximo ciclo |
| **Info** | Bom padrão já aplicado — apenas registrar | Manter e expandir | Manutenção contínua |

---

## Parte 3 — Cenários e Resultado Esperado — Backend (Spring Boot / Java)

> Positivo = comportamento seguro. Negativo = falha de segurança que deve ser detectada ou corrigida. Triagem = requer análise humana antes de decisão.

---

### B-MS-01 — Hardcoded Secrets no Código (CWE-259 / Blocker)

| # | Cenário | Esperado |
|---|---|---|
| 1 | JWT_SECRET definido como string literal em `TokenService.java` | Gitleaks e SonarCloud S2068 sinalizam Blocker — credencial no código-fonte |
| 2 | `datasource.password=MinhaSecreta123` em `application.properties` commitado | Gitleaks detecta no pre-commit e bloqueia o commit |
| 3 | `OTLP_AUTH_HEADER=Basic eyJ...` com valor real commitado em `application-prod.properties` | TruffleHog detecta no scan histórico mesmo se o arquivo foi removido depois |
| 4 | Credencial hardcoded removida do arquivo mas presente no histórico Git | TruffleHog ainda detecta via scan de histórico — credencial deve ser revogada independente da remoção |
| 5 | `@Value("${JWT_SECRET}")` lendo de variável de ambiente sem valor no arquivo | Nenhum alerta — padrão correto de externalização |
| 6 | `application.properties` usando `${DB_PASSWORD}` sem valor real no arquivo | Nenhum alerta — placeholder sem valor hardcoded |
| 7 | Token expirado de mock em classe de teste unitário | Gitleaks pode sinalizar — avaliar e marcar como falso positivo com justificativa se o token for inerte |

---

### B-MS-02 — Armazenamento e Exposição de Segredos em Variáveis de Ambiente (Major)

| # | Cenário | Esperado |
|---|---|---|
| 8 | JWT_SECRET como variável de ambiente no Elastic Beanstalk visível no console AWS | Risco aceito no curto prazo; meta é migrar para AWS Secrets Manager |
| 9 | `aws configure` com `AWS_ACCESS_KEY_ID` long-lived nos GitHub Secrets sem expiração | Credencial sem rotação — ideal substituir por OIDC para eliminar chaves de longa duração |
| 10 | `ENV JWT_SECRET=valorReal` no `Dockerfile` | Gitleaks e Docker Scout detectam — segredo na camada da imagem vai para o Docker Hub público |
| 11 | `ARG JWT_SECRET` no `Dockerfile` sem valor hardcoded — valor injetado em tempo de build via `--build-arg` | Melhor que `ENV` mas ainda exposto em `docker history` — ideal usar Secret Manager |
| 12 | Spring Boot lendo `JWT_SECRET` via `@Value("${JWT_SECRET}")` e o valor vindo de AWS Secrets Manager em produção | Padrão correto — segredo nunca toca o código ou o console em texto claro |
| 13 | Variável de ambiente exposta em log de erro durante startup do Spring Boot | SonarCloud S4792 sinaliza — variáveis sensíveis não devem aparecer em logs |

---

### B-MS-03 — Rotação de Segredos (Major)

| # | Cenário | Esperado |
|---|---|---|
| 14 | JWT_SECRET sem rotação documentada — o mesmo valor usado desde o deploy inicial | Risco crescente com o tempo — política de rotação deve ser definida e documentada |
| 15 | Rotação manual do JWT_SECRET exige alterar variável no EB console e reiniciar a aplicação | Aceitável no curto prazo — meta é automação via AWS Secrets Manager |
| 16 | JWT_SECRET rotacionado sem invalidar tokens ativos — usuários logados perdem sessão imediatamente | Impacto em UX — planejar janela de rotação ou suporte a dois segredos simultâneos |
| 17 | AWS Secrets Manager configurado para rotacionar a senha do RDS automaticamente a cada 30 dias | Rotação automática sem intervenção humana — nenhum alerta — comportamento correto |
| 18 | Token Grafana Cloud sem expiração e sem documentação de quando foi gerado | Sem rastreabilidade de uso — revogar e criar novo token com data de geração documentada |
| 19 | Processo de rotação emergencial documentado: revogar → gerar novo → atualizar → testar → verificar logs | Nenhum alerta — documentação de resposta a incidente disponível |

---

### B-MS-04 — Detecção de Segredos no Pipeline (Critical)

| # | Cenário | Esperado |
|---|---|---|
| 20 | Push para `main` com arquivo contendo `AWS_SECRET_ACCESS_KEY` hardcoded sem Gitleaks no CI | Credencial entra no repositório sem detecção — vulnerabilidade crítica |
| 21 | Gitleaks executado como passo no `ci.yml` antes de qualquer outro step | Pipeline falha imediatamente se segredo for detectado — merge bloqueado |
| 22 | Gitleaks com `.gitleaks.toml` customizado para prefixos do projeto (`VLF_`) | Detecção mais precisa com menos falsos positivos |
| 23 | TruffleHog rodando scan completo do histórico Git no repositório | Detecta credenciais em commits antigos — relatório gerado como artefato do pipeline |
| 24 | Pre-commit hook configurado com Gitleaks na máquina do desenvolvedor | Commit bloqueado localmente antes de chegar ao repositório — primeira linha de defesa |
| 25 | Desenvolvedor tenta commitar arquivo com token de teste inerte — Gitleaks sinaliza | Avaliar: marcar como exceção com justificativa no `.gitleaks.toml` ou remover o valor do arquivo |
| 26 | Relatório JSON do Gitleaks incluindo regra detectada, arquivo, linha, commit hash, autor e data | Rastreabilidade completa para resposta a incidente |

---

### B-MS-05 — Princípio do Menor Privilégio em Segredos (Major)

| # | Cenário | Esperado |
|---|---|---|
| 27 | `AWS_ACCESS_KEY_ID` do CI/CD com permissões `AdministratorAccess` na conta AWS | Escopo excessivo — comprometimento do secret dá acesso total à conta |
| 28 | IAM policy do CI/CD restrita a `ecr:GetAuthorizationToken`, `elasticbeanstalk:CreateApplicationVersion`, `s3:PutObject` para o bucket de deploy | Menor privilégio aplicado — comprometimento limita-se às ações de deploy |
| 29 | Token Grafana com permissão `metrics/logs/traces:write` sem permissões de leitura ou admin | Correto — token de produção com escopo mínimo necessário |
| 30 | Chave AWS S3 com acesso total ao bucket e a todos os outros buckets da conta | Escopo excessivo — restringir ao bucket `vidalongaflix-media` com políticas de ARN específico |
| 31 | RDS com usuário de banco com permissões `GRANT ALL` em todas as tabelas | Escopo excessivo — usuário da aplicação deve ter apenas `SELECT/INSERT/UPDATE/DELETE` nas tabelas necessárias |

---

### B-MS-06 — Audit Log e Rastreabilidade (Major)

| # | Cenário | Esperado |
|---|---|---|
| 32 | Acesso ao JWT_SECRET no EB console sem registro de quem acessou e quando | Sem audit trail — qualquer pessoa com acesso ao console pode ler e não há rastreabilidade |
| 33 | AWS CloudTrail habilitado na conta — registra todos os acessos ao Secrets Manager | Audit log disponível: quem acessou, de onde, quando e qual segredo |
| 34 | Log de startup do Spring Boot imprimindo valor de variável de ambiente | SonarCloud S4792 — dado sensível em log |
| 35 | `GlobalExceptionHandler` capturando exceção de conexão com banco e imprimindo a connection string no body da resposta | Stack trace com credencial exposta na resposta HTTP — Critical |
| 36 | AWS Secrets Manager com VPC endpoint configurado — acesso ao secret não passa pela internet pública | Nenhum alerta — rota privada de acesso ao secret |

---

### B-MS-07 — Resposta a Incidentes de Segredos Expostos (Critical)

| # | Cenário | Esperado |
|---|---|---|
| 37 | JWT_SECRET detectado em repositório público — procedimento: revogar, gerar novo, atualizar EB, reiniciar app, verificar logs | Todos os tokens existentes ficam inválidos — usuários precisam fazer login novamente |
| 38 | Credencial AWS detectada em repositório — procedimento: desativar no IAM imediatamente, criar nova, atualizar GitHub Secrets | Comprometimento limitado ao período entre o vazamento e a revogação |
| 39 | Segredo exposto sem política de rotação — impossível saber há quanto tempo estava sendo explorado | Sem auditoria, o impacto total nunca será completamente conhecido |
| 40 | Incidente documentado: tipo de segredo, repositório, branch, data de detecção, ação tomada, responsável | Rastreabilidade para auditoria e prevenção de recorrência |
| 41 | Desenvolvedor que expôs credencial tratado com empatia — foco em processo e treinamento, não em punição | Cultura de segurança fortalecida — próximos incidentes serão reportados mais rapidamente |

---

## Parte 4 — Cenários e Resultado Esperado — Frontend (Angular / TypeScript)

---

### F-MS-01 — Secrets no Código Angular (CWE-259 / Blocker)

| # | Cenário | Esperado |
|---|---|---|
| 42 | API key do Google Analytics hardcoded em `environment.ts` ou em arquivo TypeScript | SonarCloud S2068 sinaliza Blocker — secret no código que vai para o bundle público |
| 43 | `environment.prod.ts` com apenas `apiUrl: 'https://api.vidalongaflix.com.br/api'` sem nenhum secret | Nenhum alerta — URL pública não é secret |
| 44 | Arquivo `.env` com variáveis do Angular commitado no repositório | Gitleaks detecta — mesmo que o Angular não use `.env` nativamente, o arquivo existe no repositório |
| 45 | Chave de API de serviço externo (Stripe, Google Maps) hardcoded em service Angular | Bundle JavaScript é público — qualquer usuário abre DevTools e extrai a chave |
| 46 | Chave de API pública (destinada a uso no browser, sem permissões sensíveis) no `environment.ts` | Risco menor — mas documentar escopo restrito da chave e configurar restrições de domínio na plataforma do serviço |

---

### F-MS-02 — Histórico Git e Exposição Acidental no Frontend (Critical)

| # | Cenário | Esperado |
|---|---|---|
| 47 | `.gitignore` do repositório Angular não bloqueia `environment.local.ts` com valores reais | Arquivo de configuração local commitado por engano — Gitleaks detecta no CI |
| 48 | `.gitignore` com `environment.local.ts`, `.env`, `*.local.ts` e `*.secret.ts` listados | Nenhum alerta — arquivos sensíveis bloqueados do repositório |
| 49 | Commit antigo no repositório Angular com token de CI/CD no arquivo de configuração | TruffleHog detecta no scan histórico — token deve ser revogado |
| 50 | Pre-commit hook com Gitleaks configurado no repositório Angular | Commit bloqueado antes de chegar ao GitHub — primeira linha de defesa para o frontend |

---

### F-MS-03 — Token JWT no Browser (CWE-312 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 51 | Token JWT armazenado em `localStorage` — acessível por qualquer script na página | SonarCloud marca Hotspot — risco de exfiltração via XSS |
| 52 | Token armazenado em `sessionStorage` em vez de `localStorage` | Hotspot ainda presente — `sessionStorage` também acessível via JavaScript |
| 53 | Mitigação documentada: Angular escapa conteúdo por padrão, CSP configurado, sem `[innerHTML]` direto | Marcar Hotspot como Acknowledged com justificativa de mitigação |
| 54 | Token em HttpOnly Cookie com SameSite=Strict | Nenhum Hotspot — token inacessível por JavaScript |
| 55 | Token decodificado localmente via `atob()` para extrair `userId` sem verificar assinatura | Risco — claims do JWT devem ser verificados pelo backend, não confiados pelo frontend sem assinatura |

---

### F-MS-04 — CI/CD e Detecção no Repositório Angular (Critical)

| # | Cenário | Esperado |
|---|---|---|
| 56 | Repositório Angular sem Gitleaks no pipeline de CI | Segredos commitados passam despercebidos até serem explorados |
| 57 | Job `secret-scan` com Gitleaks antes do step de build no GitHub Actions do Angular | Pipeline falha se segredo for detectado — merge bloqueado |
| 58 | `npm audit --audit-level=high` no pipeline Angular sem step de detecção de secrets | CVEs detectados mas secrets não — necessário ter ambos |
| 59 | Gitleaks + npm audit + SonarCloud em série no pipeline do repositório Angular | Cobertura completa: secrets + CVEs + vulnerabilidades de código no frontend |

---

## Parte 5 — Passo a Passo de Implementação por Sprint

---

### Sprint 0 — Análise e Mapeamento do Estado Atual (sem código novo)

**Objetivo**: entender o que existe antes de corrigir.

1. Clonar o repositório backend localmente e executar `git log --all --full-history -- "**/*.properties" "**/*.env"` para verificar se arquivos sensíveis aparecem no histórico
2. Executar TruffleHog no repositório backend: `trufflehog git file://. --json` — revisar relatório
3. Executar TruffleHog no repositório Angular: `trufflehog git https://github.com/FabricioMartinhoEngeroff/vida-longa-flix.git --json`
4. Revisar o `Dockerfile` do backend — verificar se `ENV` contém valores hardcoded
5. Listar todas as variáveis no console do Elastic Beanstalk — documentar quais são críticas (JWT_SECRET, DB_URL, OTLP_AUTH_HEADER)
6. Verificar se `.gitignore` do backend bloqueia `.env`, `*.local.properties` e similares
7. Verificar se `.gitignore` do Angular bloqueia `environment.local.ts` e arquivos locais
8. Registrar resultado de cada item como GitHub Issue com label `security`

---

### Sprint 1 — Gitleaks no CI/CD Backend + Angular (Prioridade: Critical)

**Objetivo**: detectar automaticamente qualquer secret commitado antes que chegue ao repositório.

**Backend — editar `.github/workflows/ci.yml`:**

1. Adicionar novo job `secret-scan` como primeiro step do pipeline (antes do `mvn test`):
```yaml
secret-scan:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
      with:
        fetch-depth: 0          # histórico completo para scan profundo
    - name: Run Gitleaks
      uses: gitleaks/gitleaks-action@v2
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
2. Garantir que o job de build depende do `secret-scan`: `needs: [secret-scan]`
3. Criar `.gitleaks.toml` na raiz do projeto backend com regras customizadas:
   - Adicionar prefixos do projeto (`VLF_`) para melhorar detecção
   - Adicionar exceções para tokens de mock em testes (`allowlist` com paths de `src/test/`)
4. Commit no repositório, abrir PR, verificar que o pipeline roda o scan
5. Testar: criar branch com token fake, verificar que o pipeline falha corretamente

**Angular — no repositório https://github.com/FabricioMartinhoEngeroff/vida-longa-flix.git:**

1. Adicionar mesmo job `secret-scan` no CI do Angular (se houver `ci.yml`)
2. Se não houver CI no Angular: criar `.github/workflows/ci.yml` mínimo com Gitleaks + npm audit
3. Verificar `.gitignore` — adicionar: `environment.local.ts`, `.env`, `*.secret.ts`

---

### Sprint 2 — Pre-commit Hook Local com Gitleaks (Prioridade: Major)

**Objetivo**: bloquear o commit na máquina do desenvolvedor antes de chegar ao repositório.

**Instalação (Ubuntu/WSL)**:
1. Instalar Gitleaks: `sudo apt install gitleaks` ou `brew install gitleaks` (Mac)
2. Na raiz do repositório backend, criar `.git/hooks/pre-commit`:
```bash
#!/bin/sh
gitleaks protect --staged --redact -v
exit_code=$?
if [ $exit_code -ne 0 ]; then
  echo "Gitleaks detectou segredos! Commit bloqueado."
  exit 1
fi
```
3. Tornar executável: `chmod +x .git/hooks/pre-commit`
4. Repetir para o repositório Angular
5. Testar: criar arquivo com token fake, tentar commitar — verificar que é bloqueado

---

### Sprint 3 — Migração JWT_SECRET para AWS Secrets Manager (Prioridade: Critical)

**Objetivo**: eliminar JWT_SECRET como variável de ambiente em texto claro no Elastic Beanstalk.

**No console AWS**:
1. Acessar AWS Secrets Manager → Criar segredo
2. Tipo: "Other type of secret" → Chave: `JWT_SECRET`, Valor: gerar novo secret com alta entropia
3. Nome do segredo: `vidalongaflix/prod/jwt-secret`
4. Habilitar rotação automática: configurar para 90 dias
5. Anotar o ARN do segredo gerado

**No IAM**:
1. Criar política IAM com permissão mínima:
   - `secretsmanager:GetSecretValue` apenas no ARN `vidalongaflix/prod/jwt-secret`
2. Associar política ao role do EC2/EB da aplicação (`aws-elasticbeanstalk-ec2-role`)

**No backend (Spring Boot)**:
1. Adicionar dependência `spring-cloud-aws-secrets-manager-config` ou usar SDK AWS diretamente
2. No `application-prod.properties`: trocar `${JWT_SECRET}` por leitura via SDK ou Spring Cloud AWS
3. Alternativa simples sem library extra: criar `SecretsManagerConfig.java` que lê o secret no startup e popula o `Environment` do Spring
4. Remover `JWT_SECRET` das variáveis de ambiente do Elastic Beanstalk após confirmar que a app lê do Secrets Manager

**Testar**:
1. Deploy em staging com a configuração nova
2. Verificar log de startup — confirmar que JWT_SECRET não aparece
3. Fazer login com usuário real — confirmar que JWT é gerado e validado corretamente

---

### Sprint 4 — Migração DB_URL para AWS Secrets Manager (Prioridade: Critical)

**Objetivo**: eliminar a senha do RDS como variável de ambiente.

1. No Secrets Manager: criar segredo `vidalongaflix/prod/db-credentials` com keys `username` e `password`
2. Habilitar rotação automática do Secrets Manager integrada ao RDS — AWS atualiza a senha no banco e no secret automaticamente
3. No backend: ler `DB_USERNAME` e `DB_PASSWORD` do Secrets Manager no startup, injetar via `DataSourceConfig.java`
4. Atualizar IAM policy: adicionar `vidalongaflix/prod/db-credentials` na lista de ARNs permitidos
5. Remover `DB_URL` com credencial das env vars do EB após confirmar que a app conecta via Secrets Manager
6. Testar: provocar rotação manual no console → verificar que a app continua funcionando sem restart

---

### Sprint 5 — OIDC para GitHub Actions (substituir long-lived AWS credentials) (Prioridade: Critical)

**Objetivo**: eliminar `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` dos secrets do GitHub.

1. No console AWS IAM: criar Identity Provider OIDC para `token.actions.githubusercontent.com`
2. Criar IAM Role com trust policy para o repositório `FabricioMartinhoEngeroff/VidaLongaFlix` branch `main`
3. Associar política de deploy com escopo mínimo (ECR + EB + S3)
4. Editar `.github/workflows/ci.yml`:
   - Adicionar `permissions: id-token: write` no job de deploy
   - Substituir `aws-access-key-id` / `aws-secret-access-key` por `role-to-assume: arn:aws:iam::ACCOUNT:role/GithubActionsRole`
5. Remover `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` dos GitHub Secrets após confirmar que o deploy funciona com OIDC
6. Testar: push na `main` → verificar que o deploy ocorre sem credenciais long-lived

---

### Sprint 6 — Audit Log e Monitoramento de Acessos a Secrets (Prioridade: Major)

**Objetivo**: saber quem acessa cada secret, quando e de onde.

1. No console AWS: habilitar **AWS CloudTrail** para a conta — garante log de todos os acessos ao Secrets Manager
2. Criar alarme no CloudWatch: disparar se `GetSecretValue` for chamado de IP fora dos ranges do Elastic Beanstalk
3. Verificar se logs do Spring Boot estão limpos: buscar em Loki por queries com termos como `JWT`, `password`, `secret`, `token` nas mensagens de log
4. Criar alerta no Grafana: se qualquer log contendo esses termos aparecer no Loki → notificação imediata
5. Documentar no `menage_secrets.md`: data de criação de cada secret, responsável, política de rotação e data da próxima rotação planejada

---

### Sprint 7 — Documentação de Processos e Checklist de Desligamento (Prioridade: Major)

**Objetivo**: garantir rastreabilidade e procedimentos documentados para cada tipo de secret.

1. Criar `docs/security/security_menage_secrets/runbook-secrets.md` com:
   - Lista de todos os secrets ativos em produção com ARN, data de criação e responsável
   - Procedimento de rotação manual para cada secret (com screenshots)
   - Procedimento de rotação emergencial (em caso de vazamento suspeito)
   - Checklist de desligamento de colaborador: revogar acesso ao console AWS, revogar tokens GitHub, revogar SSH keys
2. Registrar no GitHub como issue recorrente (scheduled): "Revisar data de rotação dos secrets" — a cada 90 dias
3. Criar `SECURITY.md` na raiz do repositório documentando como reportar vulnerabilidades de segurança

---

### Referências de Ferramentas

| Ferramenta | Uso | Instalação |
|---|---|---|
| **Gitleaks** | Detecção de secrets em commits | `brew install gitleaks` / `apt install gitleaks` / `gitleaks/gitleaks-action@v2` (GitHub Actions) |
| **TruffleHog** | Scan profundo do histórico Git | `pip install trufflehog` / `trufflesecurity/trufflehog@main` (GitHub Actions) |
| **AWS Secrets Manager** | Armazenamento seguro + rotação automática | Console AWS → Secrets Manager |
| **AWS CloudTrail** | Audit log de acessos a secrets | Console AWS → CloudTrail → Criar trilha |
| **HowToRotate** | Playbooks de rotação por plataforma | `howtorotate.com` |
| **jwt.io** | Debug de tokens JWT em desenvolvimento | Browser: `jwt.io` |
| **HashiCorp Vault** | Alternativa open source ao AWS SM | `docker run -p 8200:8200 hashicorp/vault` |