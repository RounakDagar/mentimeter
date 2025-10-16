package com.example.mentimeter.Service;

import com.example.mentimeter.DTO.QuestionDTO;
import com.example.mentimeter.DTO.SessionResponse;
import com.example.mentimeter.Model.*;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Repository.SessionRepo;
import com.example.mentimeter.Util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepo sessionRepository;
    private final QuizRepo quizRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SessionResponse createSession(String quizId) {
        // Ensure the quiz exists before creating a session for it
        if (!quizRepository.existsById(quizId)) {
            throw new IllegalStateException("Cannot create session for a non-existent quiz with ID: " + quizId);
        }
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Session newSession = new Session();
        newSession.setQuizId(quizId);
        newSession.setHostUsername(currentUsername);
        newSession.setJoinCode(JoinCodeGenerator.generate());
        newSession.setStatus(SessionStatus.WAITING);
        newSession.setCurrentQuestionIndex(-1);
        newSession.setParticipants(new HashSet<>());
        newSession.setResponse(new HashMap<>());

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

    // Inside your SessionService.java

    public void recordAnswer(String joinCode, String username, int answerIndex) {
        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session not found with code: " + joinCode));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot record answer. Session '" + joinCode + "' is not active.");
        }

        int currentQuestionIndex = session.getCurrentQuestionIndex();

        session.getResponse()
                .computeIfAbsent(username, k -> new HashMap<>())
                .put(currentQuestionIndex, answerIndex);

        sessionRepository.save(session);

        String hostDestination = "/topic/session/" + joinCode + "/host";
        messagingTemplate.convertAndSend(hostDestination, Map.of(
                "eventType", "USER_ANSWERED",
                "name", username
        ));
    }

    public Session findSessionByJoinCode(String joinCode) {
        return  sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));
    }

    public void pauseSession(String joinCode) {
        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));

        session.setStatus(SessionStatus.WAITING);

        sessionRepository.save(session);

        sessionRepository.save(session);

        String destination = "/topic/session/" + joinCode;
        messagingTemplate.convertAndSend(destination, Map.of(
                "eventType", "STATUS_UPDATE",
                "status", SessionStatus.WAITING
        ));
    }

    public void resumeSession(String joinCode){
        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));

        session.setStatus(SessionStatus.ACTIVE);

        sessionRepository.save(session);

        sessionRepository.save(session);

        String destination = "/topic/session/" + joinCode;
        messagingTemplate.convertAndSend(destination, Map.of(
                "eventType", "STATUS_UPDATE",
                "status", SessionStatus.ACTIVE
        ));
    }

    public void endSession(String joinCode){
        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalStateException("Session with join code '" + joinCode + "' not found."));

        session.setStatus(SessionStatus.ENDED);

        sessionRepository.save(session);

        String destination = "/topic/session/" + joinCode;
        messagingTemplate.convertAndSend(destination, Map.of(
                "eventType", "STATUS_UPDATE",
                "status", SessionStatus.ENDED
        ));
    }

    public List<QuestionAnalytics> getAnalysis(String joinCode) {

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();


        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Session not there to analyse"));

        Quiz currentQuiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new RuntimeException("No quiz for this session"));


        boolean isHost = currentUsername.equals(session.getHostUsername());


        List<QuestionAnalytics> questionAnalyticsList = new ArrayList<>();


        for (int i = 0; i < currentQuiz.getQuestionList().size(); i++) {
            Question currentQuestion = currentQuiz.getQuestionList().get(i);
            int currentQuestionIndex = i;


            Map<Integer, Integer> optionFrequency = new HashMap<>();
            Map<Integer, List<String>> usernamesByOption = new HashMap<>();

            for (Map.Entry<String, Map<Integer, Integer>> participantEntry : session.getResponse().entrySet()) {
                String username = participantEntry.getKey();
                Map<Integer, Integer> answers = participantEntry.getValue();

                if (answers.containsKey(currentQuestionIndex)) {
                    int chosenOptionIndex = answers.get(currentQuestionIndex);
                    optionFrequency.merge(chosenOptionIndex, 1, Integer::sum);

                    if (isHost) {
                        usernamesByOption.computeIfAbsent(chosenOptionIndex, k -> new ArrayList<>()).add(username);
                    }
                }
            }


            int userAnswerIndex = -1; // Default for host or if participant didn't answer

            // If the user is a participant, find their specific answer for this question
            if (!isHost && session.getResponse().containsKey(currentUsername)) {
                userAnswerIndex = session.getResponse().get(currentUsername).getOrDefault(currentQuestionIndex, -1);
            }

            QuestionAnalytics questionAnalytics = new QuestionAnalytics(
                    currentQuestion.getText(),
                    currentQuestion.getOptions(),
                    currentQuestion.getCorrectAnswerIndex(),
                    optionFrequency,
                    userAnswerIndex,
                    isHost ? usernamesByOption : null // Only include detailed list for the host
            );

            questionAnalyticsList.add(questionAnalytics);
        }

        return questionAnalyticsList;
    }

}

