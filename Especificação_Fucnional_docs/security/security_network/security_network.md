# Segurança de Redes — Fundamentos e Práticas no VidaLongaFlix

> Baseado no curso "Cibersegurança: Fundamentos e Práticas de Redes" (181 páginas).
> Este documento descreve: fundamentos completos de redes e segurança, protocolos e seus vetores de ataque, ferramentas de análise e defesa, vulnerabilidades reais mapeadas no projeto (backend Spring Boot + frontend Angular), cenários de teste com resultado esperado, e passo a passo de implementação por sprint.

---

## Parte 1 — Fundamentos de Redes e Segurança

---

### Modelo OSI — As 7 Camadas

O **Modelo OSI (Open Systems Interconnection)** é o modelo teórico de referência para comunicação em redes. Divide a comunicação em 7 camadas, cada uma com responsabilidade específica:

| Camada | Nome | Função | Protocolos/Tecnologias |
|---|---|---|---|
| 7 | **Aplicação** | Interface com o usuário e aplicações | HTTP, HTTPS, FTP, SMTP, DNS, SSH |
| 6 | **Apresentação** | Codificação, criptografia, compressão | TLS/SSL, JPEG, ASCII, Base64 |
| 5 | **Sessão** | Gerenciamento de sessões entre hosts | NetBIOS, RPC, SQL Session |
| 4 | **Transporte** | Entrega confiável ponta a ponta, portas lógicas | TCP, UDP |
| 3 | **Rede** | Endereçamento lógico e roteamento | IP, ICMP, ARP, BGP, OSPF |
| 2 | **Enlace** | Endereçamento físico (MAC), quadros | Ethernet, Wi-Fi (802.11), ARP |
| 1 | **Física** | Transmissão de bits no meio físico | Cabos, fibra, rádio, sinais elétricos |

**Importância para segurança**: ataques ocorrem em camadas específicas. Sniffing atua na camada 2, IP Spoofing na camada 3, SYN Flood na camada 4, XSS/SQLi na camada 7. Firewalls, IDS e WAF operam em camadas diferentes.

---

### Modelo TCP/IP — As 4 Camadas

O **Modelo TCP/IP** é o modelo prático implementado na internet. Agrupa as camadas do OSI em 4:

| Camada TCP/IP | Equivalente OSI | Protocolos principais |
|---|---|---|
| **Aplicação** | Camadas 5, 6, 7 | HTTP, HTTPS, DNS, FTP, SMTP, SSH, Telnet |
| **Transporte** | Camada 4 | TCP, UDP |
| **Internet** | Camada 3 | IP, ICMP, ARP |
| **Acesso à Rede** | Camadas 1, 2 | Ethernet, Wi-Fi, driver de interface |

---

### Endereçamento IP — IPv4, IPv6, CIDR e NAT

**IPv4**: endereço de 32 bits escrito em 4 octetos decimais (ex: `192.168.1.1`). Esgotamento do espaço público gerou o NAT.

**IPv6**: endereço de 128 bits em hexadecimal (ex: `2001:0db8:85a3::8a2e:0370:7334`). Resolve o esgotamento do IPv4 e tem IPSec nativo.

**CIDR (Classless Inter-Domain Routing)**: notação de máscara de rede — `/24` = 256 endereços, `/16` = 65.536 endereços. Exemplo: `192.168.1.0/24` = rede com IPs de `192.168.1.1` a `192.168.1.254`.

**NAT (Network Address Translation)**: converte endereços privados (192.168.x.x, 10.x.x.x, 172.16-31.x.x) em públicos. Permite que múltiplos hosts internos compartilhem um único IP público. Dificulta rastreamento de origem em ataques.

**DMZ (Demilitarized Zone)**: segmento de rede exposto à internet (servidores web, APIs, CDN) separado da rede interna. Limita o blast radius se um servidor público for comprometido.

---

### TCP — Protocolo de Transporte Confiável

O **TCP (Transmission Control Protocol)** garante entrega ordenada, sem perda, com verificação de erros.

#### Three-Way Handshake

```
Cliente  ──SYN──►  Servidor
Cliente  ◄──SYN-ACK──  Servidor
Cliente  ──ACK──►  Servidor
[conexão estabelecida]
```

#### Flags TCP e seus significados

| Flag | Descrição | Uso em ataques |
|---|---|---|
| **SYN** | Inicia conexão | SYN Flood — envia milhares de SYN sem completar o handshake |
| **ACK** | Confirmação de recebimento | Presente em toda comunicação estabelecida |
| **FIN** | Encerra conexão graciosamente | — |
| **RST** | Termina conexão abruptamente | IPS usa para bloquear conexões suspeitas |
| **PSH** | Envia dados imediatamente sem buffer | — |
| **URG** | Dados urgentes (raro) | XMAS Scan ativa SYN+URG+FIN para fingerprinting |

#### Ataques TCP

| Ataque | Mecanismo | Impacto | Defesa |
|---|---|---|---|
| **SYN Flood** | Envia inúmeros SYN sem completar handshake — esgota tabela de conexões do servidor | DoS | SYN Cookies, Firewall Stateful, rate limiting por IP |
| **SYN Scan** | Envia SYN, recebe SYN-ACK, envia RST — mapeia portas abertas sem completar conexão | Reconhecimento | IDS detecta padrão; Firewall bloqueia varreduras |
| **XMAS Scan** | Liga flags SYN+URG+FIN — resposta indica estado da porta | Fingerprinting OS | IDS detecta combinação de flags anormal |
| **Session Hijacking** | Sequestra sequência TCP para inserir dados numa sessão estabelecida | MitM | TLS — criptografia torna o sequestro inútil |

---

### UDP — Protocolo Sem Conexão

O **UDP (User Datagram Protocol)** não garante entrega, sem handshake, mas é muito mais rápido. Usado em: DNS (port 53), VoIP, streaming, games, NTP.

**UDP Amplification Attack**: atacante envia pequenas queries UDP com IP de origem falsificado (da vítima) para servidores que respondem com respostas muito maiores. Fator de amplificação:
- DNS: ~28x-50x
- NTP: até 556x
- SSDP: até 30x

---

### Portas Lógicas — Referência de Portas Importantes

| Porta | Protocolo | Serviço | Notas de Segurança |
|---|---|---|---|
| 20/21 | TCP | FTP | Sem criptografia — substituir por SFTP (22) ou FTPS |
| 22 | TCP | SSH / SFTP | Chave RSA obrigatória; desativar autenticação por senha |
| 23 | TCP | Telnet | **Obsoleto** — sem criptografia, substituir por SSH |
| 25 | TCP | SMTP | Relay aberto — configurar SPF/DKIM/DMARC |
| 53 | TCP/UDP | DNS | Alvo de spoofing e cache poisoning — usar DNSSEC |
| 67/68 | UDP | DHCP | Vulnerável a spoofing e starvation |
| 80 | TCP | HTTP | Sem criptografia — redirecionar para HTTPS (443) |
| 110 | TCP | POP3 | Email sem criptografia |
| 123 | UDP | NTP | Crítico para logs forenses; vulnerável a amplificação |
| 139/445 | TCP | SMB | SMB v1 = EternalBlue/WannaCry — desativar v1 |
| 143 | TCP | IMAP | Email sem criptografia |
| 443 | TCP | HTTPS | TLS obrigatório; certificado válido e atualizado |
| 587 | TCP | SMTP (TLS) | Envio autenticado de email |
| 853 | TCP | DNS over TLS | DNS criptografado |
| 3306 | TCP | MySQL/MariaDB | Nunca expor publicamente |
| 3389 | TCP | RDP | Alvo frequente de brute force — expor apenas via VPN |
| 8080 | TCP | HTTP alt | Porta alternativa HTTP — comum em ambientes de dev |
| 8090 | TCP | HTTP (VidaLongaFlix) | Backend local do projeto |

---

### ARP — Resolução de Endereços

**ARP (Address Resolution Protocol)** traduz IPs para endereços MAC na camada 2. Não tem autenticação.

#### ARP Spoofing (ARP Poisoning)
O atacante envia respostas ARP falsas, associando seu MAC ao IP da vítima ou do gateway. Isso redireciona o tráfego da rede para o atacante — ataque MitM (Man-in-the-Middle).

**Impacto**: captura de credenciais, sessões HTTP, tokens JWT.
**Defesa**: Dynamic ARP Inspection (DAI) em switches gerenciados, uso de TLS (criptografia torna captura inútil), segmentação de rede.

#### MAC Flooding
Envia inúmeros quadros Ethernet com MACs falsificados ao switch, esgotando a tabela CAM. O switch passa a funcionar como hub — transmitindo tráfego para todas as portas. Atacante captura tráfego de todos na rede.

**Defesa**: Port Security nos switches — limita número de MACs por porta.

---

### DNS — Sistema de Nomes de Domínio

**DNS (Domain Name System)** converte nomes legíveis (vidalongaflix.com) em IPs.

#### Tipos de registro DNS

| Tipo | Função | Relevância de Segurança |
|---|---|---|
| **A** | Nome → IPv4 | Base para DNS Spoofing |
| **AAAA** | Nome → IPv6 | — |
| **MX** | Servidor de email | Falsificado em phishing — proteger com SPF/DKIM/DMARC |
| **TXT** | Texto livre | Usado para SPF, DKIM, verificação de domínio |
| **CNAME** | Alias para outro nome | Risco de takeover se apontar para serviço deletado |
| **NS** | Servidor DNS autoritativo | Alvo em ataques de sequestro de domínio |

