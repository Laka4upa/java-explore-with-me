package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.model.dto.ViewStats;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final WebClient webClient;

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void saveHit(String app, String uri, String ip, LocalDateTime timestamp, Long eventId) {
        try {
            String finalUri = (eventId != null) ? "/events/" + eventId : uri;

            // Сохраняем хит для конкретного события
            webClient.post()
                    .uri(statsServerUrl + "/hit")
                    .bodyValue(Map.of(
                            "app", app,
                            "uri", finalUri,
                            "ip", ip,
                            "timestamp", timestamp.format(formatter)
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Saved hit for event {}: app={}, uri={}, ip={}", eventId, app, finalUri, ip);

        } catch (Exception e) {
            log.error("Failed to save hit to stats service: {}", e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getViews(List<String> uris) {
        Map<Long, Long> views = new HashMap<>();

        if (uris.isEmpty()) {
            return views;
        }

        try {
            ViewStats[] response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(statsServerUrl + "/stats")
                            .queryParam("start", "2020-05-05 00:00:00")
                            .queryParam("end", "2035-05-05 00:00:00")
                            .queryParam("uris", String.join(",", uris))
                            .queryParam("unique", false)
                            .build())
                    .retrieve()
                    .bodyToMono(ViewStats[].class)
                    .block();


            if (response != null) {
                log.info("🌐 Received {} stats entries", response.length);
                for (ViewStats stats : response) {
                    Long eventId = extractEventIdFromUri(stats.getUri());
                    log.info("🌐 Stats entry - URI: {}, Event ID: {}, Hits: {}",
                            stats.getUri(), eventId, stats.getHits());
                    if (eventId != -1L) {
                        views.put(eventId, stats.getHits());
                    }
                }
            } else {
                log.warn("No response from stats service");
            }

        } catch (Exception e) {
            log.error("Failed to get views from stats service: {}", e.getMessage());
        }

        log.info("Final views map: {}", views);
        return views;
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            if (uri.startsWith("/events/")) {
                String idStr = uri.substring("/events/".length());
                return Long.parseLong(idStr);
            }
            return -1L;
        } catch (Exception e) {
            log.warn("Failed to extract event ID from URI: {}", uri);
            return -1L;
        }
    }

    public List<ViewStats> getStatsForUris(List<String> uris) {
        try {
            String encodedStart = URLEncoder.encode("2020-05-05 00:00:00", StandardCharsets.UTF_8);
            String encodedEnd = URLEncoder.encode("2035-05-05 00:00:00", StandardCharsets.UTF_8);
            String encodedUris = URLEncoder.encode(String.join(",", uris), StandardCharsets.UTF_8);

            String url = String.format("%s/stats?start=%s&end=%s&uris=%s&unique=false",
                    statsServerUrl, encodedStart, encodedEnd, encodedUris);

            log.info("Requesting stats from: {}", url);

            ViewStats[] response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ViewStats[].class)
                    .block();

            return response != null ? Arrays.asList(response) : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to get stats from stats service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}