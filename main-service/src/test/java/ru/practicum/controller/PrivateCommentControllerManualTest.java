package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.model.dto.CommentDto;
import ru.practicum.model.dto.NewCommentDto;
import ru.practicum.model.dto.UpdateCommentDto;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PrivateCommentControllerManualTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private PrivateCommentController privateCommentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(privateCommentController).build();
    }

    @Test
    void createComment_shouldReturnCreated() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test Comment")
                .eventId(1L)
                .build();

        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.PENDING)
                .build();

        when(commentService.createComment(anyLong(), any(NewCommentDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(post("/users/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Test Comment"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(commentService).createComment(1L, newCommentDto);
    }

    @Test
    void updateComment_shouldReturnOk() throws Exception {
        UpdateCommentDto updateDto = UpdateCommentDto.builder()
                .text("Updated Comment")
                .build();

        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Updated Comment")
                .status(CommentStatus.PENDING)
                .build();

        when(commentService.updateComment(anyLong(), anyLong(), any(UpdateCommentDto.class)))
                .thenReturn(commentDto);

        mockMvc.perform(patch("/users/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated Comment"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(commentService).updateComment(1L, 1L, updateDto);
    }

    @Test
    void deleteComment_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/1/comments/1"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(1L, 1L);
    }

    @Test
    void getUserComments_shouldReturnComments() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.APPROVED)
                .build();

        when(commentService.getUserComments(anyLong(), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/users/1/comments?from=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Test Comment"));

        verify(commentService).getUserComments(1L, 0, 10);
    }

    @Test
    void createComment_withEmptyText_shouldReturnBadRequest() throws Exception {
        NewCommentDto invalidDto = NewCommentDto.builder()
                .text("") // Пустой текст
                .eventId(1L)
                .build();

        mockMvc.perform(post("/users/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_withNullEventId_shouldReturnBadRequest() throws Exception {
        NewCommentDto invalidDto = NewCommentDto.builder()
                .text("Valid text")
                .eventId(null) // Null eventId
                .build();

        mockMvc.perform(post("/users/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateComment_withEmptyText_shouldReturnBadRequest() throws Exception {
        UpdateCommentDto invalidDto = UpdateCommentDto.builder()
                .text("")
                .build();

        mockMvc.perform(patch("/users/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}