package com.example.mentimeter.Service;

import com.example.mentimeter.DTO.SessionResponse;
import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Model.Session;
import com.example.mentimeter.Model.SessionStatus;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Repository.SessionRepo;
import com.example.mentimeter.Util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


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
        newSession.setResults(new HashMap<>());

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

    public void recordAnswer(String joinCode, int answerIndex) {
        Session session = findSessionByJoinCode(joinCode);

        if (session.getStatus() != SessionStatus.ACTIVE) {
            // Optionally, you can throw an exception or just ignore the answer
            return;
        }

        int currentQuestionIndex = session.getCurrentQuestionIndex();

        // Get the results map for the current question, or create it if it's the first vote
        Map<Integer, Integer> questionResults = session.getResults()
                .computeIfAbsent(currentQuestionIndex, k -> new HashMap<>());

        // Increment the vote count for the chosen answer index
        questionResults.merge(answerIndex, 1, Integer::sum);

        sessionRepository.save(session);
    }

    private Session findSessionByJoinCode(String joinCode) {
        return  sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));
    }
}
