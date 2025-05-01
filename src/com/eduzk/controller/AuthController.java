package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.entities.Student;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.LoginView;
import com.eduzk.view.MainView;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.util.Optional;

public class AuthController {
    private final IUserDAO userDAO;
    private ITeacherDAO teacherDAO;
    private IStudentDAO studentDAO;
    private LoginView loginView;
    private User loggedInUser;

    public AuthController(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setTeacherDAO(ITeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    public void setStudentDAO(IStudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }

    public void attemptLogin(String username, String password) {
        // Validation cơ bản đầu vào
        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            UIUtils.showWarningMessage(loginView, "Login Failed", "Username/Phone and password cannot be empty.");
            return;
        }
        try {
            // Tìm user bằng username (hoặc SĐT đã đăng ký làm username)
            Optional<User> userOptional = userDAO.findByUsername(username.trim());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPassword().equals(password)) {
                    if (user.isActive()) {
                        this.loggedInUser = user;
                        loginSuccess();
                    } else {
                        UIUtils.showErrorMessage(loginView, "Login Failed", "User account is inactive. Please contact administrator.");
                    }
                } else {
                    UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
                }
            } else {
                UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
            }
        } catch (DataAccessException e) {
            System.err.println("Login DAO Error: " + e.getMessage());
            e.printStackTrace(); // In stack trace ra console để debug
            UIUtils.showErrorMessage(loginView, "Login Error", "A data access error occurred. Please try again later or contact support.");
        } catch (Exception e) {
            System.err.println("Unexpected Login Error: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(loginView, "Login Error", "An unexpected error occurred during login.");
        }
    }

    private void loginSuccess() {
        System.out.println("AuthController: Login successful. Closing LoginView and opening MainView...");
        if (loginView != null) {
            loginView.dispose();
            loginView = null;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                MainController mainController = new MainController(loggedInUser, this); // <-- SỬA Ở ĐÂY
                MainView mainView = new MainView(mainController);
                mainController.setMainView(mainView);
                mainView.setVisible(true);
                System.out.println("AuthController: MainView opened.");
            } catch (Exception e) {
                System.err.println("!!! CRITICAL ERROR OPENING MAIN VIEW !!!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to open the main application window.\nError: " + e.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
                showLoginView();
            }
        });
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void logout() {
        System.out.println("AuthController: Clearing loggedInUser session.");
        this.loggedInUser = null;
    }

