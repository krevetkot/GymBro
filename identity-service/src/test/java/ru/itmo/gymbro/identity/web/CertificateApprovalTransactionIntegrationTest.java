package ru.itmo.gymbro.identity.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.Certificate;
import ru.itmo.gymbro.identity.model.CertificateStatus;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.CertificateRepository;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.identity.security.JwtTokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * Одобрение сертификата — одна транзакция: если решение по сертификату не сохранилось,
 * роль автора тоже не должна смениться на TRAINER.
 */
class CertificateApprovalTransactionIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    private CertificateRepository certificates;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtTokenService tokens;

    @Test
    @DisplayName("Сбой при сохранении решения откатывает и смену роли")
    void failedDecisionRollsBackRoleChange() {
        User author = users.save(User.register("rollback@cert.test", "hash"));
        long id = certificates.save(Certificate.submit(author.getId(), 3, "https://files.test/a.pdf")).getId();
        doThrow(new DataAccessResourceFailureException("Сбой базы при сохранении решения"))
                .when(certificates).save(argThat(certificate -> certificate.getStatus() == CertificateStatus.APPROVED));
        String admin = tokens.issue(users.findByEmail("admin@gymbro.local").orElseThrow()).accessToken();

        client.post().uri("/api/v1/certificates/" + id + "/approve")
                .header("Authorization", "Bearer " + admin)
                .exchange().expectStatus().is5xxServerError();

        assertThat(users.findById(author.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(certificates.findById(id).orElseThrow().getStatus()).isEqualTo(CertificateStatus.PENDING);
    }
}
