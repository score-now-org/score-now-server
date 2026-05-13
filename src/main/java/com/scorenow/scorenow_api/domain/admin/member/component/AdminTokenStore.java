package com.scorenow.scorenow_api.domain.admin.member.component;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AdminTokenStore {
    private final Set<String> tokens = new HashSet<>();

    public void add(String token) {
        tokens.add(token);
    }

    public boolean isValid(String token) {
        return tokens.contains(token);
    }
}
