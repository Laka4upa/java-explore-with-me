package ru.practicum.service;

import ru.practicum.model.dto.CommentDto;
import ru.practicum.model.dto.NewCommentDto;
import ru.practicum.model.dto.UpdateCommentDto;
import ru.practicum.model.enums.CommentStatus;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    CommentDto moderateComment(Long commentId, CommentStatus status, String rejectionReason);

    List<CommentDto> getEventComments(Long eventId, CommentStatus status, Integer from, Integer size);

    List<CommentDto> getUserComments(Long userId, Integer from, Integer size);

    List<CommentDto> getCommentsForModeration(CommentStatus status, Integer from, Integer size);

    CommentDto getCommentByIdAndEventId(Long commentId, Long eventId);
}