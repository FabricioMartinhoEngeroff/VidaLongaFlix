# Segurança de Containers com Docker Scout e Trivy

## Objetivos do Curso
- Descobrir vulnerabilidades de segurança em containers
- Gerar relatórios de vulnerabilidades
- Gerenciar versões de imagens base
- Aplicar técnicas para prevenir problemas de segurança
- Automatizar o escaneamento de vulnerabilidades em pipelines CI/CD

---

## Módulo 1 — Encontrando Vulnerabilidades com Docker Scout

### Conceitos Principais

**Docker Scout**
Ferramenta que analisa imagens Docker contra um banco de dados de CVEs conhecidos (Common Vulnerabilities and Exposures). Funciona como um scanner de segurança para imagens de container — verifica tanto a camada do sistema operacional quanto as dependências da aplicação.

**CVE (Common Vulnerabilities and Exposures)**
Identificador único para uma vulnerabilidade de segurança conhecida. Cada CVE possui:
- **CVSS Score**: escala de 0 a 10 (0 = sem risco, 10 = risco máximo)
- **Attack Vector**: como o atacante chega à vulnerabilidade (Network, Local, Physical)
- **Severity**: Critical, High, Medium, Low

**Exemplo de leitura do CVSS Score:**
- Attack Vector = Network → explorável remotamente (pior cenário)
- Attack Vector = Local → atacante precisa de acesso ao console (mais difícil de explorar)
- Confidentiality/Integrity/Availability = High → atacante pode roubar dados, alterar dados ou derrubar o serviço

**Dockerfile Multi-stage**
Boa prática que separa o ambiente de build (JDK, Maven, ferramentas de build) do ambiente de runtime (apenas JRE). Reduz significativamente a superfície de ataque da imagem — menos pacotes = menos CVEs potenciais.

### Comandos

```bash
# Instalar Docker Scout (Linux)
curl -fsSL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh -o install-scout.sh
sh install-scout.sh

# Instalação manual (se o script falhar)
mkdir -p ~/.docker/cli-plugins
curl -sSfL https://github.com/docker/scout-cli/releases/download/v1.20.3/docker-scout_1.20.3_linux_amd64.tar.gz | tar -xz -C ~/.docker/cli-plugins
chmod +x ~/.docker/cli-plugins/docker-scout

# Visão geral rápida de uma imagem
docker scout quickview <image>

# Listar todos os CVEs
docker scout cves <image>

# Filtrar por severidade
docker scout cves <image> --only-severity critical,high

# Ver recomendações de atualização da imagem base
docker scout recommendations <image>
```

### Aplicado ao VidaLongaFlix

**Resultado do scan** — `fabricioengeroff/vidalongaflix:latest` (Spring Boot 3.3.3):

| Severidade | Quantidade |
|------------|-----------|
| Critical   | 3         |
| High       | 15        |
| Medium     | 0         |
| Low        | 0         |

**Imagem base** `eclipse-temurin:17-jre` tinha 0 críticos e 0 altos — todas as 18 vulnerabilidades vieram das dependências Java dentro do JAR.

**Pacotes vulneráveis encontrados:**

| Pacote | Versão | Severidade | CVE | Correção |
|--------|--------|------------|-----|----------|
| `spring-security-web` | 6.3.3 | CRITICAL | CVE-2024-38821 — Autorização Inadequada | 6.3.4 |
| `spring-security-web` | 6.3.3 | CRITICAL | CVE-2026-22732 — Forced Browsing | sem correção ainda |
| `tomcat-embed-core` | 10.1.28 | CRITICAL 🚨 CISA KEV | CVE-2025-24813 — Path Equivalence → RCE | 10.1.35 |
| `tomcat-embed-core` | 10.1.28 | HIGH (x8) | DoS, race condition, path traversal | 10.1.52 |
| `spring-webmvc` | 6.1.12 | HIGH (x2) | CVE-2024-38816/38819 — Path Traversal | 6.1.14 |
| `commons-beanutils` | 1.9.4 | HIGH | CVE-2025-48734 — Controle de Acesso Inadequado | 1.11.0 |
| `spring-core` | 6.1.12 | HIGH | CVE-2025-41249 — Autorização Inadequada | sem correção ainda |
| `spring-boot` | 3.3.3 | HIGH | CVE-2025-22235 — Validação de Input | 3.3.11 |
| `jackson-core` | 2.17.2 | HIGH | GHSA-72hv — DoS por Alocação de Recursos | 2.18.6 |
| `spring-security-crypto` | 6.3.3 | HIGH | CVE-2025-22228 — Autenticação Inadequada | 6.3.8 |

