# Container Security with Docker Scout and Trivy

## Course Objectives
- Discover security vulnerabilities in containers
- Generate security vulnerability reports
- Manage base image versions
- Apply techniques to prevent security issues
- Automate vulnerability scanning in CI/CD pipelines

---

## Module 1 — Finding Vulnerabilities with Docker Scout

### Key Concepts

**Docker Scout**
Tool that analyzes Docker images against a database of known CVEs (Common Vulnerabilities and Exposures). Works as a security scanner for container images — checks both the OS layer and application dependencies.

**CVE (Common Vulnerabilities and Exposures)**
Unique identifier for a known security vulnerability. Each CVE has:
- **CVSS Score**: 0–10 scale (0 = no risk, 10 = maximum risk)
- **Attack Vector**: how the attacker reaches the vulnerability (Network, Local, Physical)
- **Severity**: Critical, High, Medium, Low

**CVSS Score reading example:**
- Attack Vector = Network → exploitable remotely (worse)
- Attack Vector = Local → attacker needs console access (harder to exploit)
- Confidentiality/Integrity/Availability = High → attacker can steal data, alter data, or crash the service

**Multi-stage Dockerfile**
Best practice that separates the build environment (JDK, Maven, build tools) from the runtime environment (JRE only). Reduces the image attack surface significantly — fewer packages = fewer potential CVEs.

### Commands

```bash
# Install Docker Scout (Linux)
curl -fsSL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh -o install-scout.sh
sh install-scout.sh

# Manual install (if script fails)
mkdir -p ~/.docker/cli-plugins
curl -sSfL https://github.com/docker/scout-cli/releases/download/v1.20.3/docker-scout_1.20.3_linux_amd64.tar.gz | tar -xz -C ~/.docker/cli-plugins
chmod +x ~/.docker/cli-plugins/docker-scout

# Quick overview of an image
docker scout quickview <image>

# List all CVEs
docker scout cves <image>

# Filter by severity
docker scout cves <image> --only-severity critical,high

# View base image update recommendations
docker scout recommendations <image>
```

### Applied to VidaLongaFlix

**Scan result** — `fabricioengeroff/vidalongaflix:latest` (Spring Boot 3.3.3):

| Severity | Count |
|----------|-------|
| Critical | 3     |
| High     | 15    |
| Medium   | 0     |
| Low      | 0     |

**Base image** `eclipse-temurin:17-jre` had 0 critical and 0 high — all 18 vulnerabilities came from Java dependencies inside the JAR.

**Vulnerable packages found:**

| Package | Version | Severity | CVE | Fix |
|---------|---------|----------|-----|-----|
| `spring-security-web` | 6.3.3 | CRITICAL | CVE-2024-38821 — Improper Authorization | 6.3.4 |
| `spring-security-web` | 6.3.3 | CRITICAL | CVE-2026-22732 — Forced Browsing | no fix yet |
| `tomcat-embed-core` | 10.1.28 | CRITICAL 🚨 CISA KEV | CVE-2025-24813 — Path Equivalence → RCE | 10.1.35 |
| `tomcat-embed-core` | 10.1.28 | HIGH (x8) | DoS, race condition, path traversal | 10.1.52 |
| `spring-webmvc` | 6.1.12 | HIGH (x2) | CVE-2024-38816/38819 — Path Traversal | 6.1.14 |
| `commons-beanutils` | 1.9.4 | HIGH | CVE-2025-48734 — Improper Access Control | 1.11.0 |
| `spring-core` | 6.1.12 | HIGH | CVE-2025-41249 — Improper Authorization | no fix yet |
| `spring-boot` | 3.3.3 | HIGH | CVE-2025-22235 — Input Validation | 3.3.11 |
| `jackson-core` | 2.17.2 | HIGH | GHSA-72hv — Resource Allocation DoS | 2.18.6 |
| `spring-security-crypto` | 6.3.3 | HIGH | CVE-2025-22228 — Improper Authentication | 6.3.8 |

> **CISA KEV** = vulnerability actively exploited in the real world. Highest priority to fix.

**Fix applied** in branch `feat/docker-scout-security-fixes` — `pom.xml` changes:

| Change | Before | After | Reason |
|--------|--------|-------|--------|
| Spring Boot parent | 3.3.3 | 3.5.12 | Fixes Tomcat, Spring Security, Spring Framework, Jackson transitively |
| Flyway version override | 9.22.0 (forced) | removed (managed by Spring Boot BOM) | Spring Boot 3.5.x uses Flyway 10.x |
| `commons-beanutils` override | none | 1.11.0 in dependencyManagement | Fixes CVE-2025-48734 from opencsv transitive dep |
| springdoc-openapi | 2.2.0 | 2.8.9 | Compatibility with Spring Boot 3.5.x |
| `spring-boot.version` property | 3.1.0 (wrong/unused) | removed | Cleanup |
| `junit.platform.version` override | 1.10.0 | removed (managed by BOM) | Cleanup |

**Key insight:** Updating the Spring Boot parent version transitively fixes Tomcat, Spring Security, Spring Web, and Jackson because Spring Boot uses a BOM (Bill of Materials) that pins all dependency versions together.

---

## Module 2 — Securing the Container

### How Docker Scout works internally

Docker Scout scans every package installed in the image — both the OS layer (e.g. Ubuntu packages) and the application layer (e.g. JAR dependencies). For each package + version, it checks against a known CVE database. Different versions of the same package can have different CVEs.

### The 3 main vulnerability types

**1. Remote Code Execution (RCE)**
The attacker can run arbitrary code on the server as if they were physically at the terminal. Highest severity. Full access to the system, files, databases, network.

**2. Data Leak**
Sensitive data is exposed. Scope varies — a vulnerability in one module may only expose that module's data (e.g. only transfers, not account balances). Not always a full breach, but still serious.