#### Ataques DNS

| Ataque | Mecanismo | Defesa |
|---|---|---|
| **DNS Spoofing** | Resposta DNS falsificada — redireciona para IP malicioso | DNSSEC — assina registros com criptografia |
| **Cache Poisoning** | Injeta registros falsos no cache do resolver DNS | DNSSEC, randomização de porta e query ID |
| **DNS Tunneling** | Exfiltra dados codificados em queries DNS (malware C&C) | Monitorar padrões anômalos de DNS no SIEM |
| **DNS Amplification** | UDP Spoofing + grandes respostas DNS | Desabilitar resolvers recursivos abertos |

**DNS over TLS** (porta 853) e **DNS over HTTPS** (DoH) criptografam queries DNS, impedindo espionagem.

---

### HTTP — Protocolo de Transferência Hipertexto

#### Métodos HTTP

| Método | Uso | Segurança |
|---|---|---|
| **GET** | Leitura de dados | Parâmetros na URL — nunca enviar dados sensíveis via GET |
| **POST** | Criação de dados | Dados no body — usar HTTPS |
| **PUT** | Atualização total | Requer autenticação e ownership |
| **PATCH** | Atualização parcial | Requer autenticação e ownership |
| **DELETE** | Remoção | Requer autenticação, ownership e role check |
| **OPTIONS** | Negociação CORS | Deve retornar apenas métodos permitidos |
| **HEAD** | Retorna apenas headers | — |

#### Códigos de Status HTTP Relevantes para Segurança

| Código | Significado | Contexto de Segurança |
|---|---|---|
| 200 | OK | — |
| 201 | Created | — |
| 204 | No Content | Sucesso sem corpo |
| 400 | Bad Request | Validação de entrada falhou |
| 401 | Unauthorized | Token ausente ou inválido — forçar login |
| 403 | Forbidden | Autenticado mas sem permissão — ownership ou role |
| 404 | Not Found | Não expor detalhes sobre recursos inexistentes |
| 429 | Too Many Requests | Rate limiting ativo — resposta correta |
| 500 | Internal Server Error | Nunca expor stack trace Java |

#### Headers HTTP de Segurança Importantes

**Headers de Resposta** (servidor → cliente):
| Header | Função |
|---|---|
| `Strict-Transport-Security` (HSTS) | Força HTTPS para futuros acessos |
| `Content-Security-Policy` (CSP) | Restringe fontes de scripts, estilos, imagens |
| `X-Frame-Options` | Previne clickjacking — `DENY` ou `SAMEORIGIN` |
| `X-Content-Type-Options` | `nosniff` — previne MIME sniffing |
| `Cache-Control` | Controla caching — `no-store` para dados sensíveis |
| `Server` | Ocultar versão do servidor — `Server: Apache` vaza info |

**Headers de Requisição** (cliente → servidor):
| Header | Função | Risco |
|---|---|---|
| `Authorization` | Carrega Bearer token JWT | Nunca enviar para URLs de terceiros |
| `Host` | Domínio do servidor | Host Header Injection se mal validado |
| `User-Agent` | Identificação do cliente | Usado para fingerprinting |
| `Sec-Fetch-Site` | Origem da requisição (same-origin, cross-site) | Útil para validação CSRF |
| `Referer` | URL anterior | Pode vazar informações sensíveis em URLs |

---

### HTTPS e TLS — Comunicação Segura

**HTTPS = HTTP + TLS (Transport Layer Security)**. Garante confidencialidade (criptografia), integridade (hash) e autenticidade (certificados).

#### TLS Handshake

```
Cliente                              Servidor
  │──────── Client Hello ──────────►│  (versão TLS, cipher suites suportados)
  │◄──────── Server Hello ──────────│  (versão escolhida, cipher suite)
  │◄──────── Certificate ───────────│  (certificado X.509 com chave pública)
  │──────── Key Exchange ──────────►│  (chave de sessão negociada)
  │──────── Change Cipher ─────────►│  (habilita criptografia simétrica)
  │◄──────── Change Cipher ─────────│
  [comunicação criptografada com AES]
```

#### Certificados Digitais e CAs

- **CA (Certificate Authority)**: entidade que valida a identidade e emite certificados — Let's Encrypt (gratuita), DigiCert, Sectigo.
- **Certificado X.509**: contém: domínio, organização, chave pública, período de validade, assinatura da CA.
- **Validação no browser**: browser verifica assinatura da CA contra sua lista de CAs confiáveis. Exibe cadeado verde se válido.

#### Ataques HTTPS

| Ataque | Mecanismo | Defesa |
|---|---|---|
| **Certificado expirado** | Conexão aceita sem validação de validade | Monitorar expiração — Let's Encrypt auto-renova via certbot |
| **TLS Downgrade** | Força negociação para versão TLS antiga e vulnerável (TLS 1.0, SSL 3.0) | Configurar versão mínima TLS 1.2 no servidor |
| **SSL/TLS Inspection** | Corporações interceptam HTTPS instalando CA corporativa nos dispositivos | Válido em ambientes controlados, mas cria risco se CA é comprometida |
| **Certificate Pinning Bypass** | App mobile com pinning — atacante instala CA fake no dispositivo | Implementar certificate pinning em apps mobile |

---

### SSH — Secure Shell

**SSH** (porta 22) é o protocolo padrão para acesso remoto seguro a servidores. Usa criptografia assimétrica para autenticação e simétrica para a sessão.

#### Autenticação por Chave

```bash
# Gerar par de chaves RSA 4096 bits
ssh-keygen -t rsa -b 4096

# Copiar chave pública para o servidor
ssh-copy-id usuario@servidor
# ou: adicionar conteúdo de ~/.ssh/id_rsa.pub ao ~/.ssh/authorized_keys no servidor

# Conectar sem senha
ssh usuario@servidor
```

#### SSH Tunneling e Port Forwarding

```bash
# Local port forwarding: acessa serviço remoto via porta local
ssh -L 8080:localhost:80 usuario@servidor
# Acesso local: http://localhost:8080 → tunelado para porta 80 do servidor remoto

# Dynamic port forwarding (proxy SOCKS)
ssh -D 1080 usuario@servidor
```

**SCP — Cópia Segura de Arquivos**:
```bash
scp arquivo.txt usuario@servidor:/destino/
scp usuario@servidor:/origem/arquivo.txt ./local/
```

#### Boas Práticas de Segurança SSH

- Desativar autenticação por senha: `PasswordAuthentication no` em `/etc/ssh/sshd_config`
- Alterar porta padrão (security by obscurity — reduz scanning automatizado)
- Restringir IPs autorizados com `AllowUsers` ou firewall
- Monitorar `/var/log/auth.log` para tentativas de brute force
- Usar `fail2ban` — bloqueia IPs após N tentativas falhas

---

### FTP, SFTP e FTPS

| Protocolo | Porta | Criptografia | Uso Recomendado |
|---|---|---|---|
| **FTP** | 20 (dados), 21 (controle) | Nenhuma — credenciais e dados em clear text | **Nunca em produção** |
| **SFTP** | 22 | SSH completo — criptografia total | Recomendado — usa infraestrutura SSH existente |
| **FTPS** | 21 ou 990 | TLS sobre FTP | Alternativa ao SFTP |

**Risco FTP**: Wireshark com filtro `ftp contains "PASS"` captura senhas em texto plano em redes onde o tráfego não é criptografado.

---

### SMB — Compartilhamento de Arquivos Windows

**SMB (Server Message Block)** é usado para compartilhamento de arquivos e impressoras em redes Windows.

| Versão | Porta | Status | Risco |
|---|---|---|---|
| SMB v1 | 139 (NetBIOS), 445 (TCP) | **Obsoleto — desativar** | Vulnerável ao EternalBlue (MS17-010) |
| SMB v2/v3 | 445 | Ativo | Versões modernas, menos vulnerável |

**EternalBlue / WannaCry**: exploração do SMB v1 que permitiu o ransomware WannaCry se propagar lateralmente por redes Windows em 2017 sem interação do usuário.

**Lateral movement via SMB**: após comprometer um host, atacante usa ferramentas como `PsExec`, `CrackMapExec` para mover-se lateralmente na rede usando credenciais capturadas.

**Defesa**: desativar SMB v1, segmentar rede (VLANs), aplicar patches imediatamente, monitorar tráfego na porta 445.

---

### SMTP, SPF, DKIM e DMARC

**SMTP (Simple Mail Transfer Protocol)** transfere emails entre servidores. Sem autenticação de remetente por padrão — facilita spoofing e phishing.

| Porta | Uso | Segurança |
|---|---|---|
| 25 | Servidor a servidor (relay) | Sem TLS obrigatório — vetor de spam |
| 587 | Envio autenticado de usuários (submission) | TLS obrigatório — porta correta para apps |
| 465 | SMTP sobre TLS (legacy) | Depreciado mas ainda usado |

#### Mecanismos de Autenticação de Email

| Mecanismo | Registro DNS | O que valida |
|---|---|---|
| **SPF** (Sender Policy Framework) | `TXT "v=spf1 include:sendgrid.net ~all"` | Lista de IPs autorizados a enviar email pelo domínio |
| **DKIM** (DomainKeys Identified Mail) | `TXT` com chave pública | Assinatura criptográfica no header do email — garante que não foi alterado |
| **DMARC** | `TXT "v=DMARC1; p=reject; rua=..."` | Define política se SPF/DKIM falham (none/quarantine/reject) + relatórios |

