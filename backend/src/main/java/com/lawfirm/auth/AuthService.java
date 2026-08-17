package com.lawfirm.auth;

import com.lawfirm.auth.dto.ChangePasswordRequest;
import com.lawfirm.auth.dto.LoginRequest;
import com.lawfirm.auth.dto.LoginResponse;
import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.BizException;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.security.JwtService;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException(403, "账号已被停用，请联系管理员");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new LoginResponse(token, UserInfo.from(user));
    }

    public UserInfo me() {
        return UserInfo.from(CurrentUser.user());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = CurrentUser.user();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
