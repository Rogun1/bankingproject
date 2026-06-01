package com.digitalbanking.bankingproject.config;

import com.digitalbanking.bankingproject.exceptions.CustomAccesDeniedHandler;
import com.digitalbanking.bankingproject.exceptions.CustomBasicAuthenticationEntryPoint;
import com.digitalbanking.bankingproject.filters.JWTTokenGeneratorFilter;
import com.digitalbanking.bankingproject.filters.JWTTokenValidatorFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;


@Configuration
@Profile("prod")
@EnableWebSecurity
@EnableMethodSecurity
public class BankingProdSecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {

        http
                .sessionManagement(smc -> smc
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .redirectToHttps((https) -> https.requestMatchers(AnyRequestMatcher.INSTANCE))
                .csrf(csrfConfig -> csrfConfig.disable()
                .addFilterAfter(new JWTTokenGeneratorFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/users/update").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/users/me").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/users/*/roles").hasAnyRole("MANAGER","ADMIN")
                        .requestMatchers("/users/*/delete").hasAnyRole("MANAGER","ADMIN")
                        .requestMatchers("/accounts").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/accounts/myAccounts").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/accounts/cards").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/accounts/cards/**").hasAnyRole("CUSTOMER","EMPLOYER")
                        .requestMatchers("/accounts/*/delete").hasAnyRole("ADMIN","EMPLOYER")
                        .requestMatchers("/transactions").hasAnyRole("CUSTOMER")
                        .requestMatchers("/users/login").authenticated()
                        .requestMatchers("/contact", "/error", "/users/register").permitAll()));
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
