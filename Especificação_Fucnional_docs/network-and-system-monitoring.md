# Redes e Monitoramento do Sistema — VidaLongaFlix

## Contexto

Este documento registra os conceitos de redes estudados e como eles foram aplicados de forma prática no sistema VidaLongaFlix, que está em produção na AWS. O objetivo é alinhar o entendimento técnico com as decisões de implementação, servir como referência futura e documentar o raciocínio por trás de cada melhoria.

O sistema tem limite de 100 usuários cadastrados. Nem todos estarão online ao mesmo tempo. O cenário realista é de 20 a 30 usuários navegando simultaneamente.

---

## O que a AWS já resolve por nós

Antes de qualquer implementação, é importante entender o que a infraestrutura já faz automaticamente. Esses conceitos foram estudados na aula e estão presentes no sistema, mas gerenciados pela AWS:

| Conceito da aula | Como existe no sistema |
|---|---|
| **DHCP** | AWS atribui IPs automaticamente às instâncias EC2 |
| **NAT** | EC2 fica em subnet privada e acessa a internet via NAT Gateway da AWS |
| **VLAN / Sub-redes** | VPC com subnets públicas (Beanstalk) e privadas (RDS) |
| **Roteador de borda** | CloudFront recebe o tráfego externo e roteia para o Load Balancer |
| **STP / Redundância** | AWS gerencia redundância entre zonas de disponibilidade |
| **Subnetting / CIDR** | AWS escolheu os blocos de IP da VPC na criação |

**Conclusão**: a camada de rede está pronta. 
---

## Arquitetura de rede real do sistema

O caminho que uma requisição percorre do usuário até o banco de dados:

```
Usuário (IP público, qualquer rede)
    │
    │  DNS resolve vidalongaflix.com.br
    ▼
CloudFront (IP público da AWS — borda da rede)
    │
    │  HTTPS / TLS — camada de transporte (TCP)
    ▼
Application Load Balancer (AWS)
    │
    │  Roteia para a instância saudável
    ▼
EC2 / Elastic Beanstalk (IP privado: 10.x.x.x — subnet pública da VPC)
    │
    │  JDBC via TCP — conexão interna da VPC
    ▼
RDS PostgreSQL (IP privado — subnet privada, sem acesso externo)
```

Mapeando com os conceitos:
- **DNS** → resolve o nome para o IP do CloudFront
- **TCP** → o protocolo de transporte em todas as etapas
- **NAT** → o EC2 usa IP privado, o CloudFront expõe o IP público
- **ACL / Firewall** → Security Groups da AWS controlam as portas abertas
- **Subnet privada** → o RDS nunca é acessível diretamente da internet (como o servidor restrito da aula)

---

## O que vamos implementar e por quê

### 1. Observação da rede real (sem código)

**Conceito:** ping, traceroute, DNS
**O que faremos:** rodar comandos no terminal contra o sistema em produção para visualizar os conceitos estudados em um ambiente real.

```bash
# Ver como o DNS resolve o domínio para IPs do CloudFront
dig vidalongaflix.com.br
nslookup api.vidalongaflix.com.br

# Ver os "hops" (saltos entre roteadores) até a AWS
traceroute vidalongaflix.com.br

# Medir latência real do servidor
ping vidalongaflix.com.br
```

**O que aprendemos:** quantos roteadores existem entre o usuário e a AWS, qual a latência real, e como o CloudFront distribui o tráfego em múltiplos IPs.

---

### 2. Spring Actuator — o "Wireshark" da aplicação

**Conceito:** Wireshark monitora pacotes na rede. O Actuator monitora requisições na aplicação.
**Status:** dependência já existe no `pom.xml`, mas os endpoints não estão expostos.

**O que faremos:** adicionar configuração no `application.properties` para expor os endpoints de monitoramento.

**Endpoints que serão ativados:**

| Endpoint | O que mostra |
|---|---|
| `GET /api/actuator/health` | Saúde da aplicação: banco conectado, disco, memória |
| `GET /api/actuator/metrics` | Lista de todas as métricas disponíveis |
| `GET /api/actuator/metrics/http.server.requests` | Contagem de requisições, tempo médio, erros por endpoint |
| `GET /api/actuator/info` | Versão e nome da aplicação |

