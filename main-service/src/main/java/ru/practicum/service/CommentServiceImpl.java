package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.EntityNotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.model.dto.CommentDto;
import ru.practicum.model.dto.NewCommentDto;
import ru.practicum.model.dto.UpdateCommentDto;
import ru.practicum.model.entity.Comment;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(newCommentDto.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + newCommentDto.getEventId() + " was not found"));

        // Проверяем, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        // Проверяем, не комментировал ли уже пользователь это событие
        if (commentRepository.existsByEventIdAndAuthorId(event.getId(), userId)) {
            throw new ConflictException("User already commented this event");
        }

        Comment comment = commentMapper.toEntity(newCommentDto);
        comment.setAuthor(user);
        comment.setEvent(event);
        comment.setStatus(CommentStatus.PENDING);

        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment with id={} for event id={} by user id={}",
                savedComment.getId(), event.getId(), userId);

        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id=" + commentId + " was not found"));

        // Проверяем, что пользователь является автором комментария
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only comment author can update the comment");
        }

        // Проверяем, что комментарий можно редактировать
        if (comment.getStatus() == CommentStatus.DELETED_BY_USER ||
                comment.getStatus() == CommentStatus.DELETED_BY_ADMIN) {
            throw new ConflictException("Cannot update deleted comment");
        }

        // Проверяем, что прошло не более 24 часов с момента создания
        if (comment.getCreated().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new ValidationException("Comment can only be edited within 24 hours of creation");
        }

        // Проверяем лимит редактирований (максимум 5)
        if (comment.getEditCount() >= 5) {
            throw new ValidationException("Maximum 5 edits allowed per comment");
        }

        comment.setText(updateCommentDto.getText());
        comment.setStatus(CommentStatus.PENDING); // После редактирования - снова на модерацию
        comment.setEditCount(comment.getEditCount() + 1);

        Comment updatedComment = commentRepository.save(comment);
        log.info("Updated comment with id={} by user id={}. Edit count: {}",
                commentId, userId, updatedComment.getEditCount());

        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id=" + commentId + " was not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only comment author can delete the comment");
        }

        comment.setStatus(CommentStatus.DELETED_BY_USER);
        commentRepository.save(comment);
        log.info("Comment with id={} was deleted by author id={}", commentId, userId);
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, CommentStatus status, String rejectionReason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id=" + commentId + " was not found"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Only pending comments can be moderated");
        }

        comment.setStatus(status);

        if (status == CommentStatus.REJECTED) {
            comment.setRejectionReason(rejectionReason);
            log.info("Comment {} rejected with reason: {}", commentId, rejectionReason);
        } else {
            comment.setRejectionReason(null); // Очищаем причину при одобрении
        }

        Comment moderatedComment = commentRepository.save(comment);
        log.info("Comment with id={} moderated to status: {}", commentId, status);

        return commentMapper.toDto(moderatedComment);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, CommentStatus status, Integer from, Integer size) {
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event with id=" + eventId + " was not found");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments;

        if (status != null) {
            comments = commentRepository.findByEventIdAndStatus(eventId, status, pageable);
        } else {
            // По умолчанию показываем только APPROVED комментарии
            comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, pageable);
        }

        return commentMapper.toDtoList(comments);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Integer from, Integer size) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User with id=" + userId + " was not found");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        return commentMapper.toDtoList(comments);
    }

    @Override
    public List<CommentDto> getCommentsForModeration(CommentStatus status, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments;

        if (status != null) {
            comments = commentRepository.findByStatus(status, pageable);
        } else {
            // По умолчанию показываем комментарии на модерации
            comments = commentRepository.findByStatus(CommentStatus.PENDING, pageable);
        }

        return commentMapper.toDtoList(comments);
    }
}