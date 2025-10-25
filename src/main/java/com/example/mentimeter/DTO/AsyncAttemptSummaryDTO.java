// src/main/java/com/example/mentimeter/DTO/AsyncAttemptSummaryDTO.java
package com.example.mentimeter.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AsyncAttemptSummaryDTO {
    private String quizId;
    private String quizTitle;
    private int score;
    private int totalQuestions;
    private LocalDateTime attemptedAt;
}