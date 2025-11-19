package ru.practicum.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.model.enums.CommentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private CommentStatus status;
    private String created;
    private String updated;
    private Integer editCount;
    private String rejectionReason;
    private UserShortDto author;
    private Long eventId;
}