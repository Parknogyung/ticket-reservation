package com.client.portFolio.client;

import com.ticket.portfolio.*;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class TicketServiceClient {

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    public ConcertListResponse getConcerts() {
        return ticketStub.getConcerts(GetConcertsRequest.newBuilder().build());
    }

    public RegisterConcertResponse registerConcert(String title, int seatCount, String concertDate) {
        RegisterConcertRequest request = RegisterConcertRequest.newBuilder()
                .setTitle(title)
                .setSeatCount(seatCount)
                .setConcertDate(concertDate)
                .build();
        return ticketStub.registerConcert(request);
    }

    public SeatListResponse getAvailableSeats(String token, long concertId) {
        Metadata headers = new Metadata();
        Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(authKey, "Bearer " + token);

        TicketServiceGrpc.TicketServiceBlockingStub stubWithAuth = ticketStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        SeatSearchRequest request = SeatSearchRequest.newBuilder()
                .setToken(token)
                .setConcertId(concertId)
                .build();

        return stubWithAuth.getAvailableSeats(request);
    }

    public ReservationResponse reserveSeat(String token, String userId, long seatId) {
        Metadata headers = new Metadata();
        Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(authKey, "Bearer " + token);

        TicketServiceGrpc.TicketServiceBlockingStub stubWithAuth = ticketStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        ReservationRequest request = ReservationRequest.newBuilder()
                .setToken(token)
                .setUserId(userId)
                .setSeatId(seatId)
                .build();

        return stubWithAuth.reserveSeat(request);
    }
}
