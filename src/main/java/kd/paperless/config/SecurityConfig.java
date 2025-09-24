// src/main/java/kd/paperless/config/SecurityConfig.java
package kd.paperless.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

<<<<<<< Updated upstream

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // 1) 동일 출처 iframe 허용(내장 PDF 뷰어가 iframe에서 차단되지 않게)
      .headers(h -> h
        .frameOptions(fo -> fo.sameOrigin())
        .contentSecurityPolicy(csp ->
          csp.policyDirectives("frame-ancestors 'self'")
        )
      )

      // 2) URL 접근 권한 (필요 경로 permitAll)
=======
  private final CustomUserDetailsService userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  /** 🔐 단일 SecurityFilterChain */

  @Bean
public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

    http
      .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
>>>>>>> Stashed changes
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/", "/error",
              "/login/**", "/logout", "/signup/**", "/api/**",
              "/css/**", "/js/**", "/images/**","main/js/**",
              "/residentregistration/**",
              "/residentregistration/preview/**",
              "/residentregistration/pdf/**",
              "main/**","paperless/**","paperless/js/**",
              "sinmungo/**","sinmungo/css/**","sinmungo/js/**",
              "portal/**","portal/css/**","portal/js/**",
              "header-footer/**","/paperless/fragments/**,/paperless/js/form/rr_apply.js"
          ).permitAll()
          .anyRequest().authenticated()
      )

        .formLogin(login -> login
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("userId")
            .passwordParameter("password")
            .defaultSuccessUrl("/", true)
            .failureUrl("/login?error"))
        .logout(
            l -> l.logoutUrl("/logout").logoutSuccessUrl("/").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
        .sessionManagement(sess -> sess.sessionFixation(fix -> fix.migrateSession()));

    return http.build();
  }
}
