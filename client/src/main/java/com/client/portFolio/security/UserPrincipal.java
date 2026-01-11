package com.client.portFolio.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Serializable {
    private final Long userId;
    private final String email;
    private final String accessToken;

    @Override
    public String toString() {
        return email;
    }
}
