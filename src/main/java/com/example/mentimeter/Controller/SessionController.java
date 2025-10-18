package com.example.mentimeter.Controller;

import com.example.mentimeter.Model.QuestionAnalytics;
import com.example.mentimeter.Model.Session;
import com.example.mentimeter.Model.SessionStatus;
import com.example.mentimeter.Service.SessionService;
import com.example.mentimeter.DTO.CreateSessionRequest;
import com.example.mentimeter.DTO.SessionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    @Autowired
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }


    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        SessionResponse sessionResponse = sessionService.createSession(request.getQuizId());
        return ResponseEntity.ok(sessionResponse);
    }

    @GetMapping("/{joinCode}/validate")
    public ResponseEntity<Session> isValidSession(@PathVariable String joinCode){
        return ResponseEntity.ok(sessionService.findSessionByJoinCode(joinCode));
    }

    /**
     * [Phase 4] Fetches the final, aggregated results for a completed quiz session.
     * (Placeholder for future implementation)
     */
    @GetMapping("/{joinCode}/{username}/analytics")
    public ResponseEntity<List<QuestionAnalytics>> getSessionAnalytics(@PathVariable String joinCode,@PathVariable String username) {
        // TODO: Implement logic to fetch and calculate final results.


        return ResponseEntity.ok(sessionService.getAnalysis(joinCode,username));
    }

    @PutMapping("/{joinCode}/pause")
    public ResponseEntity<Map<String, String>> pauseSession(@PathVariable String joinCode) {

        sessionService.pauseSession(joinCode);
        Map<String, String> response = Map.of("message", "Session " + joinCode + " has been paused.");

        return ResponseEntity.ok(response);
    }


    @PutMapping("/{joinCode}/resume")
    public ResponseEntity<Map<String, String>> resumeSession(@PathVariable String joinCode) {
        sessionService.resumeSession(joinCode);
        return ResponseEntity.ok(Map.of("message", "Session " + joinCode + " has been resumed."));
    }

    @PutMapping("/{joinCode}/end")
    public ResponseEntity<Map<String, String>> endSession(@PathVariable String joinCode) {
        sessionService.endSession(joinCode);
        return ResponseEntity.ok(Map.of("message", "Session " + joinCode + " has been ended."));
    }

    @DeleteMapping("/{joinCode}/deleteSession")
    public void deleteSession(@PathVariable String joinCode){

       sessionService.deleteSession(joinCode);
    }



}