# SRE Observability 2 — Terraform, Elasticidade e AWS no VidaLongaFlix

## Objetivos
- Entender os conceitos de infraestrutura elastica vistos no curso e traduzi-los para a arquitetura real do VidaLongaFlix
- Explicar quais recursos AWS fazem parte da base de escalabilidade: VPC, subnets, security groups, load balancer e auto scaling
- Definir como o Terraform pode virar a fonte de verdade da infraestrutura do projeto
- Separar o que faz sentido aplicar agora do que seria retrabalho ou conflito com o Elastic Beanstalk
- Conectar teste de carga, health check e observabilidade com decisoes de capacidade

---

## Modulo 1 — Infraestrutura como Codigo e Elasticidade

### Infraestrutura como Codigo (IaC)
- IaC e a pratica de descrever infraestrutura em arquivos versionados, revisaveis e reproduziveis
- Em vez de clicar manualmente no console da AWS, o time declara o estado desejado e a ferramenta aplica as mudancas
- O principal ganho nao e apenas automacao: e **consistencia**
- O principal risco de nao usar IaC e o **drift**: producao fica diferente do que o time acha que existe

### Terraform
- Terraform descreve recursos de nuvem em arquivos `.tf`
- Ele compara o estado atual com o estado desejado e mostra o plano de mudancas antes de aplicar
- Permite modularizar ambientes, compartilhar variaveis e padronizar configuracoes entre dev, staging e prod
- No VidaLongaFlix, o Terraform nao entraria para substituir Docker, Spring Boot ou GitHub Actions. Ele entraria para descrever a **infraestrutura AWS** que hospeda tudo isso

### Elasticidade
- Infraestrutura elastica cresce quando a carga aumenta e reduz quando a carga cai
- O objetivo nao e apenas "aguentar pico", mas equilibrar **disponibilidade + custo**
- Elasticidade sem observabilidade e perigosa: o ambiente escala, mas o time nao sabe se escalou pelo motivo correto

### Como aplicar no VidaLongaFlix

Hoje o projeto usa:
- **Elastic Beanstalk** para orquestrar a aplicacao em producao
- **CloudFront** na borda
- **RDS PostgreSQL** para persistencia
- **S3** para midia
- **Grafana Cloud + OTLP** para metricas, logs e traces

Portanto, o papel do Terraform neste projeto e:
1. Codificar a infraestrutura AWS que hoje depende de configuracao manual
2. Padronizar rede, seguranca e ambientes
3. Permitir evolucao controlada da plataforma sem perder o modelo atual de deploy

> O ponto central: no VidaLongaFlix, Terraform deve descrever a infraestrutura **real** do projeto, e nao reproduzir literalmente um laboratorio de EC2 "na mao".

---

## Modulo 2 — Recursos Fundamentais da AWS

### Regiao e Availability Zones

**Conceito**
- Uma regiao AWS e uma area geografica, como `us-east-2`
- Cada regiao possui varias **Availability Zones (AZs)**, isoladas entre si
- Distribuir recursos em duas ou mais AZs reduz risco de indisponibilidade por falha localizada

**Aplicacao no VidaLongaFlix**
- O projeto ja opera na AWS com Elastic Beanstalk em `us-east-2`, conforme o pipeline
- A recomendacao e manter o ambiente distribuido em pelo menos **duas subnets em duas AZs**
- Isso e especialmente importante para o **Application Load Balancer** e para alta disponibilidade da camada web

### VPC

**Conceito**
- VPC (Virtual Private Cloud) e a rede isolada do projeto na AWS
- Dentro dela, os recursos trocam trafego de forma controlada
- E a base sobre a qual ficam subnets, tabelas de rota, gateways e security groups

**Aplicacao no VidaLongaFlix**
- O desenho atual do projeto ja pressupoe uma VPC separando:
  - borda publica
  - camada web
  - banco privado
- O Terraform pode consolidar essa topologia como codigo para evitar configuracoes manuais divergentes

### Subnets

**Conceito**
- Subnets segmentam a VPC
- Subnets publicas recebem recursos acessiveis a partir da internet ou da borda AWS
- Subnets privadas guardam componentes internos, como banco de dados

**Aplicacao no VidaLongaFlix**

Arquitetura alvo:

```
Internet
   |
   v
CloudFront
   |
   v
ALB / Elastic Beanstalk  -> subnets publicas
   |
   v
RDS PostgreSQL           -> subnets privadas
```

- O backend do VidaLongaFlix pode ficar em subnets publicas gerenciadas pelo Beanstalk
- O RDS deve continuar em subnets privadas, sem exposicao direta a internet
- O Terraform pode criar ou importar as subnets corretas e associar cada recurso ao local certo

### Security Groups

**Conceito**
- Security Groups funcionam como firewall stateful por recurso
- Definem quem pode falar com quem, em quais portas e protocolos

**Aplicacao no VidaLongaFlix**
- Restringir a entrada do backend para trafego originado da borda correta
- Permitir JDBC apenas entre a camada de aplicacao e o RDS
- Manter SSH totalmente restrito ou removido quando possivel
- Preservar a regra de confianca em `X-Forwarded-For`, ja usada pelo rate limit do login

> O curso mostra "grupo de seguranca por ambiente". Esse conceito encaixa bem aqui: `dev`, `staging` e `prod` devem ter regras separadas e nomes explicitos.

---

## Modulo 3 — Compute, Launch Template e Auto Scaling

### Launch Template

**Conceito**
- Launch Template define como uma instancia EC2 nasce: AMI, tipo de maquina, disco, chave, rede e bootstrap
- Ele e a receita usada pelo Auto Scaling Group para criar novas instancias

**Como interpretar isso no VidaLongaFlix**
- No projeto atual, quem abstrai essa camada e o **Elastic Beanstalk**
- Ou seja: o conceito continua valido, mas geralmente nao precisamos gerenciar `aws_launch_template` diretamente enquanto o Beanstalk continuar sendo a camada de orquestracao da aplicacao

