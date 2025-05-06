// src/com/eduzk/model/entities/Assignment.java
package com.eduzk.model.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int assignmentId;
    private int eduClassId; // Liên kết với lớp học
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Assignment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Assignment(int assignmentId, int eduClassId, String title, String description, LocalDate dueDate) {
        this.assignmentId = assignmentId;
        this.eduClassId = eduClassId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setter không cần thiết cho createdAt

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // --- Logic cập nhật thời gian ---
    public void touch() { // Gọi khi có cập nhật
        this.updatedAt = LocalDateTime.now();
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
                ", dueDate=" + dueDate +
                '}';
    }
}