> **CISA KEV** = vulnerabilidade ativamente explorada no mundo real. Prioridade máxima para correção.

**Correção aplicada** na branch `feat/docker-scout-security-fixes` — mudanças no `pom.xml`:

| Mudança | Antes | Depois | Motivo |
|---------|-------|--------|--------|
| Spring Boot parent | 3.3.3 | 3.5.12 | Corrige Tomcat, Spring Security, Spring Framework, Jackson transitivamente |
| Override versão Flyway | 9.22.0 (forçado) | removido (gerenciado pelo BOM do Spring Boot) | Spring Boot 3.5.x usa Flyway 10.x |
| Override `commons-beanutils` | nenhum | 1.11.0 em dependencyManagement | Corrige CVE-2025-48734 da dep transitiva opencsv |
| springdoc-openapi | 2.2.0 | 2.8.9 | Compatibilidade com Spring Boot 3.5.x |
| Propriedade `spring-boot.version` | 3.1.0 (errada/não usada) | removida | Limpeza |
| Override `junit.platform.version` | 1.10.0 | removido (gerenciado pelo BOM) | Limpeza |

**Conceito chave:** Atualizar a versão do parent do Spring Boot corrige transitivamente o Tomcat, Spring Security, Spring Web e Jackson, porque o Spring Boot usa um BOM (Bill of Materials) que fixa todas as versões de dependências juntas.

---

## Módulo 2 — Protegendo o Container

### Como o Docker Scout funciona internamente

O Docker Scout escaneia todos os pacotes instalados na imagem — tanto a camada do SO (ex: pacotes Ubuntu) quanto a camada da aplicação (ex: dependências JAR). Para cada pacote + versão, ele verifica contra um banco de dados de CVEs conhecidos. Versões diferentes do mesmo pacote podem ter CVEs diferentes.

### Os 3 principais tipos de vulnerabilidade

**1. Remote Code Execution (RCE)**
O atacante consegue executar código arbitrário no servidor como se estivesse fisicamente no terminal. Maior severidade. Acesso total ao sistema, arquivos, bancos de dados, rede.

**2. Vazamento de Dados**
Dados sensíveis são expostos. O escopo varia — uma vulnerabilidade em um módulo pode expor apenas os dados daquele módulo (ex: apenas transferências, não saldos de contas). Nem sempre é uma violação total, mas ainda é grave.

**3. Denial of Service (DoS)**
Um serviço fica indisponível. Pode ser localizado: um DoS no serviço de autenticação significa que ninguém consegue logar (todos os serviços caem); um DoS em um endpoint específico afeta apenas aquela funcionalidade. Degradação de performance sem indisponibilidade total também é possível.

> **Regra geral:** CVSS baixo + vetor de ataque Local + sem exposição de dados = baixa urgência. CVSS alto + vetor de ataque Network + Confidentiality/Integrity/Availability = High = corrigir imediatamente.

### Docker Scout recommendations

```bash
docker scout recommendations <image>
```

Mostra opções de atualização da imagem base em uma tabela com: nova tag, delta de tamanho e contagem de CVEs restantes após a atualização. Útil para eliminar vulnerabilidades da camada do SO com uma mudança de uma linha no Dockerfile.

**Limitação importante:** As recomendações do Docker Scout cobrem apenas a imagem base (pacotes do SO). Ele não consegue sugerir correções para CVEs no nível da aplicação (suas dependências JAR). Essas devem ser tratadas pelo time de desenvolvimento, que precisa atualizar versões, rodar a suite de testes e validar que não há regressões antes de publicar uma nova tag de imagem.

**Fluxo para corrigir CVEs em produção:**
1. Rodar `docker scout cves` → gerar relatório
2. Passar relatório para o time de dev (CVEs da aplicação)
3. Time de dev atualiza dependências, roda suite completa de testes
4. Rebuildar imagem com base atualizada + app atualizado
5. Rodar `docker scout cves` novamente na nova imagem para confirmar redução

### Aplicado ao VidaLongaFlix

