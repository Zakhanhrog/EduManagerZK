package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EduClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private int classId;
    private String className; // E.g., "Java Beginners - Fall 2024", "TOEIC Prep Evening"
    private Course course; // Reference to the Course entity
    private Teacher primaryTeacher; // Reference to the main Teacher entity
    private int maxCapacity;
    private String academicYear; // E.g., "2024-2025"
    private String semester; // E.g., "Fall", "Spring", "1", "2"
    private List<Integer> studentIds; // List of enrolled Student IDs

    public EduClass() {
        this.studentIds = new ArrayList<>();
    }

    public EduClass(int classId, String className, Course course, Teacher primaryTeacher, int maxCapacity, String academicYear, String semester) {
        this.classId = classId;
        this.className = className;
        this.course = course;
        this.primaryTeacher = primaryTeacher;
        this.maxCapacity = maxCapacity;
        this.academicYear = academicYear;
        this.semester = semester;
        this.studentIds = new ArrayList<>();
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Teacher getPrimaryTeacher() {
        return primaryTeacher;
    }

    public void setPrimaryTeacher(Teacher primaryTeacher) {
        this.primaryTeacher = primaryTeacher;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public List<Integer> getStudentIds() {
        // Return a copy to prevent external modification of the internal list
        return new ArrayList<>(studentIds);
    }

    public void setStudentIds(List<Integer> studentIds) {
        this.studentIds = (studentIds != null) ? new ArrayList<>(studentIds) : new ArrayList<>();
    }

    public void addStudentId(int studentId) {
        if (!this.studentIds.contains(studentId)) {
            this.studentIds.add(studentId);
        }
    }

    public void removeStudentId(int studentId) {
        this.studentIds.remove(Integer.valueOf(studentId));
    }

    public int getCurrentEnrollment() {
        return this.studentIds.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EduClass eduClass = (EduClass) o;
        return classId == eduClass.classId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId);
    }

    @Override
    public String toString() {
        // Concise representation for display
        return className + " (" + (course != null ? course.getCourseCode() : "N/A") + ")";
        // Or a more detailed version:
        // return "EduClass{" +
        //        "classId=" + classId +
        //        ", className='" + className + '\'' +
        //        ", course=" + (course != null ? course.getCourseName() : "null") +
        //        ", primaryTeacher=" + (primaryTeacher != null ? primaryTeacher.getFullName() : "null") +
        //        ", maxCapacity=" + maxCapacity +
        //        ", currentEnrollment=" + getCurrentEnrollment() +
        //        '}';
    }
}