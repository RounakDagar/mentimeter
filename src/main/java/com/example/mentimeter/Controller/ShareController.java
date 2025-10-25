// src/main/java/com/example/mentimeter/Controller/ShareController.java
package com.example.mentimeter.Controller;

import com.example.mentimeter.DTO.AsyncAttemptSummaryDTO;
import com.example.mentimeter.DTO.ParticipantAsyncResultDTO;
import com.example.mentimeter.Model.AsyncQuizAnalytics;
import com.example.mentimeter.Model.Quiz;
import com.example.mentimeter.Repository.QuizRepo;
import com.example.mentimeter.Service.QuizService;
import com.example.mentimeter.Service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/share") // All endpoints will be at /share/...
@RequiredArgsConstructor
public class ShareController {

    private final QuizService quizService;

    private final ShareService shareService;

    @PostMapping("/quiz/{quizId}/enable")
    public ResponseEntity<Map<String, String>> enableSharing(@PathVariable String quizId, Authentication auth) {
        String shareCode = quizService.enableSharing(quizId, auth.getName());
        return ResponseEntity.ok(Map.of("shareCode", shareCode));
    }

    @PostMapping("/quiz/{quizId}/disable")
    public ResponseEntity<Void> disableSharing(@PathVariable String quizId, Authentication auth) {
        quizService.disableSharing(quizId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{shareCode}/quiz")
    public ResponseEntity<Quiz> getSharedQuiz(@PathVariable String shareCode) {
        Quiz safeQuiz = shareService.getQuizByShareCode(shareCode);
        return ResponseEntity.ok(safeQuiz);
    }

    @PostMapping("/{shareCode}/attempt")
    public ResponseEntity<Void> submitAttempt(
            @PathVariable String shareCode,
            @RequestBody Map<Integer, Integer> answers,
            Authentication auth
    ) {
        shareService.recordAsyncAttempt(shareCode, auth.getName(), answers);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/quiz/{quizId}/async-analytics")
    public ResponseEntity<AsyncQuizAnalytics> getAsyncAnalytics(@PathVariable String quizId, Authentication auth) {
        AsyncQuizAnalytics analytics = shareService.getAggregatedAnalytics(quizId, auth.getName());
        return ResponseEntity.ok(analytics);
    }


    @GetMapping("/attempt/my-result/{quizId}")
    public ResponseEntity<ParticipantAsyncResultDTO> getMyAsyncResult(
            @PathVariable String quizId,
            Authentication auth
    ) {
        ParticipantAsyncResultDTO result = shareService.getParticipantAsyncResult(quizId, auth.getName());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/attempt/delete")
    public ResponseEntity<Void> deleteAsyncAttempt(
            @RequestParam String quizId,
            @RequestParam String userId,
            Authentication auth
    ) {

        shareService.deleteAttempt(quizId, userId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-async-attempts")
    public ResponseEntity<List<AsyncAttemptSummaryDTO>> getMyAsyncAttempts(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = auth.getName();
        List<AsyncAttemptSummaryDTO> attempts = shareService.getMyAsyncAttempts(userId);
        return ResponseEntity.ok(attempts);
    }
    @DeleteMapping("/my-attempt/async/{quizId}")
    public ResponseEntity<Void> deleteMyAsyncAttempt(
            @PathVariable String quizId,
            Authentication auth
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = auth.getName();
        // The service method handles deletion and analytics update
        shareService.deleteMyAsyncAttempt(quizId, userId);
        return ResponseEntity.ok().build();
    }
}