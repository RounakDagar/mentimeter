package com.example.mentimeter.Service;

import com.example.mentimeter.DTO.QuestionDTO;
import com.example.mentimeter.DTO.SessionResponse;
import com.example.mentimeter.Model.*;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Repository.SessionRepo;
import com.example.mentimeter.Util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepo sessionRepository;
    private final QuizRepo quizRepository;

    public SessionResponse createSession(String quizId) {
        // Ensure the quiz exists before creating a session for it
        if (!quizRepository.existsById(quizId)) {
            throw new IllegalStateException("Cannot create session for a non-existent quiz with ID: " + quizId);
        }

        Session newSession = new Session();
        newSession.setQuizId(quizId);

        newSession.setJoinCode(JoinCodeGenerator.generate());
        newSession.setStatus(SessionStatus.WAITING);
        newSession.setCurrentQuestionIndex(-1);
        newSession.setParticipants(new HashSet<>());
        newSession.setSubmittedAnswer(new ArrayList<>());

        sessionRepository.save(newSession);
        SessionResponse sessionResponse = new SessionResponse(newSession.getJoinCode());
        return sessionResponse;
    }

    public Session addParticipant(String joinCode, String participantName) {
        Session session = findSessionByJoinCode(joinCode);

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new IllegalStateException("Cannot join a session that is not in the WAITING state.");
        }

        session.getParticipants().add(participantName);
        return sessionRepository.save(session);
    }

    public Session startSession(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);
        session.setStatus(SessionStatus.ACTIVE);
        session.setCurrentQuestionIndex(0); // Set to the first question
        return sessionRepository.save(session);
    }

    public Session advanceToNextQuestion(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);
        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Associated quiz not found for session."));

        int nextIndex = session.getCurrentQuestionIndex() + 1;
        if (nextIndex >= quiz.getQuestionList().size()) {
            // If there are no more questions, end the session
            session.setStatus(SessionStatus.ENDED);
        } else {
            session.setCurrentQuestionIndex(nextIndex);
        }

        return sessionRepository.save(session);
    }

    public Optional<QuestionDTO> getCurrentQuestionForSession(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);


        if (session.getStatus() != SessionStatus.ACTIVE || session.getCurrentQuestionIndex() < 0) {
            return Optional.empty();
        }

        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Associated quiz not found for session."));

        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex >= quiz.getQuestionList().size()) {
            return Optional.empty(); // No more questions
        }

        Question currentQuestion = quiz.getQuestionList().get(currentIndex);

        // Map the Question entity to a QuestionDTO
        QuestionDTO questionDTO = new QuestionDTO(
                currentQuestion.getText(),
                currentQuestion.getOptions()
        );

        return Optional.of(questionDTO);
    }

    public void recordAnswer(String joinCode,String username, int answerIndex) {
        Session session = findSessionByJoinCode(joinCode);

        if (session.getStatus() != SessionStatus.ACTIVE) {
            // Optionally, you can throw an exception or just ignore the answer
            System.out.println("Session is not active");
            return;
        }

        int currentQuestionIndex = session.getCurrentQuestionIndex();

        ParticipantAnswer participantAnswer = new ParticipantAnswer(currentQuestionIndex,answerIndex,username);

        session.getSubmittedAnswer().add(participantAnswer);

        sessionRepository.save(session);
    }

    private Session findSessionByJoinCode(String joinCode) {
        return  sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));
    }
}
