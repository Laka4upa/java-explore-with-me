package ru.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private User otherUser;
    private Event event;
    private Comment comment;
    private NewCommentDto newCommentDto;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("User").email("user@mail.com").build();
        otherUser = User.builder().id(2L).name("Other User").email("other@mail.com").build();
        event = Event.builder().id(1L).state(EventState.PUBLISHED).initiator(otherUser).build();

        comment = Comment.builder()
                .id(1L)
                .text("Original Comment")
                .status(CommentStatus.APPROVED)
                .created(LocalDateTime.now().minusHours(1))
                .author(user)
                .event(event)
                .editCount(0)
                .build();

        newCommentDto = NewCommentDto.builder()
                .text("New Comment")
                .eventId(1L)
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("New Comment")
                .status(CommentStatus.PENDING)
                .build();
    }

    @Test
    void createComment_WithValidData_ShouldCreateComment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.existsByEventIdAndAuthorId(1L, 1L)).thenReturn(false);
        when(commentMapper.toEntity(newCommentDto)).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.createComment(1L, newCommentDto);

        assertNotNull(result);
        assertEquals(CommentStatus.PENDING, comment.getStatus());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_WhenEventNotPublished_ShouldThrowConflictException() {
        event.setState(EventState.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> commentService.createComment(1L, newCommentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_WhenUserAlreadyCommented_ShouldThrowConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.existsByEventIdAndAuthorId(1L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> commentService.createComment(1L, newCommentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_WithValidData_ShouldUpdateAndResetStatus() {
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.updateComment(1L, 1L, updateDto);

        assertNotNull(result);
        assertEquals("Updated Text", comment.getText());
        assertEquals(CommentStatus.PENDING, comment.getStatus()); // Должен сброситься на модерацию
        assertEquals(1, comment.getEditCount()); // Счетчик должен увеличиться
        verify(commentRepository).save(comment);
    }

    @Test
    void updateComment_WhenEditCountIs5_ShouldThrowValidationException() {
        comment.setEditCount(5);
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));

        assertEquals("Maximum 5 edits allowed per comment", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_When25HoursPassed_ShouldThrowValidationException() {
        comment.setCreated(LocalDateTime.now().minusHours(25));
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));

        assertEquals("Comment can only be edited within 24 hours of creation", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_WhenUserNotAuthor_ShouldThrowConflictException() {
        comment.setAuthor(otherUser); // Другой пользователь
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));

        assertEquals("Only comment author can update the comment", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_WhenCommentDeleted_ShouldThrowConflictException() {
        comment.setStatus(CommentStatus.DELETED_BY_USER);
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));

        assertEquals("Cannot update deleted comment", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteComment_ByAuthor_ShouldMarkAsDeletedByUser() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.deleteComment(1L, 1L);

        assertEquals(CommentStatus.DELETED_BY_USER, comment.getStatus());
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_ByNonAuthor_ShouldThrowConflictException() {
        comment.setAuthor(otherUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> commentService.deleteComment(1L, 1L));

        assertEquals("Only comment author can delete the comment", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void moderateComment_ApprovePendingComment_ShouldChangeStatus() {
        comment.setStatus(CommentStatus.PENDING);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.moderateComment(1L, CommentStatus.APPROVED, null);

        assertNotNull(result);
        assertEquals(CommentStatus.APPROVED, comment.getStatus());
        assertNull(comment.getRejectionReason());
        verify(commentRepository).save(comment);
    }

    @Test
    void moderateComment_RejectPendingComment_ShouldSetRejectionReason() {
        comment.setStatus(CommentStatus.PENDING);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.moderateComment(1L, CommentStatus.REJECTED, "Spam content");

        assertNotNull(result);
        assertEquals(CommentStatus.REJECTED, comment.getStatus());
        assertEquals("Spam content", comment.getRejectionReason());
        verify(commentRepository).save(comment);
    }

    @Test
    void moderateComment_NonPendingComment_ShouldThrowConflictException() {
        comment.setStatus(CommentStatus.APPROVED);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> commentService.moderateComment(1L, CommentStatus.APPROVED, null));

        assertEquals("Only pending comments can be moderated", exception.getMessage());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_WhenUserNotFound_ShouldThrowEntityNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commentService.createComment(1L, newCommentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_WhenEventNotFound_ShouldThrowEntityNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commentService.createComment(1L, newCommentDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_WhenCommentNotFound_ShouldThrowEntityNotFoundException() {
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void moderateComment_WhenCommentNotFound_ShouldThrowEntityNotFoundException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> commentService.moderateComment(1L, CommentStatus.APPROVED, null));
        verify(commentRepository, never()).save(any(Comment.class));
    }
}