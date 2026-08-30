package com.lawfirm.auth;

import com.lawfirm.auth.dto.ChangePasswordRequest;
import com.lawfirm.auth.dto.LoginRequest;
import com.lawfirm.auth.dto.LoginResponse;
import com.lawfirm.auth.dto.ProfileRequest;
import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.BizException;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.security.JwtService;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.upload-dir}")
    private String uploadDir;

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

    /** 更新个人资料（姓名/邮箱/电话） */
    @Transactional
    public UserInfo updateProfile(ProfileRequest request) {
        User user = CurrentUser.user();
        user.setRealName(request.realName().trim());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        return UserInfo.from(userRepository.save(user));
    }

    /** 上传头像：存文件并更新用户头像地址 */
    @Transactional
    public UserInfo uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        String ext = safeExt(file.getOriginalFilename());
        String storedName = "avatar/" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = base.resolve(storedName).normalize();
        if (!target.startsWith(base)) {
            throw new BizException("非法文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException("头像上传失败");
        }
        User user = CurrentUser.user();
        user.setAvatar("/uploads/" + storedName);
        return UserInfo.from(userRepository.save(user));
    }

    private String safeExt(String original) {
        if (original == null) {
            return ".jpg";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return ".jpg";
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        Set<String> allowed = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowed.contains(ext)) {
            throw new BizException("头像仅支持图片格式(jpg/png/gif/webp)");
        }
        return ext;
    }
}
