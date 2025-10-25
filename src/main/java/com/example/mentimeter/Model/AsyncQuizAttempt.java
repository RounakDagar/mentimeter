
package com.example.mentimeter.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class AsyncQuizAttempt {
    @Id
    private String id;

    @Indexed
    private String quizId;
    private String shareCode;

    @Indexed
    private String userId;

    private Map<Integer, Integer> answers;
    private int score;
    private int totalQuestions;
    private LocalDateTime attemptedAt;
}