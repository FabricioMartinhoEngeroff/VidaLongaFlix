# SRE Observability — Metrics, Logs, Traces and Grafana

## Objetivos
- Integrar métricas, logs e traces para obter uma visão completa do desempenho dos sistemas
- Identificar e priorizar sinais críticos usando Golden Signals e percentis para reduzir o impacto de incidentes
- Implementar health checks, liveness e readiness para monitorar a saúde de serviços em produção
- Aplicar tracing de ponta a ponta em arquiteturas de microserviços para rastrear transações entre serviços
- Desenvolver dashboards e configurar alertas no Grafana para transformar métricas em ações operacionais
- Utilizar OpenTelemetry e profiling contínuo para coletar dados e otimizar a performance de aplicações

---

## Módulo 1 — Introdução à Observabilidade e Monitoramento

### Monitoramento
- Acompanha **problemas já conhecidos**: CPU, memória, disco, processos críticos.
- Abordagem **reativa** — age sobre sintomas já identificados.
- Foco em componentes individuais e infraestrutura tradicional.
- Objetivo: **informar que algo não está funcionando**.
- Exemplo: alertar quando memória ultrapassa 95% para evitar impacto no sistema.

### Observabilidade
- Investiga **problemas desconhecidos** a partir das saídas do sistema (métricas, logs, traces).
- Abordagem **preditiva** — identifica tendências antes do colapso.
- Foco em sistemas distribuídos, microserviços e ambientes de nuvem.
- Objetivo: **entender causa e efeito** de uma falha.
- Mais próxima do usuário — evidencia o impacto real sofrido.

### Diferença central
| | Monitoramento | Observabilidade |
|---|---|---|
| Foco | Problemas conhecidos | Problemas desconhecidos |
| Abordagem | Reativa | Preditiva |
| Escopo | Componentes isolados | Sistema distribuído |
| Pergunta | "O quê está errado?" | "Por quê está errado?" |

> A observabilidade **abraça** o monitoramento — ele é a base, ela é a camada externa mais próxima do usuário.

### Ciclo de Observabilidade
1. **Alerta** — limite (threshold) extrapolado (ex: tempo de resposta > 500ms)
2. **Trace** — rastrear o caminho percorrido pela requisição
3. **Métricas + Logs** — identificar o serviço lento e o impacto na rede
4. **Decisão** — dados suficientes para convocar o time e implementar solução
5. **Validação** — manter a métrica ativa para garantir que regressões não passem despercebidas

### Threshold (Limite)
- Ciclo: **criar → observar → validar → ajustar**
- Se o limite de 500ms é crítico, alertar já aos 400–450ms
- Nunca remover uma métrica sem validar que o problema foi realmente resolvido

---

## Módulo 2 — Pilares da Observabilidade

### Os três pilares (obrigatoriamente integrados)

**1. Métricas**
- Dados quantitativos ao longo do tempo (CPU, memória, latência, taxa de erros)
- Base para alertas e análise de tendências — ex: P95 alto indica degradação de desempenho
- Sozinhas, podem não fornecer contexto suficiente (ping OK + CPU OK ≠ usuário OK)
- Úteis para evidenciar anomalias em grandes volumes de dados

**2. Logs**
- Registros textuais de eventos do sistema e infraestrutura
- Contexto rico, mas podem ser volumosos em situações de erro (muitos registros por tentativa)
- Problema comum: logs mal categorizados (ex: erro registrado como `debug`) dificultam o diagnóstico
- Boas práticas: categorizar corretamente (`ERROR`, `WARN`, `INFO`, `DEBUG`) e anexar ao trace

**3. Traces (Rastreamentos)**
- Rastreiam toda a jornada de uma requisição de ponta a ponta entre microserviços
- Cada span representa uma etapa: chamada de banco, serviço externo, fila, etc.
- **Span Attributes**: informações sobre o microserviço (o quê aconteceu, onde)
- **Resources**: onde o serviço está alocado — detalhes de infraestrutura
- Correlacionam logs e métricas, permitindo ver o que aconteceu exatamente durante aquela requisição

> **Atenção:** ter apenas um ou dois pilares isolados não é observabilidade. Os três devem estar integrados e correlacionados.

### Fluxo prático de investigação

Exemplo: alerta de alta latência no microserviço de checkout

1. **Métrica** dispara alerta — latência P95 acima de 500ms
2. Abrir **trace** — identificar os spans mais lentos e o microserviço em comum
3. Verificar **logs** anexados ao trace — buscar erros de timeout, falhas externas
4. Analisar **atributos** e **resources** do span suspeito
5. Determinar se a anomalia faz sentido para a regra de negócio ou pode ser ignorada
6. Definir threshold de alerta (ex: alertar a partir de 400ms se o crítico é 500ms)

### Golden Signals (Google SRE)

Os quatro sinais de ouro para análise de sistemas em produção:

| Sinal | O que medir | Exemplo |
|---|---|---|
| **Latência** | Tempo de processamento de uma requisição | P95 da API de checkout |
| **Tráfego** | Volume de dados/requisições no sistema | 2 tx às 3h vs 1 milhão no dia do pagamento |
| **Erros** | Taxa de falhas (5xx, 4xx relevantes, exceções) | Aumento de 8 para 16 mil erros |
| **Saturação** | Quão cheios estão os recursos | Disco 80%, fila acumulando |

> Erros 4xx de cliente (formulário incorreto, recurso inexistente) podem ser excluídos das métricas de erro do sistema para focar nos problemas reais.

### RED e USE (frameworks complementares)

**RED** — foco na saúde dos **serviços**:
- **R**ate — taxa de requisições por segundo
- **E**rrors — taxa de erros
- **D**uration — tempo de resposta

**USE** — foco na saúde da **infraestrutura**:
- **U**tilization — uso do recurso (CPU, memória, disco)
- **S**aturation — quão sobrecarregado está
- **E**rrors — erros de infraestrutura

### Maturidade em Observabilidade
- **Nível 1 — Monitoramento base:** parque de máquinas monitorado, alertas básicos ativos
- **Nível 2 — Instrumentação:** métricas, traces e logs coletados de todas as aplicações
- **Nível 3 — Observabilidade real:** método e processo para analisar os dados — sem isso, ter os três pilares ainda gera falta de visibilidade

### Tomada de Decisão baseada em dados
- Decisões precisam ser baseadas em dados, não em intuição
- Fluxo: **dados → insights → decisões → otimização de processos**
- Processos otimizados são alinhados com regras de negócio e monitorados continuamente

---

## Módulo 3 — Monitoramento e Diagnóstico de Serviços

### Profiling Contínuo
- Identifica consumo de recursos que **não é visível nas métricas tradicionais**
- Exemplo: CPU atingindo 100% de carga → travamento → reinicialização silenciosa do serviço
- Permite descobrir se o problema é **mal dimensionamento** da aplicação ou falha subjacente
- Curiosidade e investigação são essenciais — nunca aceitar "reiniciou e voltou" como resposta

### Health Checks
- Verificam se o serviço está **vivo** (liveness) e **pronto para receber tráfego** (readiness)
- **Liveness**: o processo está rodando? Se não, reinicia o container
- **Readiness**: o serviço está pronto para responder? Se não, remove do load balancer
- Monitorar: tempo de resposta dos endpoints de health, verificações externas de latência

**Armadilha comum:** health checks mal configurados causam reinicializações desnecessárias que **mascaram o problema real** em vez de resolvê-lo. A automação ajuda, mas não substitui investigar a causa raiz.

### Causa Raiz vs. Sintoma
- Reinicialização automática resolve o sintoma, mas o problema persiste e **vira retrabalho**
- Cada alerta recorrente compromete os indicadores de serviço: **SLI → SLO → SLA**
- MTTR (Mean Time to Recovery) alto = problemas não resolvidos na raiz

### SLI, SLO, SLA e Error Budget

| Conceito | Significado | Exemplo |
|---|---|---|
| **SLI** (Service Level Indicator) | Métrica real medida | 99,2% das requisições com < 300ms |
| **SLO** (Service Level Objective) | Meta interna de desempenho | 99,5% das requisições com < 300ms |
| **SLA** (Service Level Agreement) | Contrato com o cliente | 99% de disponibilidade mensal |
| **Error Budget** | Margem tolerada para falhas | SLO 99,5% → 0,5% de budget para incidentes |

- Antes de um deploy, verificar se há **error budget disponível**
- Se o budget estiver esgotado, o deploy deve ser bloqueado ou adiado

### Kubernetes: verificando saúde dos pods

