package com.dvFabricio.VidaLongaFlix.user.repository;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setup() {
        user = new User("testUser", "test@example.com", "password123");
        entityManager.persistAndFlush(user);
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        assertAll(
                () -> assertTrue(foundUser.isPresent(), "O usuário deve ser encontrado"),
                () -> assertEquals("testUser", foundUser.get().getLogin(), "O login do usuário deve ser 'testUser'"),
                () -> assertEquals("test@example.com", foundUser.get().getEmail(), "O email do usuário deve ser 'test@example.com'")
        );
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(foundUser.isPresent(), "Nenhum usuário deve ser encontrado");
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertTrue(exists, "O método deve retornar true quando o email existir");
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists, "O método deve retornar false quando o email não existir");
    }
}