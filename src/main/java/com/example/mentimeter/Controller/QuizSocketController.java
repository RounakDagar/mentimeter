package com.example.mentimeter.Controller;

import com.example.mentimeter.Model.Session;
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
}