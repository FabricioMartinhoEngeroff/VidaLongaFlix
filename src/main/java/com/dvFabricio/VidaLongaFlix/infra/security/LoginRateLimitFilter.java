package com.dvFabricio.VidaLongaFlix.infra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Filtro de rate limiting para o endpoint de login.
 *
 * Conceito da aula de redes: ACL (Access Control List).
 * Assim como uma ACL em roteadores analisa origem e destino antes de permitir
 * o tráfego, este filtro analisa o IP de origem e o endpoint de destino
 * antes de deixar a requisição chegar ao controller.
 *
 * Regra aplicada:
 *   - Máximo de 5 tentativas de login por IP a cada 1 minuto
 *   - Se ultrapassar: HTTP 429 Too Many Requests
 *   - Após 1 minuto: o balde de tokens enche novamente
 *
 * Por que 5 tentativas:
 *   Um usuário legítimo que esqueceu a senha tenta no máximo 2 ou 3 vezes.
 *   5 é generoso o suficiente para não bloquear usuários reais e restritivo
 *   o suficiente para impedir ataques automatizados de força bruta.
 */
@Component
@Order(1)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    // Mapa em memória: IP → balde de tokens
    // ConcurrentHashMap é thread-safe — múltiplas requisições simultâneas não corrompem o estado
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Só aplica a regra no endpoint de login via POST
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);

        // Localhost não é bloqueado — permite testes de carga locais sem distorção
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }

        // computeIfAbsent: cria o balde na primeira vez que esse IP aparece
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> createBucket());

        // tryConsume(1): tenta consumir 1 ficha do balde
        // retorna true se tinha ficha disponível, false se o balde está vazio
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"Muitas tentativas de login. Aguarde 1 minuto.\",\"code\":\"RATE_LIMIT_EXCEEDED\"}"
            );
        }
    }

    /**
     * Cria um balde com capacidade de 5 tokens.
     * O Refill.intervally repõe todos os 5 tokens de uma vez a cada 1 minuto
     * (diferente de Refill.greedy que repõe gradualmente).
     *
     * Analogia: um porteiro que distribui 5 senhas a cada minuto.
     * Quando as senhas acabam, ninguém mais entra até o minuto seguinte.
     */
    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build())
                .build();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().contains("/auth/login");
    }

    /**
     * Resolve o IP real do cliente.
     * Quando existe um proxy ou load balancer (como na AWS com CloudFront + ALB),
     * o IP real do usuário vem no header X-Forwarded-For, não em getRemoteAddr().
     *
     * Conceito da aula: NAT — o roteador (CloudFront/ALB) substitui o IP de origem.
     * Sem esse header, todos os usuários apareceriam com o IP do load balancer.
     */
    String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}