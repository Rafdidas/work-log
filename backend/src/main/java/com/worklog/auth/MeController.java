package com.worklog.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.worklog.common.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/me")
public class MeController {

    public record Profile(long id, String email, String name, String teamLabel) {
        static Profile of(User u) {
            return new Profile(u.getId(), u.getEmail(), u.getName(), u.getTeamLabel());
        }
    }

    public record UpdateRequest(@NotBlank @Size(max = 100) String teamLabel) {}

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    Profile me(@AuthenticationPrincipal CurrentUser current) {
        return Profile.of(load(current));
    }

    @PatchMapping
    @Transactional
    Profile update(@AuthenticationPrincipal CurrentUser current, @Valid @RequestBody UpdateRequest req) {
        User user = load(current);
        user.setTeamLabel(req.teamLabel().trim());
        return Profile.of(user);
    }

    private User load(CurrentUser current) {
        return users.findById(current.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNKNOWN_USER", "사용자를 찾을 수 없습니다."));
    }
}