Nossa imagem base `eclipse-temurin:17-jre` mostrou 0 CVEs críticos/altos — todos os 18 vieram das dependências JAR. A correção foi atualizar o `pom.xml` (Spring Boot 3.3.3 → 3.5.12), não a imagem base do Dockerfile. Após a atualização, a imagem reconstruída deve mostrar uma redução significativa na contagem de CVEs.

### Boas Práticas de Segurança Docker (regras 0–13 do cheat sheet)

**Regra 0 — Manter o host e o Docker atualizados**
Atualizações são principalmente patches de segurança, não apenas novas funcionalidades. Um host ou daemon Docker sem patches pode expor todos os containers independentemente de quão seguras sejam as imagens.

**Regra 2 — Definir um usuário não-root**
Por padrão, processos dentro de containers rodam como `root`. Se um atacante conseguir RCE dentro do container, ele terá acesso root total. Adicionar um usuário dedicado limita o raio de impacto.

```dockerfile
# Exemplo — usuário não-root no Dockerfile
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
```

**Regra 3 — Remover capabilities desnecessárias**
`docker run` concede muitas capabilities Linux por padrão. A maioria das aplicações precisa apenas de CPU, memória e rede básica. Remove todo o resto:

```bash
docker run --cap-drop ALL --cap-add NET_BIND_SERVICE <image>
```

**Regra — Isolamento de rede entre containers**
Por padrão, todos os containers compartilham a rede Bridge do Docker e podem se alcançar livremente (sem firewall entre eles). Se um container for comprometido, o atacante pode se mover lateralmente para outros na mesma rede.

Solução: criar redes separadas por aplicação/serviço. Docker Compose faz isso automaticamente por projeto. Com `docker run` simples, você deve criar e atribuir redes manualmente.

```yaml
# Exemplo docker-compose.yml
networks:
  backend-net:
  db-net:

services:
  api:
    networks: [backend-net]
  db:
    networks: [db-net, backend-net]
```

> O deploy do VidaLongaFlix no Elastic Beanstalk roda um único container, portanto o isolamento de rede entre serviços é feito no nível do VPC/security group da AWS, não no nível da rede Docker.

### Aplicado ao VidaLongaFlix — Hardening do Dockerfile

**Antes (rodando como root):**
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /workspace/app.jar ./app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

**Depois (non-root + tratamento correto de sinais):**
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app

# Regra 2: usuário não-root limita o raio de impacto se o container for comprometido
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --from=build /workspace/app.jar ./app.jar

# exec substitui o shell pelo java como PID 1, para que SIGTERM chegue ao JVM no shutdown gracioso
ENV JAVA_OPTS=""
USER appuser
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

**Por que o `exec` importa:** Sem `exec`, a árvore de processos é `sh (PID 1) → java (filho)`. O Docker envia `SIGTERM` para o PID 1 (o shell). O shell pode ignorá-lo ou não repassá-lo para o Java, fazendo o container ser forçosamente encerrado após o timeout em vez de fazer um shutdown gracioso. Com `exec`, o Java se torna o PID 1 e trata o `SIGTERM` diretamente — o Spring Boot completa as requisições em andamento antes de encerrar.

---

## Módulo 3 — Encontrando Vulnerabilidades com Trivy

### Por que usar Trivy via Docker (sem instalação local)

O Trivy possui uma imagem Docker oficial. Em vez de instalar o binário na máquina, você o executa como container. Regra: onde o curso escrever `trivy image`, substitua por `docker run <imagem-trivy> image`. O resultado é idêntico.

**Importante:** A tag `:latest` não está publicada no Docker Hub (`aquasec/trivy`). O repositório oficial mantido pelo projeto está no GitHub Container Registry. Sempre use `ghcr.io/aquasecurity/trivy:latest` ou uma versão pinada como `ghcr.io/aquasecurity/trivy:0.62.0`.

### Montando o socket Docker (obrigatório para escanear imagens locais)

Quando o Trivy roda dentro de um container, ele está isolado do Docker do host e não enxerga as imagens locais. Para dar acesso, é preciso montar o socket Unix do Docker com `-v /var/run/docker.sock:/var/run/docker.sock`:

```
[Container Trivy] ──→ /var/run/docker.sock ──→ [Docker daemon do host] ──→ imagem local
```

