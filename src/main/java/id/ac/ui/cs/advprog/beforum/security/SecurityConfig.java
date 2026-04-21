package id.ac.ui.cs.advprog.beforum.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/messages/**", "/api/messages/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/messages", "/api/messages").authenticated()
            .requestMatchers(HttpMethod.POST, "/messages/*/replies", "/api/messages/*/replies")
            .authenticated()
            .requestMatchers(HttpMethod.PUT, "/messages/*", "/api/messages/*").authenticated()
            .requestMatchers(
                HttpMethod.PUT,
                "/messages/*/replies/*",
                "/api/messages/*/replies/*")
            .authenticated()
            .requestMatchers(HttpMethod.DELETE, "/messages/*", "/api/messages/*").authenticated()
            .requestMatchers(
                HttpMethod.DELETE,
                "/messages/*/replies/*",
                "/api/messages/*/replies/*")
            .authenticated()
            .anyRequest().permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    return http.build();
  }
}
