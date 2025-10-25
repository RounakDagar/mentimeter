// src/main/java/com/example/mentimeter/Repository/AsyncQuizAttemptRepo.java
package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.AsyncQuizAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AsyncQuizAttemptRepo extends MongoRepository<AsyncQuizAttempt, String> {
    List<AsyncQuizAttempt> findByQuizId(String quizId);
    // This is crucial to check if a user has already attempted
    Optional<AsyncQuizAttempt> findByQuizIdAndUserId(String quizId, String userId);

    List<AsyncQuizAttempt> findByUserIdOrderByAttemptedAtDesc(String userId);
}