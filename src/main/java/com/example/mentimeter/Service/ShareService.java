
package com.example.mentimeter.Service;

import com.example.mentimeter.DTO.AsyncAttemptSummaryDTO;
import com.example.mentimeter.DTO.ParticipantAsyncResultDTO;
import com.example.mentimeter.DTO.ParticipantAttemptDTO;
import com.example.mentimeter.DTO.ParticipantQuestionResultDTO;
import com.example.mentimeter.Model.*;
import com.example.mentimeter.Repository.*;
import com.example.mentimeter.Util.JoinCodeGenerator; // You already have this
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final QuizRepo quizRepo;
    private final AsyncQuizAttemptRepo asyncAttemptRepo;
    private final AsyncQuizAnalyticsRepo asyncAnalyticsRepo;

    // Called by the attempter to get question data
    public Quiz getQuizByShareCode(String shareCode) {
        Quiz quiz = quizRepo.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Invalid share code"));
        if (!quiz.isShared()) {
            throw new RuntimeException("Sharing is disabled for this quiz");
        }

        // --- CRITICAL ---
        // Create a DTO or a new Quiz object that *omits* the correct answers
        Quiz safeQuiz = new Quiz();
        safeQuiz.setId(quiz.getId());
        safeQuiz.setTitle(quiz.getTitle());
        safeQuiz.setQuestionList(quiz.getQuestionList().stream().map(q ->
                new Question(q.getText(), q.getOptions(), -1) // Set correct answer index to -1
        ).collect(Collectors.toList()));

        return safeQuiz;
    }

    // Called by the attempter to submit their answers
    public void recordAsyncAttempt(String shareCode, String userId, Map<Integer, Integer> answers) {
        Quiz quiz = quizRepo.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Invalid share code"));
        if (!quiz.isShared()) {
            throw new RuntimeException("Sharing is disabled for this quiz");
        }

        // Prevent duplicate attempts
        if(asyncAttemptRepo.findByQuizIdAndUserId(quiz.getId(), userId).isPresent()) {
            throw new RuntimeException("You have already attempted this quiz.");
        }

        int score = 0;
        int totalQuestions = quiz.getQuestionList().size();
        Map<Integer, Integer> validAnswers = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : answers.entrySet()) {
            int qIndex = entry.getKey();
            int aIndex = entry.getValue();
            if (qIndex >= 0 && qIndex < totalQuestions) {
                Question question = quiz.getQuestionList().get(qIndex);
                if (aIndex >= 0 && aIndex < question.getOptions().size()) {
                    validAnswers.put(qIndex, aIndex);
                    if (aIndex == question.getCorrectAnswerIndex()) {
                        score++;
                    }
                }
            }
        }

        AsyncQuizAttempt attempt = new AsyncQuizAttempt(null, quiz.getId(), shareCode, userId, validAnswers, score, totalQuestions, LocalDateTime.now());
        asyncAttemptRepo.save(attempt);

        // Update the aggregated analytics
        updateAggregatedAnalytics(quiz.getId());
    }

    // Called by the creator to view aggregated results
    public AsyncQuizAnalytics getAggregatedAnalytics(String quizId, String username) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getUsername().equals(username)) {
            throw new RuntimeException("User not authorized");
        }

        // 1. Get the analytics object
        AsyncQuizAnalytics analytics = asyncAnalyticsRepo.findByQuizId(quizId)
                .orElse(new AsyncQuizAnalytics(quizId, quizId, 0, 0.0, new HashMap<>(), null));

        // 2. --- MODIFY THIS SECTION ---
        // Fetch all attempts for this quiz
        List<AsyncQuizAttempt> attempts = asyncAttemptRepo.findByQuizId(quizId);

        // 3. Create the DTO list
        List<ParticipantAttemptDTO> attemptDTOs = attempts.stream()
                .map(attempt -> new ParticipantAttemptDTO(
                        attempt.getUserId(),
                        attempt.getScore(),
                        attempt.getTotalQuestions(),
                        attempt.getAnswers() // This is the map of answers
                ))
                .sorted(Comparator.comparing(ParticipantAttemptDTO::getUsername)) // Sort by username
                .collect(Collectors.toList());

        // 4. Set the new DTO list on the analytics object
        analytics.setAttempts(attemptDTOs);
        // --- END OF MODIFICATION ---

        return analytics;
    }

    // Private helper to re-calculate analytics after each attempt
    private void updateAggregatedAnalytics(String quizId) {
        List<AsyncQuizAttempt> attempts = asyncAttemptRepo.findByQuizId(quizId);
        int totalAttempts = attempts.size();

        // Find the analytics doc, or create a new one if it doesn't exist
        AsyncQuizAnalytics analytics = asyncAnalyticsRepo.findByQuizId(quizId)
                .orElse(new AsyncQuizAnalytics());

        analytics.setQuizId(quizId); // Ensure quizId is set

        // --- THIS IS THE FIX ---
        if (totalAttempts == 0) {
            // Instead of deleting, reset the document to a zero state
            analytics.setTotalAttempts(0);
            analytics.setAverageScore(0.0);
            analytics.setQuestionOptionCounts(new HashMap<>()); // Clear the counts
        } else {
            // This is your existing logic, which is correct
            double totalScoreSum = attempts.stream().mapToInt(AsyncQuizAttempt::getScore).sum();
            analytics.setAverageScore(totalScoreSum / totalAttempts);
            analytics.setTotalAttempts(totalAttempts);

            Map<Integer, Map<Integer, Integer>> questionOptionCounts = new HashMap<>();
            Quiz quiz = quizRepo.findById(quizId).orElse(null);

            if (quiz != null) {
                // Initialize the map
                for (int i = 0; i < quiz.getQuestionList().size(); i++) {
                    questionOptionCounts.put(i, new HashMap<>());
                }

                // Aggregate all answers
                for (AsyncQuizAttempt attempt : attempts) {
                    for (Map.Entry<Integer, Integer> answerEntry : attempt.getAnswers().entrySet()) {
                        int qIndex = answerEntry.getKey();
                        int oIndex = answerEntry.getValue();
                        if (questionOptionCounts.containsKey(qIndex)) {
                            questionOptionCounts.get(qIndex).merge(oIndex, 1, Integer::sum);
                        }
                    }
                }
            }
            analytics.setQuestionOptionCounts(questionOptionCounts);
        }

        analytics.setLastUpdatedAt(LocalDateTime.now());

        asyncAnalyticsRepo.save(analytics);
    }

    public String enableSharing(String quizId, String username) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getUsername().equals(username)) {
            throw new RuntimeException("User not authorized to share this quiz");
        }

        if (quiz.getShareCode() == null) {
            // Generate a unique 12-char code
            String shareCode = JoinCodeGenerator.generate() + JoinCodeGenerator.generate();
            // TODO: Add a check to ensure this code is unique in quizRepo
            quiz.setShareCode(shareCode);
        }
        quiz.setShared(true);
        quizRepo.save(quiz);
        return quiz.getShareCode();
    }

    public void disableSharing(String quizId, String username) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        if (!quiz.getUsername().equals(username)) {
            throw new RuntimeException("User not authorized");
        }
        quiz.setShared(false);
        quizRepo.save(quiz);
    }

    public ParticipantAsyncResultDTO getParticipantAsyncResult(String quizId, String userId) {

        // 1. Get the user's attempt
        AsyncQuizAttempt attempt = asyncAttemptRepo.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("No attempt found for this quiz."));

        // 2. Get the quiz itself (for questions/correct answers)
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found."));




        // 3. Get the aggregated analytics (for vote percentages)
        //    Fall back to empty analytics if none exist yet
        AsyncQuizAnalytics analytics = asyncAnalyticsRepo.findByQuizId(quizId)
                .orElse(new AsyncQuizAnalytics(quizId, quizId, 0, 0.0, new HashMap<>(), null));

        Map<Integer, Map<Integer, Integer>> allOptionCounts = analytics.getQuestionOptionCounts();
        if (allOptionCounts == null) {
            allOptionCounts = new HashMap<>();
        }

        // 4. Combine the data
        List<ParticipantQuestionResultDTO> questionResults = new ArrayList<>();

        for (int i = 0; i < quiz.getQuestionList().size(); i++) {
            Question q = quiz.getQuestionList().get(i);

            // Get the user's answer for this question (or -1 if they skipped)
            int userAnswer = attempt.getAnswers().getOrDefault(i, -1);

            // Get the vote counts for this question (or an empty map)
            Map<Integer, Integer> optionCounts = allOptionCounts.getOrDefault(i, Collections.emptyMap());

            questionResults.add(new ParticipantQuestionResultDTO(
                    q.getText(),
                    q.getOptions(),
                    q.getCorrectAnswerIndex(),
                    userAnswer,
                    optionCounts
            ));
        }

        return new ParticipantAsyncResultDTO(
                quiz.getTitle(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                questionResults
        );
    }

    public void deleteAttempt(String quizId, String userId, String creatorUsername) {

        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!quiz.getUsername().equals(creatorUsername)) {
            throw new RuntimeException("User not authorized to delete this attempt");
        }

        AsyncQuizAttempt attempt = asyncAttemptRepo.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("Attempt not found for user: " + userId));

        asyncAttemptRepo.delete(attempt);

         updateAggregatedAnalytics(quizId);
    }

    public List<AsyncAttemptSummaryDTO> getMyAsyncAttempts(String userId) {
        // 1. Find all async attempts for the user
        List<AsyncQuizAttempt> attempts = asyncAttemptRepo.findByUserIdOrderByAttemptedAtDesc(userId);

        // 2. Map them to the DTO, fetching quiz title for each
        return attempts.stream()
                .map(attempt -> {
                    // Find the quiz to get its title
                    Optional<Quiz> quizOpt = quizRepo.findById(attempt.getQuizId());
                    String title = quizOpt.map(Quiz::getTitle).orElse("Deleted Quiz");

                    return new AsyncAttemptSummaryDTO(
                            attempt.getQuizId(),
                            title,
                            attempt.getScore(),
                            attempt.getTotalQuestions(),
                            attempt.getAttemptedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public void deleteMyAsyncAttempt(String quizId, String userId) {
        // 1. Find the specific attempt to delete
        AsyncQuizAttempt attempt = asyncAttemptRepo.findByQuizIdAndUserId(quizId, userId)
                .orElseThrow(() -> new RuntimeException("Attempt not found for user: " + userId));

        // 2. Delete the attempt (No creator check needed, user deletes their own)
        asyncAttemptRepo.delete(attempt);

        // 3. CRITICAL: Recalculate the aggregate analytics
        updateAggregatedAnalytics(quizId);
    }
}