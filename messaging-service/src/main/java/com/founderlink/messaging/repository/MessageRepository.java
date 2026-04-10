package com.founderlink.messaging.repository;

import com.founderlink.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
           "(m.senderId = :user2 AND m.receiverId = :user1) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query(
            value = "SELECT m FROM Message m WHERE " +
                    "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
                    "(m.senderId = :user2 AND m.receiverId = :user1)",
            countQuery = "SELECT COUNT(m) FROM Message m WHERE " +
                    "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
                    "(m.senderId = :user2 AND m.receiverId = :user1)"
    )
    Page<Message> findConversationPage(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);

    @Query("SELECT DISTINCT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END " +
           "FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<Long> findConversationPartners(@Param("userId") Long userId);

    @Query(
            value = "SELECT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END " +
                    "FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId " +
                    "GROUP BY CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END " +
                    "ORDER BY MAX(m.createdAt) DESC",
            countQuery = "SELECT COUNT(DISTINCT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END) " +
                    "FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId"
    )
    Page<Long> findConversationPartnersPage(@Param("userId") Long userId, Pageable pageable);

    // ── Cursor-based queries ──────────────────────────────────────────────────

    /**
     * Load messages BEFORE a cursor id (for "load older history").
     * Returns results in DESC order so the caller can efficiently take the top N
     * and then reverse them for chronological (ASC) display.
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :u1 AND m.receiverId = :u2) OR (m.senderId = :u2 AND m.receiverId = :u1)) " +
           "AND m.id < :beforeId ORDER BY m.id DESC")
    List<Message> findBefore(@Param("u1") Long u1,
                             @Param("u2") Long u2,
                             @Param("beforeId") Long beforeId,
                             Pageable pageable);

    /**
     * Load messages AFTER a cursor id (for real-time catch-up / "load newer").
     * Returns results in ASC order, ready for direct appending.
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.senderId = :u1 AND m.receiverId = :u2) OR (m.senderId = :u2 AND m.receiverId = :u1)) " +
           "AND m.id > :afterId ORDER BY m.id ASC")
    List<Message> findAfter(@Param("u1") Long u1,
                            @Param("u2") Long u2,
                            @Param("afterId") Long afterId,
                            Pageable pageable);
}
