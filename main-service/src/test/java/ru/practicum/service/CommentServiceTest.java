package ru.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.exception.ConflictException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

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
    private Event event;
    private Comment comment;
    private NewCommentDto newCommentDto;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("User").email("user@mail.com").build();
        event = Event.builder().id(1L).state(EventState.PUBLISHED).build();
        comment = Comment.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.PENDING)
                .created(LocalDateTime.now())
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
    void createComment_shouldCreateComment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.existsByEventIdAndAuthorId(1L, 1L)).thenReturn(false);
        when(commentMapper.toEntity(newCommentDto)).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.createComment(1L, newCommentDto);

        assertNotNull(result);
        assertEquals("New Comment", result.getText());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_whenEventNotPublished_shouldThrowException() {
        event.setState(EventState.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> commentService.createComment(1L, newCommentDto));
    }

    @Test
    void createComment_whenUserAlreadyCommented_shouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.existsByEventIdAndAuthorId(1L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> commentService.createComment(1L, newCommentDto));
    }

    @Test
    void updateComment_shouldUpdateComment() {
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.updateComment(1L, 1L, updateDto);

        assertNotNull(result);
        verify(commentRepository).save(comment);
        assertEquals(CommentStatus.PENDING, comment.getStatus());
        assertEquals(1, comment.getEditCount());
    }

    @Test
    void updateComment_whenEditLimitExceeded_shouldThrowException() {
        comment.setEditCount(5);
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(ValidationException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));
    }

    @Test
    void updateComment_whenTimeLimitExceeded_shouldThrowException() {
        comment.setCreated(LocalDateTime.now().minusHours(25));
        UpdateCommentDto updateDto = UpdateCommentDto.builder().text("Updated Text").build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(ValidationException.class,
                () -> commentService.updateComment(1L, 1L, updateDto));
    }

    @Test
    void moderateComment_shouldApproveComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.moderateComment(1L, CommentStatus.APPROVED, null);

        assertNotNull(result);
        assertEquals(CommentStatus.APPROVED, comment.getStatus());
    }

    @Test
    void deleteComment_shouldMarkAsDeleted() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.deleteComment(1L, 1L);

        assertEquals(CommentStatus.DELETED_BY_USER, comment.getStatus());
        verify(commentRepository).save(comment);
    }
}