package cl.nextech.dashboard.service;

import cl.nextech.dashboard.dto.AuthDto;
import cl.nextech.dashboard.entity.AppUser;
import cl.nextech.dashboard.repository.AppUserRepository;
import cl.nextech.dashboard.security.CustomUserDetailsService;
import cl.nextech.dashboard.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository      userRepo;
    private final PasswordEncoder        encoder;
    private final JwtUtil                jwtUtil;
    private final AuthenticationManager  authManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService           emailService;

    // ── Login ────────────────────────────────────────────────────────────

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        AppUser user = userRepo.findByEmailAndActiveTrue(req.email())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserDetails ud    = userDetailsService.loadUserByUsername(req.email());
        String      token = jwtUtil.generateToken(ud, Map.of("role", user.getRole()));

        return new AuthDto.LoginResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    // ── Forgot password ──────────────────────────────────────────────────

    public void forgotPassword(String email) {
        // Si el email no existe, respondemos igual para no revelar información
        userRepo.findByEmailAndActiveTrue(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(2));
            userRepo.save(user);
            emailService.sendPasswordReset(email, token);
        });
    }

    // ── Reset password ───────────────────────────────────────────────────

    public void resetPassword(String token, String newPassword) {
        AppUser user = userRepo.findByResetToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token inválido o expirado");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepo.save(user);
    }

    // ── Register ─────────────────────────────────────────────────────────

    public AuthDto.LoginResponse register(AuthDto.RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new RuntimeException("El email ya está registrado");
        }

        AppUser user = AppUser.builder()
            .email(req.email())
            .password(encoder.encode(req.password()))
            .name(req.name())
            .role("MANAGER")   // rol por defecto; el ADMIN se asigna manualmente en DB
            .active(true)
            .build();

        userRepo.save(user);

        UserDetails ud    = userDetailsService.loadUserByUsername(req.email());
        String      token = jwtUtil.generateToken(ud, Map.of("role", user.getRole()));

        return new AuthDto.LoginResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}
