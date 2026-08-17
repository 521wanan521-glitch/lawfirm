package com.lawfirm.user;

import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.user.dto.ResetPasswordRequest;
import com.lawfirm.user.dto.UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserInfo> page(String keyword, User.Role role, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result;
        if (StringUtils.hasText(keyword) && role != null) {
            result = userRepository.findByUsernameContainingOrRealNameContainingAndRole(keyword, keyword, role, pageable);
        } else if (StringUtils.hasText(keyword)) {
            result = userRepository.findByUsernameContainingOrRealNameContaining(keyword, keyword, pageable);
        } else if (role != null) {
            result = userRepository.findByRole(role, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }
        return PageResult.of(result, UserInfo::from);
    }

    /** 启用的用户（下拉选择用） */
    public List<UserInfo> options() {
        return userRepository.findByEnabledTrueOrderByRealNameAsc().stream().map(UserInfo::from).toList();
    }

    @Transactional
    public UserInfo create(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BizException("用户名已存在");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BizException("创建用户时必须设置初始密码");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        apply(user, request);
        user.setEnabled(true);
        return UserInfo.from(userRepository.save(user));
    }

    @Transactional
    public UserInfo update(Long id, UserRequest request) {
        User user = getById(id);
        userRepository.findByUsername(request.getUsername().trim())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new BizException("用户名已存在");
                });
        user.setUsername(request.getUsername().trim());
        apply(user, request);
        return UserInfo.from(userRepository.save(user));
    }

    private void apply(User user, UserRequest request) {
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setDepartment(request.getDepartment());
        user.setTitle(request.getTitle());
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        User user = getById(id);
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = getById(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException("用户不存在"));
    }
}
