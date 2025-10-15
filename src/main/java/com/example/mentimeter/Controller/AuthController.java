package com.example.mentimeter.Controller;

import com.example.mentimeter.Model.User;
import com.example.mentimeter.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user){
        String token = authService.verifyUser(user);

        if(token!=null){
            return ResponseEntity.ok(token);
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user){
        String token = authService.registerUser(user);

        return ResponseEntity.ok(token);

    }

    @GetMapping("/usernameTaken")
    public Boolean verifyUsername(@RequestBody String username){
        return authService.usernameTaken(username);
    }
}
