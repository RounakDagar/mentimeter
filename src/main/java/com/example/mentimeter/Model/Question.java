package com.example.mentimeter.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String text;
    private List<String> options;
    private int correctAnswerIndex;
}
