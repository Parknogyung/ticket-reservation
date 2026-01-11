package com.client.portFolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ClientSecurityConfig {

        private final com.client.portFolio.security.GrpcAuthenticationProvider authProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()
                                                .anyRequest().authenticated())
                                .authenticationProvider(authProvider) // gRPC 인증 프로바이더 등록
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public org.springframework.security.authentication.AuthenticationManager authenticationManager(
                        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config)
                        throws Exception {
                org.springframework.security.authentication.AuthenticationManager am = config
                                .getAuthenticationManager();
                if (am instanceof org.springframework.security.authentication.ProviderManager pm) {
                        pm.setEraseCredentialsAfterAuthentication(false);
                }
                return am;
        }
}
