package me.silvernine.tutorial.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class JwtFilter extends GenericFilterBean {

   private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
   public static final String AUTHORIZATION_HEADER = "Authorization";
   private TokenProvider tokenProvider;
   public JwtFilter(TokenProvider tokenProvider) {
      this.tokenProvider = tokenProvider;
   }

   @Override
   public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
      HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
      String jwt = resolveToken(httpServletRequest);
      String requestURI = httpServletRequest.getRequestURI();

      if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
         Authentication authentication = tokenProvider.getAuthentication(jwt);
         SecurityContextHolder.getContext().setAuthentication(authentication);
         logger.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), requestURI);
      } else {
         logger.debug("유효한 JWT 토큰이 없습니다, uri: {}", requestURI);
      }

      filterChain.doFilter(servletRequest, servletResponse);
   }

   private String resolveToken(HttpServletRequest request) {
      String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

      if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
         return bearerToken.substring(7);
      }

      return null;
   }
}
