// src/main/java/com/example/mentimeter/Model/AsyncQuizAnalytics.java
package com.example.mentimeter.Model;

import com.example.mentimeter.DTO.ParticipantAttemptDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class AsyncQuizAnalytics {
    @Id
    private String id;
    @Indexed(unique = true)
    private String quizId;

    private int totalAttempts;
    private double averageScore;

    // Map<QuestionIndex, Map<OptionIndex, Count>>
    private Map<Integer, Map<Integer, Integer>> questionOptionCounts;
    private LocalDateTime lastUpdatedAt;

    @Transient
    private List<ParticipantAttemptDTO> attempts;

    public AsyncQuizAnalytics(String id, String quizId, int totalAttempts, double averageScore, Map<Integer, Map<Integer, Integer>> questionOptionCounts, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.quizId = quizId;
        this.totalAttempts = totalAttempts;
        this.averageScore = averageScore;
        this.questionOptionCounts = questionOptionCounts;
        this.lastUpdatedAt = lastUpdatedAt;
    }
}