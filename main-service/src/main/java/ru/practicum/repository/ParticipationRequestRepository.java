package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.entity.ParticipationRequest;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr WHERE pr.event.id = :eventId AND pr.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("""
            SELECT pr.event.id, COUNT(pr)
            FROM ParticipationRequest pr
            WHERE pr.event.id IN :eventIds AND pr.status = 'CONFIRMED'
            GROUP BY pr.event.id"
            """)
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);

    Boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);
}