package com.digitalbanking.bankingproject.config;

import com.digitalbanking.bankingproject.service.BankProjectUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class BankAppUsernamePwdAutheticationProvider implements AuthenticationProvider {

    private final BankProjectUserDetailsService bankProjectUserDetailsService;
    private final PasswordEncoder passwordEncoder;


    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String pwd = authentication.getCredentials().toString();
        UserDetails userDetails = bankProjectUserDetailsService.loadUserByUsername(username);

        if (passwordEncoder.matches(pwd, userDetails.getPassword())){
            //costum authetication logic goes here, like age, etc

            return new UsernamePasswordAuthenticationToken(username,pwd,userDetails.getAuthorities());
        }else {
            throw new BadCredentialsException("Invalid password");
        }

    }

    @Override
    public boolean supports(Class<?> authentication) {
        //Dao implementation of support method
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
