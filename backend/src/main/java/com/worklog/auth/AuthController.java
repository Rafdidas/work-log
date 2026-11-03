package com.worklog.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record SignupRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String name) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    ResponseEntity<MeController.Profile> signup(@Valid @RequestBody SignupRequest req) {
        User user = authService.signup(req.email(), req.password(), req.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie(jwtService.issue(user.getId()), JwtService.TTL.getSeconds()))
                .body(MeController.Profile.of(user));
    }

    @PostMapping("/login")
    ResponseEntity<MeController.Profile> login(@Valid @RequestBody LoginRequest req) {
        User user = authService.login(req.email(), req.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(jwtService.issue(user.getId()), JwtService.TTL.getSeconds()))
                .body(MeController.Profile.of(user));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0)).build();
    }

    private static String cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(JwtCookieFilter.COOKIE_NAME, value)
                .httpOnly(true).sameSite("Lax").path("/").maxAge(maxAgeSeconds).build().toString();
    }
}
