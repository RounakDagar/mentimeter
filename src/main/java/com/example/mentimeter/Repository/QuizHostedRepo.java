package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.QuizHost;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizHostedRepo extends MongoRepository<QuizHost,String> {

    List<QuizHost> findByUserIdOrderByHostedAtDesc(String username);

    QuizHost findByJoinCode(String joinCode);
}
