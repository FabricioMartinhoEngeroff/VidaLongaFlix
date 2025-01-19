package com.dvFabricio.VidaLongaFlix.user.domain;


import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation() {
        User user = new User("fabricio", "fabricio@example.com", "password123");

        assertNotNull(user);
        assertEquals("fabricio", user.getLogin());
        assertEquals("fabricio@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
    }

    @Test
    void testUserRolesAssociation() {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");

        User user = new User("fabricio", "fabricio@example.com", "password123");
        user.setRoles(List.of(adminRole, userRole));

        assertNotNull(user.getRoles());
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")));
        assertTrue(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_USER")));
    }

    @Test
    void testUserDetailsImplementation() {
        Role userRole = new Role("ROLE_USER");
        User user = new User("fabricio", "fabricio@example.com", "password123");
        user.setRoles(List.of(userRole));

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        assertEquals("fabricio", user.getUsername());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_USER", user.getAuthorities().iterator().next().getAuthority());
    }
}

