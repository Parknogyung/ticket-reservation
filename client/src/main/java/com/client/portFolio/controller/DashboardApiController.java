package com.client.portFolio.controller;

import com.client.portFolio.client.TicketServiceClient;
import com.ticket.portfolio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final TicketServiceClient ticketServiceClient;

    @GetMapping("/concerts")
    public Map<String, Object> getConcerts() {
        log.info("Fetching concert listings");
        ConcertListResponse response = ticketServiceClient.getConcerts();

        Map<String, Object> result = new HashMap<>();
        result.put("concerts", response.getConcertsList().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("concertId", c.getConcertId());
            map.put("title", c.getTitle());
            map.put("date", c.getConcertDate());
            map.put("availableSeats", c.getAvailableSeats());
            return map;
        }).collect(Collectors.toList()));

        return result;
    }

    @PostMapping("/concerts")
    public Map<String, Object> registerConcert(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        int seatCount = (Integer) request.get("seatCount");
        String date = (String) request.get("date");
        // Handle number/string price input
        long price = Long.parseLong(request.getOrDefault("price", "0").toString());

        log.info("Registering new concert: {} with price {}", title, price);
        RegisterConcertResponse response = ticketServiceClient.registerConcert(title, seatCount, date, price);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getSuccess());
        result.put("message", response.getMessage());
        result.put("concertId", response.getConcertId());
        return result;
    }

    @GetMapping("/seats")
    public Map<String, Object> getSeats(@RequestParam Long concertId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.client.portFolio.security.UserPrincipal) {
            com.client.portFolio.security.UserPrincipal principal = (com.client.portFolio.security.UserPrincipal) auth
                    .getPrincipal();
            String token = principal.getAccessToken();
            log.info("Fetching seats for concertOptionId: {}", concertId);
            SeatListResponse response = ticketServiceClient.getAvailableSeats(token, concertId);

            Map<String, Object> result = new HashMap<>();
            result.put("seats", response.getSeatsList().stream().map(s -> {
                Map<String, Object> seatMap = new HashMap<>();
                seatMap.put("seatId", s.getSeatId());
                seatMap.put("seatNumber", s.getSeatNumber());
                seatMap.put("status", s.getStatus());
                return seatMap;
            }).collect(Collectors.toList()));

            return result;
        }

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", "인증 정보가 유효하지 않습니다.");
        return errorResult;
    }

    @PostMapping("/reserve")
    public Map<String, Object> reserveSeat(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Integer> seatIdsInt = (List<Integer>) request.get("seatIds");
        List<Long> seatIds = seatIdsInt.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.client.portFolio.security.UserPrincipal)) {
            log.error("Invalid auth state for reserveSeat: {}", auth);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "인증 정보가 유효하지 않습니다.");
            return errorResult;
        }
        com.client.portFolio.security.UserPrincipal principal = (com.client.portFolio.security.UserPrincipal) auth
                .getPrincipal();
        String userId = String.valueOf(principal.getUserId());
        String token = principal.getAccessToken();

        log.info("Web reservation request: seatIds={}, userId={}", seatIds, userId);

        ReservationResponse response = ticketServiceClient.reserveSeat(token, userId, seatIds);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getSuccess());
        result.put("message", response.getMessage());
        if (response.getSuccess()) {
            result.put("reservationIds", response.getReservationIdsList());
        }
        return result;
    }

    @GetMapping("/my-reservations")
    public Map<String, Object> getMyReservations() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.client.portFolio.security.UserPrincipal)) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "인증 정보가 유효하지 않습니다.");
            return errorResult;
        }

        com.client.portFolio.security.UserPrincipal principal = (com.client.portFolio.security.UserPrincipal) auth
                .getPrincipal();
        String userId = String.valueOf(principal.getUserId());

        log.info("Fetching my reservations for userId: {}", userId);
        MyReservationListResponse response = ticketServiceClient.getMyReservations(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("reservations", response.getReservationsList().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("reservationId", r.getReservationId());
            map.put("concertTitle", r.getConcertTitle());
            map.put("concertDate", r.getConcertDate());
            map.put("seatNumber", r.getSeatNumber());
            map.put("status", r.getStatus());
            map.put("amount", r.getAmount());
            map.put("paymentId", r.getPaymentId());
            return map;
        }).collect(Collectors.toList()));

        return result;
    }
}
