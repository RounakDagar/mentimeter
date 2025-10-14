package com.example.mentimeter.DTO;

public class SessionResponse {
    private String joinCode;

    public SessionResponse(String joinCode) {
        this.joinCode = joinCode;
    }

    // --- Getter ---

    public String getJoinCode() {
        return joinCode;
    }
}
