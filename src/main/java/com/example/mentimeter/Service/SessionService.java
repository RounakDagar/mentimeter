package com.example.mentimeter.Service;

import com.example.mentimeter.DTO.AnalyticsResponseDTO;
import com.example.mentimeter.DTO.LeaderboardEntryDTO;
import com.example.mentimeter.DTO.QuestionDTO;
import com.example.mentimeter.DTO.SessionResponse;
import com.example.mentimeter.Model.*;
import com.example.mentimeter.Repository.QuizAttemptRepo;
import com.example.mentimeter.Repository.QuizHostedRepo;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Repository.SessionRepo;
import com.example.mentimeter.Util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepo sessionRepository;
    private final QuizRepo quizRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QuizAttemptRepo quizAttemptRepo;
    private final QuizHostedRepo quizHostedRepo;

    public SessionResponse createSession(String quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new IllegalStateException("Cannot create session for a non-existent quiz with ID: " + quizId);
        }
        // DEFENSIVE FIX: Check if user is authenticated before creating a session
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("User must be authenticated to create a session.");
        }
        String currentUsername = authentication.getName();

        Session newSession = new Session();
        newSession.setQuizId(quizId);
        newSession.setHostUsername(currentUsername);
        newSession.setJoinCode(JoinCodeGenerator.generate());
        newSession.setStatus(SessionStatus.WAITING);
        newSession.setCurrentQuestionIndex(-1);
        newSession.setParticipants(new HashSet<>());
        newSession.setResponse(new HashMap<>()); // Renamed to 'response' as per your model
        sessionRepository.save(newSession);
        return new SessionResponse(newSession.getJoinCode());
    }

    public Session addParticipant(String joinCode, String participantName) {
        Session session = findSessionByJoinCode(joinCode);
        if (session.getStatus() != SessionStatus.WAITING && session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot join a session that is not in the WAITING or ACTIVE state. " + session.getStatus() + " "+ participantName);
        }
        if (session.getParticipants().add(participantName)) { // Only add score if participant was newly added
            session.getScores().putIfAbsent(participantName, 0); // Initialize score to 0
            System.out.println("Participant '{}'" + participantName + "joined session {} and score initialized." + joinCode);
        }
        return sessionRepository.save(session);
    }

    public Session startSession(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);
        // Ensure all current participants have a score entry before starting
        session.getParticipants().forEach(pName -> session.getScores().putIfAbsent(pName, 0));

        session.setStatus(SessionStatus.ACTIVE);
        session.setCurrentQuestionIndex(0);
        Session savedSession = sessionRepository.save(session);

        // Broadcast initial leaderboard
        broadcastLeaderboardUpdate(joinCode); // Broadcast when starting


        log.info("Sent first question for session {}.", joinCode);

        return savedSession;
    }

    public void advanceToNextQuestion(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);
        if (session.getStatus() == SessionStatus.ENDED) {
            return;
        }
        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Associated quiz not found for session."));

        broadcastLeaderboardUpdate(joinCode);

        int nextIndex = session.getCurrentQuestionIndex() + 1;

        if (nextIndex >= quiz.getQuestionList().size()) {
            endSession(joinCode);
        } else {
            session.setCurrentQuestionIndex(nextIndex);
            sessionRepository.save(session);
        }
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
                currentIndex,
                currentQuestion.getText(),
                currentQuestion.getOptions()
        );

        return Optional.of(questionDTO);
    }



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

        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Associated quiz not found for scoring."));

        if (currentQuestionIndex >= 0 && currentQuestionIndex < quiz.getQuestionList().size()) {
            Question currentQuestion = quiz.getQuestionList().get(currentQuestionIndex);
            if (answerIndex == currentQuestion.getCorrectAnswerIndex()) {
                // Atomically update the score
                session.getScores().compute(username, (user, score) -> (score == null ? 0 : score) + 1);
                log.info("User '{}' answered question {} correctly. New score: {}", username, currentQuestionIndex, session.getScores().get(username));
                // Consider broadcasting leaderboard update here if you want immediate updates,
                // otherwise wait until question advance.
                // broadcastLeaderboardUpdate(joinCode); // Optional: Immediate update
            } else {
                log.info("User '{}' answered question {} incorrectly.", username, currentQuestionIndex);
                // Ensure score exists even if answer is wrong (might have joined late)
                session.getScores().putIfAbsent(username, 0);
            }
        }

        sessionRepository.save(session);

        String hostDestination = "/topic/session/" + joinCode + "/host";
        messagingTemplate.convertAndSend(hostDestination, Map.of(
                "eventType", "USER_ANSWERED",
                "name", username,
                "optionIndex", answerIndex
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

        String destination = "/topic/session/" + joinCode;
        messagingTemplate.convertAndSend(destination, Map.of(
                "eventType", "STATUS_UPDATE",
                "status", SessionStatus.ACTIVE
        ));
    }

    public void endSession(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);
        if (session.getStatus() == SessionStatus.ENDED) {
            return;
        }

        broadcastLeaderboardUpdate(joinCode);
        session.setStatus(SessionStatus.ENDED);
        sessionRepository.save(session);

        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new IllegalStateException("Quiz not found for session " + joinCode));



        Set<String> allUsersToProcess = new HashSet<>(session.getParticipants());
        allUsersToProcess.add(session.getHostUsername());

        for (String username : allUsersToProcess) {

            Map<Integer, Integer> userAnswersMap = session.getResponse().getOrDefault(username, Collections.emptyMap());
            List<ParticipantAnswer> userAnswersList = userAnswersMap.entrySet().stream()
                    .map(entry -> new ParticipantAnswer(entry.getKey(), entry.getValue(), username))
                    .collect(Collectors.toList());
            int score = 0;
            for (ParticipantAnswer answer : userAnswersList) {
                // Defensive check to prevent crash if question doesn't exist
                if (answer.getQuestionIndex() < quiz.getQuestionList().size()) {
                    int correctIndex = quiz.getQuestionList().get(answer.getQuestionIndex()).getCorrectAnswerIndex();
                    if (answer.getAnswerIndex() == correctIndex) {
                        score++;
                    }
                }
            }


            if(username.equals(session.getHostUsername())){
                QuizHost quizHost = new QuizHost();

                quizHost.setHostedAt(LocalDateTime.now());
                quizHost.setQuizTitle(quiz.getTitle());
                quizHost.setJoinCode(joinCode);
                quizHost.setUserId(username);
                quizHost.setTotalQuestions(quiz.getQuestionList().size());
                quizHost.setQuizId(quiz.getId());
                quizHost.setAnswers(userAnswersList);


                quizHostedRepo.save(quizHost);
                System.out.println(STR."SUCCESS: Saved QuizHost for user: \{username}");

//                messagingTemplate.convertAndSend("/topic/session/" + joinCode, Map.of("eventType", "QUIZ_ENDED"));
                continue;
            }


            QuizAttempt attempt = new QuizAttempt();
            attempt.setUserId(username);
            attempt.setQuizId(quiz.getId());
            attempt.setQuizTitle(quiz.getTitle());
            attempt.setSessionId(session.getJoinCode());
            attempt.setScore(score);
            attempt.setTotalQuestions(quiz.getQuestionList().size());
            attempt.setAttemptedAt(LocalDateTime.now());
            attempt.setAnswers(userAnswersList);
            quizAttemptRepo.save(attempt);// ...fetch hosted quizzes logic here if you have it...
            // setQuizzes(...)
            System.out.println("SUCCESS: Saved QuizAttempt for user: " + username);
        }
        messagingTemplate.convertAndSend("/topic/session/" + joinCode + "/ended", Map.of("eventType", "QUIZ_ENDED"));
    }


    public AnalyticsResponseDTO getAnalysis(String joinCode,String currentUsername) {

        Session session = sessionRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new RuntimeException("Session not there to analyse: " + joinCode));

        Quiz currentQuiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new RuntimeException("No quiz found for session: " + joinCode));


        boolean isHost = currentUsername.equals(session.getHostUsername());


        List<QuestionAnalytics> questionAnalyticsList = new ArrayList<>();


        for (int i = 0; i < currentQuiz.getQuestionList().size(); i++) {
            Question currentQuestion = currentQuiz.getQuestionList().get(i);
            int currentQuestionIndex = i;

            // --- Calculate optionFrequency and usernamesByOption ---
            Map<Integer, Integer> optionFrequency = new HashMap<>();
            Map<Integer, List<String>> usernamesByOption = new HashMap<>();

            for (Map.Entry<String, Map<Integer, Integer>> participantEntry : session.getResponse().entrySet()) {
                String username = participantEntry.getKey();
                Map<Integer, Integer> answers = participantEntry.getValue();

                // Ensure participant is in the session's participant list before processing their answers
                // (handles edge cases where someone might have left but responses remain)
                // Although, currently, responses are likely cleared or not added if not participant.
                // This adds an extra layer of safety.
                if (session.getParticipants().contains(username) || username.equals(session.getHostUsername())) {
                    if (answers.containsKey(currentQuestionIndex)) {
                        int chosenOptionIndex = answers.get(currentQuestionIndex);

                        // Validate chosenOptionIndex against available options
                        if (chosenOptionIndex >= 0 && chosenOptionIndex < currentQuestion.getOptions().size()) {
                            optionFrequency.merge(chosenOptionIndex, 1, Integer::sum);
                            if (isHost) {
                                usernamesByOption.computeIfAbsent(chosenOptionIndex, k -> new ArrayList<>()).add(username);
                            }
                        } else {
                            log.warn("User {} provided an invalid option index {} for question {} in session {}. Skipping.", username, chosenOptionIndex, currentQuestionIndex, joinCode);
                        }
                    } else {
                        // User did not answer this question
                        log.debug("User {} did not answer question {} in session {}.", username, currentQuestionIndex, joinCode);
                    }
                } else {
                    log.warn("User {} found in responses but not in current participant list for session {}. Skipping.", username, joinCode);
                }
            }
            // --- End calculation ---


            int userAnswerIndex = -1; // Default for host or if participant didn't answer

            // If the user is a participant, find their specific answer for this question
            if (!isHost && session.getResponse().containsKey(currentUsername)) {
                userAnswerIndex = session.getResponse().get(currentUsername).getOrDefault(currentQuestionIndex, -1);
            }

            // Validate userAnswerIndex before adding
            if (userAnswerIndex >= currentQuestion.getOptions().size()) {
                log.warn("User {}'s recorded answer index {} is out of bounds for question {}. Setting to -1.", currentUsername, userAnswerIndex, currentQuestionIndex);
                userAnswerIndex = -1; // Set to invalid index if out of bounds
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

        // --- Calculate Leaderboard ---
        List<LeaderboardEntryDTO> leaderboard = getLeaderboard(joinCode);
        // --------------------------

        log.info("Successfully generated analytics for session {} requested by user {}", joinCode, currentUsername);
        // --- Return the combined DTO ---
        return new AnalyticsResponseDTO(questionAnalyticsList, leaderboard);
    }

    public void deleteSession(String joinCode) {
        Session session = sessionRepository.findByJoinCode(joinCode).orElse(null);

        if(session==null){
            throw new RuntimeException("No Session with joinCode " + joinCode + " to delete");
        }
        QuizHost quizHost = quizHostedRepo.findByJoinCode(joinCode);
        quizHostedRepo.delete(quizHost);
        sessionRepository.delete(session);

    }

    private List<LeaderboardEntryDTO> getLeaderboard(String joinCode) {
        Session session = findSessionByJoinCode(joinCode);

        // 1. Create initial list sorted by score (highest first)
        List<LeaderboardEntryDTO> sortedEntries = session.getScores().entrySet().stream()
                .map(entry -> new LeaderboardEntryDTO(entry.getKey(), entry.getValue(), 0)) // Rank initially 0
                .sorted(Comparator.comparingInt(LeaderboardEntryDTO::getScore).reversed())
                .collect(Collectors.toList());

        // 2. Assign ranks, handling ties
        int currentRank = 0;
        int playersAtCurrentRank = 0;
        int lastScore = -1; // Use a score that won't naturally occur

        List<LeaderboardEntryDTO> rankedList = new ArrayList<>();
        for (int i = 0; i < sortedEntries.size(); i++) {
            LeaderboardEntryDTO currentEntry = sortedEntries.get(i);

            if (currentEntry.getScore() != lastScore) {
                // New score tier, update rank based on how many players were before this tier
                currentRank = i + 1; // Rank is 1-based index
                playersAtCurrentRank = 1;
                lastScore = currentEntry.getScore();
            } else {
                // Same score as previous player, they share the same rank
                playersAtCurrentRank++;
            }

            currentEntry.setRank(currentRank); // Assign the calculated rank
            rankedList.add(currentEntry);
        }

        return rankedList; // Return the list with calculated ranks
    }
    // ---------------------------------------------

    // --- New Method: Broadcast Leaderboard Update ---
    private void broadcastLeaderboardUpdate(String joinCode) {
        List<LeaderboardEntryDTO> leaderboard = getLeaderboard(joinCode);
        String destination = "/topic/session/" + joinCode + "/leaderboard";
        messagingTemplate.convertAndSend(destination, leaderboard);
        log.info("Broadcasted leaderboard update for session {}", joinCode);
    }
}

