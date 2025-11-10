package ru.practicum.statsserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHit endpointHit) {
        statsService.saveHit(endpointHit);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("Stats request received: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        // ✅ ЯВНАЯ ПРОВЕРКА ДАТ
        if (start.isAfter(end)) {
            log.warn("Invalid date range: start {} is after end {}", start, end);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Incorrectly made request.",
                            "message", "Start date must be before end date",
                            "status", "BAD_REQUEST",
                            "reason", "Validation failed"
                    ));
        }

        try {
            List<ViewStats> stats = statsService.getStats(start, end, uris, unique);
            log.info("Stats returned: {} items", stats.size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error in stats service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }
}