### Auto Scaling Group (ASG)

**Conceito**
- O ASG garante quantidade minima, maxima e desejada de instancias
- Se uma instancia falha, ele repoe
- Se a carga cresce, ele adiciona capacidade
- Se a carga cai, ele reduz custo

**Aplicacao no VidaLongaFlix**
- Esse conceito encaixa 100% no projeto
- Mas a implementacao mais coerente e deixar o **Elastic Beanstalk** gerenciar o grupo de instancias do ambiente
- O Terraform pode configurar o ambiente do Beanstalk com:
  - `MinSize`
  - `MaxSize`
  - tipo de instancia
  - politica de rolling update
  - health reporting

### Politicas de Escala

**Conceito**
- Uma politica de escala define **quando** subir ou descer capacidade
- O curso usa CPU media como gatilho
- Isso e util, mas isoladamente pode ser uma metrica pobre

**Aplicacao no VidaLongaFlix**

Melhor combinacao para este projeto:
- **CPU**: bom sinal de saturacao da instancia
- **Latencia p95**: sinal mais proximo da experiencia do usuario
- **Taxa de erro**: protege contra degradacao funcional
- **Throughput / RPS**: ajuda a contextualizar se o pico faz sentido

Pratica recomendada:
1. Comecar com politica simples de CPU no Beanstalk
2. Validar comportamento com k6
3. Ajustar thresholds olhando Grafana e Actuator
4. Evoluir para alertas e capacidade baseados em dados reais

> Escalar so por CPU e melhor que nada. Escalar com CPU e observabilidade e o caminho maduro.

### Minimo e Maximo

**Conceito**
- `min` define o piso de capacidade
- `max` define o teto operacional
- O erro comum e escolher numeros arbitrarios sem medir

**Aplicacao no VidaLongaFlix**
- Em producao, o minimo pratico tende a ser **2 instancias**, nao 1, para reduzir risco de indisponibilidade durante reinicio, deploy ou falha isolada
- Em dev, o minimo pode ser 0 ou 1 dependendo do custo e do uso do ambiente
- O maximo deve ser calibrado com teste de carga e custo aceito

---

## Modulo 4 — Load Balancer, Target Group e Listener

### Load Balancer

**Conceito**
- O load balancer recebe trafego e distribui entre instancias saudaveis
- Tambem centraliza health checks e ajuda a desacoplar cliente de instancia especifica

### Target Group

**Conceito**
- O target group define o conjunto de destinos que vai receber trafego
- Tambem define health checks e a porta/protocolo do backend

### Listener

**Conceito**
- O listener define em qual porta o load balancer escuta e qual acao executa
- Exemplo: `HTTPS 443 -> encaminhar para target group do backend`

### Aplicacao no VidaLongaFlix

O projeto ja trabalha com a cadeia:

```
Cliente
  -> CloudFront
  -> Application Load Balancer
  -> Elastic Beanstalk / EC2
  -> Spring Boot (porta 8090 no container)
```

Portanto, o curso encaixa diretamente em tres ideias:
- o cliente **nao deve** apontar para uma instancia individual
- o health check deve decidir quais instancias recebem trafego
- o balanceamento deve ficar na camada AWS, nao dentro da aplicacao

**Como aplicar na pratica**
- Manter o endpoint de health no Actuator como referencia da saude da app
- Configurar o ALB para validar a aplicacao antes de enviar trafego
- Deixar o CloudFront como borda e proxy para `/api/*`
- Garantir que o backend respeite cabecalhos encaminhados (`X-Forwarded-For`, `X-Forwarded-Proto`)

---

## Modulo 5 — Health Checks, Readiness e Deploy Seguro

### Health Check

**Conceito**
- Health check verifica se o servico esta respondendo
- Em nuvem, ele nao serve apenas para monitorar: ele decide se a instancia fica no balanceamento

### Readiness vs. Liveness

**Conceito**
- **Liveness**: o processo esta vivo?
- **Readiness**: ele esta pronto para atender trafego agora?

### Aplicacao no VidaLongaFlix
- O projeto ja expoe Actuator e health check para o ambiente
- Em producao, o profile `prod` ja restringe os endpoints e ativa probes
- O Terraform deve garantir que o ambiente AWS use esse endpoint como parte do controle operacional

**Por que isso importa**
- Deploy sem health check real pode parecer "verde" mesmo com erro funcional
- Escalabilidade sem health check remove ou adiciona instancias erradas
- Rollback automatico depende de um sinal de saude confiavel

> Um health check ruim e pior que nenhum: cria falsa confianca e mascara falhas.

---

## Modulo 6 — Bootstrap das Maquinas: quando usar e quando evitar

### `user_data` e scripts de bootstrap

**Conceito**
- `user_data` executa comandos na inicializacao de uma instancia
- Pode instalar dependencias, criar arquivos e preparar o ambiente

### O que o curso mostra
- Baixar dependencias
- Instalar ferramentas
- Rodar script de configuracao da maquina

### O que faz sentido no VidaLongaFlix
- Usar bootstrap apenas para configuracoes realmente de infraestrutura
- Evitar instalar aplicacao, dependencias e playbooks mutaveis na subida da EC2

### Motivo
- O projeto ja usa **container Docker versionado**
- O pipeline gera bundle para o **Elastic Beanstalk**
- O deploy ideal aqui e **imutavel**: imagem pronta, variaveis de ambiente, health check e rollout controlado

**Conclusao pratica**
- O conceito de bootstrap e valido
- Mas, neste projeto, a maior parte da configuracao deve ficar em:
  - imagem Docker
  - variaveis do Elastic Beanstalk
  - arquivos do ambiente
  - Terraform para descrever a infra

> Traduzindo o curso para a realidade do repo: menos "configurar a maquina na subida", mais "subir ambiente padronizado e container pronto".

