package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByEventId(Long eventId, Pageable pageable);

    List<Comment> findAllByAuthorId(Long userId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
            "WHERE (:users IS NULL OR c.author.id IN :users) " +
            "AND (:text IS NULL OR LOWER(c.text) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:rangeStart IS NULL OR c.created >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR c.created <= :rangeEnd) ")
    Page<Comment> searchAdmin(@Param("users") List<Long> users,
                              @Param("text") String text,
                              @Param("rangeStart") LocalDateTime rangeStart,
                              @Param("rangeEnd") LocalDateTime rangeEnd,
                              Pageable pageable);
}