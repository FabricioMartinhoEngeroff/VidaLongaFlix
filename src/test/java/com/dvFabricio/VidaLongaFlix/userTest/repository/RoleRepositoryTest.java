package com.dvFabricio.VidaLongaFlix.userTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private RoleRepository roleRepository;

    @BeforeEach
    void setup() {
        roleRepository.deleteAll();
        Role role = new Role("ROLE_USER");
        entityManager.persistAndFlush(role);
    }

    @Test
    void shouldFindRoleByName() {
        Optional<Role> result = roleRepository.findByName("ROLE_USER");
        assertTrue(result.isPresent());
        assertEquals("ROLE_USER", result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenRoleNotFound() {
        Optional<Role> result = roleRepository.findByName("ROLE_ADMIN");
        assertFalse(result.isPresent());
    }
}