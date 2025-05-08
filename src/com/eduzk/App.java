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
import com.eduzk.view.SplashScreen;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.model.dao.interfaces.IAcademicRecordDAO;
import com.eduzk.model.dao.impl.AcademicRecordDAOImpl;
import com.eduzk.utils.PasswordUtils;

public class App {
    public static void main(String[] args) {
        try {
            System.setProperty( "apple.laf.useScreenMenuBar", "true" );
            System.setProperty( "apple.awt.application.name", "EduZakhanh" );
            System.setProperty( "apple.awt.application.appearance", "light" );
            UIManager.setLookAndFeel( new FlatMacLightLaf());
            System.out.println("FlatLaf Look and Feel initialized successfully.");
        } catch( UnsupportedLookAndFeelException ex ) {
            System.err.println( "Failed to initialize FlatLaf Look and Feel: " + ex.getMessage() );
        } catch (Exception e) {
            System.err.println( "An error occurred while setting Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {

            SplashScreen splash = new SplashScreen(null);
            splash.setVisible(true);

            SwingWorker<AuthController, String> initializer = new SwingWorker<>() {

                @Override
                protected AuthController doInBackground() throws Exception {
                    try {
                        publish("Initializing Core Components...");
                        Thread.sleep(200);

                        publish("Setting up Data Paths...");
                        final String dataDir = "data/";
                        final String idFile = dataDir + "next_ids.dat";
                        final String logFile = dataDir + "logs.dat";
                        Thread.sleep(200);

                        publish("Initializing ID Generator...");
                        IdGenerator sharedIdGenerator = new IdGenerator(idFile);
                        System.out.println("Shared IdGenerator initialized.");
                        Thread.sleep(200);

                        publish("Initializing Data Access Objects (DAO)...");
                        IUserDAO userDAO = new UserDAOImpl(dataDir + "users.dat", idFile);
                        ITeacherDAO teacherDAO = new TeacherDAOImpl(dataDir + "teachers.dat", sharedIdGenerator);
                        IEduClassDAO eduClassDAO = new EduClassDAOImpl(dataDir + "educlasses.dat", sharedIdGenerator);
                        IStudentDAO studentDAO = new StudentDAOImpl(dataDir + "students.dat", sharedIdGenerator, eduClassDAO);
                        ICourseDAO courseDAO = new CourseDAOImpl(dataDir + "courses.dat", idFile);
                        IRoomDAO roomDAO = new RoomDAOImpl(dataDir + "rooms.dat", idFile);
                        IScheduleDAO scheduleDAO = new ScheduleDAOImpl(dataDir + "schedules.dat", idFile);
                        IAcademicRecordDAO academicRecordDAO = new AcademicRecordDAOImpl(dataDir + "academic_records.dat", sharedIdGenerator);
                        System.out.println("ALL DAOs initialized.");
                        Thread.sleep(200);

                        publish("Initializing Services...");
                        LogService logService = new LogService(logFile);
                        System.out.println("LogService initialized.");
                        Thread.sleep(200);

                        publish("Checking Default Admin Account...");
                        initializeDefaultAdminAccount((UserDAOImpl) userDAO);
                        Thread.sleep(200);

                        publish("Initializing Authentication Controller...");
                        AuthController authController = new AuthController(
                                userDAO,
                                teacherDAO,
                                studentDAO,
                                courseDAO,
                                roomDAO,
                                eduClassDAO,
                                scheduleDAO,
                                logService,
                                academicRecordDAO,
                                sharedIdGenerator);
                        authController.setTeacherDAO(teacherDAO);
                        authController.setStudentDAO(studentDAO);
                        authController.setCourseDAO(courseDAO);
                        authController.setRoomDAO(roomDAO);
                        authController.setEduClassDAO(eduClassDAO);
                        authController.setScheduleDAO(scheduleDAO);
                        authController.setLogService(logService);
                        authController.setAcademicRecordDAO(academicRecordDAO);
                        System.out.println("AuthController initialized.");
                        Thread.sleep(200);

                        publish("Initialization Complete!");
                        Thread.sleep(300);

                        return authController;
                    } catch (Throwable t) {
                        System.err.println("!!! CRITICAL ERROR DURING BACKGROUND INITIALIZATION !!!");
                        t.printStackTrace();
                        publish("Error: Initialization Failed! " + t.getMessage());
                        Thread.sleep(2000);
                        throw new Exception("Background initialization failed", t);
                    }
                }

                @Override
                protected void process(List<String> chunks) {
                    String latestStatus = chunks.get(chunks.size() - 1);
                    splash.setStatus(latestStatus);
                }

                @Override
                protected void done() {
                    splash.dispose();

                    try {
                        AuthController authController = get();
                        System.out.println("Initializing LoginView...");
                        LoginView loginView = new LoginView(authController);
                        authController.setLoginView(loginView);
                        System.out.println("LoginView initialized.");
                        loginView.setVisible(true);
                        System.out.println("Application startup complete.");

                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Initialization failed in background thread!");
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        JOptionPane.showMessageDialog(null,
                                "A critical error occurred during application startup.\nPlease check the console output.\n\nError: " + cause.getMessage(),
                                "Startup Failed", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    } catch (Exception e) {
                        System.err.println("Failed to initialize LoginView!");
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null,
                                "Could not launch the login screen.\nError: " + e.getMessage(),
                                "Startup Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                }
            };
            initializer.execute();
        });
        System.out.println("main() method finished scheduling EDT task.");
    }

    private static void initializeDefaultAdminAccount(UserDAOImpl userDAO) {
        try {
            System.out.println("Checking if any users exist...");
            if (userDAO.findByUsername("admin").isEmpty()) {
                System.out.println("Default admin 'admin' not found. Creating...");
                try {
                    User adminUser = new User();
                    adminUser.setUsername("admin");
                    String hashedPassword = PasswordUtils.hashPassword("admin");
                    adminUser.setPassword(hashedPassword);
                    System.out.println("Hashed password for default admin: " + hashedPassword);
                    adminUser.setRole(Role.ADMIN);
                    adminUser.setActive(true);
                    adminUser.setRequiresPasswordChange(false);
                    userDAO.add(adminUser);
                    System.out.println("- Default admin user (admin/admin) created successfully.");
                } catch (Exception adminEx) {
                    System.err.println("! Error creating default admin user: " + adminEx.getMessage());
                    adminEx.printStackTrace();
                }
            } else {
                System.out.println("Default admin user 'admin' already exists.");
                 User existingAdmin = userDAO.findByUsername("admin").get();
                 if (existingAdmin.getPassword() != null &&
                     !(existingAdmin.getPassword().startsWith("$2a$") ||
                       existingAdmin.getPassword().startsWith("$2b$") ||
                       existingAdmin.getPassword().startsWith("$2y$"))) {
                     System.out.println("Admin 'admin' exists with plain text password. Updating to hashed version...");
                     String hashedAdminPassword = PasswordUtils.hashPassword(existingAdmin.getPassword());
                     existingAdmin.setPassword(hashedAdminPassword);
                     existingAdmin.setRequiresPasswordChange(false);
                     userDAO.update(existingAdmin);
                     System.out.println("- Password for admin 'admin' updated to hashed version.");
                 }
            }
        } catch (Exception generalEx) {
            System.err.println("!! Error during default admin account check/creation process: " + generalEx.getMessage());
            generalEx.printStackTrace();
        }
    }

}