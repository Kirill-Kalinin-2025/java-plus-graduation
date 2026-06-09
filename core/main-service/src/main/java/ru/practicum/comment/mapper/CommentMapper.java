package ru.practicum.comment.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CommentMapper {

    public static Comment returnComment(NewCommentDto dto, User user, Event event) {
        return Comment.builder()
                .author(user)
                .event(event)
                .text(dto.getText())
                .status(CommentStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
    }

    public static CommentDto returnCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .status(comment.getStatus().name())
                .build();
    }

    public static List<CommentDto> returnCommentDtoList(Iterable<Comment> comments) {
        List<CommentDto> result = new ArrayList<>();
        for (Comment comment : comments) {
            result.add(returnCommentDto(comment));
        }
        return result;
    }
}