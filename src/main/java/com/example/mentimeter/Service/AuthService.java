package com.example.mentimeter.Service;


import com.example.mentimeter.Model.AuthProvider;
import com.example.mentimeter.Model.User;
import com.example.mentimeter.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.PasswordAuthentication;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String registerUser(User user){

        String newPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(newPassword);
        user.setProvider(AuthProvider.LOCAL);
        userRepo.save(user);

        return jwtService.generateToken(user.getUsername());
    }

    public String verifyUser(User user){
        User newUser = userRepo.findByUsername(user.getUsername()).orElse(null);

        if(newUser==null){
            return "No user with this username.";
        }


        if(passwordEncoder.matches(user.getPassword(), newUser.getPassword())){
            return jwtService.generateToken(user.getUsername());
        }else{
            return "Wrong Password";
        }
    }
}
