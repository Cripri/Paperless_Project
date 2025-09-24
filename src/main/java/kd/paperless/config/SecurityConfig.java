// src/main/java/kd/paperless/config/SecurityConfig.java
package kd.paperless.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

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
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/", "/css/**", "/js/**", "/images/**",
          "/residentregistration/apply",
          "/residentregistration/preview/**",
          "/residentregistration/pdf/**",
          "/h2-console/**"            // 쓰는 경우만
        ).permitAll()
        .anyRequest().permitAll()     // 지금은 전체 공개 정책
      )

      .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
