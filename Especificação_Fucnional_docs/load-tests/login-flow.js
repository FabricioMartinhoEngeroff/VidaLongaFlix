/**
 * VidaLongaFlix — Teste de Carga Realista
 *
 * Conceito da aula de redes:
 *   Ping e Traceroute testam conectividade e latência ponto a ponto.
 *   O k6 faz o equivalente para uma aplicação HTTP: simula usuários reais
 *   e mede como o sistema responde sob carga.
 *
 * Cenário:
 *   - 25 usuários virtuais navegando simultaneamente por 2 minutos
 *   - Cada usuário faz o fluxo real: login → vídeos → comentários → favoritar
 *   - Reflete o uso real: 100 cadastros, ~25% online ao mesmo tempo
 *
 * Conceito de connection pool (domínio de broadcast da aula):
 *   O HikariCP (pool do banco) tem padrão de 10 conexões simultâneas.
 *   Com 25 usuários fazendo requisições ao mesmo tempo, algumas vão esperar
 *   por uma conexão livre. O teste vai mostrar se isso gera atraso visível.
 *
 * Como rodar:
 *   1. Instale o k6: https://k6.io/docs/get-started/installation/
 *      Linux: sudo apt install k6
 *      Mac:   brew install k6
 *
 *   2. Suba o backend local:
 *      ./mvnw spring-boot:run
 *
 *   3. Execute:
 *      k6 run load-tests/login-flow.js
 *
 *   4. Para ver métricas em tempo real no Actuator (outra aba):
 *      curl http://localhost:8090/api/actuator/metrics/http.server.requests | jq
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ─── Métricas customizadas ────────────────────────────────────────────────────
// Taxa de erro específica do login (separada da taxa geral)
const loginErrorRate = new Rate('login_errors');
// Tempo de resposta do login separado para análise
const loginDuration = new Trend('login_duration_ms');

// ─── Configuração do teste ────────────────────────────────────────────────────
export const options = {
  scenarios: {
    usuarios_navegando: {
      executor: 'constant-vus',  // mantém VUs constantes (não aumenta gradualmente)
      vus: 25,                   // 25 usuários virtuais simultâneos
      duration: '2m',            // duração de 2 minutos
    },
  },

  // Limites aceitáveis — se o sistema ultrapassar, o teste falha
  // Analogia: o "ping" retornar tempo aceitável para considerar a rede saudável
  thresholds: {
    'http_req_duration': [
      'p(95)<2000',   // 95% das requisições em menos de 2 segundos
      'p(99)<5000',   // 99% em menos de 5 segundos
    ],
    'http_req_failed':  ['rate<0.05'],   // menos de 5% de erro total
    'login_errors':     ['rate<0.02'],   // menos de 2% de erro no login especificamente
    'login_duration_ms':['p(95)<1500'],  // login em menos de 1.5s para 95% dos casos
  },
};

// ─── Configuração do ambiente ─────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090/api';

// Usuário padrão que o DataInitializer cria (application.properties)
const DEFAULT_USER = {
  email: __ENV.TEST_EMAIL    || 'admin@vidalongaflix.com',
  password: __ENV.TEST_PASSWORD || 'Admin@123456',
};

// ─── Fluxo principal de cada usuário virtual ─────────────────────────────────
export default function () {

  // PASSO 1: Login
  // Conceito: cada VU abre uma conexão TCP com o servidor (handshake)
  // e envia a requisição HTTP — o que o Wireshark mostraria como pacotes
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: DEFAULT_USER.email, password: DEFAULT_USER.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  loginDuration.add(loginRes.timings.duration);

  const loginOk = check(loginRes, {
    'login: status 200':    (r) => r.status === 200,
    'login: tem token':     (r) => r.json('token') !== null,
  });

  loginErrorRate.add(!loginOk);

  if (!loginOk) {
    // Se o login falhou, não continua — evita requisições sem token
    sleep(1);
    return;
  }

  const token = loginRes.json('token');
  const authHeaders = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };

  // Pequena pausa entre ações — simula o usuário lendo a tela
  // Sem isso, o teste seria mais agressivo do que o uso real
  sleep(randomBetween(0.5, 1.5));

  // PASSO 2: Carregar catálogo de vídeos (endpoint público)
  const videosRes = http.get(`${BASE_URL}/videos`);
  check(videosRes, {
    'videos: status 200': (r) => r.status === 200,
    'videos: tem lista':  (r) => Array.isArray(r.json()),
  });

  // Pega o ID do primeiro vídeo da lista para usar nos passos seguintes
  const videos = videosRes.json();
  const videoId = Array.isArray(videos) && videos.length > 0 ? videos[0].id : null;

  sleep(randomBetween(1, 2));

  // PASSO 3: Verificar favoritos do usuário (endpoint autenticado)
  // Conceito de connection pool: esta requisição precisa de uma conexão com o banco
  const favoritesRes = http.get(`${BASE_URL}/favorites`, authHeaders);
  check(favoritesRes, {
    'favorites: status 200': (r) => r.status === 200,
  });

  sleep(randomBetween(0.5, 1));

  // PASSO 4: Carregar comentários de um vídeo (endpoint público)
  // Usa o ID real do catálogo — pula se o banco estiver vazio (ex: ambiente local sem dados)
  if (videoId) {
    const commentsRes = http.get(`${BASE_URL}/comments/video/${videoId}`);
    check(commentsRes, {
      'comments: status 200': (r) => r.status === 200,
    });
  }

  sleep(randomBetween(1, 3));
}

// ─── Relatório final ──────────────────────────────────────────────────────────
export function handleSummary(data) {
  const metrics = data.metrics;

  const p95 = metrics['http_req_duration']?.values?.['p(95)']?.toFixed(0) ?? 'N/A';
  const p99 = metrics['http_req_duration']?.values?.['p(99)']?.toFixed(0) ?? 'N/A';
  const errorRate = ((metrics['http_req_failed']?.values?.rate ?? 0) * 100).toFixed(2);
  const totalReqs = metrics['http_reqs']?.values?.count ?? 0;
  const loginP95 = metrics['login_duration_ms']?.values?.['p(95)']?.toFixed(0) ?? 'N/A';

  const summary = `
╔══════════════════════════════════════════════════════════╗
║           VidaLongaFlix — Resultado do Teste             ║
╠══════════════════════════════════════════════════════════╣
║  Usuários simultâneos: 25         Duração: 2 minutos     ║
╠══════════════════════════════════════════════════════════╣
║  LATÊNCIA (todas as requisições)                         ║
║    p95: ${p95.padEnd(10)} ms  (95% responderam abaixo disso)     ║
║    p99: ${p99.padEnd(10)} ms  (99% responderam abaixo disso)     ║
╠══════════════════════════════════════════════════════════╣
║  LOGIN ESPECÍFICO                                        ║
║    p95: ${loginP95.padEnd(10)} ms                                    ║
╠══════════════════════════════════════════════════════════╣
║  ERROS: ${errorRate.padEnd(6)} %      TOTAL DE REQUISIÇÕES: ${String(totalReqs).padEnd(6)}    ║
╚══════════════════════════════════════════════════════════╝

Interpretação:
  p95 < 2000ms  → ✅ Dentro do limite aceitável
  p95 >= 2000ms → ❌ Sistema sofrendo com a carga
  erros < 5%    → ✅ Sistema estável
  erros >= 5%   → ❌ Verificar logs e connection pool

Para ver detalhes internos durante o teste:
  curl http://localhost:8090/api/actuator/health | jq
  curl "http://localhost:8090/api/actuator/metrics/http.server.requests" | jq '.measurements'
`;

  console.log(summary);
  return { stdout: summary };
}

// ─── Utilitário ───────────────────────────────────────────────────────────────
function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}
