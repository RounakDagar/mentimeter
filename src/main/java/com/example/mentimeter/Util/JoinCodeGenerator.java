package com.example.mentimeter.Util;

import java.security.SecureRandom;
import java.util.Random;

public class JoinCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final Random RANDOM = new SecureRandom();

    /**
     * Generates a random, 6-character alphanumeric join code.
     * @return A unique string for the session join code.
     */
    public static String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
