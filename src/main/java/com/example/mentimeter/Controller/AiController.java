package com.example.mentimeter.Controller;

import com.example.mentimeter.Model.Quiz; // Import Quiz
import com.example.mentimeter.DTO.AiQuizFromTextRequest; // Import new DTO
import com.example.mentimeter.Service.GeminiAiService;
import jakarta.validation.Valid; // For validating the DTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult; // To check validation errors
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.stream.Collectors; // For formatting validation errors

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private GeminiAiService geminiAiService;

    // --- NEW ENDPOINT ---
    @PostMapping("/generate-quiz-from-text")
    public ResponseEntity<?> generateQuizFromText(@Valid @RequestBody AiQuizFromTextRequest textRequest, BindingResult bindingResult) {

        // Handle validation errors defined in the DTO
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body("Validation failed: " + errors);
        }

        try {
            Quiz generatedQuiz = geminiAiService.generateQuizFromText(textRequest);
            // Return the full Quiz object (Spring converts to JSON)
            return ResponseEntity.ok(generatedQuiz);
        } catch (IOException e) {
            System.err.println("AI generation from text failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating quiz from text: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.internalServerError().body("An unexpected error occurred during AI generation.");
        }
    }

    // Keep your original generateQuizQuestions endpoint if needed
    // @PostMapping("/generate-questions") ...
}