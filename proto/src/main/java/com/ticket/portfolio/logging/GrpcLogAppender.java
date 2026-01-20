package com.ticket.portfolio.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.protobuf.Empty;
import com.ticket.portfolio.LogMessage;
import com.ticket.portfolio.LogStreamServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class GrpcLogAppender extends AppenderBase<ILoggingEvent> {

    private String host = "monitor-service"; // Default inside Docker
    private int port = 9095;
    private String serviceName = "unknown";

    private ManagedChannel channel;
    private LogStreamServiceGrpc.LogStreamServiceStub stub;
    private StreamObserver<LogMessage> requestObserver;

    // Getters and Setters for Logback configuration
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void start() {
        if (host == null || host.isEmpty()) {
            addError("Host is missing for GrpcLogAppender");
            return;
        }

        try {
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
            stub = LogStreamServiceGrpc.newStub(channel);

            // Initial connection
            connect();

            super.start();
        } catch (Exception e) {
            addError("Failed to start GrpcLogAppender", e);
        }
    }

    private void connect() {
        requestObserver = stub.streamLogs(new StreamObserver<com.google.protobuf.Empty>() {
            @Override
            public void onNext(com.google.protobuf.Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                // Reconnection logic could go here, for now just print
                System.err.println("GrpcLogAppender Error: " + t.getMessage());
                requestObserver = null;
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (requestObserver == null) {
            // Try to reconnect? Simple fail-fast for now or silent drop
            try {
                connect();
            } catch (Exception e) {
                return;
            }
        }

        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(eventObject.getTimeStamp()));

            LogMessage message = LogMessage.newBuilder()
                    .setTimestamp(timestamp)
                    .setLevel(eventObject.getLevel().toString())
                    .setLoggerName(eventObject.getLoggerName())
                    .setMessage(eventObject.getFormattedMessage())
                    .setServiceName(serviceName)
                    .build();

            requestObserver.onNext(message);
        } catch (Exception e) {
            // Failsafe
            System.err.println("Failed to send log via gRPC: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (requestObserver != null) {
            requestObserver.onCompleted();
        }
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        super.stop();
    }
}
