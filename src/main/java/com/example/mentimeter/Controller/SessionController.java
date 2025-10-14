package com.example.mentimeter.Controller;

import com.example.mentimeter.Service.SessionService;
import com.example.mentimeter.DTO.CreateSessionRequest;
import com.example.mentimeter.DTO.SessionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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

    /**
     * [Phase 4] Fetches the final, aggregated results for a completed quiz session.
     * (Placeholder for future implementation)
     */
    @GetMapping("/{joinCode}/analytics")
    public ResponseEntity<String> getSessionAnalytics(@PathVariable String joinCode) {
        // TODO: Implement logic to fetch and calculate final results.
        return ResponseEntity.ok("Analytics for session " + joinCode + " are not yet available.");
    }
}