package com.example.mentimeter.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Session {
    private String id;
    private String quizId;
    private String joinCode;
    private String hostUsername;
    private SessionStatus status;
    private int currentQuestionIndex;
    private Set<String> participants;
    private Map<String,Map<Integer,Integer>> response;
    private Map<String,Integer> scores = new ConcurrentHashMap<>() ;
}
