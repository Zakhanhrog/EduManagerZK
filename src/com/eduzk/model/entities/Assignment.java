// src/com/eduzk/model/entities/Assignment.java
package com.eduzk.model.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int assignmentId;
    private int eduClassId;
    private String title;
    private String description;
    private LocalDateTime dueDateTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Assignment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Assignment(int assignmentId, int eduClassId, String title, String description, LocalDateTime dueDateTime) {
        this.assignmentId = assignmentId;
        this.eduClassId = eduClassId;
        this.title = title;
        this.description = description;
        this.dueDateTime = dueDateTime;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getEduClassId() {
        return eduClassId;
    }

    public void setEduClassId(int eduClassId) {
        this.eduClassId = eduClassId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }
    public void setDueDateTime(LocalDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() { // Gọi khi có cập nhật
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        if (this.dueDateTime == null) {
            return false; // Không có hạn, không bao giờ quá hạn
        }
        return LocalDateTime.now().isAfter(this.dueDateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return assignmentId == that.assignmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "assignmentId=" + assignmentId +
                ", eduClassId=" + eduClassId +
                ", title='" + title + '\'' +
                ", dueDateTime=" + dueDateTime +
                '}';
    }
}