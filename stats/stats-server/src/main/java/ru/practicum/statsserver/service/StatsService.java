package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.model.EndpointHitEntity;
import ru.practicum.statsserver.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository statsRepository;

    public void saveHit(EndpointHit endpointHit) {
        EndpointHitEntity entity = EndpointHitEntity.builder()
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp())
                .build();

        EndpointHitEntity saved = statsRepository.save(entity);
        log.info("Saved hit: id={}, app={}, uri={}, ip={}", saved.getId(), saved.getApp(), saved.getUri(), saved.getIp());
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        log.info("Getting stats from repository...");

        List<Object[]> results;
        if (Boolean.TRUE.equals(unique)) {
            results = statsRepository.getUniqueStats(start, end, uris);
        } else {
            results = statsRepository.getStats(start, end, uris);
        }

        List<ViewStats> viewStats = results.stream()
                .map(result -> ViewStats.builder()
                        .app((String) result[0])
                        .uri((String) result[1])
                        .hits((Long) result[2])
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} results", viewStats.size());
        return viewStats;
    }
}