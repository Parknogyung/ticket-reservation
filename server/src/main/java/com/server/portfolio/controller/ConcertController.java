package com.server.portfolio.controller;

import com.server.portfolio.domain.ConcertOption;
import com.server.portfolio.repository.ConcertOptionRepository;
import com.server.portfolio.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
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
            return Map.<String, Object>of(
                    "concertId", option.getId(),
                    "title", option.getConcert().getTitle(),
                    "concertDate", option.getConcertDate().format(DATE_FORMATTER),
                    "price", option.getPrice(),
                    "availableSeats", availableSeats);
        }).collect(Collectors.toList());

        return Map.of("concerts", concerts);
    }
}
