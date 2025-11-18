package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.entity.Comment;
import ru.practicum.model.enums.CommentStatus;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorId(Long authorId, Pageable pageable);

    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.status IN :statuses")
    List<Comment> findByEventIdAndStatusIn(@Param("eventId") Long eventId,
                                           @Param("statuses") List<CommentStatus> statuses,
                                           Pageable pageable);

    Boolean existsByEventIdAndAuthorId(Long eventId, Long authorId);
}