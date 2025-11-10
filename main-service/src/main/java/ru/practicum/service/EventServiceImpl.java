package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.EntityNotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.model.dto.*;
import ru.practicum.model.entity.Category;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.StateAction;
import ru.practicum.model.mapper.EventMapper;
import ru.practicum.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsService statsService;
    private final Map<Long, Set<String>> eventViewIps = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto eventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new EntityNotFoundException("Category with id=" + eventDto.getCategory() + " was not found"));

        LocalDateTime eventDate = LocalDateTime.parse(eventDto.getEventDate(), formatter);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Field: eventDate. Error: must be at least 2 hours from now. Value: " + eventDto.getEventDate());
        }

        Event event = eventMapper.toEntity(eventDto);
        event.setInitiator(user);
        event.setCategory(category);

        Event savedEvent = eventRepository.save(event);
        log.info("Created event with id={} for user id={}", savedEvent.getId(), userId);

        EventFullDto eventFullDto = eventMapper.toFullDto(savedEvent, 0, 0L);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User with id=" + userId + " was not found");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViewsForEvents(events);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        confirmedRequests.getOrDefault(event.getId(), 0),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found for user id=" + userId));

        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId).intValue();
        Long views = getViewsForEvents(List.of(event)).getOrDefault(eventId, 0L);

        return eventMapper.toFullDto(event, confirmedRequests, views);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found for user id=" + userId));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(updateRequest.getEventDate(), formatter);
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Field: eventDate. Error: must be at least 2 hours from now. Value: " + updateRequest.getEventDate());
            }
            event.setEventDate(newEventDate);
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromUserRequest(updateRequest, event);

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId).intValue();
        Long views = getViewsForEvents(List.of(event)).getOrDefault(eventId, 0L);

        return eventMapper.toFullDto(updatedEvent, confirmedRequests, views);
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                             String rangeStart, String rangeEnd, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        Specification<Event> specification = Specification.where(EventSpecifications.withUsers(users));

        if (states != null && !states.isEmpty()) {
            List<EventState> eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
            specification = specification.and(EventSpecifications.withStates(eventStates));
        }

        specification = specification.and(EventSpecifications.withCategories(categories));

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : null;

        specification = specification.and(EventSpecifications.withRangeStart(start));
        specification = specification.and(EventSpecifications.withRangeEnd(end));

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViewsForEvents(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(event,
                        confirmedRequests.getOrDefault(event.getId(), 0),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(updateRequest.getEventDate(), formatter);
            if (newEventDate.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Event date must be in the future");
            }
            event.setEventDate(newEventDate);
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                event.setViews(0L); // ✅ Сбрасываем счетчик просмотров при публикации
                eventViewIps.remove(eventId); // ✅ Очищаем кэш IP для этого события
            } else if (updateRequest.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromAdminRequest(updateRequest, event);

        Event updatedEvent = eventRepository.save(event);
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId).intValue();

        return eventMapper.toFullDto(updatedEvent, confirmedRequests, updatedEvent.getViews());
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(from / size, size);

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : null;

        Specification<Event> specification = EventSpecifications.forPublicSearch(text, categories, paid, start, end, onlyAvailable);
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        for (Event event : events) {
            statsService.saveHit("main-service", request.getRequestURI(), request.getRemoteAddr(),
                    LocalDateTime.now(), event.getId());
        }

        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViewsForEvents(events);

        List<EventShortDto> result = events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        confirmedRequests.getOrDefault(event.getId(), 0),
                        views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        if (sort != null) {
            if (sort.equals("EVENT_DATE")) {
                result.sort(Comparator.comparing(EventShortDto::getEventDate));
            } else if (sort.equals("VIEWS")) {
                result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            }
        }

        return result;
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long id, HttpServletRequest request) {
        log.info("🎯 GET /events/{} called from IP: {}", id, request.getRemoteAddr());

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + id + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new EntityNotFoundException("Event with id=" + id + " was not found");
        }

        increaseViews(event, request.getRemoteAddr());

        statsService.saveHit("main-service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now(), id);

        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(id).intValue();

        log.info("📊 Event id={} has {} views from IP: {}", id, event.getViews(), request.getRemoteAddr());

        return eventMapper.toFullDto(event, confirmedRequests, event.getViews());
    }

    @Transactional
    public void increaseViews(Event event, String ip) {
        Set<String> viewerIps = eventViewIps.computeIfAbsent(event.getId(), k -> new HashSet<>());

        if (!viewerIps.contains(ip)) {
            viewerIps.add(ip);
            Long currentViews = event.getViews() != null ? event.getViews() : 0L;
            event.setViews(currentViews + 1);
            eventRepository.save(event);
            log.info("👀 Increased views for event {} from IP: {}. Total views: {}",
                    event.getId(), ip, event.getViews());
        }
    }

    private Map<Long, Integer> getConfirmedRequests(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Integer> confirmedRequests = new HashMap<>();

        for (Long eventId : eventIds) {
            Long count = requestRepository.countConfirmedRequestsByEventId(eventId);
            confirmedRequests.put(eventId, count != null ? count.intValue() : 0);
        }

        return confirmedRequests;
    }

    private Map<Long, Long> getViewsForEvents(List<Event> events) {
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        Map<Long, Long> views = statsService.getViews(uris);

        return views;
    }
}