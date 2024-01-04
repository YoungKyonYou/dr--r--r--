package com.drrr.web.security.filter;

import com.drrr.core.exception.jwt.JwtExceptionCode;
import com.drrr.web.jwt.util.JwtProvider;
import com.drrr.web.security.exception.JwtExpiredTokenException;
import com.drrr.web.security.exception.NotRegisteredIpException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@PropertySource(value = "classpath:security-storage-api/front/front-ip.properties")
@RequiredArgsConstructor
public class JwtTokenValidationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private final JwtProvider jwtTokenProvider;
    private final Set<String> IgnoreUrlsSet = new HashSet<>(List.of("/actuator/prometheus"));
    @Value("${api.acceptance.local.ipv4.ip}")
    private String ipv4AcceptIp;
    @Value("${api.acceptance.local.ipv6.ip}")
    private String ipv6AcceptIp;
    //@Value("${api.acceptance.front.ip}")
    private String frontIp ="118.235.7.224";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, @NotNull final HttpServletResponse response,
                                    @NotNull final FilterChain filterChain)
            throws ServletException, IOException {

        if (!ipv4AcceptIp.equals(request.getRemoteAddr()) && !ipv6AcceptIp.equals(request.getRemoteAddr())
                && !frontIp.equals(request.getRemoteAddr())) {
            log.info("-------------------request URI: " + request.getRequestURI() + "---------------");
            log.info("등록되지 않은 IP 요청 -> " + request.getRemoteAddr());
            log.info("requested session id -> "+request.getRequestedSessionId());
            throw new NotRegisteredIpException("등록되지 않은 IP 주소의 요청입니다.");
        }

        //prometheus의 지표 수집을 위한 주기적인 request는 무시
        if (IgnoreUrlsSet.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("-------------------JwtTokenValidationFilter CALL-------------------");
        log.info("-------------------request URI: " + request.getRequestURI() + "---------------");
        final String token = extractToken(request);

        if (Objects.isNull(token)) {
            log.info("-----------JWT Token null-------------------");
            filterChain.doFilter(request, response);
            return;
        }

        final Long memberId = jwtTokenProvider.extractToValueFrom(token);

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException(JwtExceptionCode.JWT_UNAUTHORIZED.newInstance("토큰 검증 실패"));
            }

            // 권한 부여
            final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(memberId, null,
                    List.of(new SimpleGrantedAuthority("USER")));
            // Detail을 넣어줌
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (JwtExpiredTokenException expiredJwtException) {
            // Handle expired token exception
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Token expired");
            response.getWriter().flush();
            log.error("JWT 토큰이 만료되었습니다.");
            throw new IllegalArgumentException(JwtExceptionCode.JWT_UNAUTHORIZED.newInstance());
        }
    }

    private String extractToken(final HttpServletRequest request) {
        final String token = request.getHeader(HEADER_AUTHORIZATION);

        if (token != null && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