**Por que isso é útil:** sem o Actuator, você manda 25 usuários bater no sistema e só sabe se funcionou ou não. Com ele, você vê em tempo real quantas requisições chegaram, quanto tempo cada endpoint levou e se houve erros. É a visão interna do sistema, equivalente ao Wireshark na rede.

**Atenção em produção:** os endpoints do Actuator devem ser protegidos. Em produção, `health` pode ser público (usado pelo Load Balancer), mas `metrics` deve exigir autenticação ou ficar em porta separada.

---

### 3. Rate Limiting no login — a "ACL" da aplicação

**Conceito:** ACL é uma lista de regras no roteador que permite ou bloqueia tráfego com base na origem e destino. Lida de cima para baixo, a primeira regra que bate é aplicada.
**Problema atual:** o endpoint `POST /api/auth/login` não tem nenhuma proteção contra tentativas excessivas. Um bot pode tentar milhares de combinações de senha sem ser bloqueado.

**O que faremos:** implementar rate limiting no login usando a biblioteca Bucket4j, que funciona com o conceito de "balde de tokens":

```
Regra (ACL):
  - Permitir até 5 tentativas de login por IP a cada 1 minuto
  - Se ultrapassar: bloquear com HTTP 429 (Too Many Requests)
  - Após 1 minuto: liberar novamente
```

**Por que 5 tentativas:** um usuário real que esqueceu a senha tenta no máximo 2 ou 3 vezes. 5 é generoso o suficiente para não atrapalhar usuários legítimos e restritivo o suficiente para bloquear ataques automatizados.

**conclusão:** é exatamente a ACL extended, já no nível de aplicação: verificamos a origem (IP), o destino (endpoint de login) e decidimos se liberamos ou bloqueamos.

---

### 4. Teste de carga realista com k6

**Conceito:** ping e traceroute testam conectividade e latência. O k6 testa como o sistema se comporta sob carga real.
**Contexto:** com 100 usuários cadastrados e não todos online ao mesmo tempo, o cenário realista é 20 a 30 navegando simultaneamente.

**O que faremos:** criar um script k6 que simula o fluxo real de um usuário:

```
25 usuários virtuais, durante 2 minutos:
  1. POST /auth/login          → autentica e obtém JWT
  2. GET /videos               → carrega o catálogo
  3. GET /comments/video/{id}  → carrega comentários de um vídeo
  4. POST /favorites/VIDEO/{id}→ favorita um vídeo
  5. PATCH /videos/{id}/view   → registra visualização
```

**O que vamos medir:**

| Métrica | O que significa |
|---|---|
| `p95` | 95% das requisições responderam em menos de X ms |
| `p99` | 99% das requisições responderam em menos de X ms |
| Taxa de erro | Percentual de requisições que falharam |
| Requests/s | Quantas requisições por segundo o sistema processa |

**Conexão — connection pool como "domínio de broadcast":**
No sistema, muitas requisições simultâneas podem esgotar o pool de conexões do banco de dados (HikariCP, padrão de 10 conexões). O teste vai revelar se isso acontece e qual é o limite real.

---

## Ordem de implementação

```
Etapa 1 — Observar (sem código, agora)
  → dig, traceroute, ping no terminal

Etapa 2 — Monitorar (configuração)
  → Expor Spring Actuator

Etapa 3 — Proteger (implementação)
  → Rate limiting no /auth/login com Bucket4j

Etapa 4 — Testar (script)
  → k6 com 25 usuários, fluxo real, 2 minutos
  → Observar métricas no Actuator durante o teste
```

---

## Resultado esperado

Ao final das quatro etapas, o sistema terá:

1. **Visibilidade**: saber em tempo real o que está acontecendo dentro da aplicação
2. **Proteção**: login protegido contra tentativas automatizadas
3. **Evidência**: dados concretos sobre o comportamento do sistema sob carga real
4. **Conhecimento**: os conceitos da aula de redes aplicados em um sistema em produção, com registros e resultados documentados

---

*Documento criado em: março de 2026*
*Referência: aula de Redes — conceitos de DNS, Ping, Traceroute, Wireshark, ACL, VLAN, NAT, DHCP, STP aplicados ao VidaLongaFlix*