---

## Modulo 7 — Teste de Carga e Calibracao de Escala

### Teste de carga

**Conceito**
- Teste de carga mede o comportamento do sistema sob concorrencia
- Sem esse teste, qualquer numero de auto scaling vira chute

### Ferramenta
- O curso usa **Locust**
- O VidaLongaFlix ja possui **k6**, que cumpre melhor esse papel no estado atual do projeto

### Aplicacao no VidaLongaFlix

O teste de carga ja pode ser usado para responder perguntas reais:
- Quantos usuarios simultaneos uma instancia suporta?
- Em qual ponto a latencia p95 comeca a subir?
- O gargalo esta na CPU, no pool JDBC, no banco ou em I/O?
- O `min` e `max` do ambiente fazem sentido?

### Fluxo operacional recomendado
1. Rodar k6 com o fluxo real do usuario
2. Observar Actuator, Grafana e logs
3. Relacionar p95, erro e CPU
4. Ajustar politica de escala do ambiente
5. Repetir o teste para validar

> Auto scaling sem teste de carga e apenas automacao cega.

---

## Conceitos na Pratica — VidaLongaFlix

### Arquitetura alvo com Terraform + AWS

```
GitHub Actions
   |
   v
Elastic Beanstalk Application / Environment
   |
   +--> Auto Scaling gerenciado pelo EB
   +--> Application Load Balancer
   +--> EC2 rodando container Docker do backend
   |
   v
RDS PostgreSQL (privado)

CloudFront
   |
   +--> S3 (frontend / midia publica quando aplicavel)
   +--> ALB / API do backend

Spring Boot
   |
   +--> Actuator (health / metrics)
   +--> OTLP -> Grafana Cloud
```

### O que o Terraform pode gerenciar

**Camada de rede**
- VPC
- subnets publicas e privadas
- internet gateway e tabelas de rota
- security groups

**Camada de aplicacao**
- Elastic Beanstalk application
- Elastic Beanstalk environment
- configuracoes de capacidade, instancia e rolling updates
- variaveis de ambiente nao sensiveis

**Camada de dados e borda**
- RDS PostgreSQL
- S3
- CloudFront

**Camada de observabilidade e operacao**
- alarmes AWS complementares
- SNS/webhooks
- parametros padronizados por ambiente

### O que nao precisa virar Terraform imediatamente
- toda a logica de deploy do GitHub Actions
- configuracoes internas do Spring Boot
- definicao do fluxo de teste do k6
- dashboards locais de laboratorio

---

## Plano de Implementacao

### Fase 1 — Codificar a infraestrutura existente

Objetivo: tirar do modo manual o que ja existe hoje.

Criar no Terraform:
- VPC e subnets, caso ainda nao estejam versionadas
- security groups de `prod` e `dev`
- RDS PostgreSQL
- bucket S3
- distribuicao CloudFront
- Elastic Beanstalk application e environment

Resultado esperado:
- ambiente reproduzivel
- menor drift entre console e repositorio
- onboarding mais simples

### Fase 2 — Formalizar capacidade e elasticidade

Objetivo: transformar escalabilidade em regra explicita.

Configurar no ambiente:
- minimo de instancias
- maximo de instancias
- tipo de instancia
- health checks
- parametros de rolling deploy

Resultado esperado:
- deploy mais previsivel
- recuperacao automatica melhor definida
- custo sob controle

### Fase 3 — Calibrar com dados reais

Objetivo: parar de escolher thresholds no escuro.

Usar:
- k6 para gerar carga
- Grafana para latencia, erros e traces
- Actuator para metricas internas

Decisoes guiadas por dados:
- se `min=1` ou `min=2`
- em que CPU escalar
- qual teto de instancias e economicamente aceitavel
- se o gargalo real esta no app ou no banco

### Fase 4 — Refinar ambientes

Objetivo: evitar que `dev` e `prod` tenham o mesmo comportamento.

Exemplos:
- `dev`: capacidade minima menor, sem distribuicao publica completa se nao precisar
- `prod`: alta disponibilidade, capacidade minima maior, regras de rede mais rigidas

---

## Exemplo de Estrutura Terraform para o Projeto

```text
infra/
  environments/
    dev/
      main.tf
      variables.tf
      terraform.tfvars
    prod/
      main.tf
      variables.tf
      terraform.tfvars
  modules/
    network/
    security/
    rds/
    s3/
    cloudfront/
    elastic_beanstalk/
```

### Por que essa estrutura faz sentido
- separa ambientes de modulos reutilizaveis
- evita copiar recurso inteiro entre `dev` e `prod`
- permite mudar capacidade por variavel em vez de editar bloco por bloco

---

## Riscos e Cuidados

### 1. Reproduzir o curso literalmente
- Criar EC2, ASG e ALB manualmente fora do Beanstalk pode duplicar responsabilidade
- Isso aumenta complexidade sem necessidade imediata

### 2. Escalar por metrica errada
- CPU alta nem sempre significa experiencia ruim
- Latencia alta com CPU normal pode apontar gargalo em banco, rede ou dependencia externa

### 3. Health check superficial
- Se o endpoint sempre responde `200`, mesmo com dependencia critica indisponivel, o balanceador vai considerar a instancia saudavel por engano

### 4. Bootstrap mutavel
- Quanto mais configuracao dinamica por script em instancia, maior o risco de drift e comportamento inconsistente

### 5. Ambientes sem isolamento real
- `dev` e `prod` precisam ter regras, capacidade e exposicao diferentes

---

## Modulo 8 — ECS, Fargate e Orquestracao de Containers

Este modulo cobre um segundo curso sobre Terraform com foco em ECS (Elastic Container Service) e Fargate. Esses conceitos sao a **evolucao direta** do que o VidaLongaFlix ja usa hoje com Elastic Beanstalk.

---

### ECS Cluster

