package com.dvFabricio.VidaLongaFlix.userTest.domain;

import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithBasicConstructor() {
        User user = new User("João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");

        assertEquals("João Silva", user.getName());
        assertEquals("joao@example.com", user.getEmail());
        assertEquals("Password1@", user.getPassword());
        assertFalse(user.isProfileComplete());
        assertNotNull(user.getRoles());
    }

    @Test
    void shouldAssociateRoles() {
        User user = new User("João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");
        user.setRoles(List.of(new Role("ROLE_USER"), new Role("ROLE_ADMIN")));

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_USER")));
        assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldImplementUserDetailsCorrectly() {
        User user = new User("João Silva", "joao@example.com", "Password1@", "(11) 99999-9999");
        user.setRoles(List.of(new Role("ROLE_USER")));

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
        assertEquals("joao@example.com", user.getUsername());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("ROLE_USER", user.getAuthorities().iterator().next().getAuthority());
    }
}