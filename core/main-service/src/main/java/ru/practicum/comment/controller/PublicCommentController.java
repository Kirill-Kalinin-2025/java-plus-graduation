package ru.practicum.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.service.CommentService;


import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/{eventId}")
    @ResponseStatus(value = HttpStatus.OK)
    public List<CommentShortDto> getCommentsByEventId(@PathVariable Long eventId,
                                                      @RequestParam(required = false, name = "rangeStart") String rangeStart,
                                                      @RequestParam(required = false, name = "rangeEnd") String rangeEnd,
                                                      @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                                      @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {

        return commentService.getCommentsByEventId(rangeStart, rangeEnd, eventId, from, size);
    }
}