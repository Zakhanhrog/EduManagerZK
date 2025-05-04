package com.eduzk;

import com.eduzk.controller.AuthController;
import com.eduzk.model.dao.impl.*;
import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.view.LoginView;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JOptionPane;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.model.dao.impl.IdGenerator;

public class App {
    // look and feel
    public static void main(String[] args) {
        try {
            System.setProperty( "apple.laf.useScreenMenuBar", "true" );
            System.setProperty( "apple.awt.application.name", "EduZakhanh" );
            System.setProperty( "apple.awt.application.appearance", "light" );
            UIManager.setLookAndFeel( new FlatMacLightLaf());
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
            try {
                System.out.println("Initializing ALL DAOs...");
                final String dataDir = "data/";
                final String idFile = dataDir + "next_ids.dat";
                final String logFile = dataDir + "logs.dat";

                //khoi tao ID
                IdGenerator sharedIdGenerator = new IdGenerator(idFile);
                System.out.println("Shared IdGenerator initialized.");

                // Khai báo dùng Interface cho linh hoạt
                IUserDAO userDAO = new UserDAOImpl(dataDir + "users.dat", idFile);
                ITeacherDAO teacherDAO = new TeacherDAOImpl(dataDir + "teachers.dat", sharedIdGenerator);
                IStudentDAO studentDAO = new StudentDAOImpl(dataDir + "students.dat", sharedIdGenerator);
                ICourseDAO courseDAO = new CourseDAOImpl(dataDir + "courses.dat", idFile);
                IRoomDAO roomDAO = new RoomDAOImpl(dataDir + "rooms.dat", idFile);
                IEduClassDAO eduClassDAO = new EduClassDAOImpl(dataDir + "educlasses.dat", sharedIdGenerator);
                IScheduleDAO scheduleDAO = new ScheduleDAOImpl(dataDir + "schedules.dat", idFile);
                System.out.println("ALL DAOs initialized.");

                LogService logService = new LogService(logFile);
                System.out.println("LogService initialized.");

                // Tạo tài khoản admin mặc định nếu cần
                initializeDefaultAdminAccount((UserDAOImpl) userDAO);

                // Khởi tạo AuthController và INJECT TẤT CẢ DAO vào nó
                System.out.println("Initializing AuthController and injecting DAOs...");
                AuthController authController = new AuthController(userDAO);
                authController.setTeacherDAO(teacherDAO);
                authController.setStudentDAO(studentDAO);
                authController.setCourseDAO(courseDAO);
                authController.setRoomDAO(roomDAO);
                authController.setEduClassDAO(eduClassDAO);
                authController.setScheduleDAO(scheduleDAO);
                authController.setLogService(logService);
                System.out.println("AuthController initialized.");

                // Khởi tạo và hiển thị LoginView (giữ nguyên)
                System.out.println("Initializing LoginView...");
                LoginView loginView = new LoginView(authController);
                authController.setLoginView(loginView);
                System.out.println("LoginView initialized.");
                loginView.setVisible(true);
                System.out.println("Application startup complete.");

            } catch (Throwable t) {
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