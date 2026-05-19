package br.com.ada.quarkus.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordService {

    private Argon2 argon2;

    @PostConstruct
    void init() {
        argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }

    public String hash(String rawPassword) {
        return argon2.hash(2, 65536, 1, rawPassword.toCharArray());
    }

    public boolean verify(String hashedPassword, String rawPassword) {
        return hashedPassword != null
                && rawPassword != null
                && argon2.verify(hashedPassword, rawPassword.toCharArray());
    }
}