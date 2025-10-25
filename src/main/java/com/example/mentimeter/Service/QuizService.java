package com.example.mentimeter.Service;

import com.example.mentimeter.Model.Question;
import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Model.QuizAttempt;
import com.example.mentimeter.Model.QuizHost;
import com.example.mentimeter.Repository.QuizAttemptRepo;
import com.example.mentimeter.Repository.QuizHostedRepo;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final QuizHostedRepo quizHostedRepo;



    public Quiz createQuiz(Quiz quiz){
        return quizRepo.save(quiz);

    }

    public Optional<Quiz> getQuizById(String id){
        return quizRepo.findById(id);
    }

    public ResponseEntity<List<QuizAttempt>> getAttemptedQuiz(String username) {
        return ResponseEntity.ok(quizAttemptRepo.findByUserIdOrderByAttemptedAtDesc(username));
    }

    public ResponseEntity<List<QuizHost>> getHostedQuiz(String username) {
        return ResponseEntity.ok(quizHostedRepo.findByUserIdOrderByHostedAtDesc(username));
    }

    public List<Quiz> finddQuizBYUsername(String username) {
        return quizRepo.findByUsername(username);
    }

    public void deleteQuiz(String quizId) {
        quizRepo.deleteById(quizId);
    }

    public ResponseEntity<QuizAttempt> getQuizAttempt(String joinCode, String username) {
        QuizAttempt quizAttempt = quizAttemptRepo.findBySessionIdAndUserId(joinCode,username);

        return  ResponseEntity.ok(quizAttempt);
    }

    public ResponseEntity<Quiz> editQuiz(String quizId,Quiz quiz) {
        Quiz newQuiz = quizRepo.findById(quizId).orElse(null);

        if(quiz==null){
            throw new RuntimeException("No quiz with QuizId " + quizId + " found to edit");

        }

        newQuiz.setTitle(quiz.getTitle());
        List<Question> questionList = new ArrayList<>();

        for(Question ques : quiz.getQuestionList()){
            questionList.add(ques);
        }
        newQuiz.setQuestionList(questionList);
        quizRepo.save(newQuiz);

        return ResponseEntity.ok(newQuiz);


    }

    public String enableSharing(String quizId, String username) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getUsername().equals(username)) {
            throw new RuntimeException("User not authorized to share this quiz");
        }

        if (quiz.getShareCode() == null) {
            // Generate a unique 12-char code
            String shareCode = JoinCodeGenerator.generate() + JoinCodeGenerator.generate();
            // TODO: Add a check to ensure this code is unique in quizRepo
            quiz.setShareCode(shareCode);
        }
        quiz.setShared(true);
        quizRepo.save(quiz);
        return quiz.getShareCode();
    }

    public void disableSharing(String quizId, String username) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getUsername().equals(username)) {
            throw new RuntimeException("User not authorized");
        }
        quiz.setShared(false);
        quizRepo.save(quiz);
    }

    public void deleteMyLiveAttempt(String sessionId, String username) {

        QuizAttempt attempt = quizAttemptRepo.findBySessionIdAndUserId(sessionId, username);


        if (attempt == null) {
            throw new RuntimeException("Attempt not found or does not belong to the user.");
        }

        quizAttemptRepo.delete(attempt);
    }
//    Quiz generateQuizFromAI_API()

}