**Sem SPF/DKIM/DMARC**: qualquer servidor pode enviar email "de" `@vidalongaflix.com.br` — vetor de phishing perfeito.

---

### NTP — Sincronização de Tempo

**NTP (Network Time Protocol)** sincroniza relógios via UDP porta 123. Crítico para segurança porque:
- Logs de auditoria e SIEM dependem de timestamps corretos para correlacionar eventos
- Certificados TLS têm período de validade — relógio errado invalida certificados
- Forense digital requer ordenação temporal precisa dos eventos

**Time Manipulation Attack**: atacante altera o relógio do servidor para invalidar certificados, confundir logs ou escapar de janelas de tempo de tokens.

---

### DHCP — Atribuição Dinâmica de IPs

**DHCP** atribui automaticamente IPs, máscara, gateway e DNS a dispositivos na rede (UDP portas 67/68).

| Ataque | Mecanismo | Defesa |
|---|---|---|
| **DHCP Spoofing** | Atacante coloca servidor DHCP falso — atribui IP próprio como gateway → MitM | DHCP Snooping em switches — apenas porta autorizada pode responder DHCP |
| **DHCP Starvation** | Requisita todos os IPs disponíveis com MACs falsificados — esgota pool | Rate limiting por porta no switch |

---

### ICMP — Internet Control Message Protocol

**ICMP** é usado para diagnóstico de rede (Ping, TraceRoute) e mensagens de controle. Não tem porta — opera na camada 3.

| Ataque | Mecanismo | Defesa |
|---|---|---|
| **Ping Flood** | Inundação de ICMP Echo Requests — DoS | Rate limiting ICMP, bloquear ICMP de origens externas |
| **Smurf Attack** | Ping com IP de origem falsificado para endereço broadcast — amplificação | Bloquear broadcasts direcionados nas interfaces de rede |
| **ICMP Redirect Attack** | Mensagem ICMP Redirect falsa — redireciona tráfego para roteador do atacante | Ignorar ICMP Redirect em produção |
| **ICMP Tunneling** | Exfiltra dados dentro de pacotes ICMP | Monitorar payload incomum em ICMP |

---

### Firewalls — Tipos e Funcionamento

**Firewall** controla o tráfego de rede com base em regras. É a primeira linha de defesa no perímetro.

#### Tipos por posição

| Tipo | Localização | Escopo |
|---|---|---|
| **Host-based** | No próprio dispositivo (SO) | Protege apenas o host onde está instalado |
| **Network-based** | Borda da rede (roteador, appliance dedicado) | Protege toda a rede atrás dele |

#### Tipos por funcionamento

| Tipo | Funcionamento | Prós | Contras |
|---|---|---|---|
| **Stateless** | Analisa cada pacote isoladamente sem contexto de conexão | Rápido, simples, baixo overhead | Não entende estado — pode ser enganado com pacotes soltos |
| **Stateful (SPI)** | Mantém tabela de conexões — sabe se pacote faz parte de sessão legítima | Contexto completo, segurança superior | Mais pesado em CPU/memória |

#### Princípios de configuração

- **Implicit Deny** (negar por padrão): tudo bloqueado, só passa o que está explicitamente permitido
- **Regras top-down**: primeiro match ganha — ordem das regras importa
- **Menor superfície de ataque**: apenas portas e serviços necessários abertos

#### NGFW — Next Generation Firewall

O **NGFW** vai além do firewall tradicional:
- **Deep Packet Inspection (DPI)**: analisa conteúdo do payload, não só cabeçalhos
- **Contexto de aplicação**: identifica a aplicação (não só a porta) — bloqueia P2P mesmo na porta 80
- **IPS integrado**: detecta e bloqueia ataques em tempo real
- **Threat Intelligence Feeds**: listas atualizadas de IPs/domínios maliciosos
- **Descriptografia TLS**: inspeciona tráfego HTTPS (requer instalação de CA corporativa)

---

### CDN — Content Delivery Network

**CDN (Rede de Distribuição de Conteúdo)** distribui servidores geograficamente próximos dos usuários.

**Benefícios de segurança**:
- **Resiliência a DDoS**: tráfego de ataque absorvido pela rede de CDN antes de chegar à origem
- **WAF integrado**: proteção contra SQLi, XSS diretamente no edge
- **Gestão de certificados TLS**: renovação automática, terminação TLS no edge
- **IP de origem oculto**: atacante não conhece o IP real do servidor de origem

**No VidaLongaFlix**: CloudFront (AWS) na frente do frontend Angular; backend EB exposto via domínio `api.vidalongaflix.com.br`.

---

### IDS e IPS — Detecção e Prevenção de Intrusões

#### IDS — Intrusion Detection System

**Passivo** — monitora e alerta, não bloqueia.

| Tipo | Localização | Monitora |
|---|---|---|
| **HIDS** (Host-based) | No host | Logs do SO, alterações em arquivos, processos |
| **NIDS** (Network-based) | Na rede | Tráfego de rede — analisa pacotes em tempo real |

**Vantagens**: baixo impacto no tráfego; falsos positivos são aceitáveis (alertam sem bloquear legítimos).

#### IPS — Intrusion Prevention System

**Ativo** — detecta e bloqueia ou reseta conexões em tempo real.

| Tipo | Ação ao detectar ataque |
|---|---|
| **HIPS** (Host-based) | Termina processo, bloqueia chamada de sistema |
| **NIPS** (Network-based) | Descarta pacotes, envia RST para encerrar conexão |

**Desvantagem**: falsos positivos bloqueiam tráfego legítimo — tuning crítico.

---

### WAF — Web Application Firewall

**WAF** opera na **Camada 7** (Aplicação) — entende HTTP/HTTPS e protege especificamente aplicações web.

**Protege contra**:
- SQL Injection
- Cross-Site Scripting (XSS)
- Broken Authentication / Session Hijacking
- Insecure Deserialization
- File Inclusion (LFI/RFI)
- Brute Force em formulários
- Ataques OWASP Top 10

| Tipo de WAF | Posição | Prós | Contras |
|---|---|---|---|
| **Network-based** (appliance) | Antes dos servidores | Alto throughput, baixa latência | Custo elevado |
| **Host-based** (plugin/agent) | No servidor da aplicação | Contexto da aplicação | Overhead no servidor |
| **Cloud-based** (CDN/SaaS) | Edge da CDN | Fácil deploy, atualização automática de regras | Latência adicional; privacidade dos dados |

**AWS WAF** + **CloudFront**: solução cloud-based para proteger a API do VidaLongaFlix.

---

### VPN — Virtual Private Network

**VPN** cria um túnel criptografado sobre rede pública (internet) — usuário aparece na rede privada como se estivesse local.

| Tipo | Uso | Exemplo |
|---|---|---|
| **Remote Access VPN** | Funcionário conecta ao escritório de casa | AWS Client VPN, Cisco AnyConnect |
| **Site-to-Site VPN** | Interconecta dois datacenters ou escritórios | AWS Site-to-Site VPN |

#### Protocolos VPN

| Protocolo | Status | Segurança | Performance |
|---|---|---|---|
| **PPTP** | Obsoleto | **Inseguro** — criptografia fraca | Alta |
| **L2TP/IPSec** | Ativo | Boa (dupla encapsulação) | Mais lenta — overhead duplo |
| **OpenVPN** | Padrão atual | **Excelente** — TLS, auditável, open source | Boa |
| **WireGuard** | Emergente | **Excelente** — código simples, moderno, fast | Muito alta |

---

### VLANs e Microsegmentação — Zero Trust de Rede

#### VLANs (Virtual LANs)

Segmentação lógica na **Camada 2** — isola grupos de dispositivos na mesma infraestrutura física.
- VLAN 10: servidores de produção
- VLAN 20: desenvolvimento
- VLAN 30: IoT / câmeras

**Comunicação entre VLANs** requer roteador ou switch Layer 3 — tráfego entre VLANs passa pelo firewall.

#### Microsegmentação

Mais granular que VLANs — aplica políticas por workload/container/serviço, não por segmento de rede.
- **Zero Trust Network**: "nunca confiar, sempre verificar" — mesmo dentro da rede interna, todo acesso precisa ser autenticado e autorizado
- Software-defined firewalls por serviço (AWS Security Groups, Kubernetes NetworkPolicy)

---

### Wi-Fi — Segurança de Redes Sem Fio

#### Protocolos de Segurança Wi-Fi

| Protocolo | Criptografia | Status | Uso |
|---|---|---|---|
| **WEP** | RC4 (quebrado) | **Inseguro** — obsoleto | Não usar |
| **WPA** | TKIP | Fraco | Não usar |
| **WPA2 Personal** | AES-CCMP | Bom se senha forte | Redes domésticas |
| **WPA2 Enterprise** | AES-CCMP + RADIUS | **Recomendado para corporativo** | Cada usuário tem credencial própria |
| **WPA3** | SAE (Dragonfly) | **Melhor atualmente** | Forward Secrecy; 192-bit para Enterprise |

**WPA2 Enterprise com RADIUS**: autenticação 802.1X — cada usuário tem credencial individual; compromisso de uma não afeta as outras. Suportado pelo AWS Certificate Manager com NPS/FreeRADIUS.