**Conceito**
- O cluster e o ambiente gerenciado onde os containers vao executar
- No curso e criado via `terraform-aws-modules/ecs`, o modulo oficial da AWS
- O `container_insights = true` habilita metricas e logs dos containers no CloudWatch
- O `capacity_providers = ["FARGATE"]` define que as instancias serao gerenciadas pelo Fargate, nao por EC2 manual

**Aplicacao no VidaLongaFlix**
- Hoje o Elastic Beanstalk abstrai o cluster de forma automatica
- Migrar para ECS explicito via Terraform e a proxima evolucao natural do projeto
- O cluster ECS com Fargate elimina a necessidade de gerenciar EC2: a AWS provisiona capacidade sob demanda

```hcl
module "ecs" {
  source  = "terraform-aws-modules/ecs/aws"

  cluster_name       = var.ambiente
  cluster_configuration = {
    execute_command_configuration = {
      logging = "OVERRIDE"
      log_configuration = {
        cloud_watch_log_group_name = "/ecs/${var.ambiente}"
      }
    }
  }
  fargate_capacity_providers = {
    FARGATE = {
      default_capacity_provider_strategy = {
        weight = 1
      }
    }
  }
}
```

---

### Task Definition

**Conceito**
- A task definition e a receita do container: o que executar, quanto de CPU e memoria usar, qual porta expor e qual IAM role usar
- O `requires_compatibilities = ["FARGATE"]` garante que a task so rode em Fargate
- O `network_mode = "awsvpc"` e obrigatorio para Fargate e da a cada task seu proprio IP na VPC
- O `execution_role_arn` e a role que permite ao ECS baixar a imagem do ECR e publicar logs

**CPU e Memoria no Fargate**
- Valores sao pares validos, nao arbitrarios
- Minimo: 256 CPU units (1/4 vCPU) + 512 MiB de memoria
- Para o VidaLongaFlix (Spring Boot), o minimo razoavel e **512 CPU / 1024 MiB** em producao

```
256 CPU units  = 1/4 vCPU
512 CPU units  = 1/2 vCPU
1024 CPU units = 1 vCPU
```

**Aplicacao no VidaLongaFlix**

A task definition do backend Spring Boot ficaria assim:

```hcl
resource "aws_ecs_task_definition" "vidalongaflix-api" {
  family                   = "vidalongaflix-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  container_definitions = jsonencode([{
    name      = "vidalongaflix-api"
    image     = "${aws_ecr_repository.backend.repository_url}:latest"
    cpu       = 512
    memory    = 1024
    essential = true

    portMappings = [{
      containerPort = 8090
      hostPort      = 8090
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" }
    ]
  }])
}
```

> A porta do VidaLongaFlix e **8090**, nao 8000 como no curso (que usava Django).

---

### Container Definition

**Conceito**
- E o bloco dentro da task definition que descreve o container em si
- `essential = true` significa que se esse container morrer, toda a task e encerrada
- `portMappings` mapeia a porta do container para a porta do host (no Fargate, sao iguais)
- O campo `image` aponta para o repositorio ECR, nao para Docker Hub

**ECR — Elastic Container Registry**
- O curso usa ECR como registro de imagens Docker na AWS
- Hoje o VidaLongaFlix usa **Docker Hub**
- Migrar para ECR e recomendado ao usar ECS: mais seguro, sem rate limit e integrado ao IAM

---

### ECS Service

**Conceito**
- O service e a cola entre o cluster e a task definition
- Define quantas instancias da task devem estar rodando (`desired_count`)
- Linka o container ao ALB via `load_balancer` block
- Garante que se uma instancia cair, ela e resubida automaticamente

**Aplicacao no VidaLongaFlix**

```hcl
resource "aws_ecs_service" "vidalongaflix-api" {
  name            = "vidalongaflix-api"
  cluster         = module.ecs.cluster_id
  task_definition = aws_ecs_task_definition.vidalongaflix-api.arn
  desired_count   = 2  # minimo 2 para alta disponibilidade

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.privado.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "vidalongaflix-api"
    container_port   = 8090
  }
}
```

---

### Network Configuration (obrigatoria no Fargate)

**Conceito**
- Fargate com `network_mode = "awsvpc"` exige `network_configuration` no service
- Os containers ficam em **subnets privadas** e se comunicam com o exterior via ALB
- Os security groups controlam quem pode falar com quem

**Aplicacao no VidaLongaFlix**
- Containers Spring Boot: subnet privada, security group que aceita apenas trafego do ALB na porta 8090
- ALB: subnet publica, aceita HTTPS (443) da internet
- RDS: subnet privada, aceita apenas JDBC (5432) do security group da aplicacao

---

### Capacity Provider Strategy

**Conceito**
- Define quem fornece a capacidade de compute: FARGATE, FARGATE_SPOT ou EC2
- `weight = 1` significa 100% da capacidade nesse provider
- FARGATE_SPOT e mais barato (ate 70%) mas pode ser interrompido — bom para tarefas batch, arriscado para API

**Aplicacao no VidaLongaFlix**
- Producao: `FARGATE` com `weight = 1` — previsivel, sem interrupcoes
- Futuro: mistura FARGATE (70%) + FARGATE_SPOT (30%) para reduzir custo em cargas nao criticas

---

### Elastic Beanstalk vs ECS + Fargate

Esta e a decisao arquitetural central ao aplicar este curso ao VidaLongaFlix:

| Criterio | Elastic Beanstalk (hoje) | ECS + Fargate (evolucao) |
|---|---|---|
| Complexidade | Baixa | Media |
| Controle de rede | Limitado | Total (awsvpc, SG granular) |
| Visibilidade do container | Via EB health | Container Insights + Actuator |
| Deploy | GitHub Actions → EB bundle | GitHub Actions → ECR → ECS |
| Custo | t3.small ~$15/mes | 512 CPU / 1 GB ~$8-12/mes |
| Escalabilidade | EB gerencia | ECS Service Auto Scaling |
| Terraform | Modulo `elastic_beanstalk` | Modulo `ecs` nativo |

