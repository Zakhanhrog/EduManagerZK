package com.eduzk;

import com.eduzk.controller.AuthController;
import com.eduzk.model.dao.impl.UserDAOImpl;
import com.eduzk.model.dao.impl.TeacherDAOImpl;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Teacher;
import com.eduzk.view.LoginView;
import com.eduzk.model.dao.impl.StudentDAOImpl;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JOptionPane;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;


public class App {
    // look and feel
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel( new FlatIntelliJLaf() );
            System.out.println("FlatLaf Look and Feel initialized successfully.");
        } catch( UnsupportedLookAndFeelException ex ) {
            System.err.println( "Failed to initialize FlatLaf Look and Feel: " + ex.getMessage() );
            ex.printStackTrace();
        } catch (Exception e) {
            System.err.println( "An error occurred while setting Look and Feel: " + e.getMessage());
            e.printStackTrace();
        }

        // Chạy logic chính trên Event Dispatch Thread (EDT)
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
                System.out.println("Checking/Initializing default accounts if necessary...");
                initializeDefaultAdminAccount(userDAO);

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

    private static void initializeDefaultAdminAccount(UserDAOImpl userDAO) {
        try {
            System.out.println("Checking if any users exist...");
            if (userDAO.getAll().isEmpty()) {
                System.out.println("No users found. Creating default ADMIN account...");
                try {
                    User adminUser = new User(0, "admin", "admin", Role.ADMIN, null, null);
                    userDAO.add(adminUser);
                    System.out.println("- Default admin user (admin/admin) created successfully.");
                } catch (Exception adminEx) {
                    System.err.println("! Error creating default admin user: " + adminEx.getMessage());
                    adminEx.printStackTrace();
                }
            } else {
                System.out.println("Users already exist. Skipping default admin account creation.");
            }
        } catch (Exception generalEx) {
            System.err.println("!! Error during default admin account check/creation process: " + generalEx.getMessage());
            generalEx.printStackTrace();
        }

    }

}