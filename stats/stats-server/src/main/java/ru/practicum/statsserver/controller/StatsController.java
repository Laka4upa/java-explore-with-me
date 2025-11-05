package ru.practicum.statsserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class StatsController {
    private final StatsService statsService;

    @Autowired
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHit endpointHit) {
        statsService.saveHit(endpointHit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        return statsService.getStats(start, end, uris, unique);
    }
}