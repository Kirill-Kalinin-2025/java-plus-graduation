package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class StatsClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    private String getBaseUrl() {
        return discoveryClient.getInstances("stats-server")
                .stream()
                .findFirst()
                .map(instance -> "http://" + instance.getHost() + ":" + instance.getPort())
                .orElse("http://localhost:9090");
    }

    public void hit(EndpointHitDto hitDto) {
        try {
            restTemplate.postForEntity(getBaseUrl() + "/hit", hitDto, Object.class);
        } catch (Exception e) {
            log.error("Не удалось отправить hit: app={}, uri={}", hitDto.app(), hitDto.uri(), e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/stats")
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER));

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            if (unique != null) {
                builder.queryParam("unique", unique);
            }

            ResponseEntity<ViewStatsDto[]> response =
                    restTemplate.getForEntity(builder.build().toUriString(), ViewStatsDto[].class);

            return Arrays.asList(Objects.requireNonNull(response.getBody()));

        } catch (Exception e) {
            log.error("Не удалось получить статистику просмотров", e);
            return Collections.emptyList();
        }
    }
}