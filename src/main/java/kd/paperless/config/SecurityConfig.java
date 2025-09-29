package kd.paperless.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import kd.paperless.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

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
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/", "/error",
              "/favicon.ico",
              "/login/**", "/logout", "/signup/**", "/api/**",
              "/css/**", "/js/**", "/images/**",
              "/residentregistration/apply",
              "/residentregistration/preview/**",
              "/residentregistration/pdf/**",
              "/findAccount/**", "/resetPassword/**", "/account/**",
              "paperless/**","main/**","/residentregistration/**","paperless/fragments/**",
              "/oauth/**", "/sns/**","portal","portal/js/**","portal/css/**","header-footer/**"
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
