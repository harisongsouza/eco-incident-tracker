package com.unip.api.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // the order defines the ids (1 to 4) on an empty database
        seed("Employee One", "employee1@demo.com", Role.EMPLOYEE);
        seed("Employee Two", "employee2@demo.com", Role.EMPLOYEE);
        seed("Environmental Officer", "officer@demo.com", Role.ENVIRONMENTAL_OFFICER);
        seed("Manager", "manager@demo.com", Role.MANAGER);
    }

    private void seed(String name, String email, Role role) {
        if (!users.existsByEmail(email)) {
            users.save(new User(name, email, encoder.encode("demo1234"), role));
        }
    }
}