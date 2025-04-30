package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.ICourseDAO;
import com.eduzk.model.entities.Course;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.CoursePanel; // To update the panel's table

import java.util.Collections;
import java.util.List;

public class CourseController {

    private final ICourseDAO courseDAO;
    private final User currentUser;
    private CoursePanel coursePanel;

    public CourseController(ICourseDAO courseDAO, User currentUser) {
        this.courseDAO = courseDAO;
        this.currentUser = currentUser;
    }

    public void setCoursePanel(CoursePanel coursePanel) {
        this.coursePanel = coursePanel;
    }

    public List<Course> getAllCourses() {
        try {
            return courseDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading courses: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to load course data.");
            return Collections.emptyList();
        }
    }

    public List<Course> searchCoursesByName(String name) {
        if (!ValidationUtils.isNotEmpty(name)) {
            return getAllCourses(); // Return all if search is empty
        }
        try {
            return courseDAO.findByName(name);
        } catch (DataAccessException e) {
            System.err.println("Error searching courses: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to search courses.");
            return Collections.emptyList();
        }
    }

    public boolean addCourse(Course course) {
        if (course == null || !ValidationUtils.isNotEmpty(course.getCourseCode()) || !ValidationUtils.isNotEmpty(course.getCourseName())) {
            UIUtils.showWarningMessage(coursePanel, "Validation Error", "Course code and name cannot be empty.");
            return false;
        }
        // Add more validation (credits, level)

        try {
            courseDAO.add(course);
            if (coursePanel != null) {
                coursePanel.refreshTable(); // Assumes CoursePanel has this method
                UIUtils.showInfoMessage(coursePanel, "Success", "Course added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding course: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to add course: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCourse(Course course) {
        if (course == null || course.getCourseId() <= 0 || !ValidationUtils.isNotEmpty(course.getCourseCode()) || !ValidationUtils.isNotEmpty(course.getCourseName())) {
            UIUtils.showWarningMessage(coursePanel, "Validation Error", "Invalid course data for update.");
            return false;
        }
        // Add more validation...

        try {
            courseDAO.update(course);
            if (coursePanel != null) {
                coursePanel.refreshTable();
                UIUtils.showInfoMessage(coursePanel, "Success", "Course updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating course: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to update course: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCourse(int courseId) {
        if (courseId <= 0) {
            UIUtils.showWarningMessage(coursePanel, "Error", "Invalid course ID for deletion.");
            return false;
        }
        // Confirmation dialog in View layer
        try {
            courseDAO.delete(courseId);
            if (coursePanel != null) {
                coursePanel.refreshTable();
                UIUtils.showInfoMessage(coursePanel, "Success", "Course deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Handle specific errors, e.g., course used in classes
            System.err.println("Error deleting course: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to delete course: " + e.getMessage());
            return false;
        }
    }

    public Course getCourseById(int courseId) {
        if (courseId <= 0) return null;
        try {
            return courseDAO.getById(courseId);
        } catch (DataAccessException e) {
            System.err.println("Error getting course by ID: " + e.getMessage());
            return null;
        }
    }
}