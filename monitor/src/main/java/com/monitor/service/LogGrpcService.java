package com.monitor.service;

import com.google.protobuf.Empty;
import com.ticket.portfolio.LogMessage;
import com.ticket.portfolio.LogStreamServiceGrpc;
import com.monitor.controller.LogStreamController;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class LogGrpcService extends LogStreamServiceGrpc.LogStreamServiceImplBase {

    private final LogStreamController logStreamController;

    @Override
    public StreamObserver<LogMessage> streamLogs(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<LogMessage>() {
            @Override
            public void onNext(LogMessage value) {
                logStreamController.broadcastLog(value);
            }

            @Override
            public void onError(Throwable t) {
                // Log error?
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }
}
