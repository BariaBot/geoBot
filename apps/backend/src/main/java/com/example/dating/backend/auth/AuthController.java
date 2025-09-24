package com.example.dating.backend.auth;

import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/telegram")
    public ResponseEntity<TokenResponse> auth(@RequestBody TelegramAuthRequest request) {
        // TODO validate initData
        User user = userService.ensureUserExists(request.telegramId(), request.username());
        String token = jwtService.generate(user.getTelegramId());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public record TelegramAuthRequest(Long telegramId, String username) {}
    public record TokenResponse(String token) {}
}