**3. Denial of Service (DoS)**
A service is made unavailable. Can be scoped: a DoS on the auth service means nobody can log in (all services down); a DoS on a specific endpoint only affects that feature. Performance degradation without full outage is also possible.

> **Rule of thumb:** Low CVSS + Local attack vector + no data exposure = low urgency. High CVSS + Network attack vector + Confidentiality/Integrity/Availability = High = fix immediately.

### Docker Scout recommendations

```bash
docker scout recommendations <image>
```

Shows base image update options in a table with: new tag, size delta, and remaining CVE count after the update. Useful to eliminate OS-layer vulnerabilities with a one-line Dockerfile change.

**Important limitation:** Docker Scout recommendations only cover the base image (OS packages). It cannot suggest fixes for application-level CVEs (your JAR dependencies). Those must be handled by the development team, who needs to update versions, run tests, and validate no regressions before releasing a new image tag.

**Workflow for fixing CVEs in production:**
1. Run `docker scout cves` → generate report
2. Pass report to dev team (application CVEs)
3. Dev team updates dependencies, runs full test suite
4. Rebuild image with updated base + updated app
5. Run `docker scout cves` again on the new image to confirm reduction

### Applied to VidaLongaFlix

Our base image `eclipse-temurin:17-jre` showed 0 critical/high CVEs — all 18 came from JAR dependencies. The fix was updating `pom.xml` (Spring Boot 3.3.3 → 3.5.12), not the Dockerfile base image. After the update, the rebuilt image should show a significantly reduced CVE count.

### Docker Security Best Practices (cheat sheet rules 0–13)

**Rule 0 — Keep host and Docker updated**
Updates are primarily security patches, not just new features. An unpatched host or Docker daemon can expose all containers regardless of how secure the images are.

**Rule 2 — Define a non-root user**
By default, processes inside containers run as `root`. If an attacker achieves RCE inside the container, they have full root access. Adding a dedicated user limits the blast radius.

```dockerfile
# Example — non-root user in Dockerfile
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
```

**Rule 3 — Drop unnecessary capabilities**
`docker run` grants many Linux capabilities by default. Most apps only need CPU, memory, and basic networking. Drop everything else:

```bash
docker run --cap-drop ALL --cap-add NET_BIND_SERVICE <image>
```

**Rule — Network isolation between containers**
By default, all containers share the Docker Bridge network and can reach each other freely (no firewall between them). If one container is compromised, the attacker can pivot laterally to others on the same network.

Solution: create separate networks per application/service. Docker Compose does this automatically per project. With plain `docker run`, you must create and assign networks manually.

```yaml
# docker-compose.yml example
networks:
  backend-net:
  db-net:

services:
  api:
    networks: [backend-net]
  db:
    networks: [db-net, backend-net]
```

> VidaLongaFlix deployment on Elastic Beanstalk runs a single container, so network isolation between services is handled at the AWS VPC/security group level, not at the Docker network level.

### Applied to VidaLongaFlix — Dockerfile hardening

**Before (running as root):**
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /workspace/app.jar ./app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

**After (non-root + proper signal handling):**
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app

# Rule 2: non-root user limits blast radius if container is compromised
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --from=build /workspace/app.jar ./app.jar

# exec replaces the shell with java as PID 1, so SIGTERM reaches the JVM for graceful shutdown
ENV JAVA_OPTS=""
USER appuser
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

**Why `exec` matters:** Without `exec`, the process tree is `sh (PID 1) → java (child)`. Docker sends `SIGTERM` to PID 1 (the shell). The shell may ignore it or not forward it to Java, causing the container to be force-killed after the timeout instead of shutting down gracefully. With `exec`, Java becomes PID 1 and handles `SIGTERM` directly — Spring Boot completes in-flight requests before shutting down.

---

## Module 3 — Finding Vulnerabilities with Trivy

> _Content to be added as the course progresses._

Topics: Trivy installation and configuration, first scan, CVE classification, different scanner targets, optimizing scans.

---

## Module 4 — Reports and Analysis

> _Content to be added as the course progresses._

Topics: generating vulnerability reports (JSON), analyzing different targets, creating non-root users in images.

---

## Module 5 — Integrating Docker Scout and Trivy in CI/CD

> _Content to be added as the course progresses._

Topics: configuring Docker Scout in GitHub Actions, automated analysis with Trivy in pipeline, combining both scanners.

---

## Pre-existing Security in VidaLongaFlix (before the course)

| Feature | Implementation | File |
|---------|---------------|------|
| Multi-stage Dockerfile | JDK for build, JRE-only for runtime | `Dockerfile` |
| `.dockerignore` | Excludes target, git, logs, secrets | `.dockerignore` |
| OIDC AWS auth | Temporary credentials (15min), no long-lived keys | `.github/workflows/ci.yml` |
| JWT authentication | HMAC256, stateless, 2h expiry | `TokenService.java` |
| Login rate limiting | 5 attempts/min per IP (Bucket4j) | `LoginRateLimitFilter.java` |
| CORS | Dynamic origins from env var | `CorsConfig.java` |
| BCrypt passwords | Applied to all user passwords | `DataInitializer.java` |
| Secrets via env vars | No hardcoded credentials in code | `application-prod.properties` |
| Actuator restricted | Only health/info exposed in production | `application-prod.properties` |
| Dependabot | Weekly Maven + GitHub Actions dependency scans | `.github/dependabot.yml` |

## Pending (Phase 2)
- [ ] Docker Scout integrated in CI/CD pipeline
- [ ] Trivy integrated in CI/CD pipeline
- [ ] Gitleaks — secret scanning in git history
- [ ] SonarCloud — SAST static code analysis
