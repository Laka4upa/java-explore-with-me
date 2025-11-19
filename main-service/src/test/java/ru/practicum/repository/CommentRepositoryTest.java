package ru.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.model.entity.*;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void existsByEventIdAndAuthorId_shouldReturnTrue() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);
        createComment(user, event, CommentStatus.APPROVED);

        boolean exists = commentRepository.existsByEventIdAndAuthorId(event.getId(), user.getId());

        assertTrue(exists);
    }

    @Test
    void existsByEventIdAndAuthorId_shouldReturnFalse() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);

        boolean exists = commentRepository.existsByEventIdAndAuthorId(event.getId(), user.getId());

        assertFalse(exists);
    }

    @Test
    void findByEventIdAndStatus_shouldReturnComments() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);
        Comment comment = createComment(user, event, CommentStatus.APPROVED);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(
                event.getId(), CommentStatus.APPROVED, PageRequest.of(0, 10));

        assertEquals(1, comments.size());
        assertEquals(comment.getText(), comments.get(0).getText());
    }

    @Test
    void findByAuthorId_shouldReturnUserComments() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);
        Comment comment = createComment(user, event, CommentStatus.PENDING);

        List<Comment> comments = commentRepository.findByAuthorId(user.getId(), PageRequest.of(0, 10));

        assertEquals(1, comments.size());
        assertEquals(comment.getId(), comments.get(0).getId());
    }

    @Test
    void findByStatus_shouldReturnPendingComments() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);
        Comment comment = createComment(user, event, CommentStatus.PENDING);

        List<Comment> comments = commentRepository.findByStatus(CommentStatus.PENDING, PageRequest.of(0, 10));

        assertEquals(1, comments.size());
        assertEquals(comment.getId(), comments.get(0).getId());
    }

    @Test
    void findByEventIdAndStatusIn_shouldReturnMultipleStatuses() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);
        Comment approvedComment = createComment(user, event, CommentStatus.APPROVED);
        Comment pendingComment = createComment(user, event, CommentStatus.PENDING);

        List<Comment> comments = commentRepository.findByEventIdAndStatusIn(
                event.getId(),
                List.of(CommentStatus.APPROVED, CommentStatus.PENDING),
                PageRequest.of(0, 10)
        );

        assertEquals(2, comments.size());
    }

    @Test
    void findByEventIdAndStatus_withDifferentStatuses_shouldFilterCorrectly() {
        User user = createUser();
        Category category = createCategory();
        Location location = createLocation();
        Event event = createEvent(user, category, location);

        Comment approvedComment = createComment(user, event, CommentStatus.APPROVED);
        Comment rejectedComment = createComment(user, event, CommentStatus.REJECTED);
        Comment pendingComment = createComment(user, event, CommentStatus.PENDING);

        List<Comment> approvedComments = commentRepository.findByEventIdAndStatus(
                event.getId(), CommentStatus.APPROVED, PageRequest.of(0, 10));

        assertEquals(1, approvedComments.size());
        assertEquals(approvedComment.getId(), approvedComments.get(0).getId());
    }

    private User createUser() {
        User user = User.builder()
                .name("Test User")
                .email("test@mail.com")
                .build();
        em.persist(user);
        return user;
    }

    private Category createCategory() {
        Category category = Category.builder()
                .name("Test Category")
                .build();
        em.persist(category);
        return category;
    }

    private Location createLocation() {
        return Location.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();
    }

    private Event createEvent(User user, Category category, Location location) {
        Event event = Event.builder()
                .annotation("Test Annotation for Event")
                .description("Test Description for Event")
                .title("Test Event Title")
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .initiator(user)
                .category(category)
                .location(location)
                .build();
        em.persist(event);
        return event;
    }

    private Comment createComment(User user, Event event, CommentStatus status) {
        Comment comment = Comment.builder()
                .text("Test Comment Text for event " + event.getId())
                .status(status)
                .created(LocalDateTime.now())
                .author(user)
                .event(event)
                .editCount(0)
                .build();
        em.persist(comment);
        return comment;
    }
}