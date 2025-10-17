package com.example.mentimeter.Service;


import com.example.mentimeter.Model.AuthProvider;
import com.example.mentimeter.Model.User;
import com.example.mentimeter.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.swing.text.StyledEditorKit;
import java.net.PasswordAuthentication;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String registerUser(User user) {


        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        String newPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(newPassword);


        userRepo.save(user);

        return jwtService.generateToken(user.getUsername());
    }

    public Boolean usernameTaken(String username){
        User newUser = userRepo.findByUsername(username).orElse(null);

        if(newUser!=null) return true;

        return false;
    }

    public String verifyUser(User user) {
        User foundUser = userRepo.findByUsername(user.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("No user with this username."));

        if (passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            return jwtService.generateToken(user.getUsername());
        } else {

            throw new BadCredentialsException("Wrong Password");
        }
    }
}
