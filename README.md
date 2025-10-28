# Mentimeter Clone - Backend

This is the Spring Boot backend server for the Mentimeter Clone project. It provides RESTful APIs for application logic, manages user authentication with JWT, and handles real-time communication using WebSockets (STOMP).

## Features

* **REST APIs:** Comprehensive endpoints for managing users, quizzes, sessions, and analytics.
* **Authentication & Security: Secured with Spring Security and JSON Web Tokens (JWT).
* **WebSocket Integration:** Real-time quiz functionality (joining, starting, next question, answering, leaderboard) handled via STOMP over WebSocket.
* **Database:** Uses Spring Data MongoDB for all data persistence (Users, Quizzes, Sessions, Attempts).
* **AI Integration:** Connects to Google's Gemini AI (via Vertex AI) to generate quiz questions from text input.
* **Session Management:**
    * Handles real-time session creation with unique join codes.
    * Manages participant state and scoring.
* **Asynchronous Quizzes:**
    * Supports sharing quizzes via a link for asynchronous attempts.
    * Tracks and stores all attempts for later review and analytics.

## Tech Stack

* **Java 17**
* **Spring Boot:**
    * Spring Web (REST APIs)
    * Spring Security (JWT Authentication)
    * Spring WebSocket (STOMP)
    * Spring Data MongoDB
* **MongoDB:** NoSQL database for storing all application data.
* **Maven:** Dependency management.
* **Lombok:** To reduce boilerplate code.
* **jjwt:** For creating and parsing JSON Web Tokens.
* **Google Cloud Vertex AI:** For integration with the Gemini AI model.

## Setup and Installation

Follow these steps to get the backend server running locally.

1.  **Prerequisites:**
    * JDK 17 or later
    * Apache Maven
    * MongoDB (running locally or a cloud instance)

2.  **Clone the repository:**
    ```bash
    git clone [https://github.com/RounakDagar/mentimeter.git]
    cd mentimeter
    ```

3.  **Configure Environment Variables:**

    This application **requires** environment variables to run. You can set these in your operating system or by creating your own `application.properties` file (just make sure not to commit it if you add secrets).

    The following variables are defined in `src/main/resources/application.properties`:

    * `JWT_SECRET_KEY`: A long, random, secret string used for signing JWTs.
    * `GCP_PROJECT_ID`: Your Google Cloud Project ID (for Vertex AI).
    * `spring.data.mongodb.uri`: The connection string for your MongoDB database.
        * *Example (local):* `mongodb://localhost:27017/mentimeter_db`
        * *Example (cloud):* `mongodb+srv://user:password@cluster.mongodb.net/mentimeter_db`

4.  **Build the project:**
    ```bash
    mvn clean install
    ```

5.  Run the application:
    ```bash
    mvn spring-boot:run
    ```
    The server will start, typically on port `8080`.

## API Endpoints

A brief overview of the main controllers:

* `/auth/**`: Handles user registration (`/register`) and login (`/login`).
* `/api/quizzes/**`: CRUD operations for quizzes and questions.
* `/api/sessions/**`: Creating real-time sessions, fetching session details.
* `/api/share/**`: Creating and managing asynchronous quiz links, submitting attempts.
* `/api/ai/**`: Endpoints for generating quizzes from text (`/generate-quiz-from-text`).
* `/ws/**`: The WebSocket STOMP endpoint for real-time communication.
