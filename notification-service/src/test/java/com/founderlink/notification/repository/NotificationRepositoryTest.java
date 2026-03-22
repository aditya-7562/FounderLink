package com.founderlink.notification.repository;

import com.founderlink.notification.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationRepository.flush();

        // User 100: 2 read, 1 unread
        // @PrePersist sets read=false on initial save, so we save first then mark as read
        Notification n1 = new Notification();
        n1.setUserId(100L);
        n1.setType("STARTUP_CREATED");
        n1.setMessage("New startup created in Tech industry");
        n1 = notificationRepository.save(n1);
        n1.setRead(true);
        notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setUserId(100L);
        n2.setType("INVESTMENT_CREATED");
        n2.setMessage("New investment of $50000");
        n2 = notificationRepository.save(n2);
        n2.setRead(true);
        notificationRepository.save(n2);

        Notification n3 = new Notification();
        n3.setUserId(100L);
        n3.setType("TEAM_INVITE_SENT");
        n3.setMessage("You have been invited to join startup #1 as CTO");
        notificationRepository.save(n3); // stays unread (@PrePersist default)

        // User 200: 1 unread
        Notification n4 = new Notification();
        n4.setUserId(200L);
        n4.setType("STARTUP_CREATED");
        n4.setMessage("New startup created in Health industry");
        notificationRepository.save(n4); // stays unread (@PrePersist default)
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc - returns all notifications for user")
    void findByUserId_ReturnsAll() {
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(100L);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(n -> n.getUserId().equals(100L));
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc - returns empty for unknown user")
    void findByUserId_EmptyForUnknown() {
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndReadFalseOrderByCreatedAtDesc - returns only unread")
    void findUnread_ReturnsOnlyUnread() {
        List<Notification> result = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("TEAM_INVITE_SENT");
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("findByUserIdAndReadFalseOrderByCreatedAtDesc - returns empty when all read")
    void findUnread_EmptyWhenAllRead() {
        // Mark all as read for user 100
        notificationRepository.findByUserIdOrderByCreatedAtDesc(100L).forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });

        List<Notification> result = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(100L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdAndReadFalseOrderByCreatedAtDesc - different user has own unread")
    void findUnread_IsolatedPerUser() {
        List<Notification> user200Unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(200L);

        assertThat(user200Unread).hasSize(1);
        assertThat(user200Unread.get(0).getUserId()).isEqualTo(200L);
    }
}
