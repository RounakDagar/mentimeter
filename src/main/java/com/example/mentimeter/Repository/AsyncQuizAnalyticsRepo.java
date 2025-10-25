// src/main/java/com/example/mentimeter/Repository/AsyncQuizAnalyticsRepo.java
package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.AsyncQuizAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AsyncQuizAnalyticsRepo extends MongoRepository<AsyncQuizAnalytics, String> {
    Optional<AsyncQuizAnalytics> findByQuizId(String quizId);
}