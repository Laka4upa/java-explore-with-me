package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.model.dto.CommentDto;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.service.CommentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicCommentControllerManualTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private PublicCommentController publicCommentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicCommentController).build();
    }

    @Test
    void getEventComments_shouldReturnComments() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.APPROVED)
                .build();

        when(commentService.getEventComments(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/events/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Test Comment"));
    }

    @Test
    void getEventComments_withStatusFilter_shouldReturnFilteredComments() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.APPROVED)
                .build();

        when(commentService.getEventComments(anyLong(), eq(CommentStatus.APPROVED), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/events/1/comments?status=APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void getComment_shouldReturnComment() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.APPROVED)
                .build();

        when(commentService.getCommentByIdAndEventId(anyLong(), anyLong()))
                .thenReturn(commentDto);

        mockMvc.perform(get("/events/1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Test Comment"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}