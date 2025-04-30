package com.eduzk.controller;

import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils; // For showing messages
import com.eduzk.view.panels.StudentPanel; // To update the panel's table
import com.eduzk.model.dao.interfaces.IUserDAO; // <-- THÊM IMPORT
import com.eduzk.model.entities.User;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentController {

    private final IStudentDAO studentDAO;
    private final User currentUser;
    private final IEduClassDAO eduClassDAO;
    private StudentPanel studentPanel; // Reference to the view panel
    private final IUserDAO userDAO;

    public StudentController(IStudentDAO studentDAO, IEduClassDAO eduClassDAO, IUserDAO userDAO, User currentUser) {
        this.studentDAO = studentDAO;
        this.currentUser = currentUser;
        this.eduClassDAO = eduClassDAO;
        this.userDAO = userDAO;

    }

    public void setStudentPanel(StudentPanel studentPanel) {
        this.studentPanel = studentPanel;
    }

    public List<Student> getAllStudents() {
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser); // Gọi hàm helper
                if (teacherId > 0) {
                    System.out.println("StudentController: Filtering students for Teacher ID: " + teacherId);

                    // --- PHẦN LOGIC PHỨC TẠP ---
                    // Kiểm tra xem eduClassDAO đã được inject chưa
                    if (this.eduClassDAO == null) { // Giả sử tên biến là eduClassDAO
                        System.err.println("StudentController: EduClassDAO is required for filtering students but is null!");
                        return Collections.emptyList();
                    }

                    try {
                        // 1. Lấy các lớp của giáo viên
                        List<EduClass> teacherClasses = eduClassDAO.findByTeacherId(teacherId);
                        if (teacherClasses.isEmpty()) {
                            return Collections.emptyList(); // Giáo viên không có lớp nào
                        }

                        // 2. Lấy ID của tất cả học viên từ các lớp đó, loại bỏ trùng lặp
                        List<Integer> studentIds = teacherClasses.stream()
                                .flatMap(eduClass -> eduClass.getStudentIds().stream())
                                .distinct()
                                .collect(Collectors.toList());

                        if (studentIds.isEmpty()) {
                            return Collections.emptyList(); // Các lớp không có học viên nào
                        }

                        // 3. Lấy thông tin chi tiết các học viên từ StudentDAO
                        // Tạm thời dùng getById lặp lại (kém hiệu quả)
                        // Nên tối ưu bằng cách thêm findByIds vào StudentDAO sau này
                        System.out.println("StudentController: Fetching details for student IDs: " + studentIds);
                        return studentIds.stream()
                                .map(studentId -> studentDAO.getById(studentId)) // Gọi getById cho từng ID
                                .filter(Objects::nonNull) // Bỏ qua nếu student không tìm thấy (dữ liệu không nhất quán?)
                                .collect(Collectors.toList());

                    } catch (DataAccessException daoEx) {
                        System.err.println("StudentController: Error accessing data while filtering students for teacher: " + daoEx.getMessage());
                        UIUtils.showErrorMessage(studentPanel, "Error", "Could not load student data for your classes.");
                        return Collections.emptyList();
                    }
                } else {
                    System.err.println("StudentController: Could not determine Teacher ID for logged in user. Returning empty student list.");
                    return Collections.emptyList();
                }
            } else { // Admin hoặc vai trò khác
                System.out.println("StudentController: Getting all students for Admin/Other.");
                // Lấy tất cả học viên
                return studentDAO.getAll();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading students: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to load student data.");
            return Collections.emptyList(); // Return empty list on error
        }
    }

    private int getTeacherIdForUser(User user) {
        if (user != null && user.getRole() == Role.TEACHER && user.getTeacherId() != null) {
            return user.getTeacherId();
        }
        return -1;
    }

    public List<Student> searchStudentsByName(String name) {
        if (!ValidationUtils.isNotEmpty(name)) {
            return getAllStudents(); // Return all if search is empty
        }
        try {
            return studentDAO.findByName(name);
        } catch (DataAccessException e) {
            System.err.println("Error searching students: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to search students.");
            return Collections.emptyList();
        }
    }


    public boolean addStudent(Student student) {
        // Basic validation (more specific validation might happen in the dialog/panel before calling this)
        if (student == null || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Student name cannot be empty.");
            return false;
        }
        // Add more validation for other fields as needed (DOB, contact, etc.)

        try {
            studentDAO.add(student);
            // Optionally refresh the view if the panel is set
            if (studentPanel != null) {
                System.out.println("StudentController: Add successful, refreshing panel...");
                studentPanel.refreshTable(); // Assumes StudentPanel has this method
                UIUtils.showInfoMessage(studentPanel, "Success", "Student added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to add student: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudent(Student student) {
        if (student == null || student.getStudentId() <= 0 || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Invalid student data for update.");
            return false;
        }
        // Add more validation...

        try {
            studentDAO.update(student);
            if (studentPanel != null) {
                studentPanel.refreshTable();
                UIUtils.showInfoMessage(studentPanel, "Success", "Student updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to update student: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudent(int studentId) {
        if (studentId <= 0) {
            UIUtils.showWarningMessage(studentPanel, "Error", "Invalid student ID for deletion.");
            return false;
        }
        // Confirmation dialog should be shown in the View layer before calling this
        try {
            studentDAO.delete(studentId);
            if (studentPanel != null) {
                studentPanel.refreshTable();
                UIUtils.showInfoMessage(studentPanel, "Success", "Student deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Potentially handle specific errors, e.g., student enrolled in classes
            System.err.println("Error deleting student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to delete student: " + e.getMessage());
            return false;
        }
    }

    public Student getStudentById(int studentId) {
        if (studentId <= 0) return null;
        try {
            return studentDAO.getById(studentId);
        } catch (DataAccessException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
            // Don't usually show UI message for simple get, just log error
            return null;
        }
    }
    public String getPasswordForStudent(int studentId) {
        if (userDAO == null) return null; // Chưa inject DAO
        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            // !!! TRẢ VỀ PLAIN TEXT - RỦI RO BẢO MẬT !!!
            return userOpt.get().getPassword();
        }
        return null; // Không tìm thấy User liên kết
    }
    // --- THÊM PHƯƠNG THỨC CẬP NHẬT PASSWORD ---
    public boolean updatePasswordForStudent(int studentId, String newPassword) {
        if (userDAO == null) { System.err.println("UserDAO not injected in StudentController!"); return false; }
        if (!ValidationUtils.isValidPassword(newPassword)) { UIUtils.showWarningMessage(studentPanel, "Error", "New password is too short."); return false;}

        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            User userToUpdate = userOpt.get();
            // !!! CẬP NHẬT PLAIN TEXT - RỦI RO BẢO MẬT !!!
            userToUpdate.setPassword(newPassword);
            try {
                userDAO.update(userToUpdate);
                UIUtils.showInfoMessage(studentPanel, "Success", "Password updated for student ID " + studentId);
                return true;
            } catch (DataAccessException e) {
                System.err.println("Error updating user password: " + e);
                UIUtils.showErrorMessage(studentPanel, "Error", "Could not update password: " + e.getMessage());
                return false;
            }
        } else {
            UIUtils.showWarningMessage(studentPanel, "Error", "Could not find user account for student ID " + studentId + " to update password.");
            return false;
        }
    }
    public boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }
}