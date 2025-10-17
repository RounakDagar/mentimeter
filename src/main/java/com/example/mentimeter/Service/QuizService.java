package com.example.mentimeter.Service;

import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Model.QuizAttempt;
import com.example.mentimeter.Repository.QuizAttemptRepo;
import com.example.mentimeter.Repository.QuizRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;



    public Quiz createQuiz(Quiz quiz){
        return quizRepo.save(quiz);

    }

    public Optional<Quiz> getQuizById(String id){
        return quizRepo.findById(id);
    }

    public ResponseEntity<List<QuizAttempt>> getAttemptedQuiz(String username) {
        return ResponseEntity.ok(quizAttemptRepo.findByUserIdOrderByAttemptedAtDesc(username));
    }

//    Quiz generateQuizFromAI_API()

}
