package com.unip.api.user;

import com.unip.api.shared.exception.BusinessRuleException;
import com.unip.api.shared.exception.ResourceNotFoundException;
import com.unip.api.user.dto.UserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User create(UserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new BusinessRuleException("E-mail already registered");
        }
        return users.save(new User(req.name(), email, encoder.encode(req.password()), req.role()));
    }

    public List<User> listActive() {
        return users.findByActiveTrueOrderByNameAsc();
    }

    public User findActiveById(Long id) {
        return users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}