package ru.practicum.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.category.model.Category;
import ru.practicum.event.enums.EventState;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.user.model.User;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Краткое описание (аннотация)
    @Column(nullable = false, length = 2000)
    private String annotation;

    // Категория
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Количество одобренных заявок (вычисляемое поле, но для простоты показываем)
    @Transient
    private Long confirmedRequests;

    // Дата и время создания
    @Column(nullable = false)
    private LocalDateTime createdOn;

    // Полное описание
    @Column(nullable = false, length = 7000)
    private String description;

    // Дата и время события
    @Column(nullable = false)
    private LocalDateTime eventDate;

    // Инициатор (пользователь)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    // Координаты
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "location_lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "location_lon"))
    })
    private Location location;

    // Платное/бесплатное
    @Column(nullable = false)
    private Boolean paid;

    // Лимит участников (0 = без лимита)
    @Column(nullable = false)
    private Integer participantLimit;

    // Дата публикации
    private LocalDateTime publishedOn;

    // Нужна ли пре-модерация заявок
    @Column(nullable = false)
    private Boolean requestModeration;

    // Состояние события
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state;

    // Заголовок
    @Column(nullable = false, length = 120)
    private String title;

    // Количество просмотров (вычисляемое поле)
    @Transient
    private Long views;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ParticipationRequest> requests = new ArrayList<>();
}