```bash
# Estado dos pods no namespace atual
kubectl get pods

# Estado de todos os pods em todos os namespaces
kubectl get pods -A
```

- Em microserviços, monitorar o estado dos pods é parte do health check de infraestrutura
- Pod em `CrashLoopBackOff` ou `OOMKilled` indica problema real que precisa ser investigado, não apenas reiniciado

---

## Módulo 4 — Observabilidade com OpenTelemetry e Grafana

### Sistemas modernos e observabilidade
- Um sistema moderno = front-end + back-end + banco + gateway + infraestrutura
- Observabilidade não é só infraestrutura — cobre toda a jornada do usuário
- Em **monolitos**: complexidade menor; em **microserviços**: cada serviço tem seu próprio banco e inter-relações — a complexidade cresce exponencialmente

### Open Source vs. Proprietário

| | Open Source | Proprietário (ex: Datadog, New Relic) |
|---|---|---|
| Custo inicial | Baixo | Alto (variável por volume) |
| Controle dos dados | Total | Limitado |
| Curva de aprendizado | Alta | Baixa |
| Personalização | Alta | Limitada |
| Suporte | Comunidade | Fornecedor |
| Risco | Dependência da equipe | Lock-in do fornecedor |

> A ferramenta é apenas um meio. A escolha depende do capital disponível, maturidade da equipe e disposição para operar a infraestrutura.

### Stack de Observabilidade com OpenTelemetry + Grafana

```
Aplicação (Node/Java/etc.)
    └── OpenTelemetry SDK (traces, métricas, logs)
            └── OpenTelemetry Collector (OTLP — portas 4317 gRPC / 4318 HTTP)
                    ├── Grafana Mimir   → métricas (remote write, escalável vs Prometheus)
                    ├── Grafana Tempo   → traces
                    └── Grafana Loki    → logs
                            └── Grafana (UI: dashboards, alertas, Explorer, drilldown)
```

**OpenTelemetry Collector** — papel central:
- Recebe métricas, traces e logs via OTLP
- Processa: filtra dados sensíveis, aplica regex, enriquece atributos, cria métricas combinadas
- Distribui para múltiplos destinos (Jaeger, Datadog, etc.) — apenas troca os exporters
- Agnóstico de vendor

**Grafana Mimir** — alternativa escalável ao Prometheus:
- Modo monolítico e modo distribuído (compactação, armazenamento de longo prazo)
- Recomendado para alta carga e múltiplos sistemas

### Instalação do OpenTelemetry (Node.js)

```bash
npm i @opentelemetry/sdk-node \
    @opentelemetry/auto-instrumentations-node \
    @opentelemetry/exporter-trace-otlp-grpc \
    @opentelemetry/exporter-metrics-otlp-grpc \
    @opentelemetry/resources \
    @opentelemetry/semantic-conventions
```

- `auto-instrumentations-node` captura automaticamente HTTP, banco de dados, filas, etc.
- Cada requisição vira um **trace** composto de **spans** (etapas)

### Anatomia de um Trace

```json
[
  {"trace_id":"6a7a23cd...","span":"REQUEST_RECEIVED", "service":"ecommerce-api","duration_ms":3,  "status":"OK"},
  {"trace_id":"6a7a23cd...","span":"CATALOG_LOOKUP",   "service":"ecommerce-api","duration_ms":12, "status":"OK"},
  {"trace_id":"6a7a23cd...","span":"PRICING",          "service":"pricing",      "duration_ms":11, "status":"OK"},
  {"trace_id":"6a7a23cd...","span":"PAYMENT_AUTH",     "service":"payments",     "duration_ms":54, "status":"OK"},
  {"trace_id":"6a7a23cd...","span":"RESPONSE_SENT",    "service":"ecommerce-api","duration_ms":2,  "status":"OK"}
]
```

| Campo | Significado |
|---|---|
| `trace_id` | Identificador único da requisição |
| `span` | Ação executada nessa etapa |
| `service` | Microserviço responsável |
| `duration_ms` | Latência da etapa |
| `status` | OK ou erro |

> O Collector enriquece os spans com contexto de infraestrutura (servidor, cluster, namespace).

### Boas Práticas para Logs

- Categorizar corretamente: `ERROR`, `WARN`, `INFO`, `DEBUG`
- **Recomendação**: enviar para a stack apenas `WARN` e `ERROR` em produção
- `DEBUG` ligado em produção = arquivos crescendo no servidor + custo alto de ingestão
- Documentar códigos de erro para facilitar busca via regex no Grafana/Loki
- O Collector permite filtrar na origem — só ingere o que é necessário

### Métricas no Grafana

Métricas de **aplicação** a monitorar:
- Requisições totais, requisições com erro, latência da API, uptime

Métricas de **infraestrutura** a monitorar:
- Uso de disco, memória, CPU, tráfego de rede, serviços em execução

Histogramas importantes:
- **P95 / P99** — percentis de latência, revelam comportamento nos piores casos
- Crescimento anormal de contadores (ex: 144 → 10.000) deve ser investigado contra a regra de negócio

### Responsabilidade compartilhada
- Observabilidade não é só time de SRE — é cultura
- **DBA**: monitora queries e processos do banco
- **Dev**: acompanha o comportamento pós-deploy, compara com períodos anteriores
- **DevOps/SRE**: mantém a stack, define alertas e difunde a cultura

---

## Conceitos na Prática — VidaLongaFlix

### Esteira completa — VidaLongaFlix

```
┌─────────────────────────────────────────────────────┐
│  FRONTEND (Angular)                                  │
│  @opentelemetry/sdk-trace-web                        │
│  → captura: cliques, chamadas HTTP, erros de JS      │
└──────────────────────┬──────────────────────────────┘
                       │ OTLP (HTTP)
┌──────────────────────▼──────────────────────────────┐
│  BACKEND (Spring Boot)                               │
│  OTel Java Agent + Micrometer + Actuator             │
│  → captura: HTTP, JDBC, latência, health checks      │
└──────────────────────┬──────────────────────────────┘
                       │ OTLP (gRPC/HTTP)
┌──────────────────────▼──────────────────────────────┐
│  OpenTelemetry Collector                             │
│  → filtra, enriquece, distribui                      │
└──────┬───────────────┬───────────────┬──────────────┘
       │               │               │
┌──────▼──────┐ ┌──────▼──────┐ ┌─────▼───────┐
│ Prometheus  │ │    Loki     │ │    Tempo    │
│  métricas   │ │    logs     │ │   traces    │
└──────┬──────┘ └──────┬──────┘ └─────┬───────┘
       └───────────────▼───────────────┘
                ┌──────────────┐
                │    Grafana   │
                │  dashboards  │
                │   alertas    │
                └──────────────┘
```

### O que cada camada observa

| Camada | Ferramenta | O que captura |
|---|---|---|
| Angular | OTel JS SDK | Erros de JS, tempo de carregamento, chamadas à API, cliques |
| Spring Boot | OTel Java Agent | Latência de endpoints, queries SQL, erros HTTP, threads |
| Collector | OTel Collector | Centraliza tudo, filtra, enriquece com metadados |
| Métricas | Prometheus | P95/P99, taxa de erros, uptime, CPU/memória |
| Logs | Loki | Logs da JVM, logs do Angular, erros correlacionados |
| Traces | Tempo | Jornada completa: clique no Angular → API → banco |
| Visualização | Grafana | Dashboards, alertas, drilldown trace → log |

### Valor prático — rastreamento ponta a ponta

> Usuário clicou em "Assistir" no Angular → chamou `/api/videos/42` → Spring Boot buscou no banco → demorou 800ms → qual query travou?

Tudo rastreável com o mesmo `trace_id` ligando frontend, backend e banco.

---

### Plano de Implementação

---

#### Passo 1 — Subir a stack local com Docker Compose

Arquivo `docker-compose.observability.yml` com todos os componentes:

```yaml
# docker-compose.observability.yml
services:

  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    volumes:
      - ./otel-collector-config.yaml:/etc/otel/config.yaml
    command: ["--config=/etc/otel/config.yaml"]
    ports:
      - "4317:4317"   # gRPC — backend (Spring Boot)
      - "4318:4318"   # HTTP  — frontend (Angular)
    networks: [observability]

  mimir:
    image: grafana/mimir:latest
    volumes:
      - ./mimir-demo.yaml:/etc/mimir/demo.yaml
    command: ["--config.file=/etc/mimir/demo.yaml"]
    ports:
      - "9009:9009"
    networks: [observability]

  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    networks: [observability]

  tempo:
    image: grafana/tempo:latest
    command: ["-config.file=/etc/tempo.yaml"]
    volumes:
      - ./tempo.yaml:/etc/tempo.yaml
    ports:
      - "3200:3200"
      - "4319:4317"   # OTLP gRPC para o Tempo (porta alternativa)
    networks: [observability]

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
    networks: [observability]

networks:
  observability:
    driver: bridge
```

