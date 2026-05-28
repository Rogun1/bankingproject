package com.digitalbanking.bankingproject.config;

import com.digitalbanking.bankingproject.exceptions.CustomAccesDeniedHandler;
import com.digitalbanking.bankingproject.exceptions.CustomBasicAuthenticationEntryPoint;
import com.digitalbanking.bankingproject.filters.CsrfCookieFilter;
import com.digitalbanking.bankingproject.filters.JWTTokenGeneratorFilter;
import com.digitalbanking.bankingproject.filters.JWTTokenValidatorFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


@Configuration
@Profile("!prod")
public class BankingSecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {

        http
                .sessionManagement(smc -> smc
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .redirectToHttps((https) -> https.disable()) // Only HTTP ( for local )
                .csrf(csrfConfig -> csrfConfig.disable()
                .addFilterAfter(new JWTTokenGeneratorFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/customers/update").hasRole("USER")
                        .requestMatchers("/customers/me").hasRole("USER")
                        .requestMatchers("/accounts").hasRole("USER")
                        .requestMatchers("/accounts/myAccounts").hasRole("USER")
                        .requestMatchers("/accounts/cards").hasRole("USER")
                        .requestMatchers("/customers/login").authenticated()
                        .requestMatchers("/contact", "/error", "/customers/register").permitAll()));
        http.httpBasic(hbc -> hbc.authenticationEntryPoint(new CustomBasicAuthenticationEntryPoint()));
        http.exceptionHandling(ehc -> ehc.accessDeniedHandler(new CustomAccesDeniedHandler()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker(){
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }
}
