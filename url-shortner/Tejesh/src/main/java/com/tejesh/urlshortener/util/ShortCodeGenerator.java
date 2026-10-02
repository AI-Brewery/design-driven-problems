package com.tejesh.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Objects;

@Component
public class ShortCodeGenerator {

    private static final String BASE62_CHARACTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_CODE_LENGTH = 7;

    private final SecureRandom secureRandom;

    public ShortCodeGenerator() {
        this(new SecureRandom());
    }

    public ShortCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    public String generate() {
        StringBuilder shortCode = new StringBuilder(SHORT_CODE_LENGTH);

        for (int index = 0; index < SHORT_CODE_LENGTH; index++) {
            int characterIndex = secureRandom.nextInt(BASE62_CHARACTERS.length());
            shortCode.append(BASE62_CHARACTERS.charAt(characterIndex));
        }

        return shortCode.toString();
    }
}