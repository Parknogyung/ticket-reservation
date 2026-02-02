package com.client.portfolio.security;

import com.ticket.portfolio.LoginRequest;
import com.ticket.portfolio.LoginResponse;
import com.ticket.portfolio.TicketServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import java.util.Collections;

@Slf4j
@Component
public class GrpcAuthenticationProvider implements AuthenticationProvider {

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("Attempting gRPC authentication for user: {}", email);

        LoginRequest request = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        try {
            LoginResponse response = ticketStub.login(request);

            if (response.getSuccess()) {
                log.info("gRPC authentication successful for user: {}", email);
                UserPrincipal principal = new UserPrincipal(response.getUserId(), email, response.getAccessToken());
                return new UsernamePasswordAuthenticationToken(
                        principal,
                        response.getAccessToken(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            } else {
                log.warn("gRPC authentication failed for user: {}. Reason: {}", email, response.getMessage());
                throw new BadCredentialsException(response.getMessage());
            }
        } catch (Exception e) {
            log.error("gRPC authentication error for user: {}", email, e);
            throw new BadCredentialsException("인증 서버 통신 실패: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
