package com.dvFabricio.VidaLongaFlix.userTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User user = new User("João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");
        user.setTaxId("123.456.789-00");
        entityManager.persistAndFlush(user);
    }

    @Test
    void shouldFindByEmail() {
        Optional<User> result = userRepository.findByEmail("joao@example.com");
        assertTrue(result.isPresent());
        assertEquals("João Silva", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("notfound@example.com");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        assertTrue(userRepository.existsByEmail("joao@example.com"));
    }

    @Test
    void shouldReturnFalseWhenEmailNotExists() {
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }

    @Test
    void shouldReturnTrueWhenTaxIdExists() {
        assertTrue(userRepository.existsByTaxId("123.456.789-00"));
    }

    @Test
    void shouldReturnFalseWhenTaxIdNotExists() {
        assertFalse(userRepository.existsByTaxId("000.000.000-00"));
    }
}