package cl.nextech.dashboard.controller;

import cl.nextech.dashboard.dto.AuthDto;
import cl.nextech.dashboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(
        @Valid @RequestBody AuthDto.LoginRequest req
    ) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.LoginResponse> register(
        @Valid @RequestBody AuthDto.RegisterRequest req
    ) {
        return ResponseEntity.ok(authService.register(req));
    }
}
