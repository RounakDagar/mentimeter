package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepo extends MongoRepository<Quiz, String> {


    List<Quiz> findByUsername(String username);
}
