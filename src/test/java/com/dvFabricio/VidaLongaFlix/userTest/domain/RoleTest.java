package com.dvFabricio.VidaLongaFlix.userTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void shouldCreateRole() {
        Role role = new Role("ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", role.getName());
    }

    @Test
    void shouldHaveNullIdBeforePersist() {
        Role role = new Role("ROLE_USER");
        assertNull(role.getId());
    }
}