**Recomendacao**: o Elastic Beanstalk serve bem enquanto o projeto cresce. A migracao para ECS + Fargate faz sentido quando:
- For necessario controle granular de rede e seguranca
- O time quiser adotar multi-servico (ex: separar API de worker)
- O custo do EB nao justificar mais o nivel de controle que ele oferece

> Nao e necessario migrar tudo de uma vez. E possivel rodar um novo servico (ex: worker de notificacoes) diretamente no ECS enquanto a API continua no EB.

---

### Plano de Implementacao — Fase 5: Migracao para ECS + Fargate

Objetivo: substituir o Elastic Beanstalk pelo ECS + Fargate, ganhando controle total sobre a infraestrutura de containers.

**Passos**
1. Criar repositorio ECR para a imagem do backend
2. Adaptar o pipeline GitHub Actions para fazer `docker push` no ECR em vez do Docker Hub
3. Criar o cluster ECS com o modulo `terraform-aws-modules/ecs`
4. Criar a IAM role de execucao com permissao de leitura no ECR e escrita no CloudWatch
5. Criar a task definition com CPU 512 / MiB 1024, porta 8090, imagem do ECR
6. Criar o ECS service com `desired_count = 2`, subnets privadas e target group do ALB
7. Validar health check via `/actuator/health` no target group
8. Desativar o ambiente Elastic Beanstalk somente apos validacao completa em producao

---

## Modulo 9 — Confiabilidade, Metricas Avancadas e Alertas

Este modulo cobre conceitos de um curso de confiabilidade com Grafana. O VidaLongaFlix ja tem ~70% do que o curso ensina implementado. O que esta documentado aqui e o que ainda nao existe no projeto e tem valor real para aplicar.

---

### Metodos RED e USE

**Conceito**

Dois frameworks para saber **o que medir** e nao ficar olhando para metricas aleatorias.

**Metodo RED** — para servicos (APIs, microservicos):
- **R**ate — quantas requisicoes por segundo estao chegando
- **E**rrors — quantas estao falhando (4xx/5xx)
- **D**uration — quanto tempo cada requisicao leva (foco em p95 e p99)

**Metodo USE** — para recursos de infraestrutura (CPU, memoria, disco):
- **U**tilization — qual percentual do recurso esta sendo usado
- **S**aturation — o recurso esta na fila ou estrangulado?
- **E**rrors — ha falhas no recurso (ex: timeouts de pool, GC excessivo)?

**Estado atual do VidaLongaFlix**

| Sinal | Dashboard atual | Status |
|---|---|---|
| Rate (RPS) | golden-signals | ✅ |
| Errors (taxa de erro) | golden-signals | ✅ |
| Duration (latencia p95) | golden-signals | ✅ |
| CPU Utilization | jvm-backend | ✅ |
| Memory heap / non-heap | jvm-backend | ✅ |
| JDBC Pool (Saturation) | nenhum | ❌ faltando |
| Auth Errors (negocio) | nenhum | ❌ faltando |

---

### Metricas de JDBC Pool (HikariCP)

**Conceito**
- O Spring Boot usa HikariCP como pool de conexoes com o banco
- O pool tem um numero maximo de conexoes simultaneas (padrao: 10)
- Se todas as conexoes estiverem ocupadas, requisicoes ficam em fila
- Quando a fila estoura, o usuario recebe timeout — sem aviso no dashboard atual

**Metricas disponiveis via Micrometer (ja expostas pelo Actuator)**

| Metrica | O que representa |
|---|---|
| `hikaricp.connections.active` | Conexoes em uso agora |
| `hikaricp.connections.idle` | Conexoes livres no pool |
| `hikaricp.connections.pending` | Requisicoes esperando conexao |
| `hikaricp.connections.timeout.total` | Total de timeouts de pool (critico) |
| `hikaricp.connections.max` | Tamanho maximo configurado do pool |

**Por que importa**
- Se `active` chegar perto de `max` e `pending` > 0, o banco virou gargalo
- Isso aparece como latencia alta na API — sem o painel, e dificil saber a causa

**Passo a passo para aplicar**

1. Abrir o dashboard `jvm-backend` no Grafana Cloud
2. Adicionar novo painel com a query abaixo (Prometheus/Mimir):

```promql
# Conexoes ativas no pool
hikaricp_connections_active{service_name="NutriLongaVidaFlix"}

# Conexoes pendentes (alarme se > 0 por mais de 30s)
hikaricp_connections_pending{service_name="NutriLongaVidaFlix"}

# Tamanho maximo do pool
hikaricp_connections_max{service_name="NutriLongaVidaFlix"}
```

3. Criar painel de gauge mostrando `active / max` como percentual de uso do pool
4. Salvar o dashboard

> Se as metricas nao aparecerem, verificar se `spring.datasource.hikari.pool-name=HikariPool-1` esta configurado, pois o Micrometer usa o nome do pool como label.

---

### Metricas de Negocio com MeterRegistry

**Conceito**
- Metricas de JVM e HTTP sao geradas automaticamente pelo Micrometer
- Metricas de negocio precisam ser adicionadas manualmente no codigo
- Exemplos uteis para o VidaLongaFlix: erros de autenticacao, usuarios logados, videos acessados

**Passo a passo para aplicar**

1. Injetar `MeterRegistry` no service de autenticacao:

```java
@Service
public class AuthService {

    private final Counter authErrorCounter;

    public AuthService(MeterRegistry meterRegistry) {
        this.authErrorCounter = Counter.builder("auth.errors")
            .description("Total de tentativas de login com falha")
            .tag("tipo", "credencial_invalida")
            .register(meterRegistry);
    }

    public void falhaDeAutenticacao() {
        authErrorCounter.increment();
        // resto da logica
    }
}
```

