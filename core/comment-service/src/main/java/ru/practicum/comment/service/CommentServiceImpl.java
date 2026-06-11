package ru.practicum.comment.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentAdminRequest;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.client.EventClient;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.client.UserClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentMapper commentMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        if (!eventClient.existsById(eventId)) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено.");
        }
        if (!eventClient.isPublished(eventId)) {
            throw new BadRequestException("Нельзя комментировать неопубликованное событие.");
        }

        Comment comment = commentMapper.returnComment(dto, userId, eventId);
        comment.setCreated(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        return commentMapper.returnCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Редактировать можно только свой комментарий.");
        }

        comment.setText(dto.getText());
        comment.setStatus(CommentStatus.PENDING);
        return commentMapper.returnCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deletePrivateComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден."));

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Удалить можно только свой комментарий.");
        }

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentShortDto> getCommentsByUserId(String rangeStart, String rangeEnd, Long userId,
                                                     Integer from, Integer size) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }

        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        validateDateRange(start, end);

        PageRequest pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByAuthorId(userId, pageable);

        return comments.stream()
                .map(this::toCommentShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentShortDto> getCommentsByEventId(String rangeStart, String rangeEnd, Long eventId,
                                                      Integer from, Integer size) {
        if (!eventClient.existsById(eventId)) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено.");
        }

        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        validateDateRange(start, end);

        PageRequest pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);

        return comments.stream()
                .filter(comment -> comment.getStatus() == CommentStatus.PUBLISHED)
                .map(this::toCommentShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsAdmin(String rangeStart, String rangeEnd, List<Long> users,
                                             String text, Integer from, Integer size) {
        PageRequest pageable = PageRequest.of(from / size, size);
        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        validateDateRange(start, end);

        Page<Comment> page = commentRepository.searchAdmin(users, text, start, end, pageable);

        return page.getContent().stream()
                .map(commentMapper::returnCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto updateCommentAdmin(Long commentId, UpdateCommentAdminRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        CommentStatus newStatus = CommentStatus.valueOf(request.getStatus().toUpperCase());
        comment.setStatus(newStatus);
        return commentMapper.returnCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteAdminComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден");
        }
        commentRepository.deleteById(commentId);
    }

    private CommentShortDto toCommentShortDto(Comment comment) {
        return CommentShortDto.builder()
                .userName("User #" + comment.getAuthorId())
                .eventTitle("Event #" + comment.getEventId())
                .text(comment.getText())
                .created(comment.getCreated())
                .build();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDateTime.parse(dateStr, FORMATTER);
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Дата начала диапазона не может быть позже даты конца.");
        }
    }
}