package com.dvFabricio.VidaLongaFlix.infra.security;

import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private final UserRepository repository;

    public AuthorizationService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Buscando usuário pelo email: " + username);

        return repository.findByEmail(username)
                .orElseThrow(() -> {
                    System.out.println("Usuário não encontrado: " + username);
                    return new UsernameNotFoundException("Usuário não encontrado com o email: " + username);
                });
    }
}