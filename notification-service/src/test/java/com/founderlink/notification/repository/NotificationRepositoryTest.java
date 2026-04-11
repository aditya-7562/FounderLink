package com.founderlink.notification.repository;

import com.founderlink.notification.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import="
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationRepository.flush();

        // User 100: 2 read, 1 unread
        Notification n1 = new Notification();
        n1.setUserId(100L);
        n1.setType("T1");
        n1.setMessage("M1");
        n1.setRead(true);
        notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setUserId(100L);
        n2.setType("T2");
        n2.setMessage("M2");
        n2.setRead(true);
        notificationRepository.save(n2);

        Notification n3 = new Notification();
        n3.setUserId(100L);
        n3.setType("T3");
        n3.setMessage("M3");
        n3.setRead(false);
        notificationRepository.save(n3);

        // User 200: 1 unread
        Notification n4 = new Notification();
        n4.setUserId(200L);
        n4.setType("T4");
        n4.setMessage("M4");
        n4.setRead(false);
        notificationRepository.save(n4);
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc")
    void test1() {
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L)).hasSize(3);
    }

    @Test
    @DisplayName("findByUserIdAndReadFalseOrderByCreatedAtDesc")
    void test2() {
        assertThat(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(100L)).hasSize(1);
    }

    @Test
    @DisplayName("findByUserId (Paginated)")
    void test3() {
        Page<Notification> page = notificationRepository.findByUserId(100L, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findByUserIdAndReadFalse (Paginated)")
    void test4() {
        Page<Notification> page = notificationRepository.findByUserIdAndReadFalse(100L, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByIdAndUserId")
    void test5() {
        Notification n = notificationRepository.findByUserIdOrderByCreatedAtDesc(100L).get(0);
        Optional<Notification> found = notificationRepository.findByIdAndUserId(n.getId(), 100L);
        assertThat(found).isPresent();
        
        assertThat(notificationRepository.findByIdAndUserId(n.getId(), 999L)).isEmpty();
    }
}