**KRACK (Key Reinstallation Attack, 2017)**: vulnerabilidade no handshake do WPA2 — atacante força reinstalação de chave de sessão já usada. Corrigido por patches — atualizar dispositivos.

#### Ataques Wi-Fi

| Ataque | Mecanismo | Defesa |
|---|---|---|
| **De-Auth Attack** | Envia frames de desautenticação falsificados — força reconexão do cliente → captura handshake WPA2 → brute force offline | WPA3 (SAE não vulnerável); detectar por IDS wireless |
| **Evil Twin** | AP falso com mesmo SSID — cliente conecta ao AP do atacante → MitM | Certificados de rede; WPA2 Enterprise (RADIUS autentica o AP também) |

---

### Proxies — Forward e Reverse

#### Forward Proxy

Intermediário entre **clientes internos e a internet**.
- Filtragem de conteúdo (bloqueia sites inapropriados)
- Cache de conteúdo web
- Anonimização de IP externo
- Controle de acesso por papel do usuário
- Log centralizado de navegação

#### Reverse Proxy

Intermediário entre **a internet e os servidores de backend**.
- Oculta o IP e a estrutura dos servidores de origem
- Balanceamento de carga entre múltiplos servidores
- Integração com WAF (Nginx + ModSecurity)
- Cache de conteúdo estático
- Pré-autenticação antes de atingir a aplicação
- Terminação TLS (SSL offloading)

**Exemplos**: Nginx, Apache, AWS CloudFront (reverse proxy + CDN), AWS ALB (Application Load Balancer).

**No VidaLongaFlix**: CloudFront como reverse proxy na frente do frontend S3; ALB pode ser usado na frente do Elastic Beanstalk.

---

### ACLs — Listas de Controle de Acesso

**ACLs** definem regras de permissão/negação de tráfego para firewalls e roteadores.

**Estrutura de uma regra ACL**:
```
[ação] [protocolo] [IP origem] [porta origem] [IP destino] [porta destino]
permit tcp 192.168.1.0/24 any 10.0.0.5 443
deny   tcp any            any 10.0.0.5 3306
```

**Princípios**:
- **Top-down**: primeiro match é aplicado — ordem crítica
- **Implicit deny ao final**: `deny any any` implícito — tudo que não foi explicitamente permitido é negado
- **Especificidade**: regras mais específicas antes das mais genéricas

---

### IPTables — Firewall Linux de Host

```bash
# Política padrão: negar tudo
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# Permitir loopback
iptables -A INPUT -i lo -j ACCEPT

# Permitir conexões estabelecidas (stateful)
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Permitir SSH da rede interna
iptables -A INPUT -p tcp --dport 22 -s 192.168.1.0/24 -j ACCEPT

# Permitir HTTPS
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# Bloquear IP específico
iptables -A INPUT -s 10.0.0.3 -j DROP

# Salvar regras
iptables-save > /etc/iptables/rules.v4

# Listar regras
iptables -L -n -v
```

**Importante**: regras IPTables **não persistem** entre reinicializações sem `iptables-save` + `iptables-restore` no boot.

---

### UFW — Uncomplicated Firewall

Interface simplificada sobre IPTables para ambientes Debian/Ubuntu:

```bash
ufw enable
ufw disable

ufw default deny incoming
ufw default allow outgoing

ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp

ufw deny from 10.0.0.3

ufw status numbered
ufw delete 1
```

---

### Análise de Tráfego — Wireshark e TCPDump

#### Wireshark — Interface Gráfica

Interface dividida em 3 painéis:
- **Topo**: lista de pacotes (número, tempo, origem, destino, protocolo, tamanho)
- **Esquerda inferior**: dissecção dos cabeçalhos de cada protocolo em árvore
- **Direita inferior**: payload em hexadecimal e ASCII

**Capture Filters** (BPF syntax — aplicados durante a captura):
```
tcp port 443           # apenas tráfego TCP na porta 443
host 192.168.1.1       # todo tráfego de/para este host
not arp                # exclui ARP
port 22                # tráfego SSH
```

**Display Filters** (aplicados na visualização após captura):
```
ip.addr == 192.168.127.2
tcp.port == 80
dns
http
http.request.method == "GET"
ftp contains "PASS"                              # passwords FTP em clear text
!arp                                             # exclui ARP
tcp.flags.syn == 1 and tcp.flags.ack == 0        # detecta SYN Scan
http.request and ip.dst != 192.168.127.1/24      # requisições HTTP externas (possível reverse shell)
ip.src == 192.168.127.2 and ip.dst != 192.168.127.154  # possível exfiltração
```

**Follow TCP Stream**: reconstrói o conteúdo completo de uma sessão TCP — útil para ver transferências FTP em clear text, payloads HTTP, etc.

#### TCPDump — CLI

```bash
# Captura simples na interface eth0 (exibe em tela)
tcpdump -i eth0

# Salvar em arquivo pcap para análise no Wireshark
tcpdump -i eth0 -w captura.pcap

# Filtrar por porta
tcpdump -i eth0 port 443

# Filtrar por host
tcpdump -i eth0 host 192.168.1.1
```

#### Detecção de Tráfego Anômalo

| Indicador | O que pode indicar |
|---|---|
| Volume incomum | DDoS, exfiltração de dados |
| Portas ou protocolos inesperados | Malware, backdoor, tunnel |
| Comunicação atípica via DNS | DNS Tunneling (C&C) |
| Flags TCP suspeitas | SYN Scan, XMAS Scan |
| ICMP com payload grande | ICMP Tunneling |
| FTP com credenciais visíveis | Credencial exposta |
| Conexões de saída incomuns | Reverse shell, exfiltração |

---

### Ferramentas de Linha de Comando para Diagnóstico de Rede

#### Ping — Teste de Conectividade ICMP

```bash
ping -c 10 google.com.br
# -c: número de pacotes enviados
# Retorna: RTT (round-trip time) mínimo, médio, máximo, desvio padrão
# TTL indica quantos saltos o pacote passou
```

#### TraceRoute — Mapeamento de Rota

```bash
traceroute 192.168.1.11       # para IP local (1 salto — mesma rede)
traceroute alura.com.br       # para destino externo (múltiplos saltos)
```

- Mostra cada roteador (salto) até o destino
- Útil para identificar o ponto de maior latência
- Máximo de 30 saltos por padrão no Linux
- Envia pacotes de 60 bytes com TTL crescente
- `*` indica roteador que não responde ICMP (configuração comum)

#### NSLookup — Resolução de DNS

```bash
nslookup terra.com.br     # único IP — servidor simples
nslookup alura.com.br     # 3 IPv4 + 3 IPv6 — load balancing DNS
```

- Informa qual servidor DNS respondeu
- Informa os IPs (A records) ou outros registros do domínio
- Útil para verificar se DNS está correto, qual resolver responde, se há round-robin

#### SS — Socket Statistics (substitui netstat no Linux moderno)

```bash
ss -tunap
# -t: TCP
# -u: UDP
# -n: numérico (não resolve nomes — mais rápido)
# -a: todos os sockets (listening + non-listening)
# -p: processo associado a cada socket

man ss    # manual completo
```

**Output mostra**: endereço local, porta, endereço remoto, estado, processo e PID.
- `127.0.0.1:3306` → MariaDB/MySQL rodando localmente
- `*:80` → Apache/Nginx escutando em todas as interfaces
- `*:8080` → Burp Suite ou servidor alternativo
- Portas em LISTEN = serviços ativos

**No Windows**: `netstat -tunap` — output similar ao `ss`.

#### TCPDump — CLI (já detalhado acima)

---

### Burp Suite — Proxy para Análise HTTP/HTTPS

**Burp Suite** é a ferramenta padrão de DAST manual para análise de aplicações web. Disponível na versão Community (gratuita) e Professional.

#### Funcionalidades principais

| Módulo | Função |
|---|---|
| **Proxy** | Intercepta requisições/respostas entre browser e servidor |
| **Intercept** | Liga/desliga interceptação — `Intercept Is On/Off` |
| **Repeater** | Repete e modifica requisições isoladas — ideal para testes de BOLA/BFLA |
| **Intruder** | Automação de payloads em campos selecionados (brute force, fuzzing) |
| **Decoder** | Codifica/decodifica Base64, URL encoding, HTML — analisar JWTs |

#### Tipos de Ataque no Intruder

| Tipo | Funcionamento |
|---|---|
| **Sniper** | Um conjunto de payloads iterado sequencialmente em cada posição |
| **Bedroom Run** | Mesmo payload em todas as posições simultaneamente |
| **Pitfork** | Múltiplos payloads em múltiplas posições em paralelo |
| **Cluster Bomb** | Todas as combinações de múltiplos payloads — mais abrangente |

#### Exemplo de Interceptação — GET com XSS

```
GET /xss_app/?q=%3Cscript%3Ealert%28%27XSS%27%29%3C%2Fscript%3E HTTP/1.1
Host: localhost
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Sec-Fetch-Site: same-origin
Sec-Fetch-Mode: navigate
Connection: keep-alive
```

Parâmetros GET com payloads codificados (URL encode) — Burp decodifica e exibe em claro.

---

### OWASP ZAP — DAST Automatizado

**OWASP ZAP** (Zed Attack Proxy) é a ferramenta DAST gratuita da OWASP, mantida pela Checkmarx desde 2024.

#### Funcionalidades

