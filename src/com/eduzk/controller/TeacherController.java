package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.TeacherPanel; // To update the panel's table


import java.util.Collections;
import java.util.List;

public class TeacherController {

    private final ITeacherDAO teacherDAO;
    private final User currentUser;
    private TeacherPanel teacherPanel;

    public TeacherController(ITeacherDAO teacherDAO, User currentUser) {
        this.teacherDAO = teacherDAO;
        this.currentUser = currentUser;
    }

    public void setTeacherPanel(TeacherPanel teacherPanel) {
        this.teacherPanel = teacherPanel;
    }

    public List<Teacher> getAllTeachers() {
        try {
            return teacherDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading teachers: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to load teacher data.");
            return Collections.emptyList();
        }
    }

    public List<Teacher> searchTeachersBySpecialization(String specialization) {
        if (!ValidationUtils.isNotEmpty(specialization)) {
            return getAllTeachers(); // Return all if search is empty
        }
        try {
            return teacherDAO.findBySpecialization(specialization);
        } catch (DataAccessException e) {
            System.err.println("Error searching teachers: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to search teachers.");
            return Collections.emptyList();
        }
    }

    public boolean addTeacher(Teacher teacher) {
        if (teacher == null || !ValidationUtils.isNotEmpty(teacher.getFullName())) {
            UIUtils.showWarningMessage(teacherPanel, "Validation Error", "Teacher name cannot be empty.");
            return false;
        }
        // Add more validation (email, phone, specialization)

        try {
            teacherDAO.add(teacher);
            if (teacherPanel != null) {
                teacherPanel.refreshTable(); // Assumes TeacherPanel has this method
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to add teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTeacher(Teacher teacher) {
        if (teacher == null || teacher.getTeacherId() <= 0 || !ValidationUtils.isNotEmpty(teacher.getFullName())) {
            UIUtils.showWarningMessage(teacherPanel, "Validation Error", "Invalid teacher data for update.");
            return false;
        }
        // Add more validation...

        try {
            teacherDAO.update(teacher);
            if (teacherPanel != null) {
                teacherPanel.refreshTable();
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to update teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTeacher(int teacherId) {
        if (teacherId <= 0) {
            UIUtils.showWarningMessage(teacherPanel, "Error", "Invalid teacher ID for deletion.");
            return false;
        }
        // Confirmation dialog in View layer
        try {
            teacherDAO.delete(teacherId);
            if (teacherPanel != null) {
                teacherPanel.refreshTable();
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Handle specific errors, e.g., teacher assigned to classes
            System.err.println("Error deleting teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to delete teacher: " + e.getMessage());
            return false;
        }
    }

    public Teacher getTeacherById(int teacherId) {
        if (teacherId <= 0) return null;
        try {
            return teacherDAO.getById(teacherId);
        } catch (DataAccessException e) {
            System.err.println("Error getting teacher by ID: " + e.getMessage());
            return null;
        }
    }
}