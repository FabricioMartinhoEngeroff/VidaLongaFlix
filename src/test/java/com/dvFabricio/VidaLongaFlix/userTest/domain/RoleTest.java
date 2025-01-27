package com.dvFabricio.VidaLongaFlix.userTest.domain;


import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void testRoleCreation() {
        Role role = new Role("ROLE_ADMIN");

        assertNotNull(role);
        assertEquals("ROLE_ADMIN", role.getName());
    }

    @Test
    void testRoleIdInitialization() {
        Role role = new Role("ROLE_USER");
        assertNull(role.getId(), "ID should be null until persisted.");
    }
}