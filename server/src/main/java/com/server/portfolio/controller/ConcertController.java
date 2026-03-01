package com.server.portfolio.controller;

import com.server.portfolio.domain.ConcertOption;
import com.server.portfolio.repository.ConcertOptionRepository;
import com.server.portfolio.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    @GetMapping
    public Map<String, Object> getConcerts() {
        log.info("Fetching all concerts via REST");
        List<ConcertOption> options = concertOptionRepository.findAll();

        List<Map<String, Object>> concerts = options.stream().map(option -> {
            long availableSeats = seatRepository.findAvailableSeats(option.getId()).size();
            Map<String, Object> m = new HashMap<>();
            m.put("concertId", option.getId());
            m.put("title", option.getConcert().getTitle());
            m.put("concertDate", option.getConcertDate().format(DATE_FORMATTER));
            m.put("price", option.getPrice());
            m.put("availableSeats", availableSeats);
            m.put("venue", option.getConcert().getVenue() != null ? option.getConcert().getVenue() : "");
            m.put("imageUrl", option.getConcert().getImageUrl() != null ? option.getConcert().getImageUrl() : "");
            return m;
        }).collect(Collectors.toList());

        return Map.of("concerts", concerts);
    }
}
