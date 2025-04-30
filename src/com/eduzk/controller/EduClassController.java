package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.ICourseDAO;
import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.ClassPanel; // To update the panel's table

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EduClassController {

    private final IEduClassDAO eduClassDAO;
    private final ICourseDAO courseDAO; // Needed for Course selection
    private final ITeacherDAO teacherDAO; // Needed for Teacher selection
    private final IStudentDAO studentDAO; // Needed for Student enrollment list
    private ClassPanel classPanel;
    private User currentUser;

    public EduClassController(IEduClassDAO eduClassDAO, ICourseDAO courseDAO, ITeacherDAO teacherDAO, IStudentDAO studentDAO, User currentUser) {
        this.eduClassDAO = eduClassDAO;
        this.courseDAO = courseDAO;
        this.teacherDAO = teacherDAO;
        this.studentDAO = studentDAO;
        this.currentUser = currentUser;
    }

    public void setClassPanel(ClassPanel classPanel) {
        this.classPanel = classPanel;
    }

    public List<EduClass> getAllEduClasses() {
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser); // Gọi hàm helper
                if (teacherId > 0) {
                    System.out.println("EduClassController: Filtering classes for Teacher ID: " + teacherId);
                    // Gọi DAO để lọc theo teacherId
                    return eduClassDAO.findByTeacherId(teacherId);
                } else {
                    System.err.println("EduClassController: Could not determine Teacher ID for logged in user. Returning empty class list.");
                    return Collections.emptyList();
                }
            } else { // Admin hoặc vai trò khác
                System.out.println("EduClassController: Getting all classes for Admin/Other.");
                // Lấy tất cả các lớp
                return eduClassDAO.getAll();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading classes: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to load class data.");
            return Collections.emptyList();
        }
    }

    private int getTeacherIdForUser(User user) {
        if (user != null && user.getRole() == Role.TEACHER && user.getTeacherId() != null) {
            return user.getTeacherId();
        }
        return -1;
    }

    public List<Course> getAllCoursesForSelection() {
        try {
            return courseDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading courses for selection: " + e.getMessage());
            // Don't necessarily show error to user here, maybe just log
            return Collections.emptyList();
        }
    }

    public List<Teacher> getAllTeachersForSelection() {
        try {
            // Maybe filter only active teachers
            // return teacherDAO.getAll().stream().filter(Teacher::isActive).collect(Collectors.toList());
            return teacherDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading teachers for selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Student> getEnrolledStudents(int classId) {
        if (classId <= 0) return Collections.emptyList();
        try {
            EduClass eduClass = eduClassDAO.getById(classId);
            if (eduClass != null) {
                List<Integer> studentIds = eduClass.getStudentIds();
                if (studentIds.isEmpty()) return Collections.emptyList();
                // Fetch full student objects - potential performance issue if many students/calls
                // Consider optimizing if necessary (e.g., DAO method to get students by IDs)
                return studentIds.stream()
                        .map(studentDAO::getById)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (DataAccessException e) {
            System.err.println("Error loading enrolled students for class ID " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to load enrolled students.");
            return Collections.emptyList();
        }
    }

    public List<Student> getAvailableStudentsForEnrollment(int classId) {
        try {
            List<Student> allStudents = studentDAO.getAll();
            EduClass currentClass = eduClassDAO.getById(classId);
            if (currentClass != null) {
                List<Integer> enrolledIds = currentClass.getStudentIds();
                return allStudents.stream()
                        .filter(s -> !enrolledIds.contains(s.getStudentId()))
                        .collect(Collectors.toList());
            }
            return allStudents; // If class doesn't exist yet, all are available
        } catch (DataAccessException e) {
            System.err.println("Error loading available students: " + e.getMessage());
            return Collections.emptyList();
        }
    }


    public boolean addEduClass(EduClass eduClass) {
        if (eduClass == null || !ValidationUtils.isNotEmpty(eduClass.getClassName()) ||
                eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null || eduClass.getMaxCapacity() <= 0) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Class name, course, teacher, and positive capacity are required.");
            return false;
        }

        try {
            eduClassDAO.add(eduClass);
            if (classPanel != null) {
                classPanel.refreshTable(); // Assumes ClassPanel has this method
                UIUtils.showInfoMessage(classPanel, "Success", "Class added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to add class: " + e.getMessage());
            return false;
        }
    }

    public boolean updateEduClass(EduClass eduClass) {
        if (eduClass == null || eduClass.getClassId() <= 0 || !ValidationUtils.isNotEmpty(eduClass.getClassName()) ||
                eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null || eduClass.getMaxCapacity() <= 0) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Invalid class data for update.");
            return false;
        }

        try {
            // Check capacity constraint before calling DAO update
            EduClass existingClass = eduClassDAO.getById(eduClass.getClassId());
            if (existingClass != null && existingClass.getCurrentEnrollment() > eduClass.getMaxCapacity()) {
                UIUtils.showErrorMessage(classPanel, "Error", "Cannot update class. New maximum capacity ("
                        + eduClass.getMaxCapacity() + ") is less than current enrollment ("
                        + existingClass.getCurrentEnrollment() + ").");
                return false;
            }

            eduClassDAO.update(eduClass);
            if (classPanel != null) {
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Class updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to update class: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEduClass(int classId) {
        if (classId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class ID for deletion.");
            return false;
        }
        // Confirmation dialog in View layer
        try {
            // Check enrollment before deleting (DAO might also do this, belt and suspenders)
            EduClass classToDelete = eduClassDAO.getById(classId);
            if (classToDelete != null && classToDelete.getCurrentEnrollment() > 0) {
                UIUtils.showErrorMessage(classPanel, "Deletion Failed", "Cannot delete class. There are still students enrolled.");
                return false;
            }
            // Also check for associated schedules?

            eduClassDAO.delete(classId);
            if (classPanel != null) {
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Class deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Handle specific errors
            System.err.println("Error deleting class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to delete class: " + e.getMessage());
            return false;
        }
    }

    public boolean enrollStudent(int classId, int studentId) {
        if (classId <= 0 || studentId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student ID for enrollment.");
            return false;
        }
        try {
            eduClassDAO.addStudentToClass(classId, studentId);
            // Refresh student list in the view if applicable
            if(classPanel != null) {
                classPanel.refreshStudentListForSelectedClass(); // Assumes panel has this method
                UIUtils.showInfoMessage(classPanel, "Success", "Student enrolled successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error enrolling student: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Enrollment Failed", "Failed to enroll student: " + e.getMessage());
            return false;
        }
    }

    public boolean unenrollStudent(int classId, int studentId) {
        if (classId <= 0 || studentId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student ID for unenrolling.");
            return false;
        }
        try {
            eduClassDAO.removeStudentFromClass(classId, studentId);
            // Refresh student list in the view if applicable
            if(classPanel != null) {
                classPanel.refreshStudentListForSelectedClass();
                UIUtils.showInfoMessage(classPanel, "Success", "Student unenrolled successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error unenrolling student: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Unenrollment Failed", "Failed to unenroll student: " + e.getMessage());
            return false;
        }
    }

    public EduClass getEduClassById(int classId) {
        if (classId <= 0) return null;
        try {
            return eduClassDAO.getById(classId);
        } catch (DataAccessException e) {
            System.err.println("Error getting class by ID: " + e.getMessage());
            return null;
        }
    }
}