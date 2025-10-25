package com.example.mentimeter.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ParticipantQuestionResultDTO {
    // From Quiz
    private String text;
    private List<String> options;
    private int correctAnswerIndex;

    // From AsyncQuizAttempt
    private int userAnswerIndex;

    // From AsyncQuizAnalytics
    private Map<Integer, Integer> optionCounts; // { "0": 10, "1": 5, ... }
}