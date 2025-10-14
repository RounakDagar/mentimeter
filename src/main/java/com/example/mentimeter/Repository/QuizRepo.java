package com.example.mentimeter.Repository;

import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepo extends MongoRepository<Quiz, String> {


}
