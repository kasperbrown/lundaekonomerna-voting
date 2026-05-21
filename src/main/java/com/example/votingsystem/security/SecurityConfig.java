package com.example.votingsystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Value("${app.admin.username}")
        private String adminUsername;

        @Value("${app.admin.password}")
        private String adminPassword;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/vote/**",
                                                                "/css/**",
                                                                "/images/**",
                                                                "/login")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/admin/**",
                                                                "/results/**")
                                                .authenticated()
                                                .anyRequest().permitAll())
                                .formLogin(login -> login
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/admin", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                PasswordEncoder encoder = passwordEncoder();

                UserDetails admin = User
                                .withUsername(adminUsername)
                                .password(encoder.encode(adminPassword))
                                .roles("ADMIN")
                                .build();

                return new InMemoryUserDetailsManager(admin);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }
}