package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
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
    private ICourseDAO courseDAO;
    private IRoomDAO roomDAO;
    private IEduClassDAO eduClassDAO;
    private IScheduleDAO scheduleDAO;
    private LoginView loginView;
    private User loggedInUser;

    public AuthController(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setTeacherDAO(ITeacherDAO teacherDAO) { this.teacherDAO = teacherDAO; }
    public void setStudentDAO(IStudentDAO studentDAO) { this.studentDAO = studentDAO; }
    public void setCourseDAO(ICourseDAO courseDAO) { this.courseDAO = courseDAO; }
    public void setRoomDAO(IRoomDAO roomDAO) { this.roomDAO = roomDAO; }
    public void setEduClassDAO(IEduClassDAO eduClassDAO) { this.eduClassDAO = eduClassDAO; }
    public void setScheduleDAO(IScheduleDAO scheduleDAO) { this.scheduleDAO = scheduleDAO; }

    public IUserDAO getUserDAO() { return userDAO; }
    public ITeacherDAO getTeacherDAO() { return teacherDAO; }
    public IStudentDAO getStudentDAO() { return studentDAO; }
    public ICourseDAO getCourseDAO() { return courseDAO; }
    public IRoomDAO getRoomDAO() { return roomDAO; }
    public IEduClassDAO getEduClassDAO() { return eduClassDAO; }
    public IScheduleDAO getScheduleDAO() { return scheduleDAO; }

    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }

    public boolean attemptLogin(String username, String password) {
        // Validation cơ bản đầu vào
        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            UIUtils.showWarningMessage(loginView, "Login Failed", "Username/Phone and password cannot be empty.");
            return false;
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
                        return true;

                    } else {
                        UIUtils.showErrorMessage(loginView, "Login Failed", "User account is inactive. Please contact administrator.");
                        return false;
                    }
                } else {
                    UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
                    return false;
                }
            } else {
                UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
                return false;
            }
        } catch (DataAccessException e) {
            System.err.println("Login DAO Error: " + e.getMessage());
            e.printStackTrace(); // In stack trace ra console để debug
            UIUtils.showErrorMessage(loginView, "Login Error", "A data access error occurred. Please try again later or contact support.");
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected Login Error: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(loginView, "Login Error", "An unexpected error occurred during login.");
            return false;
        }
    }

    private void loginSuccess() {
        if (loggedInUser != null) {
            String nameToShow = loggedInUser.getUsername(); // Mặc định là username

            if (loggedInUser.getRole() == Role.TEACHER && loggedInUser.getTeacherId() != null && teacherDAO != null) {
                try {
                    Teacher teacher = teacherDAO.getById(loggedInUser.getTeacherId());
                    if (teacher != null && teacher.getFullName() != null && !teacher.getFullName().isEmpty()) {
                        nameToShow = teacher.getFullName(); // Lấy tên đầy đủ của Teacher
                        System.out.println("Found Teacher Full Name: " + nameToShow);
                    } else {
                        System.err.println("Could not find full name for Teacher ID: " + loggedInUser.getTeacherId());
                    }
                } catch (DataAccessException e) {
                    System.err.println("Error getting teacher details for display name: " + e.getMessage());
                }
            } else if (loggedInUser.getRole() == Role.STUDENT && loggedInUser.getStudentId() != null && studentDAO != null) {
                try {
                    Student student = studentDAO.getById(loggedInUser.getStudentId());
                    if (student != null && student.getFullName() != null && !student.getFullName().isEmpty()) {
                        nameToShow = student.getFullName(); // Lấy tên đầy đủ của Student
                        System.out.println("Found Student Full Name: " + nameToShow);
                    } else {
                        System.err.println("Could not find full name for Student ID: " + loggedInUser.getStudentId());
                    }
                } catch (DataAccessException e) {
                    System.err.println("Error getting student details for display name: " + e.getMessage());
                }
            }
            // Gán tên tìm được (hoặc username mặc định) vào đối tượng User
            loggedInUser.setDisplayName(nameToShow);
            System.out.println("Set displayName for loggedInUser: " + loggedInUser.getDisplayName());
        }

        System.out.println("AuthController: Login successful. Closing LoginView and opening MainView...");
        if (loginView != null) {
            loginView.dispose();
            loginView = null;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                MainController mainController = new MainController(
                        loggedInUser,
                        this,
                        this.getUserDAO(),
                        this.getStudentDAO(),
                        this.getTeacherDAO(),
                        this.getCourseDAO(),
                        this.getRoomDAO(),
                        this.getEduClassDAO(),
                        this.getScheduleDAO()
                );

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
                if (!ValidationUtils.isValidPhoneNumber(username)) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "A valid phone number is required as username for student registration.");
                    return false;
                }
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

                System.out.println("Checking if student ID " + studentIdToLink + " already has an account...");
                if (userDAO.findByStudentId(studentIdToLink).isPresent()) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "An account already exists for the student with this phone number (Student ID: " + studentIdToLink + ").");
                    return false;
                }
                System.out.println("No existing account found for student ID " + studentIdToLink + ".");


            } else if (role == Role.TEACHER) {
                System.out.println("Processing TEACHER registration...");
                if (!ValidationUtils.isValidUsername(username)) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "Invalid username format for teacher (3-20 alphanumeric/underscore).");
                    return false;
                }
                if (teacherIdInput == null || teacherIdInput <= 0) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "A valid Teacher ID (provided by Admin) is required.");
                    return false;
                }
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