**Config do Grafana Mimir** (`mimir-demo.yaml`) — modo monolítico para desenvolvimento:

```yaml
# NÃO usar em produção — apenas para laboratório
multitenancy_enabled: false

blocks_storage:
  backend: filesystem
  filesystem:
    dir: /tmp/mimir/data/tsdb
  tsdb:
    dir: /tmp/mimir/tsdb

compactor:
  data_dir: /tmp/mimir/compactor
  sharding_ring:
    kvstore:
      store: memberlist

distributor:
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: memberlist

ingester:
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: memberlist
    replication_factor: 1

server:
  http_listen_port: 9009
  log_level: error
```

---

#### Passo 2 — Configurar o OpenTelemetry Collector

Arquivo `otel-collector-config.yaml`:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317   # backend (Spring Boot via Java Agent)
      http:
        endpoint: 0.0.0.0:4318   # frontend (Angular via SDK web)

processors:
  batch: {}
  # Filtrar apenas WARN e ERROR nos logs — reduz custo de ingestão
  filter/logs:
    logs:
      exclude:
        match_type: regexp
        severity_texts: ["DEBUG", "INFO"]

exporters:
  prometheusremotewrite:
    endpoint: http://mimir:9009/api/v1/push
  loki:
    endpoint: http://loki:3100/loki/api/v1/push
  otlp/tempo:
    endpoint: http://tempo:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheusremotewrite]
    logs:
      receivers: [otlp]
      processors: [batch, filter/logs]
      exporters: [loki]
```

---

#### Passo 3 — Instrumentar o Backend (Spring Boot)

**Dependências** (`pom.xml`):

```xml
<!-- Actuator -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer → Prometheus -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**application.properties**:

```properties
# Expõe endpoints de health e métricas
management.endpoints.web.exposure.include=health,prometheus,info
management.endpoint.health.show-details=always
```

**OTel Java Agent no Dockerfile** (zero mudança no código):

```dockerfile
# Baixar o agente durante o build
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otel-agent.jar

ENTRYPOINT ["java", \
  "-javaagent:/otel-agent.jar", \
  "-Dotel.service.name=vidalongaflix-backend", \
  "-Dotel.exporter.otlp.endpoint=http://otel-collector:4317", \
  "-Dotel.exporter.otlp.protocol=grpc", \
  "-Dotel.logs.exporter=otlp", \
  "-jar", "app.jar"]
```

**Variáveis de ambiente** (alternativa ao `-D`, preferível em produção):

```
OTEL_SERVICE_NAME=vidalongaflix-backend
OTEL_ENV=production
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_LOGS_EXPORTER=otlp
```

O agente captura automaticamente: latência por endpoint, queries JDBC, erros HTTP, threads ativas, conexões ao banco.

---

#### Passo 4 — Instrumentar o Frontend (Angular)

**Instalação**:

```bash
npm i @opentelemetry/sdk-trace-web \
      @opentelemetry/auto-instrumentations-web \
      @opentelemetry/exporter-trace-otlp-http \
      @opentelemetry/resources \
      @opentelemetry/semantic-conventions
```

**`src/tracing.ts`** — arquivo de configuração (equivalente ao `otel.js` do curso):

```typescript
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';

const provider = new WebTracerProvider({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'vidalongaflix-frontend',
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: 'production',
  }),
});

provider.addSpanProcessor(
  new BatchSpanProcessor(
    new OTLPTraceExporter({
      // Collector aceita HTTP na 4318 — sem problemas de CORS em produção
      url: 'http://otel-collector:4318/v1/traces',
    })
  )
);

provider.register();
getWebAutoInstrumentations(); // captura document load, fetch, XHR automaticamente
```

**`src/main.ts`** — inicializar antes de tudo:

```typescript
import './tracing';  // DEVE ser a primeira linha
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

platformBrowserDynamic().bootstrapModule(AppModule);
```

O SDK captura automaticamente: tempo de carregamento de página, chamadas HTTP à API, erros de JS não tratados, navegação entre rotas.

---

#### Passo 5 — Configurar data sources no Grafana

Acessar `http://localhost:3000` → Configuration → Data Sources:

| Data Source | URL | Tipo |
|---|---|---|
| Prometheus/Mimir | `http://mimir:9009/prometheus` | Prometheus |
| Loki | `http://loki:3100` | Loki |
| Tempo | `http://tempo:3200` | Tempo |

Ativar **Trace to Logs** no Tempo apontando para Loki — habilita o drilldown `trace → log`.

---

#### Passo 6 — Explorar Traces no Grafana (TraceQL)

No menu **Explore → Tempo**:

```
# Filtrar traces lentos do backend
{ .service.name = "vidalongaflix-backend" && duration > 500ms }

# Filtrar traces com erro do frontend
{ .service.name = "vidalongaflix-frontend" && status = error }

# Ver todos os spans de um trace específico
{ .trace_id = "6a7a23cd-..." }
```

Campos disponíveis no View Traces:
- **Service Name** — qual serviço gerou o span
- **Span Name** — ação executada (ex: `GET /api/videos`, `SELECT videos`)
- **Status** — OK ou ERROR
- **Duration** — tempo do span em ms
- **Tags/Attributes** — metadados do span (ex: `http.status_code`, `db.statement`)

O círculo do serviço fica **verde** quando tudo está OK e **vermelho** quando há latência ou erro.

---

#### Passo 7 — Criar Dashboards no Grafana

Dashboards prioritários para o VidaLongaFlix:

**Golden Signals (backend)**:
- Latência P95/P99 por endpoint
- Taxa de requisições totais vs erros
- Saturação: CPU, memória JVM, threads
- Tráfego: req/s por rota

**Frontend Angular**:
- Tempo de carregamento de página (LCP)
- Taxa de erros de JS
- Chamadas à API com falha por rota
- Latência das chamadas HTTP frontend → backend

**Infraestrutura**:
- CPU, memória, disco do servidor
- Conexões ativas ao banco de dados PostgreSQL

---

#### Passo 8 — Configurar Alertas

```
Latência P95 > 500ms        → alerta WARN
Latência P95 > 1000ms       → alerta CRITICAL
Taxa de erros 5xx > 1%/min  → alerta CRITICAL
Health check falhando        → alerta CRITICAL
Memória JVM > 80%            → alerta WARN
Erros de JS no frontend > 10/min → alerta WARN
```

---

#### Passo 9 — Produção (Elastic Beanstalk → Grafana Cloud)

Em produção, apontar o Collector para o **Grafana Cloud free tier** (sem custo de infra):

```
# Variáveis no Elastic Beanstalk
OTEL_SERVICE_NAME=vidalongaflix-backend
OTEL_EXPORTER_OTLP_ENDPOINT=https://<grafana-cloud-otlp-endpoint>
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64-instanceid:token>
OTEL_LOGS_EXPORTER=otlp
```

Frontend Angular: trocar a URL do Collector para o endpoint público do Grafana Cloud no `tracing.ts` via variável de ambiente do build Angular (`environment.prod.ts`).

---

## Módulo 5 — Decisões Baseadas em Dados e Cultura de Observabilidade

### Latência e impacto no negócio

Latência não é um problema técnico isolado — é um problema de receita:
- Página lenta → usuário abandona → menos faturamento
- Login com erro → usuário **não entra** → zero faturamento
- CPU alta → causa raiz de lentidão e erros → impacto em cascata

A observabilidade existe para **quantificar esse impacto** e tomar decisões baseadas em dados, não em instinto.

### Correlação de alertas — War Room com dados

**Cenário real:** dois alertas ativos simultaneamente — CPU alta + serviço de checkout fora do ar.

**Sem observabilidade:** dois times agem em paralelo sem saber se é o mesmo problema. Time A reinicia servidor. Time B reinicia serviço. Problema volta em 10 minutos.

**Com observabilidade:**
```
Alerta: CPU alta + checkout fora do ar
    └── abre trace do período (Grafana → Tempo)
            └── vê checkout chamando banco 47x por requisição
                    └── HikariCP esgotado (JVM & Backend dashboard)
                            └── causa raiz: N+1 query no endpoint /checkout
                                    └── solução: corrigir query + aumentar pool
```
Um único `trace_id` conecta os dois alertas. **Não são problemas separados — é o mesmo.**

