package com.example.mentimeter.Model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantAnswer {
    private int questionIndex;
    private int answerIndex; // The option the user chose
    private String participantName; // Identifies who answered

}