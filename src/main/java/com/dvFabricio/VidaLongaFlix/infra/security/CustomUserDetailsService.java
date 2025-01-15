package com.dvFabricio.VidaLongaFlix.infra.security;


import com.dvFabricio.VidaLongaFlix.infra.exception.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Component
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    public CustomUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Usuário não encontrado."));
    }
}

