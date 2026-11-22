package com.datavvy.vms.auth.application;

import com.datavvy.vms.auth.api.AuthUser;
import com.datavvy.vms.auth.api.TokenResponse;
import com.datavvy.vms.auth.domain.UserAccount;
import com.datavvy.vms.auth.domain.UserAccountRepository;
import com.datavvy.vms.platform.error.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public TokenResponse login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim())
                .filter(UserAccount::isEnabled)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new TokenResponse(tokens.issue(user), "Bearer", tokens.ttlSeconds(), toAuthUser(user));
    }

    public AuthUser currentUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(AuthService::toAuthUser)
                .orElseThrow(() -> new NotFoundException("User", email));
    }

    private static AuthUser toAuthUser(UserAccount u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole());
    }
}
