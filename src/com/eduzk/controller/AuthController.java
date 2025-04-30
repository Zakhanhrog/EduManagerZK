package com.eduzk.controller; // Giữ nguyên package của bạn

// --- Import đầy đủ các lớp cần thiết ---
import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.dao.interfaces.IStudentDAO; // <-- Cần thêm DAO này
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.entities.Student;         // <-- Cần entity này
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.LoginView;
import com.eduzk.view.MainView;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane; // Import JOptionPane để hiển thị lỗi cuối cùng nếu cần
import java.util.Optional;

/**
 * Controller chịu trách nhiệm xử lý logic Xác thực (Đăng nhập, Đăng ký)
 * và điều hướng giữa LoginView và MainView.
 */
public class AuthController {

    private final IUserDAO userDAO;
    private ITeacherDAO teacherDAO; // Sẽ được inject từ App.java
    private IStudentDAO studentDAO; // Sẽ được inject từ App.java
    private LoginView loginView;    // Tham chiếu đến cửa sổ đăng nhập
    private User loggedInUser;      // Lưu thông tin người dùng sau khi đăng nhập thành công

    /**
     * Constructor chính.
     * @param userDAO Đối tượng DAO để truy cập dữ liệu người dùng.
     */
    public AuthController(IUserDAO userDAO) {
        this.userDAO = userDAO;
        // teacherDAO và studentDAO sẽ được inject qua setters
    }

