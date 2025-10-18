package com.example.mentimeter.Controller;

import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Model.QuizAttempt;
import com.example.mentimeter.Model.QuizHost;
import com.example.mentimeter.Service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService ;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public ResponseEntity<Quiz> createQuiz(@RequestBody Quiz quiz) {
        Quiz createdQuiz = quizService.createQuiz(quiz);
        return new ResponseEntity<>(createdQuiz, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable String id) {
        Optional<Quiz> quizOptional = quizService.getQuizById(id);


        return quizOptional.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/{username}/AttemptedQuiz")
    public ResponseEntity<List<QuizAttempt>> getAttemptedQuiz(@PathVariable String username){
        return quizService.getAttemptedQuiz(username);
    }

    @GetMapping("/{username}/HostedQuiz")
    public ResponseEntity<List<QuizHost>> getHostedQuiz(@PathVariable String username){
        return quizService.getHostedQuiz(username);
    }

    /**
     * [Phase 4] Endpoint to generate a quiz from an AI prompt.
     * (Placeholder for future implementation)
     */
    @PostMapping("/generate")
    public ResponseEntity<Quiz> generateQuiz() {
//         TODO: Implement logic to call an AI service and create a quiz
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
