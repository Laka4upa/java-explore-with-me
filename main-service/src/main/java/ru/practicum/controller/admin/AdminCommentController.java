package ru.practicum.controller.admin;

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
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping("/pending")
    public ResponseEntity<List<CommentDto>> getPendingComments(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Getting pending comments for moderation, from: {}, size: {}", from, size);

        List<CommentDto> comments = commentService.getCommentsForModeration(CommentStatus.PENDING, from, size);
        return ResponseEntity.ok(comments);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> moderateComment(
            @PathVariable Long commentId,
            @RequestParam CommentStatus status,
            @RequestParam(required = false) String reason) {

        log.info("Moderating comment id={} to status: {} with reason: {}", commentId, status, reason);

        if (status != CommentStatus.APPROVED && status != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be either APPROVED or REJECTED");
        }

        CommentDto moderatedComment = commentService.moderateComment(commentId, status, reason);
        return ResponseEntity.ok(moderatedComment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        log.info("Deleting comment id={} by admin", commentId);

        commentService.moderateComment(commentId, CommentStatus.DELETED_BY_ADMIN, "Deleted by administrator");
        return ResponseEntity.noContent().build();
    }
}