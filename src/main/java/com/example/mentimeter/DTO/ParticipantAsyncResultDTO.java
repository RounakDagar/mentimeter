package com.example.mentimeter.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ParticipantAsyncResultDTO {
    private String quizTitle;
    private int score;
    private int totalQuestions;
    private List<ParticipantQuestionResultDTO> questions;


}