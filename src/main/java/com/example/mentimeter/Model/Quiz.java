package com.example.mentimeter.Model;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Quiz {
    private String id;
    private String title;
    private String username;
    private List<Question> questionList;

    @Indexed(unique = true, sparse = true) // sparse = true allows null values (for non-shared quizzes)
    private String shareCode;
    private boolean isShared = false;

}
