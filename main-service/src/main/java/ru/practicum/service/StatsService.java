package ru.practicum.service;

import ru.practicum.model.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StatsService {
    void saveHit(String app, String uri, String ip, LocalDateTime timestamp, Long eventId);

    Map<Long, Long> getViews(List<String> uris);

    List<ViewStats> getStatsForUris(List<String> strings);
}