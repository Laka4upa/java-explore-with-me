package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.model.dto.*;

import java.util.List;

public interface EventService {
    EventFullDto createEvent(Long userId, NewEventDto eventDto);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                      String rangeStart, String rangeEnd, Integer from, Integer size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                        String sort, Integer from, Integer size);

    EventFullDto getPublicEvent(Long id, HttpServletRequest request);
}