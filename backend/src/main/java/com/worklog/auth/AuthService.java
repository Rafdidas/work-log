package com.worklog.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.ApiException;
import com.worklog.item.WorkItem;
import com.worklog.item.WorkItemRepository;
import com.worklog.item.WorkItemStatus;

@Service
public class AuthService {

    /** 상시 항목은 목록 맨 아래에 오도록 큰 sort_order를 준다. */
    private static final int RECURRING_SORT_BASE = 1000;

    private final UserRepository users;
    private final WorkItemRepository items;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final boolean signupEnabled;

    public AuthService(UserRepository users, WorkItemRepository items, PasswordEncoder encoder, Clock clock,
            @Value("${worklog.signup-enabled}") boolean signupEnabled) {
        this.users = users;
        this.items = items;
        this.encoder = encoder;
        this.clock = clock;
        this.signupEnabled = signupEnabled;
    }

    @Transactional
    public User signup(String email, String password, String name) {
        if (!signupEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SIGNUP_DISABLED", "회원가입이 비활성화되어 있습니다.");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 가입된 이메일입니다.");
        }
        Instant now = clock.instant();
        User user = users.save(new User(email, encoder.encode(password), name, now));
        LocalDate today = LocalDate.now(clock);
        String[] defaults = {"회의", "기타"};
        for (int i = 0; i < defaults.length; i++) {
            WorkItem item = new WorkItem(user.getId(), defaults[i], WorkItemStatus.IN_PROGRESS,
                    RECURRING_SORT_BASE + i, now);
            item.setRecurring(true);
            item.setShowInPlan(false);
            item.setStartedAt(today);
            items.save(item);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {
        return users.findByEmail(email)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다."));
    }
}
