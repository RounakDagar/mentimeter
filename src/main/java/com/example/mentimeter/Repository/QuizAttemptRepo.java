package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.QuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuizAttemptRepo extends MongoRepository<QuizAttempt, String> {

    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(String userId);

    QuizAttempt findBySessionIdAndUserId(String sessionId, String userId);
}