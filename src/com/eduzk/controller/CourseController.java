package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.ICourseDAO;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.panels.CoursePanel;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class CourseController {

    private final ICourseDAO courseDAO;
    private final User currentUser;
    private final LogService logService;
    private CoursePanel coursePanel;

    public CourseController(ICourseDAO courseDAO, User currentUser, LogService logService) {
        this.courseDAO = courseDAO;
        this.currentUser = currentUser;
        this.logService = logService;
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
            return getAllCourses();
        }
        try {
            return courseDAO.findByName(name);
        } catch (DataAccessException e) {
            System.err.println("Error searching courses: " + e.getMessage());
            UIUtils.showErrorMessage(coursePanel, "Error", "Failed to search courses.");
            return Collections.emptyList();
        }
    }

    // --- SỬA addCourse ĐỂ GHI LOG ---
    public boolean addCourse(Course course) {
        if (course == null || !ValidationUtils.isNotEmpty(course.getCourseCode()) || !ValidationUtils.isNotEmpty(course.getCourseName())) {
            UIUtils.showWarningMessage(coursePanel, "Validation Error", "Course code and name cannot be empty.");
            return false;
        }

        try {
            courseDAO.add(course);
            writeAddLog("Added Course", course);

            if (coursePanel != null) {
                coursePanel.refreshTable();
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
        try {
            courseDAO.update(course);
            writeUpdateLog("Updated Course", course);

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
        Course courseToDelete = null;
        try {
            courseToDelete = courseDAO.getById(courseId);
        } catch (DataAccessException e) {
            System.err.println("Error finding course to delete (for logging): " + e.getMessage());
        }
        String courseInfoForLog = (courseToDelete != null)
                ? ("ID: " + courseId + ", Code: " + courseToDelete.getCourseCode() + ", Name: " + courseToDelete.getCourseName())
                : ("ID: " + courseId);

        try {
            courseDAO.delete(courseId);

            writeDeleteLog("Deleted Course", courseInfoForLog);

            if (coursePanel != null) {
                coursePanel.refreshTable();
                UIUtils.showInfoMessage(coursePanel, "Success", "Course deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
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

    private void writeAddLog(String action, Course course) {
        writeLog(action, "ID: " + course.getCourseId() + ", Code: " + course.getCourseCode() + ", Name: " + course.getCourseName());
    }

    private void writeUpdateLog(String action, Course course) {
        writeLog(action, "ID: " + course.getCourseId() + ", Code: " + course.getCourseCode() + ", Name: " + course.getCourseName());
    }

    private void writeDeleteLog(String action, String details) {
        writeLog(action, details);
    }

    // Hàm ghi log chung
    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(),
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: " + action + " - " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }
}