| Recurso | Descrição |
|---|---|
| **Automated Scan** | Fornece URL alvo → ZAP faz spidering + scan automático |
| **Spidering** | Mapeia todas as URLs e recursos da aplicação automaticamente |
| **Alerts** | Lista de vulnerabilidades encontradas classificadas por risco |
| **CWE Classification** | Cada alerta referencia o CWE correspondente |
| **Navegador integrado** | Firefox integrado para navegar manualmente com captura automática |
| **Request History** | Histórico de todas as requisições feitas durante a análise |

#### Spidering — Conceito

O **spidering** (ou crawling) é uma técnica automatizada que mapeia todas as URLs e recursos de um site:
- Envia requisição para a URL inicial → analisa resposta por links, formulários, recursos
- Segue cada link encontrado → repete o processo
- Resultado: mapa completo da superfície de ataque

**Uso em segurança**: descoberta de endpoints não documentados, formulários vulneráveis, pontos de entrada para XSS/SQLi.

**Cuidado**: em ambientes com autenticação, o spider pode não acessar áreas restritas sem credenciais. Em produção, o volume de requisições pode acionar IPS/rate limiting ou ser interpretado como ataque.

#### Tipos de Alertas ZAP

| Risco | Exemplo | Ação |
|---|---|---|
| **High** | Informação pessoal em resposta pública | Investigar imediatamente |
| **Medium** | Ausência do header `Content-Security-Policy` (CSP) | Corrigir em sprints próximos |
| **Low** | Cookie sem flag `HttpOnly` | Backlog |
| **Informational** | Tecnologias identificadas | Documentar |

---

### Logs — Tipos e Importância para Segurança

| Tipo de Log | O que registra | Usado para |
|---|---|---|
| **Firewall** | Conexões permitidas/negadas, IPs, portas | Detectar varreduras, DDoS, tentativas de intrusão |
| **Servidor Web** | Requisições HTTP, status codes, User-Agent, payload | Detectar SQLi, XSS, scanning de endpoints |
| **IDS/IPS** | Eventos de detecção/bloqueio com regra disparada | Correlacionar com outros logs |
| **Switch/Roteador** | Mudanças de rota, tráfego por porta | Detectar lateral movement, MAC flooding |
| **SO (audit.log)** | Logins, sudo, criação de arquivos, processos | Detectar lateral movement, privilege escalation |

#### Análise de Log WAF — Exemplo

```
2024-01-15 14:32:17 | 203.0.113.45 | GET /api/search?q=<script>alert(1)</script>
Rule: XSS-PATTERN-001 | Action: BLOCK | HTTP 403
```

- Timestamp para correlação com outros sistemas
- IP de origem para blocklist
- URL e payload para análise do vetor
- Regra disparada identifica categoria do ataque
- Ação confirma que o WAF bloqueou

#### SIEM — Security Information and Event Management

**SIEM / CIEM** correlaciona logs de múltiplas fontes:
- Data Lake de logs (centralização)
- Correlação temporal de eventos de diferentes sistemas
- Alertas quando padrão suspeito é detectado
- Exemplos: Grafana Loki (logs) + Grafana Alerting; AWS CloudWatch + Security Hub; Elastic SIEM

---

## Parte 2 — Vulnerabilidades Reais Identificadas no Projeto

---

### Tabela Mestre — Backend (Spring Boot / Java 17)

| ID | Arquivo / Endpoint | Vulnerabilidade | Camada OSI | CWE | Severidade | Detectável |
|---|---|---|---|---|---|---|
| BN1 | `CorsConfig.java` | `allowedHeaders("*")` aceita qualquer header — incluindo headers personalizados usados em ataques | L7 Aplicação | CWE-346 | Major | ✅ SonarCloud S5122 |
| BN2 | `SecurityConfig.java` | `/actuator/**` exposto sem autenticação — `env`, `beans`, `mappings` revelam internos do sistema | L7 Aplicação | CWE-200 | Critical | ✅ SonarCloud S4834 |
| BN3 | `WhatsAppService.java` | `RestTemplate` sem `connectTimeout` e `readTimeout` — API externa lenta bloqueia thread indefinidamente | L7 Aplicação | CWE-400 | Major | ✅ S2755 |
| BN4 | `application.properties` | Falta dos headers de segurança HTTP: sem CSP, sem HSTS, sem X-Frame-Options, sem X-Content-Type-Options | L7 Aplicação | CWE-693 | Major | ⚠️ DAST (ZAP) |
| BN5 | `GlobalExceptionHandler.java` | Stack trace Java retornado em erros 500 — revela versão do framework, pacotes, linhas de código | L7 Aplicação | CWE-209 | Major | ✅ SonarCloud S4792 |
| BN6 | `SecurityConfig.java` | Ausência de HSTS — browser pode aceitar HTTP em vez de HTTPS após primeiro acesso | L7 Aplicação | CWE-319 | Major | ✅ DAST (ZAP) |
| BN7 | `application-prod.properties` | `server.error.include-stacktrace` não configurado como `never` em produção | L7 Aplicação | CWE-209 | Major | ⚠️ Config review |
| BN8 | Todos os controllers | Ausência de validação de tamanho de payload — request body ilimitado pode causar DoS por memória | L7 Aplicação | CWE-770 | Major | ⚠️ Hotspot |
| BN9 | `SecurityConfig.java` | `headers.disable()` pode estar presente — desabilita todos os headers de segurança do Spring Security | L7 Aplicação | CWE-693 | Critical | ✅ SonarCloud S4834 |
| BN10 | CI/CD / `pom.xml` | Dependências com vulnerabilidades conhecidas não verificadas por OWASP Dependency Check | L7 Aplicação | CWE-1104 | Major | ✅ OWASP Dep. Check |
| BN11 | Infraestrutura AWS | SMB (porta 445), Telnet (23), FTP (21) não devem estar acessíveis na instância EB — verificar Security Groups | L4 Transporte | CWE-284 | Critical | ✅ Port scan |
| BN12 | Infraestrutura AWS | NTP não configurado explicitamente na instância — relógio pode derivar e invalidar certificados/logs | L3 Rede | CWE-799 | Minor | ⚠️ Config review |
| BN13 | `.github/workflows/ci.yml` | Ausência de Gitleaks — segredos podem ser commitados acidentalmente | L7 Aplicação | CWE-312 | Critical | ✅ Gitleaks |
| BN14 | Infraestrutura AWS | WAF não configurado na frente da API — requisições maliciosas chegam diretamente ao Spring Boot | L7 Aplicação | CWE-693 | Major | ⚠️ Arquitetural |

---

### Tabela Mestre — Frontend (Angular / TypeScript)

| ID | Arquivo / Componente | Vulnerabilidade | Camada OSI | CWE | Severidade | Detectável |
|---|---|---|---|---|---|---|
| FN1 | `auth.interceptor.ts` | Token JWT enviado para todas as URLs sem filtrar por `environment.apiUrl` — vaza para CDN, analytics | L7 Aplicação | CWE-522 | Critical | ✅ SonarCloud S4784 |
| FN2 | Todos os services Angular | URL da API hardcoded como string em vez de `environment.apiUrl` | L7 Aplicação | CWE-1104 | Minor | ✅ SonarCloud S1313 |
| FN3 | Componentes que usam `innerHTML` | Risco de XSS se dados do backend são interpolados diretamente como HTML sem sanitização | L7 Aplicação | CWE-79 | Critical | ✅ SonarCloud |
| FN4 | `angular.json` | HTTPS não forçado em produção — configuração do servidor de hosting (S3/CloudFront) pode aceitar HTTP | L7 Aplicação | CWE-319 | Major | ✅ DAST (ZAP) |
| FN5 | Formulários Angular | Sem validação client-side antes de enviar ao backend — facilita DoS por dados inválidos | L7 Aplicação | CWE-20 | Minor | ⚠️ Revisão de código |
| FN6 | `auth.service.ts` | Token JWT armazenado em `localStorage` — acessível por qualquer script XSS na página | L7 Aplicação | CWE-922 | Major | ⚠️ Hotspot |
| FN7 | Interceptor Angular | Sem handler para HTTP 401 — token expirado não redireciona para login automaticamente | L7 Aplicação | CWE-287 | Major | ⚠️ Revisão de código |
| FN8 | Componentes de lista | Sem paginação — carrega catálogo completo numa chamada — impacto em performance e DoS | L7 Aplicação | CWE-400 | Major | ❌ Lógico |
| FN9 | `environment.prod.ts` | CSP ausente no Angular — sem `meta` de CSP ou configuração no CloudFront | L7 Aplicação | CWE-693 | Major | ✅ DAST (ZAP) |
| FN10 | Pacotes npm | Dependências com vulnerabilidades conhecidas não verificadas por `npm audit` | L7 Aplicação | CWE-1104 | Major | ✅ npm audit |

---

## Parte 3 — Cenários e Resultado Esperado — Backend (Spring Boot / Java)

> Positivo = comportamento seguro e correto. Negativo = comportamento inseguro. Triagem = requer revisão humana.

---