2. A metrica `auth_errors_total` aparece automaticamente no `/actuator/prometheus` e e enviada ao Grafana Cloud
3. Criar painel no Grafana com:

```promql
# Taxa de erros de autenticacao por minuto
rate(auth_errors_total{service_name="NutriLongaVidaFlix"}[1m])
```

4. Adicionar alerta: se taxa de erros de auth > 5/min por 2 minutos, pode ser ataque de brute-force

**Outras metricas de negocio uteis**

| Metrica | Onde adicionar |
|---|---|
| `video.views` | Controller de acesso ao video |
| `user.registrations` | Service de cadastro |
| `plan.activations` | Service de planos/assinaturas |

---

### Alertas via E-mail no Grafana Cloud

**Conceito**
- O VidaLongaFlix ja tem 2 SLOs com burn rate alerts configurados
- O que falta e definir o **destino** do alerta: para onde vai a notificacao quando disparar
- Sem destino configurado, o alerta existe mas nao avisa ninguem

**Passo a passo para configurar e-mail**

1. Acessar `vidalongaflix.grafana.net`
2. Menu lateral → **Alerting** → **Contact points**
3. Clicar em **Add contact point**
4. Preencher:
   - Name: `email-fabricio`
   - Integration: **Email**
   - Addresses: seu e-mail
5. Clicar em **Test** para confirmar que chega o e-mail
6. Salvar

**Vincular o contact point aos alertas existentes**

1. Menu → **Alerting** → **Notification policies**
2. Na politica `Default policy`, clicar em **Edit**
3. Em **Default contact point**, selecionar `email-fabricio`
4. Salvar

A partir daqui, quando qualquer alerta dos SLOs disparar, voce recebe e-mail automaticamente.

**Alerta recomendado para JDBC Pool (apos criar o painel)**

1. No painel de conexoes pendentes, clicar em **Edit**
2. Aba **Alert**
3. Condicao: `hikaricp_connections_pending > 0` por mais de 2 minutos
4. Vincular ao contact point `email-fabricio`

---

### Revisao dos dashboards atuais com RED e USE

**Checklist para aplicar agora**

Abrir cada dashboard e verificar:

**golden-signals** — metodo RED
- [ ] Rate (RPS): painel existe?
- [ ] Errors (% de erro): painel existe?
- [ ] Duration (p95 e p99): painel existe?
- [ ] Saturation (CPU > 80%): painel existe?

**jvm-backend** — metodo USE
- [ ] CPU Utilization: painel existe?
- [ ] Memory heap utilization (% de uso): painel existe?
- [ ] JDBC pool saturation: **adicionar** (passo descrito acima)
- [ ] GC pause duration: painel existe?

Qualquer painel faltando pode ser adicionado com as queries Prometheus correspondentes.

---

### Resumo do que aplicar (ordem de prioridade)

| Prioridade | Acao | Esforco |
|---|---|---|
| 1 | Configurar e-mail como destino dos alertas | 5 min |
| 2 | Adicionar painel HikariCP no jvm-backend | 15 min |
| 3 | Criar alerta para JDBC pool pending > 0 | 10 min |
| 4 | Adicionar metrica `auth.errors` no AuthService | 30 min |
| 5 | Revisar dashboards com checklist RED/USE | 30 min |

---

## Modulo 10 — Terraform Data Sources

**Origem**: curso de EKS/Kubernetes — unico conceito aplicavel ao VidaLongaFlix daquele curso.

### Conceito

Data Sources permitem que o Terraform **leia** recursos que ja existem na AWS sem recria-los ou destrui-los.

Sem Data Sources, adotar Terraform em producao existente e perigoso: ao tentar criar VPC, RDS ou EB que ja existem, o Terraform geraria conflito ou recriaria tudo do zero — derrubando producao.

Com Data Sources, o Terraform apenas referencia o que ja existe:

```hcl
# Le a VPC existente pelo nome
data "aws_vpc" "current" {
  filter {
    name   = "tag:Name"
    values = ["vidalongaflix-vpc"]
  }
}

# Le o RDS existente sem tocar nele
data "aws_db_instance" "postgres" {
  db_instance_identifier = "vidalongaflix-prod"
}

# Le o bucket S3 existente
data "aws_s3_bucket" "media" {
  bucket = "vidalongaflix-media"
}

# Usa os dados em novos recursos
resource "aws_security_group" "app" {
  vpc_id = data.aws_vpc.current.id
}
```

### Por que importa para o VidaLongaFlix

A Fase 1 do plano Terraform e "codificar a infraestrutura existente". O problema e que tudo ja existe: EB, RDS, VPC, S3, CloudFront. Sem Data Sources, nao ha como fazer isso com seguranca.

### Passo a passo para aplicar

1. Criar pasta `infra/environments/prod/` no repositorio
2. Criar `data.tf` com Data Sources para cada recurso existente:
   - `data "aws_vpc"` — VPC do projeto
   - `data "aws_db_instance"` — RDS PostgreSQL
   - `data "aws_s3_bucket"` — bucket de midia
   - `data "aws_elastic_beanstalk_environment"` — ambiente EB prod
3. Rodar `terraform init` e `terraform plan` — deve mostrar **0 resources to add, 0 to destroy**
4. A partir daqui, novos recursos podem referenciar os existentes com seguranca

> Data Sources sao a porta de entrada segura do Terraform em producao ja existente.

---

## Modulo 11 — PromQL: Tipos de Metricas e Queries

**Origem**: curso de PromQL/Prometheus — modulos 3 e 4 aplicaveis.

### Conceito: os 4 tipos de metrica

| Tipo | Comportamento | Exemplo no VidaLongaFlix |
|---|---|---|
| **Counter** | So cresce — total acumulado desde o inicio | Total de requisicoes HTTP, erros de auth |
| **Gauge** | Sobe e desce — valor atual | Conexoes ativas no pool, memoria usada |
| **Histogram** | Distribui valores em buckets — usado para percentis | Latencia p95, p99 das requisicoes |
| **Summary** | Percentis calculados no client-side | Menos usado no Micrometer/Spring Boot |

