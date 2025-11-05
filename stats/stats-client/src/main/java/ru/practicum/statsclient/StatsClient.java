package ru.practicum.statsclient;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public void saveHit(EndpointHit endpointHit) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(endpointHit, headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                serverUrl + "/hit",
                HttpMethod.POST,
                requestEntity,
                Object.class
        );

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Failed to save hit. Status: " + response.getStatusCode());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(formatter));
        parameters.put("end", end.format(formatter));
        parameters.put("unique", unique);

        String url = serverUrl + "/stats?start={start}&end={end}&unique={unique}";

        if (uris != null && !uris.isEmpty()) {
            url += "&uris={uris}";
            parameters.put("uris", String.join(",", uris));
        }

        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(
                url,
                ViewStats[].class,
                parameters
        );

        return Arrays.asList(response.getBody());
    }
}