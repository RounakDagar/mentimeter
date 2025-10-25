// src/main/java/com/example/mentimeter/DTO/ParticipantAttemptDTO.java
package com.example.mentimeter.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ParticipantAttemptDTO {
    private String username;
    private int score;
    private int totalQuestions;
    private Map<Integer, Integer> answers;
}