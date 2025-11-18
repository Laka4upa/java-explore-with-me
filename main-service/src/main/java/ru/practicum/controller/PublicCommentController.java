package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.model.dto.CommentDto;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
@Validated
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDto>> getEventComments(
            @PathVariable Long eventId,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Getting comments for event id={} with status: {}, from: {}, size: {}",
                eventId, status, from, size);

        List<CommentDto> comments = commentService.getEventComments(eventId, status, from, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getComment(
            @PathVariable Long eventId,
            @PathVariable Long commentId) {

        log.info("Getting comment id={} for event id={}", commentId, eventId);

        List<CommentDto> comments = commentService.getEventComments(eventId, CommentStatus.APPROVED, 0, 100);
        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ru.practicum.exception.EntityNotFoundException(
                        "Comment with id=" + commentId + " was not found"));

        return ResponseEntity.ok(comment);
    }
}