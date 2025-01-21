package com.dvFabricio.VidaLongaFlix.user.repository;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setup() {
        entityManager.clear();
        roleRepository.deleteAll();
        entityManager.flush();

        Role role = new Role();
        role.setName("ROLE_USER");
        entityManager.persistAndFlush(role);
    }

    @Test
    void findByName_ShouldReturnRole_WhenNameExists() {
        Optional<Role> foundRole = roleRepository.findByName("ROLE_USER");

        assertAll(
                () -> assertTrue(foundRole.isPresent(), "A role deve ser encontrada"),
                () -> assertEquals("ROLE_USER", foundRole.get().getName(), "O nome da role deve ser 'ROLE_USER'")
        );
    }

    @Test
    void findByName_ShouldReturnEmpty_WhenNameDoesNotExist() {
        assertEquals(1, roleRepository.findAll().size(), "A tabela 'roles' deve conter exatamente 1 role");

        Optional<Role> foundRole = roleRepository.findByName("ROLE_ADMIN");

        assertFalse(foundRole.isPresent(), "Nenhuma role deve ser encontrada");
    }
}