package ru.practicum.statsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.statsserver.model.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<EndpointHitEntity, Long> {

    @Query("""
        SELECT h.app, h.uri, COUNT(h.ip) as hits
        FROM EndpointHitEntity h
        WHERE h.timestamp BETWEEN ?1 AND ?2
        AND (?3 IS NULL OR h.uri IN ?3)
        GROUP BY h.app, h.uri
        ORDER BY COUNT(h.ip) DESC
        """)
    List<Object[]> getStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
        SELECT h.app, h.uri, COUNT(DISTINCT h.ip) as hits
        FROM EndpointHitEntity h
        WHERE h.timestamp BETWEEN ?1 AND ?2
        AND (?3 IS NULL OR h.uri IN ?3)
        GROUP BY h.app, h.uri
        ORDER BY COUNT(DISTINCT h.ip) DESC
        """)
    List<Object[]> getUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris);
}