### Exercício: qual alerta resolver primeiro?

> Três alertas ativos: latência alta, CPU alta, erro no login. Qual resolver primeiro?

| Alerta | Impacto no usuário | Impacto na receita | Prioridade |
|---|---|---|---|
| Erro no login | Usuário não consegue entrar | Zero faturamento | **1º** |
| Latência alta | Usuário lento, pode desistir | Abandono gradual | **2º** |
| CPU alta | Causa raiz dos dois acima | Indireto | **3º — investigar** |

**Regra:** o impacto no usuário define a prioridade. CPU alta é causa raiz, mas o sintoma crítico é o login — sem ele, nenhuma receita entra.

### Cultura Blameless

- Quando ocorre um incidente, o objetivo é entender **o que aconteceu**, não **quem errou**
- Post-mortem com dados: o que o time fez? O que funcionou? O que pode ser automatizado?
- Perguntas certas após um incidente:
  - Era período de pico? O que o tráfego mostrava?
  - Um deploy recente pode ter causado isso?
  - Reiniciar resolveu — mas por quanto tempo? O problema voltará?
  - O que instrumentar no próximo deploy para detectar antes?

### Por que medir tudo não funciona

- Medir tudo → perder foco no que realmente importa
- Nem toda métrica de infraestrutura reflete o que o usuário sente
- Métricas erradas → decisões erradas → investimento mal direcionado
- **Regra:** medir a **experiência do usuário**, não só a infraestrutura

### SLI — o que medir de verdade

Os 4 SLIs que refletem experiência do usuário no VidaLongaFlix:

| SLI | Métrica | Como calcular |
|---|---|---|
| **Disponibilidade** | % de requisições 2xx | `sum(rate(2xx)) / sum(rate(total))` |
| **Latência** | P95 de tempo de resposta | `histogram_quantile(0.95, ...)` |
| **Taxa de Erro** | % de requisições 5xx | `sum(rate(5xx)) / sum(rate(total))` |
| **Throughput** | Requisições por segundo | `sum(rate(total[5m]))` |

> Throughput sozinho não é problema — mas **latência alta com throughput alto** indica saturação real.

### SLO — as metas do VidaLongaFlix

| SLO | Meta | Consequência se violar |
|---|---|---|
| Disponibilidade | ≥ 99.5% | Error budget esgotado — bloquear deploys |
| Latência P95 | < 300ms | Warning; > 500ms = alerta crítico |
| Taxa de Erro | < 0.5% | Warning; > 1% = alerta crítico |

### Error Budget

- **SLO 99.5%** → tolerância de **0.5% de falhas** por mês
- 0.5% de um mês = ~**3.6 horas** de indisponibilidade permitida
- Se o budget acabar → **parar deploys arriscados** até o próximo período
- Se o budget estiver saudável → **acelerar** inovação e deploys

### Dois tipos de dashboard

| Dashboard Técnico | Dashboard de Experiência |
|---|---|
| Uptime, ping, CPU, memória | Disponibilidade, latência, erros |
| Para o time de operação/infra | Para SRE, Dev, Produto e Negócio |
| "O servidor está vivo?" | "O usuário consegue usar?" |
| → **JVM & Backend** | → **Disponibilidade SLI/SLO** + **Golden Signals** |

### Ciclo de melhoria contínua

```
1. COLETAR   → métricas, traces, logs chegando ao Grafana
2. ANALISAR  → correlacionar alertas, identificar causa raiz
3. AGIR      → corrigir o problema com evidência
4. MONITORAR → acompanhar se a latência diminuiu, se os erros cessaram
5. INSTITUCIONALIZAR → transformar em SLI/SLO para nunca deixar regredir
```

### Dados que conectam times

- **Dev** sabe como o código foi estruturado
- **DBA** sabe quais queries são lentas
- **SRE/DevOps** sabe o impacto na infraestrutura
- **Produto** sabe o impacto no usuário e na receita

Observabilidade é a **linguagem comum** entre todos esses times. Um trace mostra a chamada do frontend, o endpoint do backend, a query do banco e o tempo de cada etapa — qualquer time consegue entender e contribuir.

---

## Módulo 6 — DevOps, SRE, Platform Engineering e Contratos de Serviço

### DevOps vs SRE vs Platform Engineering

São papéis **complementares**, não concorrentes. Cada um tem foco diferente dentro da mesma organização.

| Papel | Foco principal | Pergunta central | Práticas-chave |
|---|---|---|---|
| **DevOps** | Cultura e colaboração | "Dev e Ops trabalham juntos?" | CI/CD, automação, pipeline, cultura ágil |
| **SRE** | Confiabilidade com engenharia | "O sistema está confiável?" | SLI/SLO, Error Budget, automação de respostas a incidentes |
| **Platform Engineering** | Ferramentas e plataforma interna | "O dev consegue fazer deploy sem fricção?" | IDP (Internal Developer Platform), self-service, golden paths |

**Analogia prática:**
- **DevOps** = estrada que liga Dev e Ops (cultura, processo)
- **SRE** = radar de velocidade na estrada (metricas, limites, confiabilidade)
- **Platform Engineering** = estrada de alta qualidade sem buracos (plataforma, ferramentas)

#### DevOps — Cultura de Colaboração
- Elimina o muro entre desenvolvimento e operações
- Foco em entrega contínua: CI/CD, automação, infraestrutura como código
- **Responsabilidade compartilhada:** quem escreve o código cuida do deploy
- Resultado: deploys mais frequentes, feedback mais rápido, menos silos

#### SRE — Confiabilidade como Engenharia
- Criado pelo Google: "aplicar engenharia de software para problemas de operação"
- SRE define **metas quantitativas** de confiabilidade (SLO) e as defende com dados
- Usa o Error Budget para equilibrar velocidade de inovação e estabilidade
- **Diferença do DevOps:** DevOps é cultura, SRE é uma implementação específica dessa cultura com métricas
- Resultado: decisões baseadas em dados, não em intuição ou conflito Dev vs Ops

#### Platform Engineering — A Plataforma que Acelera
- Cria ferramentas internas para que desenvolvedores sejam **autônomos**
- Golden Path: caminho recomendado e pré-configurado para criar, testar e entregar um serviço
- Reduz fricção: dev não precisa saber configurar Kubernetes, monitoring ou segurança do zero
- Resultado: produtividade maior, padrões consistentes, menos erro humano

#### No VidaLongaFlix — como se aplica
| Papel | O que fizemos |
|---|---|
| **DevOps** | CI/CD com GitHub Actions (test → build → push Docker Hub → deploy EB) |
| **SRE** | SLI/SLO definidos, Error Budget no dashboard, alertas com priorização por impacto |
| **Platform Engineering** | Docker Compose com stack local pré-configurada — qualquer dev sobe o ambiente em 1 comando |

---

### SLA — Service Level Agreement (Contrato Formal)

#### O que é um SLA
- Contrato **formal e jurídico** entre provedor de serviço e cliente
- Define: disponibilidade mínima garantida, tempo máximo de resolução (MTTR), penalidades por violação
- É **externo** — voltado ao cliente. SLO é **interno** — meta da equipe
- Regra de ouro: **SLO deve ser mais rigoroso que o SLA**

```
Exemplo:
  SLO interno: 99.5% de disponibilidade  ← equipe tenta atingir
  SLA com cliente: 99.0% de disponibilidade ← penalidade se violar

  O gap de 0.5% é o "colchão de segurança":
  se o SLO for violado, ainda não violamos o SLA
```

#### Penalidades típicas de SLA
- **Créditos de serviço:** % do valor mensal devolvido ao cliente
- **Rescisão contratual:** cliente pode sair sem multa se SLA for violado repetidamente
- **Multas financeiras:** valores fixos por hora de indisponibilidade
- **Relatórios obrigatórios:** post-mortem público entregue ao cliente em até X horas

#### SLAs de provedores de nuvem (referência)

| Provedor | Serviço | SLA |
|---|---|---|
| **AWS** | EC2 (por instância) | 99.5% |
| **AWS** | EC2 Multi-AZ | 99.99% |
| **AWS** | RDS Multi-AZ | 99.95% |
| **AWS** | S3 | 99.9% |
| **GCP** | Compute Engine | 99.99% |
| **Azure** | Virtual Machines | 99.9% (single) / 99.99% (availability set) |

> Importante: esses SLAs cobrem **a infraestrutura do provedor**, não a sua aplicação. Sua aplicação pode estar fora do ar mesmo com a infraestrutura saudável.

