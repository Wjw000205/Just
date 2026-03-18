package org.example.just.context;



import java.util.Optional;

public class UserContext {

    private static final ThreadLocal<UserInfo> userInfoHolder = new ThreadLocal<>();

    public static class UserInfo {
        private final Integer userId;
        private final String userName;
        private final Integer role;

        public UserInfo(Integer userId, String userName, Integer role) {
            this.userId = userId;
            this.userName = userName;
            this.role = role;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public Integer getRole() {
            return role;
        }
    }

    /**
     * 设置当前用户信息
     */
    public static void setUserInfo(Integer userId, String userName, Integer role) {
        userInfoHolder.set(new UserInfo(userId, userName, role));
    }

    /**
     * 获取当前用户 ID
     */
    public static Integer getCurrentUserId() {
        return Optional.ofNullable(userInfoHolder.get())
                .map(UserInfo::getUserId)
                .orElse(null);
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName() {
        return Optional.ofNullable(userInfoHolder.get())
                .map(UserInfo::getUserName)
                .orElse(null);
    }

    /**
     * 获取当前用户角色
     */
    public static Integer getCurrentUserRole() {
        return Optional.ofNullable(userInfoHolder.get())
                .map(UserInfo::getRole)
                .orElse(null);
    }

    /**
     * 获取当前用户信息
     */
    public static UserInfo getCurrentUserInfo() {
        return userInfoHolder.get();
    }

    /**
     * 清除当前用户信息（防止内存泄漏）
     */
    public static void clear() {
        userInfoHolder.remove();
    }
}