**Por que importa**: saber o tipo da metrica define qual funcao PromQL usar. Aplicar `rate()` em um Gauge ou `avg()` em um Counter gera resultado incorreto.

### Funcoes PromQL essenciais

```promql
# rate() — taxa por segundo de um Counter em uma janela de tempo
# Usar para: RPS, taxa de erros, taxa de auth failures
rate(http_server_requests_seconds_count{service_name="NutriLongaVidaFlix"}[5m])

# increase() — quanto um Counter cresceu na janela
# Usar para: total de erros na ultima hora
increase(http_server_requests_seconds_count{status="500"}[1h])

# histogram_quantile() — calcula percentil a partir de buckets
# Usar para: latencia p95, p99
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{service_name="NutriLongaVidaFlix"}[5m])
)

# avg_over_time() — media de um Gauge na janela
# Usar para: media de CPU, media de conexoes ativas
avg_over_time(hikaricp_connections_active[10m])
```

### Seletores e labels

```promql
# Filtrar por label exato
http_server_requests_seconds_count{status="500"}

# Filtrar por label com regex
http_server_requests_seconds_count{uri=~"/api/videos.*"}

# Excluir label
http_server_requests_seconds_count{status!="200"}
```

### Passo a passo para aplicar

1. Abrir Grafana Cloud → menu **Explore** → datasource **Mimir**
2. Praticar as queries abaixo para o VidaLongaFlix:

```promql
# RPS atual
rate(http_server_requests_seconds_count{service_name="NutriLongaVidaFlix"}[2m])

# Taxa de erros 5xx
rate(http_server_requests_seconds_count{service_name="NutriLongaVidaFlix", status=~"5.."}[5m])

# Latencia p95
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{service_name="NutriLongaVidaFlix"}[5m])
)

# Uso de memoria heap em %
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

3. Quando uma query retornar o resultado esperado, clicar em **Add to dashboard** para criar painel
4. Usar esse conhecimento para criar o painel de HikariCP descrito no Modulo 9

---

## Modulo 12 — LogQL: Queries em Logs

**Origem**: curso de Grafana Loki — modulos 4 e 5 aplicaveis.

### Conceito

LogQL e a linguagem de query do Loki — o equivalente do PromQL para logs. Voce ja tem logs chegando no Loki via OpenTelemetry. LogQL e o que permite extrair valor desses logs.

### Estrutura de uma query LogQL

```
{seletores}  |  filtros  |  funcoes
```

### Seletores de stream — selecionar quais logs ver

```logql
# Todos os logs do backend
{service_name="NutriLongaVidaFlix"}

# So logs de nivel ERROR
{service_name="NutriLongaVidaFlix"} | logfmt | level="ERROR"

# Logs de um endpoint especifico
{service_name="NutriLongaVidaFlix"} |= "/api/videos"
```

### Filtros de conteudo

```logql
# Contem texto
{service_name="NutriLongaVidaFlix"} |= "Exception"

# Nao contem texto
{service_name="NutriLongaVidaFlix"} != "actuator"

# Expressao regular
{service_name="NutriLongaVidaFlix"} |~ "user.*login"
```

### Queries de metrica (transformar logs em numeros)

```logql
# Taxa de logs ERROR por minuto
rate({service_name="NutriLongaVidaFlix"} | logfmt | level="ERROR" [1m])

# Total de erros na ultima hora
count_over_time({service_name="NutriLongaVidaFlix"} |= "ERROR" [1h])

# Buscar log de uma requisicao pelo traceId
{service_name="NutriLongaVidaFlix"} |= "traceId=abc123def456"
```

### Passo a passo para aplicar

1. Abrir Grafana Cloud → menu **Explore** → datasource **Loki**
2. Praticar as queries acima trocando o service_name por `NutriLongaVidaFlix`
3. Criar painel de logs de ERROR no dashboard `sli-disponibilidade`:
   - Query: `{service_name="NutriLongaVidaFlix"} | logfmt | level="ERROR"`
   - Tipo de visualizacao: **Logs**
4. Criar alerta baseado em taxa de ERROR:
   - Query: `rate({service_name="NutriLongaVidaFlix"} | logfmt | level="ERROR" [5m])`
   - Condicao: valor > 0.1 (mais de 6 erros por minuto)
   - Contact point: `email-fabricio`

---

## Modulo 13 — Troubleshooting com Traces no Tempo

**Origem**: curso de Tracing com Jaeger/OpenTelemetry — conceito de troubleshooting aplicavel ao Grafana Cloud Tempo.

### Conceito

Traces registram o caminho completo de uma requisicao no sistema — do momento que entra no backend ate a resposta. Cada operacao interna (chamada ao banco, metodo de servico, chamada externa) vira um **span**.

O VidaLongaFlix ja envia traces para o Grafana Cloud Tempo com `micrometer-tracing-bridge-otel`. O que falta e usar esses traces para investigar problemas.

### Anatomia de um trace

```
Requisicao HTTP POST /api/auth/login        <- span raiz (root span)
  └── AuthService.authenticate()            <- span filho
        └── UserRepository.findByEmail()    <- span filho (query SQL)
