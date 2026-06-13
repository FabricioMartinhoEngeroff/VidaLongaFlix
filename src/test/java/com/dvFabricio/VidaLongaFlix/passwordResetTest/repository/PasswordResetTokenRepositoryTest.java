package com.dvFabricio.VidaLongaFlix.passwordResetTest.repository;

import com.dvFabricio.VidaLongaFlix.domain.passwordreset.PasswordResetToken;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetTokenRepository repository;

    private User user;

    @BeforeEach
    void setup() {
        // Usamos o construtor (name, email, password, phone) porque
        // phone é NOT NULL no banco — o setter não bastaria sem o campo.
        user = new User("João Teste", "joao@teste.com", "hashed-password", "11999999999");
        entityManager.persistAndFlush(user);
    }

    // Constantes com exatamente 64 caracteres cada — limite do VARCHAR(64) no banco.
    private static final String TOKEN_A  = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String TOKEN_B  = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String TOKEN_DUP = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    // PRTR-01 — findByToken deve retornar o token correto
    @Test
    void findByToken_shouldReturnCorrectToken() {
        PasswordResetToken token = new PasswordResetToken(user, TOKEN_A, LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(token);

        Optional<PasswordResetToken> result = repository.findByToken(TOKEN_A);

        assertTrue(result.isPresent());
        assertEquals(TOKEN_A, result.get().getToken());
    }

    // PRTR-02 — findByToken deve retornar Optional vazio para token inexistente
    @Test
    void findByToken_shouldReturnEmptyForUnknownToken() {
        Optional<PasswordResetToken> result = repository.findByToken("tokenqueNaoExiste");

        assertFalse(result.isPresent());
    }

    // PRTR-03 — deleteByUserId deve remover todos os tokens do usuário
    @Test
    void deleteByUserId_shouldRemoveAllTokensOfUser() {
        PasswordResetToken t1 = new PasswordResetToken(user, TOKEN_A, LocalDateTime.now().plusMinutes(30));
        PasswordResetToken t2 = new PasswordResetToken(user, TOKEN_B, LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        repository.deleteByUserId(user.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(repository.findByToken(TOKEN_A).isEmpty());
        assertTrue(repository.findByToken(TOKEN_B).isEmpty());
    }

    // PRTR-04 — token deve ter constraint UNIQUE no campo token
    @Test
    void token_shouldEnforceUniqueConstraint() {
        PasswordResetToken t1 = new PasswordResetToken(user, TOKEN_DUP, LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(t1);

        User outroUser = new User("Maria Teste", "maria@teste.com", "hashed-password", "11988888888");
        entityManager.persistAndFlush(outroUser);

        PasswordResetToken t2 = new PasswordResetToken(outroUser, TOKEN_DUP, LocalDateTime.now().plusMinutes(30));

        // TestEntityManager não passa pela tradução de exceções do Spring,
        // então Hibernate lança ConstraintViolationException diretamente
        // em vez de DataIntegrityViolationException. Ambas estendem RuntimeException.
        assertThrows(RuntimeException.class, () -> entityManager.persistAndFlush(t2),
                "Banco deve rejeitar dois tokens com o mesmo valor");
    }
}