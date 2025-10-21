package com.example.mentimeter.Controller;

import com.example.mentimeter.DTO.QuestionDTO;
import com.example.mentimeter.Model.Session;
import com.example.mentimeter.Model.SessionStatus;
import com.example.mentimeter.Service.SessionService;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Controller

public class QuizSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionService sessionService;

    @Autowired
    public QuizSocketController(SimpMessagingTemplate messagingTemplate, SessionService sessionService) {
        this.messagingTemplate = messagingTemplate;
        this.sessionService = sessionService;
    }



    // A DTO for the answer payload
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnswerPayload {
        private int questionIndex;
        private int optionIndex;

    }


    @MessageMapping("/session/{joinCode}/join") // (3) How?
    public void handleJoin(@DestinationVariable String joinCode, @Payload String participantName) { // (4) How?

        Session session = sessionService.addParticipant(joinCode, participantName);

        System.out.println("Participant '" + participantName + "' joined session " + joinCode);

        String destination = "/topic/session/" + joinCode + "/participants";

        messagingTemplate.convertAndSend(destination,session.getParticipants());

        if (session.getStatus() == SessionStatus.ACTIVE) {
            Optional<QuestionDTO> currentQuestion = sessionService.getCurrentQuestionForSession(joinCode);
            currentQuestion.ifPresent(question -> {
                System.out.println("Sending current question to late-joining participant: " + participantName);
                messagingTemplate.convertAndSendToUser(
                        participantName,
                        "/queue/question", // Send to a private queue
                        question
                );
            });
        }
    }

    /**
     * Handles the host starting the quiz.
     * Broadcasts the first question to "/topic/session/{joinCode}/question".
     */
    @MessageMapping("/session/{joinCode}/start")
    public void handleStart(@DestinationVariable String joinCode) {


        Session session = sessionService.startSession(joinCode);
        System.out.println("Host started session " + joinCode);


        String destination = "/topic/session/" + joinCode + "/question";
         messagingTemplate.convertAndSend(destination, sessionService.getCurrentQuestionForSession(joinCode) );
    }

    /**
     * Handles the host advancing to the next question.
     */
    @MessageMapping("/session/{joinCode}/next")
    public void handleNext(@DestinationVariable String joinCode) {

        sessionService.advanceToNextQuestion(joinCode);
        System.out.println("Host requested next question for session " + joinCode);
        String destination = "/topic/session/" + joinCode + "/question";
        messagingTemplate.convertAndSend(destination, sessionService.getCurrentQuestionForSession(joinCode));
    }

    /**
     * Handles a participant submitting an answer for the current question.
     */
    @MessageMapping("/session/{joinCode}/answer")
    public void handleAnswer(@DestinationVariable String joinCode, @Payload AnswerPayload answer, Principal principal) {
        String participantIdentifier = (principal != null) ? principal.getName() : "anonymousUser"; // Temporary fallback

//        ("Participant '{}' in session {} answered with option {}", participantIdentifier, joinCode, answer.getOptionIndex());
        sessionService.recordAnswer(joinCode, participantIdentifier, answer.getOptionIndex());

        String hostDestination = "/topic/session/" + joinCode + "/host";
        messagingTemplate.convertAndSend(hostDestination, Map.of("eventType", "USER_ANSWERED", "name", participantIdentifier));

    }

    @MessageMapping("/session/{joinCode}/end")
    public void handleEndSession(@DestinationVariable String joinCode, Principal principal) {
        String hostUsername = principal.getName();

        // === ADD THIS TRY-CATCH BLOCK ===
        try {
            System.out.println("Attempting to end session: " + joinCode + " for host: " + hostUsername);

            // Call your existing service logic
            sessionService.endSession(joinCode);

            System.out.println("Session service finished. Sending confirmation to host.");

            // Send the private confirmation message back ONLY to the HOST
            messagingTemplate.convertAndSendToUser(
                    hostUsername,
                    "/queue/session-ended-host", // This matches the new subscription in HostPage
                    Map.of("status", "ENDED", "joinCode", joinCode)
            );

        } catch (Exception e) {
            // THIS WILL PRINT THE ERROR TO YOUR CONSOLE
            System.err.println("!!! FAILED TO END SESSION: " + joinCode + " !!!");
            e.printStackTrace(); // This is the most important line

            // (Optional) Send an error message back to the host's client
            messagingTemplate.convertAndSendToUser(
                    hostUsername,
                    "/queue/error", // A generic error queue
                    Map.of("error", "Failed to end session: " + e.getMessage())
            );
        }
    }
}