### B-NET-01 — CORS Headers (CWE-346 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 1 | `CorsConfig.java` usa `allowedHeaders("*")` para todas as origens | SonarCloud sinaliza Hotspot S5122 — aceitar qualquer header abre vetor de ataques por header customizado |
| 2 | `CorsConfig.java` lista headers explicitamente: `Authorization, Content-Type, Accept, X-Requested-With` | Nenhum alerta — headers definidos minimamente |
| 3 | Requisição CORS com header customizado `X-Custom-Attack: payload` para origem permitida | Header bloqueado se não está na lista explícita — comportamento correto |
| 4 | `allowedOrigins` lista apenas `https://vidalongaflix.com` e `https://www.vidalongaflix.com` | Nenhum alerta — origem restrita a domínios do projeto |
| 5 | `allowedOrigins("*")` com `allowCredentials(true)` simultâneos no CORS | Configuração inválida — Spring lança erro; navegador bloqueia — não combinar wildcard com credenciais |
| 6 | Requisição OPTIONS para `/api/videos` de origem não listada | HTTP 403 — CORS rejeita preflight de origem não autorizada |
| 7 | ZAP identifica header `Access-Control-Allow-Headers: *` na resposta | Alerta de risco médio — headers devem ser explícitos |

---

### B-NET-02 — Actuator e Exposição de Informações (CWE-200 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 8 | `GET /actuator/env` acessível sem autenticação | Retorna variáveis de ambiente incluindo `DB_URL`, `JWT_SECRET`, `OTLP_AUTH_HEADER` — Critical |
| 9 | `GET /actuator/beans` acessível sem autenticação | Lista todos os beans Spring com suas dependências — revela estrutura interna |
| 10 | `GET /actuator/health` acessível publicamente | HTTP 200 com status `UP` — aceito para health checks do load balancer |
| 11 | `management.endpoints.web.exposure.include=health` apenas | Somente `/actuator/health` exposto — demais retornam 404 — comportamento correto |
| 12 | `/actuator/metrics`, `/actuator/loggers`, `/actuator/mappings` protegidos por `hasRole("ADMIN")` | HTTP 401/403 para usuários não autenticados ou sem role ADMIN |
| 13 | `management.endpoints.web.exposure.include=*` em `application-prod.properties` | Critical — todas as informações internas expostas em produção |
| 14 | ZAP faz scan em `/actuator/` | ZAP alerta se encontrar endpoints actuator respondendo sem autenticação |

---

### B-NET-03 — Headers de Segurança HTTP (CWE-693 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 15 | Resposta HTTP da API não inclui header `Strict-Transport-Security` | ZAP alerta ausência de HSTS — sem HSTS, browser pode aceitar HTTP em futuros acessos |
| 16 | Resposta HTTP da API inclui `Strict-Transport-Security: max-age=31536000; includeSubDomains` | Nenhum alerta — HSTS configurado corretamente por 1 ano |
| 17 | Resposta HTTP da API não inclui `X-Frame-Options` | Clickjacking possível — ZAP alerta risco médio |
| 18 | Resposta inclui `X-Frame-Options: DENY` | Nenhum alerta — proteção contra clickjacking ativa |
| 19 | Resposta não inclui `X-Content-Type-Options: nosniff` | ZAP alerta — MIME sniffing pode causar interpretação incorreta de conteúdo |
| 20 | Resposta inclui `X-Content-Type-Options: nosniff` | Nenhum alerta |
| 21 | `SecurityConfig.java` contém `headers.disable()` | Critical — todos os headers de segurança do Spring Security removidos |
| 22 | `SecurityConfig.java` configura HSTS, X-Frame-Options, X-Content-Type-Options via `headers()` | Nenhum alerta — headers ativos |

---

### B-NET-04 — Stack Trace e Informações em Erros (CWE-209 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 23 | `GET /api/videos/id-malformado-nao-uuid` retorna stack trace Java completo no body | Revela pacotes, versão Spring, linhas de código — Critical |
| 24 | `GlobalExceptionHandler` retorna `{"error": "Not found", "traceId": "abc123"}` sem stack trace | Nenhum alerta — erro genérico sem informação interna |
| 25 | `server.error.include-stacktrace=always` em `application-prod.properties` | Critical — stack trace em produção |
| 26 | `server.error.include-stacktrace=never` em `application-prod.properties` | Nenhum alerta — stack trace bloqueado |
| 27 | Exceção não mapeada retorna `{"error": "Erro interno"}` sem detalhes do tipo de exceção | Nenhum alerta — sem vazamento de informação |
| 28 | Resposta de erro 404 inclui o caminho completo do arquivo Java (ex: `...UserService.java:142`) | Informação interna exposta — Critical |
| 29 | SonarCloud regra S4792 sinaliza uso de `logger.error(exception.getMessage())` sem tratar dados sensíveis | Hotspot — log pode incluir informação sensível como senha tentada |

---

### B-NET-05 — RestTemplate sem Timeout (CWE-400 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 30 | `WhatsAppService` cria `new RestTemplate()` sem configurar `connectTimeout` ou `readTimeout` | Thread fica bloqueada indefinidamente se a API do WhatsApp não responde — esgotamento do thread pool |
| 31 | API do WhatsApp retorna resposta em 60 segundos — sem timeout configurado | Thread do Spring Boot presa por 60s — N requisições simultâneas esgotam o pool |
| 32 | `RestTemplate` configurado com `connectTimeout=5000ms` e `readTimeout=10000ms` | Nenhum alerta — timeout definido; thread liberada se serviço externo está lento |
| 33 | SonarCloud regra S2755 sinaliza `RestTemplate` sem timeout | Major — timeout obrigatório para chamadas externas |
| 34 | `RestTemplate` substituído por `RestClient` (Spring Boot 3.2+) com `connectTimeout` e `readTimeout` | Nenhum alerta — API moderna com timeout nativo |
| 35 | Circuit breaker (Resilience4j) configurado para chamada ao WhatsApp — fallback após N falhas | Nenhum alerta — resiliência adicionada além do timeout |

---

### B-NET-06 — Infraestrutura e Portas Expostas (CWE-284 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 36 | Security Group da instância EB permite acesso público à porta 3306 (RDS) | Critical — banco de dados exposto na internet |
| 37 | Security Group permite acesso público à porta 22 (SSH) de `0.0.0.0/0` | Brute force SSH aberto — restringir para IPs de administração |
| 38 | Security Group permite apenas portas 80 e 443 de `0.0.0.0/0`; porta 22 apenas de IP do administrador | Nenhum alerta — superfície mínima exposta |
| 39 | RDS recebe conexões apenas do Security Group da instância EB (não da internet) | Correto — RDS em subnet privada, acessível apenas pelo backend |
| 40 | Nmap externo detecta porta 8080 aberta na instância EB | Porta de desenvolvimento exposta em produção — fechar via Security Group |
| 41 | Port scan detecta Telnet (23) ou FTP (21) abertos na instância | Serviços inseguros ativos — desativar e fechar portas |
| 42 | `traceroute api.vidalongaflix.com.br` mostra o IP real do servidor EB antes do proxy | IP de origem exposto — usar CloudFront/ALB na frente para ocultar IP de origem |

---

### B-NET-07 — Dependências com Vulnerabilidades (CWE-1104 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 43 | OWASP Dependency Check encontra CVE em versão atual do Spring Boot | Alerta com CVSS score — avaliar urgência do upgrade |
| 44 | OWASP Dependency Check não está no pipeline CI/CD | Vulnerabilidades em dependências passam sem detecção |
| 45 | CI/CD executa `mvn dependency-check:check` e falha a build se CVSS >= 7 | Build bloqueada em vulnerabilidades críticas |
| 46 | Gitleaks não está no pipeline — segredo commitado passa pelo CI | Credencial vaza para histórico do repositório |
| 47 | Gitleaks configurado como pre-commit hook ou step no CI — detecta `JWT_SECRET` no código | Build bloqueada com indicação da linha e arquivo |
| 48 | SonarCloud integrado no pipeline — `sonar.qualitygate.wait=true` | Build bloqueada se quality gate falhar (coverage, bugs, code smells) |

---

### B-NET-08 — TLS e HTTPS (CWE-319 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 49 | Backend responde em HTTP puro (porta 80) sem redirecionamento para HTTPS | Credenciais JWT trafegam em clear text — Critical |
| 50 | Backend responde apenas em HTTPS (443) via CloudFront/EB com certificado válido | Nenhum alerta — comunicação criptografada |
| 51 | Certificado TLS expirado em `api.vidalongaflix.com.br` | Browser rejeita conexão; API inoperante — monitorar expiração com alerta |
| 52 | Certificado TLS renovado automaticamente via AWS Certificate Manager | Nenhum alerta — renovação gerenciada pela AWS |
| 53 | TLS 1.0 ou 1.1 habilitado no servidor | ZAP alerta downgrade possível — configurar versão mínima TLS 1.2 |
| 54 | Apenas TLS 1.2 e 1.3 habilitados na configuração do CloudFront/EB | Nenhum alerta — protocolos modernos apenas |

---

## Parte 4 — Cenários e Resultado Esperado — Frontend (Angular / TypeScript)

---

### F-NET-01 — Interceptor e Vazamento de Token (CWE-522 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 60 | `auth.interceptor.ts` adiciona `Authorization: Bearer <token>` em **todas** as requisições sem filtrar a URL | Token JWT enviado para CDNs, analytics, fontes de fontes (Google Fonts) — vazamento |
| 61 | Interceptor verifica `req.url.startsWith(environment.apiUrl)` antes de adicionar o header | Nenhum alerta — token só vai para a própria API |
| 62 | API de terceiro (ex: serviço de analytics) recebe o token JWT do VidaLongaFlix | Credencial exposta para terceiro — Critical |
| 63 | Burp Suite intercepta requisição Angular para `fonts.googleapis.com` com header `Authorization` | Confirmação de vazamento — token em requisição externa |
| 64 | SonarCloud regra S4784 sinaliza `setHeaders` sem verificação da URL de destino | Hotspot — inspecionar manualmente se token pode vazar |
| 65 | Handler para HTTP 401 ausente no interceptor — token expirado retorna erro genérico | Usuário fica preso sem feedback de sessão expirada |
| 66 | Interceptor captura 401, chama `authService.logout()` e navega para `/login` | Correto — sessão expirada tratada automaticamente |

