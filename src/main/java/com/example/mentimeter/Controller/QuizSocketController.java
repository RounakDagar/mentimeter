package com.example.mentimeter.Controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller // (1) Why? Not @RestController
public class QuizSocketController {

    private final SimpMessagingTemplate messagingTemplate; // (2) Why?

    @Autowired
    public QuizSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // A DTO for the answer payload
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnswerPayload {
        private int questionIndex;
        private int optionIndex;
        // getters and setters
    }

    /**
     * Handles a new participant joining a session.
     * The message is sent to "/app/session/{joinCode}/join".
     * Broadcasts the updated participant list to "/topic/session/{joinCode}/participants".
     */
    @MessageMapping("/session/{joinCode}/join") // (3) How?
    public void handleJoin(@DestinationVariable String joinCode, @Payload String participantName) { // (4) How?
        // TODO: Add logic to register the participant in the SessionService
        System.out.println("Participant '" + participantName + "' joined session " + joinCode);

        // Broadcast the updated list of participants to everyone in the session
        String destination = "/topic/session/" + joinCode + "/participants";
        // messagingTemplate.convertAndSend(destination, /* updated participant list */);
    }

    /**
     * Handles the host starting the quiz.
     * Broadcasts the first question to "/topic/session/{joinCode}/question".
     */
    @MessageMapping("/session/{joinCode}/start")
    public void handleStart(@DestinationVariable String joinCode) {
        // TODO: Logic to get the first question from the service
        System.out.println("Host started session " + joinCode);

        // Broadcast the first question to all participants
        String destination = "/topic/session/" + joinCode + "/question";
        // messagingTemplate.convertAndSend(destination, /* first question object */);
    }

    /**
     * Handles the host advancing to the next question.
     */
    @MessageMapping("/session/{joinCode}/next")
    public void handleNext(@DestinationVariable String joinCode) {
        // TODO: Logic to get the next question
        System.out.println("Host requested next question for session " + joinCode);
        String destination = "/topic/session/" + joinCode + "/question";
        // messagingTemplate.convertAndSend(destination, /* next question object */);
    }

    /**
     * Handles a participant submitting an answer for the current question.
     */
    @MessageMapping("/session/{joinCode}/answer")
    public void handleAnswer(@DestinationVariable String joinCode, @Payload AnswerPayload answer, Principal principal) { // (5) How?
        String userId = principal.getName(); // Unique identifier for the connected user
        // TODO: Logic to process the answer using the service layer
        System.out.println("User '" + userId + "' in session " + joinCode + " answered.");

        // Optionally, send a confirmation back to the user who answered
        // messagingTemplate.convertAndSendToUser(userId, "/queue/reply", "Answer received!");

        // Broadcast updated live results to the host
        // String hostDestination = "/topic/session/" + joinCode + "/results";
        // messagingTemplate.convertAndSend(hostDestination, /* live results data */);
    }
}