package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.Objects;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private int courseId;
    private String courseCode; // E.g., ENG101, CS202
    private String courseName; // E.g., "Introduction to Programming", "Business English"
    private String description;
    private int credits; // Or duration, or some measure of size/effort
    private String level; // E.g., "Beginner", "Intermediate", "Advanced" or "Grade 10"

    public Course() {
    }

    public Course(int courseId, String courseCode, String courseName, String description, int credits, String level) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.credits = credits;
        this.level = level;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return courseId == course.courseId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }

    @Override
    public String toString() {
        // Often used in ComboBoxes, so a concise representation is useful
        return courseCode + " - " + courseName;
        // Or a more detailed version:
        // return "Course{" +
        //        "courseId=" + courseId +
        //        ", courseCode='" + courseCode + '\'' +
        //        ", courseName='" + courseName + '\'' +
        //        ", level='" + level + '\'' +
        //        '}';
    }
}