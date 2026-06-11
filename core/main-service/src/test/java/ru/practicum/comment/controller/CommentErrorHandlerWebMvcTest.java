package ru.practicum.comment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.practicum.comment.service.CommentService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ErrorHandler;
import ru.practicum.exception.NotFoundException;

import java.time.format.DateTimeParseException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentErrorHandlerWebMvcTest {

    private MockMvc mockMvc;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new PrivateCommentController(commentService),
                        new PublicCommentController(commentService),
                        new AdminCommentController(commentService)
                )
                .setControllerAdvice(new ErrorHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnConflictWhenDeletingForeignComment() throws Exception {
        doThrow(new ConflictException("Удалить можно только свой комментарий."))
                .when(commentService)
                .deletePrivateComment(2L, 10L);

        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", 2L, 10L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("Integrity constraint has been violated."))
                .andExpect(jsonPath("$.message").value("Удалить можно только свой комментарий."));
    }

    @Test
    void shouldReturnNotFoundWhenAddingCommentToMissingEvent() throws Exception {
        doThrow(new NotFoundException("Событие с id 999999 не найдено."))
                .when(commentService)
                .addComment(eq(1L), eq(999999L), any());

        mockMvc.perform(post("/users/{userId}/comments/{eventId}", 1L, 999999L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Корректный комментарий для теста\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Событие с id 999999 не найдено."));
    }

    @Test
    void shouldReturnBadRequestWhenCommentBodyIsBlank() throws Exception {
        mockMvc.perform(post("/users/{userId}/comments/{eventId}", 1L, 2L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestWhenCommentDateRangeCannotBeParsed() throws Exception {
        doThrow(new DateTimeParseException("Text 'not-a-date' could not be parsed", "not-a-date", 0))
                .when(commentService)
                .getCommentsByUserId("not-a-date", null, 1L, 0, 10);

        mockMvc.perform(get("/users/{userId}/comments", 1L)
                        .param("rangeStart", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Text 'not-a-date' could not be parsed"));
    }

    @Test
    void shouldReturnBadRequestWhenAdminUsesUnknownCommentStatus() throws Exception {
        doThrow(new IllegalArgumentException("No enum constant ru.practicum.comment.model.CommentStatus.WRONG"))
                .when(commentService)
                .updateCommentAdmin(eq(5L), any());

        mockMvc.perform(patch("/admin/comments/{commentId}", 5L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"WRONG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message")
                        .value("No enum constant ru.practicum.comment.model.CommentStatus.WRONG"));
    }
}