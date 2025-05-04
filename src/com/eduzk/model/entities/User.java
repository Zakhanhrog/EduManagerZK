package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String password;
    private Role role;
    private boolean active;
    private Integer teacherId;
    private Integer studentId;
    private transient String displayName;
    private boolean requiresPasswordChange;

    public String getDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : username;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public User() {
        this.active = true;
        this.requiresPasswordChange = true;
    }

    public User(int userId, String username, String password, Role role, Integer teacherId, Integer studentId) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = true;
        this.teacherId = teacherId;
        this.studentId = studentId;
        this.requiresPasswordChange = false;
    }
    public boolean isRequiresPasswordChange() {
        return requiresPasswordChange;
    }
    public void setRequiresPasswordChange(boolean requiresPasswordChange) {
        this.requiresPasswordChange = requiresPasswordChange;
    }
    public Integer getTeacherId() {
        return teacherId;
    }
    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", teacherId=" + teacherId +
                ", studentId=" + studentId +
                ", requiresPasswordChange=" + requiresPasswordChange +
                '}';
    }
}