    public boolean attemptRegistration(String username, String password, String confirmPassword, Role role, Integer teacherIdInput) {
        System.out.println("Attempting registration - Username/Phone: " + username + ", Role: " + role + ", Input TeacherID: " + teacherIdInput);

        // --- 1. Validation đầu vào cơ bản ---
        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            UIUtils.showWarningMessage(null, "Registration Failed", "Username/Phone and password cannot be empty.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            UIUtils.showWarningMessage(null, "Registration Failed", "Passwords do not match.");
            return false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            UIUtils.showWarningMessage(null, "Registration Failed", "Password must be at least 6 characters long.");
            return false;
        }
        if (role == Role.ADMIN) {
            UIUtils.showWarningMessage(null, "Registration Failed", "Cannot register as Administrator.");
            return false;
        }
        if (role == null) {
            UIUtils.showWarningMessage(null, "Registration Failed", "Please select a role (Student or Teacher).");
            return false;
        }


        try {
            // --- 2. Kiểm tra Username (hoặc SĐT) đã tồn tại trong bảng User chưa ---
            System.out.println("Checking if username/phone '" + username + "' already exists as a User account...");
            if (userDAO.findByUsername(username).isPresent()) {
                UIUtils.showWarningMessage(null, "Registration Failed", "Username or Phone Number '" + username + "' is already registered. Please try logging in or use a different one.");
                return false;
            }
            System.out.println("Username/phone '" + username + "' is available.");


            Integer studentIdToLink = null;
            Integer teacherIdToLink = null;

            // --- 3. Validation và Logic riêng theo từng Role ---
            if (role == Role.STUDENT) {
                System.out.println("Processing STUDENT registration...");
                // Validation: Username phải là SĐT hợp lệ
                if (!ValidationUtils.isValidPhoneNumber(username)) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "A valid phone number is required as username for student registration.");
                    return false;
                }

                // Kiểm tra sự tồn tại của Student bằng SĐT
                if (this.studentDAO == null) {
                    throw new IllegalStateException("StudentDAO is not initialized in AuthController.");
                }
                System.out.println("Finding student by phone: " + username);
                Optional<Student> studentOpt = studentDAO.findByPhone(username);
                if (studentOpt.isEmpty()) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "No student record found with phone number: " + username + ". Please contact administrator to add your profile first.");
                    return false;
                }
                Student existingStudent = studentOpt.get();
                studentIdToLink = existingStudent.getStudentId();
                System.out.println("Found student ID: " + studentIdToLink + " for phone: " + username);

                // Kiểm tra xem studentId này đã có tài khoản User nào liên kết chưa
                System.out.println("Checking if student ID " + studentIdToLink + " already has an account...");
                if (userDAO.findByStudentId(studentIdToLink).isPresent()) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "An account already exists for the student with this phone number (Student ID: " + studentIdToLink + ").");
                    return false;
                }
                System.out.println("No existing account found for student ID " + studentIdToLink + ".");


            } else if (role == Role.TEACHER) {
                System.out.println("Processing TEACHER registration...");
                // Validation: Username phải là username hợp lệ
                if (!ValidationUtils.isValidUsername(username)) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "Invalid username format for teacher (3-20 alphanumeric/underscore).");
                    return false;
                }

                // Validation: Teacher ID phải được cung cấp và hợp lệ
                if (teacherIdInput == null || teacherIdInput <= 0) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "A valid Teacher ID (provided by Admin) is required.");
                    return false;
                }

                // Kiểm tra sự tồn tại của Teacher bằng ID
                if (this.teacherDAO == null) {
                    throw new IllegalStateException("TeacherDAO is not initialized in AuthController.");
                } // Lỗi cấu hình
                System.out.println("Finding teacher by ID: " + teacherIdInput);
                Teacher existingTeacher = teacherDAO.getById(teacherIdInput);
                if (existingTeacher == null) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "Teacher with ID " + teacherIdInput + " not found. Please verify the ID with the administrator.");
                    return false;
                }
                teacherIdToLink = teacherIdInput;
                System.out.println("Found teacher: " + existingTeacher.getFullName() + " for ID: " + teacherIdInput);
            }

            System.out.println("All checks passed. Creating new User object...");
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setRole(role);
            newUser.setActive(true);
            newUser.setTeacherId(teacherIdToLink);
            newUser.setStudentId(studentIdToLink);
            System.out.println("Adding new user to database: " + newUser);
            userDAO.add(newUser);
            System.out.println("New user added successfully with ID: " + newUser.getUserId());

            return true;

        } catch (DataAccessException e) {
            System.err.println("Registration DAO Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Registration Failed", "Could not save registration data: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            System.err.println("Registration Argument Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Registration Failed", "Invalid data provided: " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            System.err.println("Registration Configuration Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Configuration Error", "Application configuration error. Please contact support.");
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected Registration Error: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(null, "Registration Error", "An unexpected error occurred during registration. Please try again.");
            return false;
        }
    }

    public void showLoginView() {
        System.out.println("AuthController: showLoginView() called.");
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("AuthController: Creating new LoginView instance...");
                LoginView newLoginView = new LoginView(this);
                this.loginView = newLoginView;
                System.out.println("AuthController: Setting new LoginView visible...");
                newLoginView.setVisible(true);
                System.out.println("AuthController: New LoginView should be visible.");
            } catch (Exception e) {
                System.err.println("!!! CRITICAL ERROR SHOWING LOGIN VIEW !!!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to return to the login screen.\nError: " + e.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}