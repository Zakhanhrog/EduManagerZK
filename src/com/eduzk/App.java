package com.eduzk; // Hoặc com.eduhub

// Import các lớp cần thiết cơ bản
import com.eduzk.controller.AuthController;
import com.eduzk.model.dao.impl.UserDAOImpl;
import com.eduzk.model.dao.impl.TeacherDAOImpl; // Vẫn cần để inject và kiểm tra Teacher ID
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Teacher; // Vẫn cần để kiểm tra Teacher ID
import com.eduzk.view.LoginView;
import com.eduzk.model.dao.impl.StudentDAOImpl;
import com.eduzk.model.entities.Student;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException; // Import cho LookAndFeelException
import javax.swing.JOptionPane;                   // Import để báo lỗi nghiêm trọng

/**
 * Lớp chính khởi chạy ứng dụng EduManager (EduHub).
 * Phiên bản này sử dụng đường dẫn tương đối cho thư mục 'data'.
 * Phù hợp để chạy từ IDE hoặc dòng lệnh khi ở thư mục gốc dự án.
 */
public class App {

    /**
     * Phương thức main, điểm vào của ứng dụng.
     * @param args Tham số dòng lệnh (không sử dụng).
     */
    public static void main(String[] args) {

        // 1. Thiết lập Look and Feel (Giữ nguyên)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("System Look and Feel set successfully.");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Warning: Failed to set system look and feel. Using default.");
        }

        // 2. Chạy logic chính trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try { // Bắt lỗi khởi tạo trên EDT

                // 3. Khởi tạo DAO với đường dẫn TƯƠNG ĐỐI
                System.out.println("Initializing DAOs using relative paths...");
                final String dataDir = "data/"; // Thư mục dữ liệu tương đối
                final String idFile = dataDir + "next_ids.dat";
                UserDAOImpl userDAO = new UserDAOImpl(dataDir + "users.dat", idFile);
                TeacherDAOImpl teacherDAO = new TeacherDAOImpl(dataDir + "teachers.dat", idFile);
                StudentDAOImpl studentDAO = new StudentDAOImpl("data/students.dat", "data/next_ids.dat"); // <-- KHỞI TẠO StudentDAO
                System.out.println("DAOs initialized.");

                // 4. Tạo tài khoản mặc định nếu cần
                // (Giữ lại logic này để tiện cho việc chạy lần đầu hoặc sau khi xóa data)
                System.out.println("Checking/Initializing default accounts if necessary...");
                initializeDefaultAccounts(userDAO, teacherDAO);

                // 5. Khởi tạo AuthController và inject TeacherDAO
                System.out.println("Initializing AuthController...");
                AuthController authController = new AuthController(userDAO);
                authController.setTeacherDAO(teacherDAO); // Vẫn inject DAO này
                authController.setStudentDAO(studentDAO);
                System.out.println("AuthController initialized.");

                // 6. Khởi tạo và hiển thị LoginView
                System.out.println("Initializing LoginView...");
                LoginView loginView = new LoginView(authController);
                authController.setLoginView(loginView);
                System.out.println("LoginView initialized.");

                System.out.println("Setting LoginView visible...");
                loginView.setVisible(true);
                System.out.println("Application startup complete.");

            } catch (Throwable t) { // Bắt lỗi nghiêm trọng
                System.err.println("!!! CRITICAL ERROR DURING EDT INITIALIZATION !!!");
                t.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "A critical error occurred during application startup.\nPlease check the console output.\n\nError: " + t.getMessage(),
                        "Startup Failed", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
        System.out.println("main() method finished scheduling EDT task.");
    }

    /**
     * Phương thức helper tạo tài khoản mặc định (Giữ nguyên logic này).
     * @param userDAO DAO User.
     * @param teacherDAO DAO Teacher.
     */
    private static void initializeDefaultAccounts(UserDAOImpl userDAO, TeacherDAOImpl teacherDAO) {
        try {
            if (userDAO.getAll().isEmpty()) {
                System.out.println("No users found. Creating default accounts...");

                // Tạo Admin
                try {
                    User adminUser = new User(0, "admin", "admin", Role.ADMIN, null);
                    userDAO.add(adminUser);
                    System.out.println("- Default admin user (admin/admin) created.");
                } catch (Exception adminEx) { System.err.println("! Error creating admin user: " + adminEx.getMessage()); }

                // Tạo Teacher
                try {
                    Integer teacherLinkId = 1; // <-- NHỚ SỬA ID NÀY NẾU CẦN
                    if (teacherLinkId != null && teacherLinkId > 0) {
                        Teacher existingTeacher = teacherDAO.getById(teacherLinkId);
                        if (existingTeacher != null) {
                            User teacherUser = new User(0, "teacher1", "teacher1", Role.TEACHER, teacherLinkId);
                            userDAO.add(teacherUser);
                            System.out.println("- Default teacher user (teacher1/teacher1) linked to Teacher ID " + teacherLinkId + " created.");
                        } else { System.err.println("! Skipping teacher user creation: Teacher with ID " + teacherLinkId + " NOT FOUND."); }
                    } else { System.err.println("! Skipping teacher user creation: Invalid teacherLinkId (" + teacherLinkId + ")."); }
                } catch (Exception teacherEx) { System.err.println("! Error creating teacher user: " + teacherEx.getMessage()); }

                // Tạo Student
                try {
                    User studentUser = new User(0, "student1", "student1", Role.STUDENT, null);
                    userDAO.add(studentUser);
                    System.out.println("- Default student user (student1/student1) created.");
                } catch (Exception studentEx) { System.err.println("! Error creating student user: " + studentEx.getMessage()); }

                System.out.println("Default account creation process finished.");

            } else {
                System.out.println("Users already exist. Skipping default account creation.");
            }
        } catch (Exception generalEx) {
            System.err.println("!! Error during default account check process: " + generalEx.getMessage());
            generalEx.printStackTrace();
        }
    }
}