---

### F-NET-02 — XSS e Content Security Policy (CWE-79 / CWE-693 / Critical)

| # | Cenário | Esperado |
|---|---|---|
| 67 | Componente Angular usa `[innerHTML]="comment.text"` com dado vindo do backend | XSS possível — se backend retorna `<script>` sem sanitizar, Angular vai executar |
| 68 | Angular DomSanitizer não é usado ao inserir HTML dinâmico via `innerHTML` | SonarCloud alerta risco de XSS — sanitizar antes de inserir |
| 69 | Componente usa `{{ comment.text }}` (interpolação) em vez de `[innerHTML]` | Nenhum alerta — Angular escapa o conteúdo automaticamente |
| 70 | ZAP identifica ausência do header `Content-Security-Policy` nas respostas do frontend | Alerta de risco médio — CSP bloqueia scripts injetados por XSS |
| 71 | CloudFront configurado para adicionar `Content-Security-Policy: default-src 'self'` no response header | Nenhum alerta — CSP ativo no edge |
| 72 | `index.html` inclui `<meta http-equiv="Content-Security-Policy" content="default-src 'self'">` | Nenhum alerta — CSP via meta tag |
| 73 | ZAP detecta que cookie de sessão não tem flag `HttpOnly` | Alerta — script XSS pode roubar o cookie; usar HttpOnly ou preferir localStorage + CSP rigorosa |

---

### F-NET-03 — Token em localStorage e Armazenamento Seguro (CWE-922 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 74 | `auth.service.ts` armazena token JWT em `localStorage` | Hotspot — acessível por qualquer JS na página; XSS pode exfiltrar |
| 75 | Token armazenado em cookie `HttpOnly; Secure; SameSite=Strict` | Mais seguro — não acessível por JavaScript; SameSite previne CSRF |
| 76 | Token em `sessionStorage` em vez de `localStorage` | Melhor que localStorage — expira ao fechar o browser; mas ainda acessível por XSS |
| 77 | CSP rigorosa (`default-src 'self'`) combinada com `localStorage` | Reduz risco de XSS que poderia acessar o token — combinação aceitável |
| 78 | SonarCloud sinaliza `localStorage.setItem` armazenando dados que parecem credenciais | Hotspot S2441 — revisar se token ou dado sensível |

---

### F-NET-04 — HTTPS e Headers de Segurança no CloudFront (CWE-319 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 79 | CloudFront Distribution aceita requisições HTTP (porta 80) sem redirecionar para HTTPS | ZAP detecta que site aceita HTTP — dados trafegam sem criptografia |
| 80 | CloudFront configurado para `Redirect HTTP to HTTPS` | HTTP automaticamente redirecionado para HTTPS — correto |
| 81 | S3 origin serve o Angular app sem CloudFront na frente | Sem TLS, sem WAF, sem geoproteção — Critical para produção |
| 82 | ZAP detecta ausência de `Strict-Transport-Security` no CloudFront | Configurar HSTS via CloudFront Response Headers Policy |
| 83 | CloudFront Response Headers Policy inclui HSTS, X-Frame-Options, X-Content-Type-Options, CSP | ZAP não reporta ausência destes headers — configuração correta |
| 84 | `angular.json` com `"scripts": []` sem bibliotecas de terceiros não auditadas | Menor superfície de ataque via supply chain |

---

### F-NET-05 — Dependências npm e Supply Chain (CWE-1104 / Major)

| # | Cenário | Esperado |
|---|---|---|
| 85 | `npm audit` no frontend identifica vulnerabilidade High em dependência Angular | Alerta com CVE, versão afetada e versão corrigida |
| 86 | `npm audit --audit-level=high` não está no pipeline CI/CD do frontend | Vulnerabilidades de dependências passam sem bloqueio |
| 87 | `npm audit --audit-level=high` no CI — build falha se vulnerabilidade High ou Critical encontrada | Build bloqueada — supply chain vulnerabilities detectadas |
| 88 | Dependência de terceiro carregada via CDN externo sem Subresource Integrity (SRI) | Script pode ser comprometido no servidor externo — usar `integrity` hash |
| 89 | Tag `<script src="..." integrity="sha384-..." crossorigin="anonymous">` com hash SRI | Nenhum alerta — integridade do script verificada pelo browser |
| 90 | `npm ci` em vez de `npm install` no CI — garante instalação exata do `package-lock.json` | Nenhum alerta — builds reprodutíveis sem drift de dependências |

---

### F-NET-06 — Burp Suite e OWASP ZAP no Frontend (DAST)

| # | Cenário | Esperado |
|---|---|---|
| 91 | Burp Suite intercepta requisição de login e permite modificar `email` no body | Testar validação do backend — se aceitar email malformado, backend não valida |
| 92 | Burp Suite Intruder testa lista de senhas comuns em `POST /api/auth/login` | Se HTTP 429 aparece após N tentativas — rate limiting ativo. Se não — brute force possível |
| 93 | OWASP ZAP Automated Scan em `https://vidalongaflix.com` com Spidering ativo | ZAP mapeia todas as URLs do SPA Angular — identifica endpoints, formulários |
| 94 | ZAP identifica formulário de login sem `autocomplete="off"` | Alerta informacional — senhas podem ser salvas pelo browser |
| 95 | ZAP identifica ausência de CSP em 100% das páginas Angular | Alerta de risco médio — CSP deve ser configurado no CloudFront |
| 96 | ZAP Spidering em produção sem autorização prévia | Tráfego intenso pode acionar IPS ou rate limiting — executar apenas em staging ou com autorização |
| 97 | Burp Suite Repeater repete requisição com token expirado | Deve retornar HTTP 401 — confirma que backend rejeita tokens vencidos |

---

## Parte 5 — Passo a Passo de Implementação por Sprint

---

### Sprint 0 — Diagnóstico e Mapeamento (sem código novo)

**Objetivo**: mapear o estado atual de segurança de rede e headers antes de corrigir.

1. Instalar OWASP ZAP (Docker: `docker pull owasp/zap2docker-stable`) e Burp Suite Community Edition
2. Executar ZAP Automated Scan apontando para `http://localhost:8090/api` (backend local)
3. Anotar os alertas: ausência de HSTS, CSP, X-Frame-Options, X-Content-Type-Options
4. Verificar `/actuator/env`, `/actuator/beans` acessíveis sem auth: `curl http://localhost:8090/actuator/env`
5. Verificar resposta de erro: `curl http://localhost:8090/api/videos/id-invalido` — checar se stack trace aparece
6. Testar CORS: `curl -H "Origin: https://evil.com" http://localhost:8090/api/videos -v` — checar `Access-Control-Allow-Origin`
7. Verificar `CorsConfig.java` — checar se `allowedHeaders("*")` está presente
8. Registrar cada achado como issue no GitHub com label `security`

---

### Sprint 1 — Configurar Headers de Segurança no Spring Security (Backend) — Prioridade: Major

**Objetivo**: ativar headers de segurança HTTP na API.

1. Abrir `SecurityConfig.java` — localizar `.headers()` ou checar se `headers.disable()` está presente
2. Remover `headers.disable()` se presente — restaurar os headers padrão do Spring Security
3. Adicionar configuração explícita de headers:
   - HSTS com `includeSubDomains(true)` e `maxAgeInSeconds(31536000)`
   - `X-Frame-Options: DENY` via `frameOptions.deny()`
   - `X-Content-Type-Options: nosniff` via `contentTypeOptions()`
   - CSP: `contentSecurityPolicy("default-src 'self'")`
4. Abrir `application-prod.properties` — adicionar `server.error.include-stacktrace=never`
5. Abrir `GlobalExceptionHandler.java` — garantir que exceções genéricas retornam mensagem sem stack trace
6. Reiniciar localmente e verificar com `curl -v http://localhost:8090/api/videos` — headers presentes na resposta
7. Executar ZAP novamente — verificar que alertas de headers desaparecem
8. Criar PR com label `security` descrevendo os headers adicionados

---

### Sprint 2 — Restringir Actuator e Corrigir CORS (Backend) — Prioridade: Critical e Major

**Objetivo**: fechar exposição do Actuator e restringir headers CORS.

1. Abrir `application.properties` — modificar:
   - `management.endpoints.web.exposure.include=health` (apenas health público)
   - `management.endpoint.health.show-details=when-authorized`
2. Abrir `SecurityConfig.java` — adicionar proteção por role:
   - `requestMatchers("/actuator/health").permitAll()`
   - `requestMatchers("/actuator/**").hasRole("ADMIN")`
3. Reiniciar localmente e testar: `curl http://localhost:8090/actuator/env` deve retornar HTTP 401
4. Abrir `CorsConfig.java` — substituir `allowedHeaders("*")` por lista explícita:
   - `Authorization, Content-Type, Accept, X-Requested-With`
5. Verificar que preflight OPTIONS funciona com os headers permitidos
6. Testar CORS com origem não autorizada — deve retornar HTTP 403
7. Criar teste de integração para Actuator: `GET /actuator/env` sem auth deve retornar 401
8. Criar PR com label `security`