#### SLI → SLO → SLA: o ciclo completo

```
MEDE (SLI) → DEFINE META (SLO) → FORMALIZA CONTRATO (SLA)
   ↑                                         ↓
   └─────── melhoria contínua ───────────────┘
```

1. **SLI** mede a realidade: "nosso P95 atual é 280ms"
2. **SLO** define a meta interna: "queremos P95 < 300ms 99.5% do tempo"
3. **SLA** formaliza com o cliente: "garantimos 99% de disponibilidade com P95 < 500ms"

---

### Por que 100% de disponibilidade é impossível

#### A matemática da indisponibilidade

| Disponibilidade | Downtime por mês | Downtime por ano |
|---|---|---|
| 99% | ~7.3 horas | ~3.65 dias |
| 99.5% | ~3.6 horas | ~1.83 dias |
| 99.9% | ~43.8 minutos | ~8.76 horas |
| 99.95% | ~21.9 minutos | ~4.38 horas |
| 99.99% | ~4.4 minutos | ~52.6 minutos |
| 99.999% ("five nines") | ~26 segundos | ~5.26 minutos |

#### Por que 100% é impossível
- **Deploys causam downtime** — mesmo com zero-downtime deploy, há risco
- **Hardware falha** — servidores, redes, datacenters têm MTBF (tempo médio entre falhas)
- **Bugs em produção** — qualquer sistema suficientemente complexo tem bugs ainda não descobertos
- **Dependências externas** — banco, CDN, serviço de pagamento — qualquer um pode cair
- **Custo proibitivo:** 99.99% → 99.999% pode custar 10x mais infraestrutura e operação

#### A curva de custo da confiabilidade

```
Custo
  ↑
  │                                          ●  (99.999%)
  │                                     ●
  │                               ●
  │                          ●
  │                     ●
  │              ●
  │         ●
  │    ●
  └──────────────────────────────────────────→ Disponibilidade
     95%   99%  99.5%  99.9%  99.95%  99.99%
```

Cada "nove" adicional aumenta custo exponencialmente. O ponto ótimo depende do impacto no negócio.

#### Decisão baseada em custo vs impacto
- Para um portfólio pessoal: 99.5% é mais que suficiente
- Para um banco: 99.99% pode ser exigência regulatória
- **Perguntar sempre:** "quanto custa 1 hora de downtime para o negócio?"

#### No VidaLongaFlix
- SLO definido: **99.5%** (razoável para startup, ~3.6h de downtime/mês permitido)
- Monitorado em tempo real no dashboard **Disponibilidade SLI/SLO**
- Error Budget gauge mostra quanto ainda pode ser consumido no mês

---

### Uptime ≠ Experiência do Usuário

Esta é a distinção mais importante e mais ignorada em operações:

| Métrica de Uptime | O que mede | O que NÃO mede |
|---|---|---|
| Ping respondendo | Servidor vivo | Se o usuário consegue usar |
| CPU em 20% | Recurso disponível | Se a resposta está lenta |
| Health check OK | App inicializada | Se o login funciona |
| Zero erros no servidor | Sem exceções internas | Se o usuário vê a página carregando |

**Exemplos reais onde uptime = 100% mas experiência = péssima:**
- CDN com cache desatualizado: servidor OK, usuário vê dados antigos
- Banco lento: app responde 200 OK, mas demora 8 segundos — usuário abandona
- Certificado SSL próximo do vencimento: health check passa, browser bloqueia
- Feature flag errada: login retorna 200 mas com campo faltando — app quebra no frontend

**A regra:** não monitore apenas "está de pé?" — monitore "o usuário consegue completar o que veio fazer?"

#### Métricas de UX que o VidaLongaFlix monitora
| O que o usuário quer fazer | SLI que mede |
|---|---|
| Acessar a plataforma | Disponibilidade (% de 2xx) |
| Ver os vídeos sem travar | Latência P95 < 300ms |
| Fazer login sem erro | Taxa de 5xx em `/auth/*` |
| Navegar sem lentidão | Throughput + P95 por endpoint |

---

---

## Fase de Produção — Grafana Cloud + AWS Elastic Beanstalk

### Estado atual — sessão 2026-03-29 (executado localmente)

| Passo | Status | O que foi feito / O que falta |
|---|---|---|
| 1 — Stack local subindo | ✅ | `docker compose -f docker-compose.observability.yml up -d` — todos os 5 containers `Up` |
| 2 — OTel Collector ouvindo | ✅ | gRPC:4317 e HTTP:4318 ativos, sem erros |
| 3 — Métricas → Mimir | ✅ | **Corrigido nesta sessão**: adicionado `micrometer-registry-otlp` ao `pom.xml` — 75 séries chegando (JVM, HikariCP, HTTP) |
| 4 — Traces → Tempo | ✅ | Funcionando antes desta sessão — spans de todos os endpoints visíveis no Tempo |
| 5 — Logs → Loki | ⚠️ | **Parcialmente corrigido**: adicionado `opentelemetry-logback-appender-1.0:2.15.0-alpha` + criado `logback-spring.xml` — **falta validar** se logs chegam ao Loki (restart interrompido) |
| 6 — Grafana local | ✅ | Datasources provisionados automaticamente (Mimir, Loki, Tempo) |
| 7 — Dashboards | ✅ | `golden-signals.json`, `jvm-backend.json`, `sli-disponibilidade.json` carregados |
| 8 — Alertas | ⚠️ | Provisionados mas com **nome errado de métrica**: OTLP usa `_milliseconds`, regras usam `_seconds` — ver fix abaixo |
| 9 — Config prod | ✅ | `application-prod.properties` + `docker-compose.eb.yml` prontos |
| **10 — Grafana Cloud** | ⏳ | Criar conta + obter endpoint OTLP + gerar token |
| **11 — Env vars no EB** | ⏳ | Adicionar `OTLP_HTTP_ENDPOINT` e `OTLP_AUTH_HEADER` |
| **12 — Validar prod** | ⏳ | Confirmar dados chegando no Grafana Cloud |
| **13 — SLIs/SLOs formais** | ⏳ | Criar no Grafana Cloud SLO plugin |
| **14 — Frontend Angular** | ⏳ | Implementar `tracing.ts` + ativar sidecar no EB |

---

### Correções aplicadas nesta sessão — detalhe técnico

#### Correção 1 — Métricas via OTLP (pom.xml)

**Problema:** `management.otlp.metrics.export.url` exige `micrometer-registry-otlp` — sem ele, métricas nunca chegam ao Mimir via push OTLP.

**Fix:** adicionado ao `pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-otlp</artifactId>
</dependency>
```

> O `micrometer-registry-prometheus` (já existente) serve apenas para o endpoint `/actuator/prometheus` (scrape). O push OTLP requer o `micrometer-registry-otlp`.

#### Correção 2 — Logs via OTLP (pom.xml + logback-spring.xml)

**Problema raiz:** dois fatores combinados:
1. `opentelemetry-logback-appender-1.0` não estava no `pom.xml`
2. Spring Boot 3.5 cria o `SdkLoggerProvider` + `OtlpHttpLogRecordExporter` automaticamente (via `OpenTelemetryLoggingAutoConfiguration` + `OtlpLoggingAutoConfiguration`) mas **NÃO instala o Logback appender automaticamente** — requer `logback-spring.xml` explícito

**Fix — pom.xml:**
```xml
<!-- Versão alinhada com opentelemetry-api 1.49.0 (Spring Boot 3.5.12 BOM) -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>2.15.0-alpha</version>
    <scope>runtime</scope>
</dependency>
```

**Fix — `src/main/resources/logback-spring.xml`** (arquivo criado):
```xml
<appender name="OpenTelemetry"
          class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    <captureCodeAttributes>true</captureCodeAttributes>
    <captureArguments>true</captureArguments>
</appender>
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="OpenTelemetry"/>
</root>
```

**Status:** adicionado e compilando — falta validar que logs chegam ao Loki (restart foi interrompido).

#### Correção 3 — application.properties (pendente reverter)

