package com.example.mentimeter.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {

    @Id
    private String id;
    private String quizId;
    private String quizTitle;
    private String sessionId;
    @Indexed
    private String userId;
    private int score;
    private int totalQuestions;
    private LocalDateTime attemptedAt;
    private List<ParticipantAnswer> answers;
}