package com.example.mentimeter.DTO;

public class CreateSessionRequest {

    private String quizId;

    // A no-argument constructor is needed for JSON deserialization
    public CreateSessionRequest() {
    }

    // --- Getters and Setters ---

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }
}