Sem isso, o Trivy tentaria buscar a imagem no Docker Hub e falharia se ela não estiver publicada lá.

### Comandos de scan

```bash
# Scan básico — mostra no terminal
docker run -v /var/run/docker.sock:/var/run/docker.sock ghcr.io/aquasecurity/trivy:latest image <image>

# Exportar para arquivo (recomendado — permite comparar versões no git)
docker run -v /var/run/docker.sock:/var/run/docker.sock ghcr.io/aquasecurity/trivy:latest image <image> > cve.txt
```

O `cve.txt` no controle de versão permite fazer diff da contagem de vulnerabilidades entre releases — ex: "saímos de 25 médios para 8 médios após a atualização."

### Estrutura do relatório CVE (colunas explicadas)

| Coluna    | O que significa |
|-----------|----------------|
| Library   | Pacote afetado (ex: `tomcat-embed-core`) |
| CVE       | Identificador da vulnerabilidade (ex: CVE-2025-24813) |
| Severity  | Low / Medium / High / Critical |
| Status    | `affected` = sem correção ainda · `fixed` = versão corrigida existe |
| Installed | Versão atualmente no seu container |
| Fixed     | Versão onde o problema foi corrigido |
| Title     | Descrição curta + link para detalhes completos do CVE |

O CVSS Score mostrado no link do CVE pode variar dependendo de quem o pontuou (NVD, Red Hat, etc.) — scorers diferentes usam critérios e escalas diferentes (v2 vs v3). Isso explica por que o mesmo CVE pode aparecer como "Low" em um banco de dados e "High" em outro.

### Scanners — o que o Trivy verifica por padrão

| Scanner     | Padrão        | O que faz |
|-------------|--------------|-----------|
| `vuln`      | ✅ habilitado | CVEs de pacotes do SO + dependências da aplicação |
| `secret`    | ✅ habilitado | Procura segredos (senhas, tokens) no filesystem |
| `misconfig` | ❌ desabilitado | Erros de configuração em IaC (YAML Kubernetes, Terraform, etc.) |
| `license`   | ❌ desabilitado | Conformidade de licenças dos pacotes instalados |

O scanner de segredos é lento em imagens grandes mas não encontra nada em uma imagem bem construída (sem segredos embutidos na camada). Para pular e escanear apenas vulnerabilidades:

```bash
docker run -v /var/run/docker.sock:/var/run/docker.sock ghcr.io/aquasecurity/trivy:latest image --scanners vuln <image> > cve.txt
```

Se a coluna Secrets mostrar `-` no relatório, significa que o scan de segredos foi pulado.

### Trivy vs Docker Scout — por que rodar os dois

São ferramentas independentes com bancos de dados de CVEs diferentes. Na mesma imagem, o Docker Scout pode encontrar 5 Críticos enquanto o Trivy encontra 4 — ou o Trivy encontra 1000 onde o Scout encontrou 350. **Boa prática: rodar os dois e corrigir tudo que cada um reportar.**

| Ferramenta    | Ponto forte |
|---------------|------------|
| Docker Scout  | Integração profunda com Docker Hub, recomendações de imagem base |
| Trivy         | Banco de dados de CVEs mais amplo, suporta mais tipos de alvo (filesystem, repositórios git, K8s, etc.) |

### Aplicado ao VidaLongaFlix

Imagem: `fabricioengeroff/vidalongaflix:latest` (Spring Boot 3.5.12, base `eclipse-temurin:17-jre`)

```bash
# Passo 1 — build da imagem
docker build -t fabricioengeroff/vidalongaflix:latest .

# Passo 2 — escanear apenas vulnerabilidades (pular scan lento de segredos)
docker run -v /var/run/docker.sock:/var/run/docker.sock ghcr.io/aquasecurity/trivy:latest image --scanners vuln fabricioengeroff/vidalongaflix:latest > cve.txt

# Passo 3 — commitar o cve.txt para rastrear versões
git add cve.txt && git commit -m "security: add Trivy CVE report for vidalongaflix:latest"
```

### Resultado real do scan (Spring Boot 3.5.12)

