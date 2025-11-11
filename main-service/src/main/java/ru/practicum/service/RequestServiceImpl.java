package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.EntityNotFoundException;
import ru.practicum.model.dto.EventRequestStatusUpdateRequest;
import ru.practicum.model.dto.EventRequestStatusUpdateResult;
import ru.practicum.model.dto.ParticipationRequestDto;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.ParticipationRequest;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.RequestStatus;
import ru.practicum.model.mapper.RequestMapper;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found"));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists for user id=" + userId + " and event id=" + eventId);
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add request to own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        return requestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User with id=" + userId + " was not found");
        }

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Request with id=" + requestId + " was not found"));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found for user id=" + userId));

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);
        return requestMapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found for user id=" + userId));

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Request does not belong to this event");
            }
        }

        Long confirmedCount = requestRepository.countConfirmedRequestsByEventId(eventId);

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
            if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }

            if (event.getParticipantLimit() > 0 && (confirmedCount + requests.size()) > event.getParticipantLimit()) {
                throw new ConflictException("The participant limit will be exceeded");
            }
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(new ArrayList<>());
        result.setRejectedRequests(new ArrayList<>());

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.CONFIRMED);
                requestRepository.save(request);
                result.getConfirmedRequests().add(requestMapper.toDto(request));
            }
        } else if (updateRequest.getStatus() == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                result.getRejectedRequests().add(requestMapper.toDto(request));
            }
        }

        return result;
    }
}