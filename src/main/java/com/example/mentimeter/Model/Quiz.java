package com.example.mentimeter.Model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Quiz {
    private String id;
    private String title;
    private List<Question> questionList;

}
