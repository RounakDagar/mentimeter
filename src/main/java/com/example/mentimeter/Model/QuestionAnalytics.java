package com.example.mentimeter.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionAnalytics {
    String text;
    List<String> options;
    int correctAnswerIndex;
    Map<Integer,Integer> optionCounts;
    int userAnswerIndex;
    Map<Integer,List<String>> usernames;
}