---

### Sprint 3 — Timeout no RestTemplate e Resiliência (Backend) — Prioridade: Major

**Objetivo**: evitar thread starvation por chamadas externas lentas.

1. Abrir `WhatsAppService.java` — localizar onde `RestTemplate` é instanciado
2. Substituir `new RestTemplate()` por `RestTemplate` com configuração de timeout:
   - Criar `SimpleClientHttpRequestFactory` com `setConnectTimeout(5000)` e `setReadTimeout(10000)`
3. Alternativamente, migrar para `RestClient` (Spring Boot 3.2+) com `.connectTimeout(Duration.ofSeconds(5))` e `.readTimeout(Duration.ofSeconds(10))`
4. Adicionar try-catch para `ResourceAccessException` (timeout) com fallback: logar o timeout e continuar sem o WhatsApp
5. Verificar que SonarCloud S2755 não aparece mais após a mudança
6. Criar teste unitário: `whatsApp_shouldNotBlockThread_whenApiTimesOut` — mock do servidor externo lento
7. Criar PR com label `security`

---

### Sprint 4 — Headers de Segurança no CloudFront (Frontend) — Prioridade: Major

**Objetivo**: ativar headers de segurança HTTP nas respostas do CloudFront.

1. Acessar AWS Console → CloudFront → Distribution do VidaLongaFlix frontend
2. Em `Response headers policies`, criar nova policy com:
   - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
   - `X-Frame-Options: DENY`
   - `X-Content-Type-Options: nosniff`
   - `Content-Security-Policy: default-src 'self'; connect-src 'self' https://api.vidalongaflix.com.br`
   - `Referrer-Policy: strict-origin-when-cross-origin`
3. Associar a policy à Distribution
4. Verificar que CloudFront está configurado para `Redirect HTTP to HTTPS`
5. Testar com: `curl -I https://vidalongaflix.com` — verificar presença dos headers
6. Executar ZAP novamente no frontend — verificar que alertas de headers desaparecem
7. Registrar evidência no GitHub Issue da sprint

---

### Sprint 5 — Interceptor Angular e Tratamento de Token (Frontend) — Prioridade: Critical

**Objetivo**: evitar vazamento do token JWT para URLs externas e tratar sessão expirada.

1. Abrir `auth.interceptor.ts` — localizar onde `Authorization` header é adicionado
2. Adicionar verificação: antes de `req.clone({ setHeaders: ... })`, verificar `if (req.url.startsWith(environment.apiUrl))`
3. Se URL não for da API: `return next(req)` sem adicionar o header
4. Adicionar handler para HTTP 401: no `pipe(catchError(...))`, capturar 401, chamar `authService.logout()`, navegar para `/login`
5. Garantir que não há loop de redirect (não redirecionar se já está em `/login`)
6. Criar testes Jasmine:
   - `interceptor should NOT add Authorization header for external URLs`
   - `interceptor should add Authorization header for API URLs`
   - `interceptor should redirect to login on 401 response`
7. Criar PR no repositório Angular com label `security`

---

### Sprint 6 — npm audit e Dependências do Frontend (Frontend) — Prioridade: Major

**Objetivo**: detectar e corrigir vulnerabilidades em dependências npm.

1. Executar `npm audit` no repositório Angular — documentar vulnerabilidades encontradas (High/Critical)
2. Executar `npm audit fix` para corrigir automaticamente o que for possível sem breaking changes
3. Verificar manualmente as vulnerabilidades restantes — avaliar impacto e atualizar dependências
4. Adicionar step no pipeline CI do frontend:
   - `npm ci`
   - `npm audit --audit-level=high`
   - Falhar build se vulnerabilidade High/Critical encontrada
5. Verificar scripts de terceiros carregados via CDN — adicionar `integrity` SRI se possível
6. Criar PR com `package-lock.json` atualizado e evidência do `npm audit` limpo

---

### Sprint 7 — Gitleaks e OWASP Dependency Check no CI/CD (Backend) — Prioridade: Critical

**Objetivo**: detectar segredos em código e vulnerabilidades em dependências Java no pipeline.

1. Abrir `.github/workflows/ci.yml`
2. Adicionar step de Gitleaks antes do build:
   - Usar action `gitleaks/gitleaks-action@v2`
   - Configurar para falhar se segredo encontrado
3. Adicionar step de OWASP Dependency Check após o build:
   - Usar `dependency-check-action` do GitHub Actions
   - Configurar `failBuildOnCVSS: 7` — falha se vulnerabilidade com CVSS >= 7
4. Verificar que `JWT_SECRET` e `DB_URL` estão apenas em variáveis de ambiente do Elastic Beanstalk (não no código)
5. Criar arquivo `.gitleaks.toml` para exceções legítimas (ex: chaves de teste em arquivos de teste)
6. Testar pipeline: fazer commit com string semelhante a um segredo em branch de feature e verificar que CI falha
7. Criar PR documentando os novos steps do CI

---

### Sprint 8 — Verificação com Burp Suite e OWASP ZAP em Staging (DAST) — Prioridade: Major

**Objetivo**: confirmar que as correções dos sprints anteriores eliminaram as vulnerabilidades.

1. Deploy da versão corrigida em ambiente de staging (ou localmente com `SPRING_PROFILES_ACTIVE=prod`)
2. Executar ZAP Automated Scan novamente apontando para staging
3. Comparar alertas antes (Sprint 0) e depois — documentar quais foram resolvidos
4. Abrir Burp Suite — testar manualmente:
   - **CORS**: `curl -H "Origin: https://evil.com" https://staging-api.vidalongaflix.com.br/api/videos` — deve retornar 403
   - **Actuator**: `GET /actuator/env` sem auth — deve retornar 401
   - **Stack trace**: `GET /api/videos/id-invalido` — resposta não deve ter stack trace
   - **Headers**: `curl -I https://staging.vidalongaflix.com` — verificar HSTS, CSP, X-Frame-Options
5. Burp Suite Intruder: testar brute force em `/api/auth/login` — verificar HTTP 429 após N tentativas
6. Verificar interceptor Angular: interceptar requisição para URL externa — confirmar ausência do header Authorization
7. Documentar resultado de cada teste como evidência no GitHub Issue
8. Criar relatório de segurança comparando estado antes/depois dos sprints

---

### Referências de Ferramentas e Recursos

| Ferramenta | Fase | Uso no Projeto | Como obter |
|---|---|---|---|
| **OWASP ZAP** | DAST automatizado | Scan de headers, XSS, missing CSP, CORS | `docker pull owasp/zap2docker-stable` |
| **Burp Suite Community** | DAST manual | Interceptar requests, testar CORS, brute force | `https://portswigger.net/burp/communitydownload` |
| **Gitleaks** | SAST — segredos | Detectar tokens e senhas em commits | GitHub Action: `gitleaks/gitleaks-action@v2` |
| **OWASP Dependency Check** | SCA — Java | CVEs em dependências Maven | Maven plugin: `org.owasp:dependency-check-maven` |
| **npm audit** | SCA — Node.js | CVEs em dependências npm do Angular | Nativo no npm: `npm audit --audit-level=high` |
| **Wireshark** | Análise de rede | Capturar e analisar tráfego em dev | `sudo apt install wireshark` |
| **TCPDump** | Análise de rede (CLI) | Capturar tráfego em servidores Linux | `sudo apt install tcpdump` |
| **nmap** | Port scanning | Verificar portas abertas na instância EB | `nmap -sV api.vidalongaflix.com.br` |
| **ss / netstat** | Sockets locais | Verificar portas em escuta no servidor | `ss -tunap` (Linux), `netstat -ano` (Windows) |
| **traceroute / nslookup** | Diagnóstico DNS/rede | Verificar rota e resolução DNS do domínio | `traceroute api.vidalongaflix.com.br` |
| **AWS Security Groups** | Firewall de rede AWS | Restringir portas abertas na instância EB | AWS Console → EC2 → Security Groups |
| **AWS WAF** | WAF cloud | Proteção contra OWASP Top 10 no edge | AWS Console → WAF & Shield |
| **AWS Certificate Manager** | TLS gerenciado | Renovação automática de certificados TLS | AWS Console → Certificate Manager |

---

### Roadmap de Segurança de Rede — Resumo por Prioridade

| Prioridade | Sprint | Ação | Benefício |
|---|---|---|---|
| Critical | 0 | Diagnóstico: ZAP + Burp Suite baseline | Mapa completo das vulnerabilidades |
| Critical | 2 | Actuator restrito + CORS headers explícitos | Elimina exposição de internos |
| Critical | 5 | Interceptor Angular com filtro de URL | Elimina vazamento de token para terceiros |
| Critical | 7 | Gitleaks no CI | Previne segredos no repositório |
| Major | 1 | Headers de segurança HTTP no Spring Security | HSTS, CSP, X-Frame-Options ativos |
| Major | 3 | Timeout no RestTemplate | Elimina thread starvation |
| Major | 4 | Headers no CloudFront + HTTPS obrigatório | Proteção no edge para o frontend |
| Major | 6 | npm audit no CI do Angular | Vulnerabilidades em dependências detectadas |
| Major | 7 | OWASP Dependency Check no CI | CVEs em dependências Java detectadas |
| Major | 8 | DAST em staging | Confirmação de que os controles funcionam |
