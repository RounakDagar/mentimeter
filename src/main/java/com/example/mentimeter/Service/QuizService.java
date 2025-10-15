package com.example.mentimeter.Service;

import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Repository.QuizRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepo quizRepo;



    public Quiz createQuiz(Quiz quiz){
        return quizRepo.save(quiz);

    }

    public Optional<Quiz> getQuizById(String id){
        return quizRepo.findById(id);
    }

//    Quiz generateQuizFromAI_API()

}
