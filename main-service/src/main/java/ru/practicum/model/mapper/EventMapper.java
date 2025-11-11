package ru.practicum.model.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.model.dto.*;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.Location;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public EventMapper(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public Event toEntity(NewEventDto newEventDto) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .title(newEventDto.getTitle())
                .eventDate(LocalDateTime.parse(newEventDto.getEventDate(), formatter))
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .location(toEntityLocation(newEventDto.getLocation()))
                .state(EventState.PENDING)
                .build();
    }

    public EventFullDto toFullDto(Event event, Integer confirmedRequests, Long views) {
        if (event == null) {
            return null;
        }

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(formatDateTime(event.getCreatedOn()))
                .description(event.getDescription())
                .eventDate(formatDateTime(event.getEventDate()))
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .location(toDtoLocation(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(formatDateTime(event.getPublishedOn()))
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public EventShortDto toShortDto(Event event, Integer confirmedRequests, Long views) {
        if (event == null) {
            return null;
        }

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(formatDateTime(event.getEventDate()))
                .initiator(userMapper.toShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        return events.stream()
                .map(event -> toShortDto(event, 0, 0L))
                .collect(Collectors.toList());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(formatter) : null;
    }

    private Location toEntityLocation(LocationDto locationDto) {
        if (locationDto == null) return null;
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    private LocationDto toDtoLocation(Location location) {
        if (location == null) return null;
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public void updateEventFromUserRequest(UpdateEventUserRequest updateRequest, Event event) {
        if (updateRequest == null) return;

        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(LocalDateTime.parse(updateRequest.getEventDate(), formatter));
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(toEntityLocation(updateRequest.getLocation()));
        }
    }

    public void updateEventFromAdminRequest(UpdateEventAdminRequest updateRequest, Event event) {
        if (updateRequest == null) return;

        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(toEntityLocation(updateRequest.getLocation()));
        }
    }
}