```
Report Summary
┌──────────────────────────────────────────────────────┬────────┬─────────────────┐
│                        Target                        │  Type  │ Vulnerabilities │
├──────────────────────────────────────────────────────┼────────┼─────────────────┤
│ fabricioengeroff/vidalongaflix:latest (ubuntu 24.04) │ ubuntu │       29        │
├──────────────────────────────────────────────────────┼────────┼─────────────────┤
│ app/app.jar                                          │  jar   │        3        │
└──────────────────────────────────────────────────────┴────────┴─────────────────┘
```

**Ubuntu 24.04 — 29 CVEs (13 LOW, 16 MEDIUM, 0 HIGH, 0 CRITICAL)**

Todas têm `Status: affected` sem `Fixed Version` — o Ubuntu ainda não lançou patches. São pacotes do SO como GnuPG, libpam, tar, wget. Não há ação possível no Dockerfile agora. Aguardar atualizações do Ubuntu.

**app.jar — 3 CVEs (0 LOW, 2 MEDIUM, 1 HIGH, 0 CRITICAL)**

| Pacote | Versão | Severidade | CVE | Problema | Fix |
|--------|--------|------------|-----|----------|-----|
| `jackson-core` | 2.19.4 | HIGH | GHSA-72hv | DoS via parser assíncrono — bypass de limite de tamanho | 2.21.1 |
| `nimbus-jose-jwt` | 9.47 | MEDIUM | CVE-2025-53864 | Recursão descontrolada no parser JWT | 10.0.2 |
| `commons-lang3` | 3.17.0 | MEDIUM | CVE-2025-48924 | Recursão descontrolada | 3.18.0 |

**Comparação com Docker Scout (Módulo 1, mesma imagem com Spring Boot 3.3.3):**

| | Docker Scout (antes) | Trivy (depois) |
|-|---------------------|----------------|
| CRITICAL (JAR) | 3 | 0 |
| HIGH (JAR) | 15 | 1 |
| MEDIUM (JAR) | 0 | 2 |

O upgrade do Spring Boot 3.3.3 → 3.5.12 eliminou 17 dos 18 CVEs do JAR. Restaram 3 CVEs que requerem pins adicionais no `pom.xml` (ver pendências abaixo).

> **Nota:** Docker Scout encontrou 18 CVEs no JAR na versão 3.3.3 e Trivy encontrou 3 na 3.5.12. Contagens diferentes entre as ferramentas são normais — bancos de dados e critérios de classificação distintos. Rodar os dois e corrigir o que cada um reportar é a boa prática.

---

## Módulo 4 — Relatórios e Análise

> _Conteúdo a ser adicionado conforme o curso avança._

Tópicos: geração de relatórios de vulnerabilidades (JSON), análise de diferentes alvos, criação de usuários não-root em imagens.

---

## Módulo 5 — Integrando Docker Scout e Trivy no CI/CD

### Por que automatizar os scanners?

Sem CI/CD, para cada nova versão você precisa: baixar o código, instalar as ferramentas, lembrar todos os parâmetros na ordem certa, rodar manualmente. Com GitHub Actions isso acontece automaticamente em todo push — sem intervenção humana.

### GitHub Secrets — como armazenar credenciais com segurança

Senhas e tokens **não podem ficar no código** (ficam visíveis no GitHub para qualquer pessoa com acesso). A solução são os GitHub Secrets:

1. `Settings → Secrets and variables → Actions → New repository secret`
2. Dê um nome (ex: `DOCKERHUB_TOKEN`) e cole o valor
3. No workflow, referencie com `${{ secrets.DOCKERHUB_TOKEN }}`
4. O valor nunca aparece nos logs — fica mascarado como `***`

### Docker Scout Action

```yaml
- name: Docker Scout
  uses: docker/scout-action@v1.17.0
  with:
    command: quickview          # resumo: Critical/High/Medium/Low por camada
    image: usuario/app:tag
    ignore-base: false          # false = reportar CVEs da imagem base também
    dockerhub-user: ${{ secrets.DOCKERHUB_USERNAME }}
    dockerhub-password: ${{ secrets.DOCKERHUB_TOKEN }}
```

O `quickview` é suficiente para o CI — mostra o resumo de contagens sem listar cada CVE individualmente (menos ruído nos logs).

### Trivy Action

```yaml
- name: Trivy
  uses: aquasecurity/trivy-action@0.30.0
  with:
    image-ref: usuario/app:tag
    scan-type: image
    scanners: vuln              # só vulnerabilidades (pular scan de segredos)
    severity: CRITICAL,HIGH     # filtrar apenas o que importa
    format: table
    exit-code: '0'              # 0 = reportar mas não falhar o build
```