Para diagnóstico foi adicionado `conditions` aos endpoints expostos:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus,conditions
```
**Reverter para:** `health,info,metrics,prometheus` antes do próximo deploy.

#### Bug identificado nos alertas — nomes de métricas errados

As regras em `observability/grafana/provisioning/alerting/rules.yaml` usam nomes no formato Prometheus (`_seconds`), mas o exporter OTLP (`micrometer-registry-otlp`) usa `_milliseconds`.

**Exemplo — rules.yaml atual (errado):**
```yaml
expr: "histogram_quantile(0.95, sum by(le) (rate(http_server_request_duration_seconds_bucket[5m])))"
```

**Correto para OTLP:**
```yaml
expr: "histogram_quantile(0.95, sum by(le) (rate(http_server_requests_milliseconds_bucket[5m]))) "
```

**Mapeamento completo de nomes:**
| Regra | Nome errado (Prometheus scrape) | Nome correto (OTLP push) |
|---|---|---|
| Latência P95 | `http_server_request_duration_seconds_bucket` | `http_server_requests_milliseconds_bucket` |
| Taxa de erros | `http_server_request_duration_seconds_count{status=~"5.."}` | `http_server_requests_milliseconds_count{http_response_status_code=~"5.."}` |
| Total req. | `http_server_request_duration_seconds_count` | `http_server_requests_milliseconds_count` |
| Rota de login | `http_route=~".*auth.*"` | `uri=~".*auth.*"` (label diferente no OTLP) |

**Ação necessária:** corrigir `rules.yaml` + `golden-signals.json` + `jvm-backend.json` nas queries HTTP.

---

### Próxima sessão — o que fazer em ordem

**1. Validar logs no Loki (5 min)**
```bash
# Subir a stack (se não estiver rodando)
docker compose -f docker-compose.observability.yml up -d

# Subir o backend
./mvnw spring-boot:run -DskipTests &

# Aguardar UP e gerar tráfego
curl http://localhost:8090/api/videos
curl -X POST http://localhost:8090/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@vidalongaflix.com","password":"Admin@123456"}'

# Aguardar 15s e verificar Loki
sleep 15
curl -s 'http://localhost:3100/loki/api/v1/labels?since=1h'
# Esperado: {"status":"success","data":["service_name","severity","...etc"]}
```

**2. Corrigir nomes de métricas nos alertas (15 min)**

Editar `observability/grafana/provisioning/alerting/rules.yaml` trocando os nomes conforme tabela acima.

**3. Corrigir nomes de métricas nos dashboards (15 min)**

Nos JSONs `golden-signals.json` e `jvm-backend.json`, substituir `http_server_request_duration_seconds` por `http_server_requests_milliseconds`.

**4. Reverter `conditions` endpoint (1 min)**

Em `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

**5. Abrir Grafana local e validar visualmente (10 min)**

Acessar `http://localhost:3000` e confirmar:
- Dashboard Golden Signals mostrando latência e throughput
- Dashboard JVM Backend mostrando heap e HikariCP
- Dashboard SLI/SLO mostrando disponibilidade
- Explore → Loki → logs chegando
- Explore → Tempo → traces com correlação de logs

**6. Fazer commit com as correções (2 min)**
```bash
git add pom.xml src/main/resources/logback-spring.xml src/main/resources/application.properties \
        observability/grafana/provisioning/alerting/rules.yaml \
        observability/grafana/provisioning/dashboards/
git commit -m "fix(observability): add missing OTLP deps and fix metric names for OTLP push format"
```

**7. Passos 10–14 (produção — requer conta Grafana Cloud)**

Ver seção "Passo 10" abaixo para o roteiro completo.

---

### Passo 10 — Criar conta no Grafana Cloud e obter credenciais

**10.1 — Criar a conta (free tier)**

1. Acesse **grafana.com → "Get Grafana Cloud"**
2. Crie uma conta gratuita (free tier cobre: 10.000 series ativas, 50GB de logs, 50GB de traces)
3. Escolha a **região** mais próxima → **South America (São Paulo)** se disponível, senão **US East**
4. Anote o nome da sua stack, ex: `vidalongaflix`

**10.2 — Obter o endpoint OTLP**

1. No painel do Grafana Cloud → **"My Account"** (canto superior esquerdo)
2. Na seção **"Stack"** → clique no nome da stack
3. Clique em **"Configure"** ao lado de **"OpenTelemetry"**
4. Você verá:
   ```
   OTLP Endpoint: https://otlp-gateway-prod-sa-east-1.grafana.net/otlp
   Instance ID:   123456
   ```
   Anote o **endpoint** e o **Instance ID**.

**10.3 — Gerar o API Token**

1. No mesmo painel → **"Generate now"** ao lado de "Password / API Token"
2. Escolha o escopo: **MetricsPublisher + LogsPublisher + TracesPublisher**
3. Copie o token gerado — ele começa com `glc_...`
   > **Atenção:** o token é exibido apenas uma vez. Copie agora.

**10.4 — Gerar o auth header Base64**

```bash
# Substitua com seus dados reais
INSTANCE_ID="123456"
API_TOKEN="glc_eyJrIjoiOWZj..."

echo -n "${INSTANCE_ID}:${API_TOKEN}" | base64
# Saída: MTIzNDU2OmdsY19leUp...
```

Guarde a string base64 — ela vai como `OTLP_AUTH_HEADER` no EB.

---

### Passo 11 — Configurar variáveis de ambiente no Elastic Beanstalk

**Acesso ao console:**

1. AWS Console → **Elastic Beanstalk** → selecione o ambiente `vidalongaflix-env`
2. Menu lateral → **"Configuration"** → seção **"Software"** → **"Edit"**
3. Role até **"Environment properties"** e adicione as variáveis abaixo

**Variáveis obrigatórias para observabilidade (adicionar às já existentes):**

| Variável | Valor | Descrição |
|---|---|---|
| `OTLP_HTTP_ENDPOINT` | `https://otlp-gateway-prod-sa-east-1.grafana.net/otlp` | URL do Grafana Cloud OTLP (sem `/v1/*`) |
| `OTLP_AUTH_HEADER` | `MTIzNDU2OmdsY19leUp...` | Base64 de `instanceId:apiToken` |

**Variável opcional:**

| Variável | Valor padrão | Quando mudar |
|---|---|---|
| `OTEL_SAMPLING_PROBABILITY` | `0.1` (10%) | Mudar para `1.0` durante investigação de incidente |

**Variáveis já existentes (verificar se estão presentes):**

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<RDS_ENDPOINT>:5432/vidalongaflix
DB_USERNAME=<usuario>
DB_PASSWORD=<senha>
JWT_SECRET=<segredo-forte-64-chars>
ADMIN_EMAIL=admin@vidalongaflix.com.br
ADMIN_PASSWORD=<senha-forte>
CORS_ALLOWED_ORIGINS=https://vidalongaflix.com.br,https://xxxx.cloudfront.net
```

4. Clique **"Apply"** — o EB vai reiniciar o ambiente com as novas variáveis.

> **Como os dados chegam ao Grafana Cloud:** o Spring Boot envia métricas, traces e logs diretamente para o endpoint OTLP do Grafana Cloud via Micrometer (`management.otlp.*` no `application-prod.properties`). Não é necessário o Collector sidecar para o backend — ele só é necessário quando quiser capturar traces do Angular (frontend).

---

### Passo 12 — Validar que os dados chegam ao Grafana Cloud

**12.1 — Verificar métricas**

1. Acesse seu Grafana Cloud: `https://vidalongaflix.grafana.net`
2. Menu → **Explore** → Data Source: **Grafana Mimir / Prometheus**
3. Query:
   ```promql
   http_server_requests_seconds_count{job="vidalongaflix-backend"}
   ```
   Se retornar dados → métricas chegando.

**12.2 — Verificar traces**

1. Menu → **Explore** → Data Source: **Grafana Tempo**
2. TraceQL:
   ```
   { .service.name = "vidalongaflix-backend" }
   ```
   Se aparecer spans → traces chegando.

**12.3 — Verificar logs**

1. Menu → **Explore** → Data Source: **Grafana Loki**
2. LogQL:
   ```logql
   {service_name="vidalongaflix-backend"} |= "ERROR"
   ```
   Faça uma chamada que gere um erro no backend e confirme que o log aparece.

**12.4 — Verificar o dashboard Golden Signals**

Os dashboards do repositório (`golden-signals.json`, `jvm-backend.json`, `sli-disponibilidade.json`) foram feitos para o Grafana local (Mimir local). Para o Grafana Cloud:

1. No Grafana Cloud → **Dashboards** → **Import**
2. Importe os JSONs de `observability/grafana/provisioning/dashboards/`
3. Ajuste o datasource para apontar para o Grafana Cloud Mimir (ao invés do local)

> Os dashboards do free tier do Grafana Cloud já têm um **Kubernetes Monitoring** e **Spring Boot** pré-instalados — verifique antes de reimportar.

---

### Passo 13 — Definir SLIs e SLOs no Grafana Cloud

#### SLIs do VidaLongaFlix (o que medimos)

