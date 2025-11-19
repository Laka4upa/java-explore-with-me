package ru.practicum.controller.admin;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCommentControllerManualTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private AdminCommentController adminCommentController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCommentController).build();
    }

    @Test
    void getCommentsForModeration_shouldReturnComments() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.PENDING)
                .build();

        when(commentService.getCommentsForModeration(any(), anyInt(), anyInt()))
                .thenReturn(List.of(commentDto));

        mockMvc.perform(get("/admin/comments/pending?status=PENDING&from=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approveComment_shouldReturnOk() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.APPROVED)
                .build();

        when(commentService.moderateComment(anyLong(), eq(CommentStatus.APPROVED), isNull()))
                .thenReturn(commentDto);

        mockMvc.perform(patch("/admin/comments/1")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectComment_shouldReturnOk() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.REJECTED)
                .build();

        when(commentService.moderateComment(anyLong(), eq(CommentStatus.REJECTED), anyString()))
                .thenReturn(commentDto);

        mockMvc.perform(patch("/admin/comments/1")
                        .param("status", "REJECTED")
                        .param("reason", "Spam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deleteCommentByAdmin_shouldReturnNoContent() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .status(CommentStatus.DELETED_BY_ADMIN)
                .build();

        when(commentService.moderateComment(anyLong(), eq(CommentStatus.DELETED_BY_ADMIN), anyString()))
                .thenReturn(commentDto);

        mockMvc.perform(delete("/admin/comments/1"))
                .andExpect(status().isNoContent());
    }
}