```

Cada span contem:
- duracao da operacao
- status (ok ou erro)
- atributos (ex: `http.url`, `db.statement`, `http.status_code`)
- traceId — identificador unico que liga todos os spans da mesma requisicao

### Como usar o Tempo para investigar incidentes

**Cenario 1: requisicao lenta**
1. Abrir Grafana Cloud → **Explore** → datasource **Tempo**
2. Em **Search**, filtrar: `service.name = "NutriLongaVidaFlix"`
3. Ordenar por **Duration** (decrescente)
4. Clicar no trace mais lento
5. No waterfall, identificar qual span esta consumindo mais tempo
6. Se o span lento e `db.query` → gargalo no banco
7. Se o span lento e no servico → gargalo na logica de negocio

**Cenario 2: erro 500**
1. Filtrar por `http.status_code = 500`
2. Clicar no trace com erro
3. O span com erro aparece em vermelho
4. Ler o atributo `exception.message` para ver a causa

**Cenario 3: correlacionar log com trace**
1. Ver um log de erro no Loki com `traceId=abc123`
2. Copiar o traceId
3. Ir no Explore → Tempo
4. Colar o traceId no campo **Trace ID**
5. Ver o trace completo daquela requisicao que gerou o log de erro

### Passo a passo para configurar correlacao Loki → Tempo

1. Abrir Grafana Cloud → **Administration** → **Data sources** → **Loki**
2. Em **Derived fields**, adicionar:
   - Name: `TraceID`
   - Regex: `traceId=(\w+)`
   - Query: `${__value.raw}`
   - URL: selecionar datasource Tempo
3. Salvar
4. Agora cada log que tiver `traceId=...` vai mostrar um link direto para o trace no Tempo

### Passo a passo para inspecionar traces periodicamente

1. Toda semana, abrir Tempo e filtrar as 10 requisicoes mais lentas
2. Verificar se algum span de banco esta acima de 500ms
3. Se sim, revisar a query SQL correspondente
4. Comparar semana a semana — latencia crescendo = sinal de degradacao

---

## Resumo Geral

| Conceito do curso | Como traduz no VidaLongaFlix |
|---|---|
| Launch Template | Conceito valido, mas abstraido pelo Elastic Beanstalk |
| Auto Scaling Group | Aplicado via capacidade do ambiente Beanstalk |
| Availability Zones | Distribuir ambiente entre duas AZs |
| Subnets | Publicas para app/ALB, privadas para RDS |
| Security Groups | Segregar trafego entre borda, app e banco |
| Load Balancer | ALB gerenciado na frente do backend |
| Target Group / Listener | Parte da configuracao do trafego para a API |
| `user_data` | Usar com cautela; preferir container imutavel |
| Teste de carga | Ja aplicavel via k6 |
| Escala por CPU | Bom inicio, mas deve ser validado com latencia e erro |
| Terraform | Forte candidato para virar a fonte de verdade da AWS |
| ECS Cluster | Evolucao do EB: cluster gerenciado pelo Fargate via Terraform |
| Task Definition | Receita do container Spring Boot (porta 8090, CPU/MiB, IAM role) |
| Container Definition | Imagem ECR, portMappings, essential, limites de recurso |
| ECS Service | Cola entre cluster e task; linka ALB, desired_count, network |
| Network Configuration | awsvpc obrigatorio no Fargate; subnets privadas + SG |
| Capacity Provider | FARGATE weight=1 em prod; FARGATE_SPOT opcional para workers |
| ECR | Registro Docker AWS; substitui Docker Hub ao migrar para ECS |
| Metodo RED | Rate, Errors, Duration — framework para metricas de API |
| Metodo USE | Utilization, Saturation, Errors — framework para recursos |
| HikariCP metrics | Painel de pool JDBC: active, pending, timeout — gargalo de banco |
| Metricas de negocio | MeterRegistry para auth errors, views, registros |
| Alertas por e-mail | Contact point no Grafana Cloud para receber alertas dos SLOs |
| Terraform Data Sources | Ler infra existente sem recriar — porta de entrada segura do Terraform |
| Counter / Gauge / Histogram | 4 tipos de metrica — define qual funcao PromQL usar |
| rate() / histogram_quantile() | Funcoes PromQL para RPS, latencia p95, taxa de erro |
| Seletores LogQL | Filtrar logs por nivel, texto, traceId no Loki |
| rate() / count_over_time() | Transformar logs em metricas de alerta no LogQL |
| Span / Waterfall / TraceID | Anatomia do trace — identifica gargalo e correlaciona com log |
| Correlacao Loki + Tempo | Link direto de log de erro para o trace da requisicao |

---

## Conclusao

Os cursos avaliados cobrem tres grandes areas que se encaixam no VidaLongaFlix:

**Infraestrutura como codigo**
- Terraform, VPC, subnets, security groups, RDS, S3, EB, ECS, Fargate
- Data Sources para adocao segura sem recriar o que ja existe

**Observabilidade — pilares de metricas e logs**
- Metodos RED e USE para saber o que medir
- PromQL para escrever queries de metricas no Grafana Cloud
- LogQL para escrever queries de logs no Loki
- HikariCP, metricas de negocio, alertas por e-mail

**Observabilidade — traces e investigacao de incidentes**
- Anatomia de trace: span, waterfall, traceId
- Troubleshooting com Grafana Cloud Tempo
- Correlacao entre log de erro e trace da requisicao

O ajuste necessario e de implementacao:
- nos cursos, a infraestrutura e montada para Django/Python com Jaeger e Prometheus self-hosted
- no VidaLongaFlix, a aplicacao e Spring Boot Java, porta 8090, com Grafana Cloud (Mimir, Loki, Tempo)

Portanto, a melhor evolucao para este projeto nao e abandonar o que ja existe, mas sim:
1. **aplicar PromQL e LogQL para ganhar autonomia no Grafana Cloud**
2. **adicionar painel HikariCP e metrica auth.errors**
3. **configurar e-mail como destino dos alertas existentes**
4. **codificar a infraestrutura atual com Terraform via Data Sources**
5. **calibrar auto scaling com k6 + observabilidade**
6. **migrar para ECS + Fargate quando o controle granular justificar o esforco**

> Os cursos nao entram como copia fiel. Entram como base conceitual para organizar a infraestrutura e a observabilidade do VidaLongaFlix de forma mais madura — usando o que ja existe como ponto de partida.

---

*Documento criado em: abril de 2026*
