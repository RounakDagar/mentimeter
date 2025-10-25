// src/main/java/com/yourcompany/mentimeter/dto/AiQuizFromTextRequest.java
package com.example.mentimeter.DTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank; // For validation
import jakarta.validation.constraints.Size;    // For validation

@Data
public class AiQuizFromTextRequest {

    @NotBlank(message = "Content cannot be empty.")
    @Size(max = 5000, message = "Content is too long. Please limit to around 800 words (~5000 characters).") // Character limit as a proxy for words
    private String content;

    private String difficulty;
    private int numQuestions = 5;
}