    // --- Setters để Inject các DAO khác ---
    public void setTeacherDAO(ITeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    public void setStudentDAO(IStudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }
    // --- Kết thúc Setters ---

    /**
     * Liên kết AuthController với LoginView của nó.
     * @param loginView Instance của LoginView.
     */
    public void setLoginView(LoginView loginView) {
        this.loginView = loginView;
    }

    /**
     * Xử lý yêu cầu đăng nhập từ LoginView.
     * @param username Username hoặc SĐT người dùng nhập.
     * @param password Mật khẩu người dùng nhập.
     */
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
                // !!! CẢNH BÁO BẢO MẬT: So sánh mật khẩu plain text !!!
                // Cần thay thế bằng cơ chế kiểm tra hash mật khẩu an toàn.
                if (user.getPassword().equals(password)) {
                    // Kiểm tra tài khoản có bị vô hiệu hóa không
                    if (user.isActive()) {
                        this.loggedInUser = user;
                        loginSuccess(); // Đăng nhập thành công
                    } else {
                        UIUtils.showErrorMessage(loginView, "Login Failed", "User account is inactive. Please contact administrator.");
                    }
                } else {
                    // Sai mật khẩu
                    UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
                }
            } else {
                // Không tìm thấy username/SĐT
                UIUtils.showErrorMessage(loginView, "Login Failed", "Invalid username/phone or password.");
            }
        } catch (DataAccessException e) {
            // Lỗi khi truy cập file dữ liệu
            System.err.println("Login DAO Error: " + e.getMessage());
            e.printStackTrace(); // In stack trace ra console để debug
            UIUtils.showErrorMessage(loginView, "Login Error", "A data access error occurred. Please try again later or contact support.");
        } catch (Exception e) {
            // Các lỗi không mong muốn khác
            System.err.println("Unexpected Login Error: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(loginView, "Login Error", "An unexpected error occurred during login.");
        }
    }

    /**
     * Hành động thực hiện sau khi đăng nhập thành công:
     * Đóng LoginView, mở MainView và truyền thông tin người dùng.
     */
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
                mainController.setMainView(mainView); // Liên kết MainController với MainView
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

    /** Trả về thông tin người dùng đang đăng nhập. */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /** Xử lý đăng xuất (logic cơ bản). */
    public void logout() {
        System.out.println("AuthController: Clearing loggedInUser session.");
        this.loggedInUser = null;
    }

    /**
     * Xử lý yêu cầu đăng ký tài khoản mới từ RegisterDialog.
     *
     * @param username        Username (cho Teacher) hoặc Số điện thoại (cho Student).
     * @param password        Mật khẩu mong muốn.
     * @param confirmPassword Mật khẩu xác nhận.
     * @param role            Vai trò đăng ký (STUDENT hoặc TEACHER).
     * @param teacherIdInput  ID của Teacher đã tồn tại (chỉ cần thiết nếu role là TEACHER, ngược lại là null).
     * @return true nếu đăng ký thành công, false nếu thất bại (lỗi sẽ được hiển thị qua UIUtils).
     */
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
        if (!ValidationUtils.isValidPassword(password)) { // Kiểm tra độ dài tối thiểu
            UIUtils.showWarningMessage(null, "Registration Failed", "Password must be at least 6 characters long.");
            return false;
        }
        if (role == Role.ADMIN) { // Không cho phép tự đăng ký Admin
            UIUtils.showWarningMessage(null, "Registration Failed", "Cannot register as Administrator.");
            return false;
        }
        if (role == null) { // Vai trò không được null
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


            Integer studentIdToLink = null; // ID học sinh sẽ liên kết
            Integer teacherIdToLink = null; // ID giáo viên sẽ liên kết

            // --- 3. Validation và Logic riêng theo từng Role ---
            if (role == Role.STUDENT) {
                System.out.println("Processing STUDENT registration...");
                // Validation: Username phải là SĐT hợp lệ
                if (!ValidationUtils.isValidPhoneNumber(username)) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "A valid phone number is required as username for student registration.");
                    return false;
                }

                // Kiểm tra sự tồn tại của Student bằng SĐT
                if (this.studentDAO == null) { throw new IllegalStateException("StudentDAO is not initialized in AuthController."); } // Lỗi cấu hình
                System.out.println("Finding student by phone: " + username);
                Optional<Student> studentOpt = studentDAO.findByPhone(username);
                if (studentOpt.isEmpty()) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "No student record found with phone number: " + username + ". Please contact administrator to add your profile first.");
                    return false;
                }
                Student existingStudent = studentOpt.get();
                studentIdToLink = existingStudent.getStudentId(); // Lấy ID để liên kết
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
                if (this.teacherDAO == null) { throw new IllegalStateException("TeacherDAO is not initialized in AuthController."); } // Lỗi cấu hình
                System.out.println("Finding teacher by ID: " + teacherIdInput);
                Teacher existingTeacher = teacherDAO.getById(teacherIdInput);
                if (existingTeacher == null) {
                    UIUtils.showWarningMessage(null, "Registration Failed", "Teacher with ID " + teacherIdInput + " not found. Please verify the ID with the administrator.");
                    return false;
                }
                teacherIdToLink = teacherIdInput; // Lấy ID để liên kết
                System.out.println("Found teacher: " + existingTeacher.getFullName() + " for ID: " + teacherIdInput);

                // (Tùy chọn nâng cao) Kiểm tra xem Teacher ID này đã được liên kết với User nào chưa
                // Cần thêm userDAO.findByTeacherId(teacherIdToLink)
                /*
                System.out.println("Checking if teacher ID " + teacherIdToLink + " already has an account...");
                if (userDAO.findByTeacherId(teacherIdToLink).isPresent()) { // Giả sử có phương thức này
                    UIUtils.showWarningMessage(null, "Registration Failed", "This Teacher ID (" + teacherIdToLink + ") is already linked to another user account.");
                    return false;
                }
                 System.out.println("No existing account found for teacher ID " + teacherIdToLink + ".");
                */

            } // Không cần else vì role đã được kiểm tra ở đầu

            // --- 4. Nếu mọi kiểm tra đều OK, tạo đối tượng User mới ---
            System.out.println("All checks passed. Creating new User object...");
            User newUser = new User();
            newUser.setUsername(username);         // Lưu username (hoặc SĐT)
            newUser.setPassword(password);         // !!! CẦN HASHING Ở ĐÂY !!!
            newUser.setRole(role);               // Gán vai trò
            newUser.setActive(true);             // Kích hoạt tài khoản
            newUser.setTeacherId(teacherIdToLink); // Sẽ là null nếu là Student
            newUser.setStudentId(studentIdToLink); // Sẽ là null nếu là Teacher

            // --- 5. Gọi UserDAO để thêm vào cơ sở dữ liệu (file .dat) ---
            System.out.println("Adding new user to database: " + newUser);
            userDAO.add(newUser); // Phương thức add trong UserDAOImpl đã có kiểm tra trùng username/studentId
            System.out.println("New user added successfully with ID: " + newUser.getUserId());

            return true; // Đăng ký thành công

        } catch (DataAccessException e) {
            // Lỗi từ DAO (ví dụ: không ghi được file, hoặc username/studentId đã tồn tại từ DAO)
            System.err.println("Registration DAO Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Registration Failed", "Could not save registration data: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            // Lỗi đầu vào không hợp lệ ném từ DAO hoặc logic khác
            System.err.println("Registration Argument Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Registration Failed", "Invalid data provided: " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            // Lỗi nếu DAO chưa được inject
            System.err.println("Registration Configuration Error: " + e.getMessage());
            UIUtils.showErrorMessage(null, "Configuration Error", "Application configuration error. Please contact support.");
            return false;
        }
        catch (Exception e) {
            // Các lỗi không mong muốn khác
            System.err.println("Unexpected Registration Error: " + e.getMessage());
            e.printStackTrace(); // In stack trace đầy đủ ra console
            UIUtils.showErrorMessage(null, "Registration Error", "An unexpected error occurred during registration. Please try again.");
            return false;
        }
    }

    public void showLoginView() {
        System.out.println("AuthController: showLoginView() called.");
        // Đảm bảo thực hiện trên EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Tạo một instance LoginView MỚI để đảm bảo trạng thái sạch
                System.out.println("AuthController: Creating new LoginView instance...");
                LoginView newLoginView = new LoginView(this); // Truyền AuthController này
                this.loginView = newLoginView; // Cập nhật tham chiếu loginView hiện tại
                System.out.println("AuthController: Setting new LoginView visible...");
                newLoginView.setVisible(true);
                System.out.println("AuthController: New LoginView should be visible.");
            } catch (Exception e) {
                System.err.println("!!! CRITICAL ERROR SHOWING LOGIN VIEW !!!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to return to the login screen.\nError: " + e.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Thoát nếu không thể quay lại màn hình login
            }
        });
    }
}