Os SLIs medem a **experiência real do usuário**, não apenas uptime de infraestrutura.

| SLI | Pergunta | Métrica OTLP | PromQL |
|---|---|---|---|
| **Disponibilidade** | A API está respondendo? | `http_server_requests_seconds_count` | `sum(rate(...{status=~"2.."}[5m])) / sum(rate(...[5m]))` |
| **Latência P95** | 95% das requisições respondem em < 300ms? | `http_server_requests_seconds_bucket` | `histogram_quantile(0.95, sum(rate(...[5m])) by (le))` |
| **Taxa de Erro** | Menos de 0.5% das requisições retornam 5xx? | `http_server_requests_seconds_count` | `sum(rate(...{status=~"5.."}[5m])) / sum(rate(...[5m]))` |
| **Throughput** | Volume de requisições (contexto) | `http_server_requests_seconds_count` | `sum(rate(...[5m]))` |

#### SLOs do VidaLongaFlix (nossas metas internas)

| SLO | Meta | Error Budget (mês) | Alerta WARN | Alerta CRITICAL |
|---|---|---|---|---|
| **Disponibilidade** | ≥ 99.5% | ~3.6h de downtime | < 99.7% | < 99.5% |
| **Latência P95** | < 300ms | — | > 250ms | > 500ms |
| **Taxa de Erro** | < 0.5% | ~3.6h equivalente | > 0.3% | > 1% |

#### SLA implícito (referência para usuários)

```
Disponibilidade garantida: 99% (colchão de 0.5% entre SLO e SLA)
Latência máxima P95:       500ms
Janela de medição:         mensal
```

#### Configurar SLOs no Grafana Cloud (via SLO plugin)

O Grafana Cloud free tier inclui o **SLO plugin**:

1. Grafana Cloud → **Alerting** → **SLOs** → **Create SLO**
2. Configure o SLO de Disponibilidade:
   ```
   Name: vidalongaflix-disponibilidade

   Query (Good events):
   sum(rate(http_server_requests_seconds_count{status=~"2..",job="vidalongaflix-backend"}[5m]))

   Query (Total events):
   sum(rate(http_server_requests_seconds_count{job="vidalongaflix-backend"}[5m]))

   Objective: 99.5%
   Rolling window: 30 days
   ```
3. O plugin gera automaticamente:
   - Error Budget gauge (quanto sobrou no mês)
   - Alertas de esgotamento do budget (Burn Rate)
   - Dashboard com histórico de SLO

4. Repita para **Latência P95**:
   ```
   Name: vidalongaflix-latencia-p95

   Query (Good events — requisições abaixo de 300ms):
   sum(rate(http_server_requests_seconds_bucket{le="0.3",job="vidalongaflix-backend"}[5m]))

   Query (Total events):
   sum(rate(http_server_requests_seconds_count{job="vidalongaflix-backend"}[5m]))

   Objective: 99.5%
   ```

#### Alertas de Burn Rate (esgotamento acelerado do Error Budget)

O Grafana SLO plugin configura alertas de **Burn Rate** automaticamente. A ideia é:

| Burn Rate | Significa | Ação |
|---|---|---|
| 1x | Consumindo budget na taxa normal | Monitorar |
| 6x | Budget vai esgotar em ~5 dias | Alerta WARN → investigar |
| 14x | Budget vai esgotar em ~2 dias | Alerta CRITICAL → acionar time |
| 36x+ | Budget vai esgotar em horas | Alerta de EMERGÊNCIA |

> Burn Rate 14x significa que o sistema está 14 vezes pior que o tolerado. Se o SLO é 99.5%, um Burn Rate de 14x implica ~93% de disponibilidade.

---

### Passo 14 — Conectar o Frontend Angular (quando implementado)

Quando o `src/tracing.ts` for implementado no repo Angular, configurar a URL do Collector:

**`environment.prod.ts`** no repo frontend:
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.vidalongaflix.com.br/api',
  // URL pública do OTel Collector sidecar (porta 4318 exposta no EB)
  otlpEndpoint: 'https://api.vidalongaflix.com.br:4318'
};
```

Para isso funcionar, ativar o `docker-compose.eb.yml` em vez do deploy single-container:
1. Renomear `docker-compose.eb.yml` → `docker-compose.yml` na raiz
2. O EB vai detectar e subir dois containers: `app` + `otel-collector`
3. O Collector fica exposto na porta 4318 para receber traces do browser

Variáveis adicionais necessárias no EB para o sidecar:
```
GRAFANA_OTLP_ENDPOINT=https://otlp-gateway-prod-sa-east-1.grafana.net/otlp
GRAFANA_AUTH_HEADER=<mesmo base64 do OTLP_AUTH_HEADER>
```

---

## Módulo 7 — Automação como Pilar de Confiabilidade (CRE)

### Por que automação é um pilar de contabilidade

Quanto menor o erro humano, maior o tempo disponível para inovação. Quando a confiabilidade depende totalmente do humano, os problemas são previsíveis:

| Problema | Consequência |
|---|---|
| Deploys manuais | Risco de quebra por diferenças de ambiente e dependências |
| Resolução manual de incidentes | MTTR alto — mais tempo parado |
| Sem padronização | Cada pessoa faz de um jeito — resultado imprevisível |
| Retrabalho sem processo | Falhas se repetem porque o processo não evolui |
| Mudanças lentas | Correções em produção demoram e geram inconsistências |

> Automatizar não é reiniciar um servidor. É levar um serviço para produção, executar testes e garantir previsibilidade — liberando tempo de tarefas repetitivas para inovação.

### Custo do trabalho manual (medir para justificar)

Antes de automatizar, medir:

```
Tarefa manual: reiniciar serviço X quando fica lento
  Frequência:  3x por semana
  Tempo gasto: 20 min por ocorrência
  Total/mês:   ~5 horas
  Risco:       humano pode esquecer, fazer no horário errado, não notificar

Após automação:
  Tempo humano: 0 (script roda sozinho + notifica)
  Ganho/mês:    5 horas de engenharia liberadas para inovação
  Bônus:        audit trail automático (quem/quando/por quê foi reiniciado)
```

Essa métrica concreta transforma a conversa: deixa de ser "seria bom automatizar" e passa a ser "custa X por mês não automatizar".

### O processo de automação — ciclo completo

```
1. IDENTIFICAR   → processo manual e repetitivo com padrão definível
2. DOCUMENTAR    → passos, pré-requisitos, exceções possíveis
3. CODIFICAR     → Python, Shell, Bash — qualquer linguagem
4. TESTAR        → homologação técnica + regras de negócio
5. FAZER DEPLOY  → disponibilizar na ferramenta de automação
6. MONITORAR     → dashboard para acompanhar execução em produção
```

Exemplo prático: instalar agente OTel em 100 máquinas

| Abordagem manual | Abordagem automatizada |
|---|---|
| Acessar servidor por servidor | Script com Ansible/shell parametrizado |
| Verificar permissões manualmente | Pré-requisitos validados no script |
| Configurar proxy/coletor individualmente | Variável de ambiente padronizada |
| ~2h por servidor | ~2min por servidor (ou paralelo) |
| Inconsistências entre servidores | Resultado idêntico em todos |
| Sem audit trail | Log de execução por servidor |

### Automação no dia a dia do SRE/CRE

**Deploys com CI/CD** (já implementado no VidaLongaFlix):
- Esteira: código → testes → build Docker → push Docker Hub → deploy EB
- Elimina "funciona na minha máquina" — ambiente reproduzível via Dockerfile
- Cada PR passa pelos mesmos gates de qualidade (GitHub Actions)

**Autorremediação (auto-remediation)**:
- Scripts que corrigem incidentes leves automaticamente
- Exemplo: se latência P95 > 1s por 5 min → reiniciar pool de conexões HikariCP + alertar
- Exemplo: se disco > 85% → limpar logs antigos + alertar
- Regra: autorremediação trata **sintoma**, não causa raiz — a causa ainda precisa ser investigada

**Infraestrutura como Código (IaC)**:
- Ambiente inteiro provisionado via código parametrizado
- Todos trabalham com o mesmo padrão — elimina "servidor especial" não documentado
- No VidaLongaFlix: `docker-compose.observability.yml` é IaC para a stack local
- Próximo nível: Terraform para EB, RDS, S3, CloudFront

**Alertas inteligentes integrados com APIs**:
- Alerta no Grafana → webhook → Slack/Teams/PagerDuty
- Alerta com contexto: qual serviço, qual SLO foi violado, link direto para o trace
- Alerta com runbook: link para o passo a passo de investigação

### Identificando alvos de automação — exercício prático

Para cada tarefa repetitiva do time, responder:

| Pergunta | Exemplo |
|---|---|
| Qual a tarefa? | Reiniciar serviço quando fica lento |
| Frequência | 3x por semana |
| Tempo gasto | 20 min cada |
| Risco de erro humano | Alto — pode ser feito tarde |
| Impacto se automatizar | 5h/mês liberadas + consistência |
| Complexidade de automatizar | Baixa — script de 20 linhas |

> Quanto maior o ganho e menor a complexidade, maior a prioridade. Comece pelos mais fáceis — o hábito de automatizar se constrói.

### Dois caminhos ao identificar uma repetição

Exemplo: serviço X precisa ser reiniciado toda sexta-feira.

**Caminho 1 — É uma regra de negócio?**
- Sim: criar script agendado (cron) + monitorar execução + documentar o porquê
- O script não substitui entender por que o reinício é necessário

**Caminho 2 — Não deveria ser necessário?**
- Investigar: por que o serviço degrada? Memory leak? Query lenta? Conexão não fechada?
- O reinício automático é um **Band-Aid** — a causa raiz ainda está lá
- Criar um SLI específico para rastrear até o problema ser resolvido na origem

> Automação sem investigação mascara problemas. Automação com investigação resolve problemas.

### Automação aplicada ao VidaLongaFlix

| Automação | Status | Como funciona |
|---|---|---|
| CI/CD completo | ✅ | GitHub Actions: test → build → push → deploy EB |
| Scan de vulnerabilidades | ✅ | Docker Scout + Trivy no pipeline (CVEs bloqueiam o build) |
| Stack de observabilidade | ✅ | `docker compose up -d` sobe todo o ambiente local |
| Deploy do Collector sidecar | ✅ | `docker-compose.eb.yml` — EB sobe dois containers automaticamente |
| Alertas automáticos | ✅ | Grafana dispara alertas quando Golden Signals violam thresholds |
| Autorremediação | ⏳ | Próximo: webhook Grafana → Lambda AWS → ação corretiva |
| IaC completo (Terraform) | ⏳ | Próximo: EB + RDS + S3 como código |
| Runbooks automatizados | ⏳ | Próximo: links nos alertas → documentação de investigação |

### Métricas de automação para o dashboard

Além dos Golden Signals, acompanhar a saúde da automação em si:

```promql
# Tempo médio desde o alerta até resolução (MTTR)
# Deve diminuir ao longo do tempo com mais automação

