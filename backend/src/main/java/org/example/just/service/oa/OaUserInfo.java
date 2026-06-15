package org.example.just.service.oa;

import org.springframework.util.StringUtils;

public record OaUserInfo(
        String userId,
        String username,
        String name,
        String email,
        String telephone
) {

    public String normalizedUsername() {
        if (StringUtils.hasText(username)) {
            return username.trim();
        }
        if (StringUtils.hasText(userId)) {
            return "oa_" + userId.trim();
        }
        throw new IllegalStateException("OA 用户信息缺少 username 和 userId");
    }

    public String normalizedName() {
        return StringUtils.hasText(name) ? name.trim() : normalizedUsername();
    }
}