### `exit_code: 1` — trava de segurança no pipeline

Quando `exit-code: '1'`, o step retorna erro se encontrar CVEs com a severidade configurada. Isso impede merges enquanto houver vulnerabilidade conhecida.

```
push → build → Scout → Trivy ✗ → pipeline vermelho → merge bloqueado
```

> **Quando ligar:** Só após ter zerado todos os críticos/altos. Se ligar com CVEs ativos, o pipeline fica permanentemente vermelho e ninguém consegue fazer deploy.

### Por que rodar os dois no CI?

A aula demonstrou: o Scout **não encontrou** uma vulnerabilidade no Ubuntu 24.10, mas o Trivy **encontrou**. Dois bancos de dados independentes = cobertura maior. Custo zero — ambos os GitHub Actions são gratuitos para repositórios públicos.

### Aplicado ao VidaLongaFlix

O projeto já tem `docker.yml` que constrói e faz push da imagem. Os scanners entram **depois do build**, usando a imagem recém-construída.

```yaml
# Adicionado no final de .github/workflows/docker.yml
# após o step "Build (e Push)"

- name: Docker Scout — quickview
  if: ${{ secrets.DOCKERHUB_TOKEN != '' }}
  uses: docker/scout-action@v1.17.0
  with:
    command: quickview
    image: ${{ env.IMAGE_NAME }}:${{ github.sha }}
    ignore-base: false
    dockerhub-user: ${{ secrets.DOCKERHUB_USERNAME }}
    dockerhub-password: ${{ secrets.DOCKERHUB_TOKEN }}

- name: Trivy — scan de vulnerabilidades
  uses: aquasecurity/trivy-action@0.30.0
  with:
    image-ref: ${{ env.IMAGE_NAME }}:${{ github.sha }}
    scan-type: image
    scanners: vuln
    severity: CRITICAL,HIGH
    format: table
    exit-code: '0'    # manter 0 enquanto houver CVEs conhecidos sem correção
```

**Por que `exit-code: 0` agora?** Após o upgrade do Spring Boot (Módulo 1), restaram 3 CVEs no JAR (1 HIGH, 2 MEDIUM) e 29 CVEs no Ubuntu sem fix disponível. Com `exit-code: 1`, o pipeline ficaria permanentemente bloqueado. Quando esses CVEs tiverem correção e forem aplicados, alterar para `exit-code: 1`.

**Pendência:** adicionar esses steps ao `docker.yml` (ver seção de pendências abaixo).

---

## Segurança Pré-existente no VidaLongaFlix (antes do curso)

| Funcionalidade | Implementação | Arquivo |
|----------------|--------------|---------|
| Dockerfile Multi-stage | JDK para build, apenas JRE para runtime | `Dockerfile` |
| `.dockerignore` | Exclui target, git, logs, segredos | `.dockerignore` |
| Autenticação AWS com OIDC | Credenciais temporárias (15min), sem chaves de longa duração | `.github/workflows/ci.yml` |
| Autenticação JWT | HMAC256, stateless, expiração em 2h | `TokenService.java` |
| Rate limiting no login | 5 tentativas/min por IP (Bucket4j) | `LoginRateLimitFilter.java` |
| CORS | Origins dinâmicas via variável de ambiente | `CorsConfig.java` |
| Senhas com BCrypt | Aplicado a todas as senhas de usuários | `DataInitializer.java` |
| Segredos via variáveis de ambiente | Sem credenciais hardcoded no código | `application-prod.properties` |
| Actuator restrito | Apenas health/info expostos em produção | `application-prod.properties` |
| Dependabot | Scans semanais de dependências Maven + GitHub Actions | `.github/dependabot.yml` |

## Pendências (Fase 2)
- [x] Docker Scout integrado no pipeline CI/CD (`docker.yml`)
- [x] Trivy integrado no pipeline CI/CD (`docker.yml`)
- [ ] Ativar `exit-code: 1` no Trivy quando os 3 CVEs restantes do JAR tiverem correção
- [ ] Gitleaks — scan de segredos no histórico git
- [ ] SonarCloud — análise estática de código (SAST)
