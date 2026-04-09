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

---

## Conclusao

O curso se encaixa bem no VidaLongaFlix quando lido no nivel de **conceitos de plataforma**:
- elasticidade
- balanceamento
- health check
- zonas de disponibilidade
- segmentacao de rede
- teste de carga
- infraestrutura como codigo

O ajuste necessario e de implementacao:
- no curso, a infraestrutura e montada com recursos EC2 de forma direta
- no VidaLongaFlix, a aplicacao ja usa **Elastic Beanstalk**, **Docker**, **CloudFront**, **RDS** e **Grafana Cloud**

Portanto, a melhor evolucao para este projeto nao e abandonar o que ja existe, mas sim:
1. **codificar a infraestrutura atual com Terraform**
2. **formalizar capacidade e auto scaling no ambiente**
3. **calibrar esses limites com k6 + observabilidade**

> Em resumo: o curso nao entra como copia fiel. Ele entra como base conceitual para organizar a infraestrutura real do VidaLongaFlix de forma mais madura.

---

*Documento criado em: abril de 2026*