# Número de deploys por mês (quanto mais, melhor — sinal de CI/CD saudável)
# Taxa de sucesso dos deploys (meta: > 99%)
# Número de incidentes com autorremediação bem-sucedida
```

---

## Pendências

### Implementados ✅
- [x] Health checks no Spring Boot Actuator (liveness/readiness)
- [x] Métricas com Micrometer + OTLP exporter
- [x] Tracing distribuído via Micrometer Tracing + OTel
- [x] Logs via OTLP (Micrometer)
- [x] Stack local completa (`docker-compose.observability.yml`)
- [x] Dashboards Grafana provisionados (Golden Signals, JVM Backend, SLI/SLO)
- [x] Alertas Grafana provisionados (latência, erros, JVM heap, HikariCP)
- [x] Config de produção documentada (`application-prod.properties`)
- [x] OTel Collector sidecar para EB (`docker-compose.eb.yml`)
- [x] **Passo 10**: Conta Grafana Cloud criada (stack `vidalongaflix.grafana.net`)
  - Endpoint: `https://otlp-gateway-prod-sa-east-1.grafana.net/otlp`
  - Instance ID: `1577558`
  - Token criado: `vidalongaflix-otel-prod` (scopes: metrics/logs/traces write, no expiry)
- [x] **Passo 11 parcial**: Vars adicionadas no Elastic Beanstalk + upgrade de instância
  - `GRAFANA_OTLP_ENDPOINT` e `GRAFANA_AUTH_HEADER` adicionados ao EB
  - Instância upgradeada de `t3.micro` (1 GB) → `t3.small` (2 GB) para suportar sidecar
  - `docker-compose.yml` criado (cópia de `docker-compose.eb.yml`) e commitado
  - `docker.yml` corrigido: expressão `secrets.*` removida de `if` conditions (incompatível com `workflow_call`)
- [x] **Passo 14**: `src/tracing.ts` no repo Angular
  - `src/tracing.ts` criado com WebTracerProvider + OTLPTraceExporter + BatchSpanProcessor + auto-instrumentation fetch/XHR
  - `src/telemetry.ts` mantido como re-export (`import './tracing'`) para não quebrar referências antigas
  - `src/main.ts` atualizado para `import './tracing'`
  - Build Angular: `npm run build` — compilou sem erros

### Correções aplicadas nesta sessão (2026-03-31)

- [x] `application.properties` — removido `conditions` de `management.endpoints.web.exposure.include`
- [x] `rules.yaml` — corrigidos nomes de métricas OTLP (`micrometer-registry-otlp` exporta em `_milliseconds`, não `_seconds`):
  - `http_server_request_duration_seconds_bucket` → `http_server_requests_milliseconds_bucket`
  - `http_server_request_duration_seconds_count` → `http_server_requests_milliseconds_count`
  - Thresholds de latência: `0.5` → `500` ms e `1.0` → `1000` ms
  - Label de rota: `http_route` → `uri` (label correto no OTLP push do Micrometer)

### Próximos passos ⏳

#### Passo 11 — Completar configuração do Elastic Beanstalk 🔴

O app em produção crasha no startup com `PlaceholderResolutionException: Could not resolve placeholder 'OTLP_HTTP_ENDPOINT'` porque o EB foi configurado com nomes errados.

**Ação**: No EB → Configuration → Software → Environment properties, adicionar:

| Variável | Valor |
|---|---|
| `OTLP_HTTP_ENDPOINT` | `https://otlp-gateway-prod-sa-east-1.grafana.net/otlp` |
| `OTLP_AUTH_HEADER` | `<base64(1577558:glc_TOKEN)>` (mesmo valor de `GRAFANA_AUTH_HEADER`) |

> `GRAFANA_OTLP_ENDPOINT` e `GRAFANA_AUTH_HEADER` são usadas apenas pelo container `otel-collector`. O Spring Boot usa `OTLP_HTTP_ENDPOINT` e `OTLP_AUTH_HEADER`.

---

#### Passo 12 — Validar dados no Grafana Cloud ⏳

Após adicionar as variáveis do Passo 11 e o EB reiniciar:

**12.1 — Métricas (Mimir)**
```promql
# Explore → Data Source: Grafana Mimir
http_server_requests_milliseconds_count{job="NutriLongaVidaFlix"}
```

**12.2 — Traces (Tempo)**
```
# Explore → Data Source: Grafana Tempo → TraceQL
{ .service.name = "NutriLongaVidaFlix" }
```

**12.3 — Logs (Loki)**
```logql
# Explore → Data Source: Grafana Loki
{service_name="NutriLongaVidaFlix"} |= "ERROR"
```

**12.4 — Importar dashboards**
1. Grafana Cloud → Dashboards → Import
2. Importar os JSONs de `observability/grafana/provisioning/dashboards/`
3. Ajustar datasource para apontar ao Mimir/Loki/Tempo do Grafana Cloud

---

#### Passo 13 — Criar SLOs no Grafana Cloud ⏳

O Grafana Cloud free tier inclui o **SLO plugin**. Criar dois SLOs:

**SLO 1 — Disponibilidade (99.5%)**

No Grafana Cloud → Alerting → SLOs → Create SLO:
```
Name: vidalongaflix-disponibilidade

Good events:
  sum(rate(http_server_requests_milliseconds_count{http_response_status_code=~"2..",job="NutriLongaVidaFlix"}[5m]))

Total events:
  sum(rate(http_server_requests_milliseconds_count{job="NutriLongaVidaFlix"}[5m]))

Objective: 99.5% — Rolling window: 30 days
```

**SLO 2 — Latência P95 < 500ms (99.5%)**
```
Name: vidalongaflix-latencia-p95

Good events (requests abaixo de 500ms):
  sum(rate(http_server_requests_milliseconds_bucket{le="500",job="NutriLongaVidaFlix"}[5m]))

Total events:
  sum(rate(http_server_requests_milliseconds_count{job="NutriLongaVidaFlix"}[5m]))

Objective: 99.5% — Rolling window: 30 days
```

O plugin gera automaticamente alertas de **Burn Rate** (6x, 14x, 36x) e o gauge de Error Budget.
