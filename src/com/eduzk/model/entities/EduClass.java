package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EduClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private int classId;
    private String className;
    private Course course;
    private Teacher primaryTeacher;
    private int maxCapacity;
    private String academicYear;
    private String semester;
    private List<Integer> studentIds;
    private int currentEnrollment;

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
        this.currentEnrollment = 0;
    }

    public int getCurrentEnrollment() {
        return currentEnrollment;
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
        return new ArrayList<>(studentIds);
    }

    public void setStudentIds(List<Integer> studentIds) {
        this.studentIds = (studentIds != null) ? new ArrayList<>(studentIds) : new ArrayList<>();
        this.currentEnrollment = this.studentIds.size();
    }

    public void addStudentId(int studentId) {
        if (!this.studentIds.contains(studentId)) {
            this.studentIds.add(studentId);
            this.currentEnrollment = this.studentIds.size();
        }
    }

    public boolean removeStudentId(int studentId) {
        boolean removed = false;
        if (this.studentIds != null) {
            removed = this.studentIds.remove(Integer.valueOf(studentId));
            if (removed) {
                this.currentEnrollment = this.studentIds.size();
            }
        }
        return removed;
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
        return className + " (" + (course != null ? course.getCourseCode() : "N/A") + ")";
    }

    public void setCurrentEnrollment(int currentEnrollment) {
        if (currentEnrollment < 0) {
            System.err.println("Warning: Attempted to set negative current enrollment for class ID " + classId);
            this.currentEnrollment = 0;
        } else if (currentEnrollment > this.maxCapacity) {
            System.err.println("Warning: Attempted to set current enrollment (" + currentEnrollment +
                    ") exceeding max capacity (" + this.maxCapacity + ") for class ID " + classId);
            this.currentEnrollment = this.maxCapacity;
        } else {
            this.currentEnrollment = currentEnrollment;
        }
    }
}