package com.eduzk.view;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import com.eduzk.controller.*;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.*; // Import all panels

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainView extends JFrame {

    private final MainController mainController;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;

    // Panels for different modules
    private StudentPanel studentPanel;
    private TeacherPanel teacherPanel;
    private CoursePanel coursePanel;
    private RoomPanel roomPanel;
    private ClassPanel classPanel;
    private SchedulePanel schedulePanel;
    // Add panels for User Management, Grades, Attendance etc.

    public MainView(MainController mainController) {
        this.mainController = mainController;
        initComponents();
        setupLayout();
        createMenuBar();
        setupWindowListener();
        configureWindow();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        statusLabel = new JLabel("Ready.");

        // Instantiate panels (controllers will be set later via setControllers)
        studentPanel = new StudentPanel(null); // Pass null controller initially
        teacherPanel = new TeacherPanel(null);
        coursePanel = new CoursePanel(null);
        roomPanel = new RoomPanel(null);
        classPanel = new ClassPanel(null);
        schedulePanel = new SchedulePanel(null);
        // Instantiate other panels...
    }

    private void setupLayout() {
        setTitle("EduZakhanh - Educational Management");
        setLayout(new BorderLayout());

        // Add panels to tabbed pane (visibility controlled later by configureViewForUser)
        // Icons can be added here using UIUtils.createImageIcon
        tabbedPane.addTab("Students", null, studentPanel, "Manage Students");
        tabbedPane.addTab("Teachers", null, teacherPanel, "Manage Teachers");
        tabbedPane.addTab("Courses", null, coursePanel, "Manage Courses");
        tabbedPane.addTab("Rooms", null, roomPanel, "Manage Rooms");
        tabbedPane.addTab("Classes", null, classPanel, "Manage Classes & Enrollment");
        tabbedPane.addTab("Schedule", null, schedulePanel, "Manage Class Schedules");
        // Add other tabs...

        add(tabbedPane, BorderLayout.CENTER);

        // Status Bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        JMenuItem exitItem = new JMenuItem("Exit");

        logoutItem.addActionListener(e -> performLogout());
        exitItem.addActionListener(e -> mainController.exitApplication());

        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Menu export
        JMenu exportMenu = new JMenu("Export");
        JMenuItem exportExcelItem = new JMenuItem("Export to Excel...");
        exportExcelItem.addActionListener(e -> showExportExcelDialog()); // Gọi hàm mới
        exportMenu.add(exportExcelItem);
        menuBar.add(exportMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About EduHub");
        aboutItem.addActionListener(e -> mainController.showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void showExportExcelDialog() {
            // 1. Tạo các lựa chọn loại dữ liệu để export
            String[] exportOptions = {
                    "Student List",
                    "Teacher List",
                    "Course List",
                    "Room List",
                    "Class List (Basic Info)",
                    "Schedule (Current View. All)", // Cần làm rõ phạm vi
                    // Thêm các lựa chọn khác nếu cần
    };
        String selectedOption = (String) JOptionPane.showInputDialog(
                this,                                     // Parent component
                "Select data to export:",                 // Message
                "Export to Excel",                        // Title
                JOptionPane.PLAIN_MESSAGE,              // Message type
                null,                                     // Icon (null for default)
                exportOptions,                            // Array of choices
                exportOptions[0]                          // Default choice
        );

        // 3. Nếu người dùng chọn một tùy chọn (không nhấn Cancel)
        if (selectedOption != null) {
            // 4. Hiển thị hộp thoại chọn nơi lưu file (JFileChooser)
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Excel File");
            // Đặt tên file gợi ý và bộ lọc file .xlsx
            String suggestedFileName = selectedOption.replace(" ", "_") + ".xlsx";
            fileChooser.setSelectedFile(new File(suggestedFileName));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                // Đảm bảo file có đuôi .xlsx
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".xlsx")) {
                    fileToSave = new File(filePath + ".xlsx");
                }

                // 5. Gọi Controller để thực hiện Export với lựa chọn và đường dẫn file
                System.out.println("Export requested for: " + selectedOption + " to file: " + fileToSave.getAbsolutePath());
                // Gọi phương thức export trong MainController (sẽ tạo ở bước sau)
                mainController.exportDataToExcel(selectedOption, fileToSave);
            } else {
                System.out.println("Export save cancelled by user.");
            }
        } else {
            System.out.println("Export type selection cancelled by user.");
        }
    }

    private void performLogout() {
        // Confirmation dialog
        if (UIUtils.showConfirmDialog(this, "Logout", "Are you sure you want to logout?")) {
            this.dispose(); // Close the main window
            // Relaunch the login view - requires access to App's main logic or AuthController
            // For now, just exit, but ideally it should show LoginView again.
            // Let AuthController handle showing LoginView again after logout
            // mainController.getAuthController().showLogin(); // If AuthController is accessible
            System.out.println("Logout requested - closing main window.");
            // A better approach involves a central navigation manager or callback to App.
            // Simple temporary solution: Re-run App.main (not ideal)
            // com.eduhub.App.main(null);
            // Or, more cleanly, have AuthController handle the transition
            // For now, we just dispose. The AuthController needs a method to re-show Login.
            // If LoginView was just hidden, make it visible again.
            // If not, AuthController needs to create and show a new one.
            mainController.logout(); // Tell controller about logout
        }
    }


    private void setupWindowListener() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close manually
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainController.exitApplication(); // Use controller logic for exit
            }
        });
    }

    private void configureWindow() {
        setMinimumSize(new Dimension(800, 600)); // Set a reasonable minimum size
        setSize(1024, 768); // Set default size
        setLocationRelativeTo(null); // Center on screen
    }

    // Called by MainController after DAOs and sub-controllers are initialized
    public void setControllers(StudentController sc, TeacherController tc, CourseController cc,
                               RoomController rc, EduClassController ecc, ScheduleController schc) {
        studentPanel.setController(sc);
        teacherPanel.setController(tc);
        coursePanel.setController(cc);
        roomPanel.setController(rc);
        classPanel.setController(ecc);
        schedulePanel.setController(schc);
        // Set controllers for other panels...

        // Initial data load for the visible panel (optional, panels can load on visibility change)
        studentPanel.refreshTable();
    }

    // Configure visible tabs based on user role
    public void configureViewForUser(User user) {
        if (user == null) {
            // Should not happen if login is successful, handle defensively
            UIUtils.showErrorMessage(this, "Error", "No logged in user found. Exiting.");
            System.exit(1);
            return;
        }

        setTitle("EduZakhanh - Welcome, " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")");
        statusLabel.setText("Logged in as: " + user.getRole().getDisplayName());

        // Example: Hide/Show tabs based on role
        tabbedPane.removeAll(); // Clear existing tabs first

        if (user.getRole() == Role.ADMIN) {
            tabbedPane.addTab("Schedule", null, schedulePanel, "Manage Class Schedules");
            tabbedPane.addTab("Classes", null, classPanel, "Manage Classes & Enrollment");
            tabbedPane.addTab("Students", null, studentPanel, "Manage Students");
            tabbedPane.addTab("Teachers", null, teacherPanel, "Manage Teachers");
            tabbedPane.addTab("Courses", null, coursePanel, "Manage Courses");
            tabbedPane.addTab("Rooms", null, roomPanel, "Manage Rooms");
            // Add Admin-specific tabs like User Management here
        } else if (user.getRole() == Role.TEACHER) {
            // Teachers might only see Schedule, their Classes, related Students
            tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule"); // SchedulePanel needs to filter by teacher
            tabbedPane.addTab("My Classes", null, classPanel, "View My Classes & Students"); // ClassPanel needs filter/mode
            tabbedPane.addTab("My Students", null, studentPanel, "View Students in My Classes"); // StudentPanel needs filter/mode
            // Add Grade/Attendance tabs for teachers
        }else if (user.getRole() == Role.STUDENT) { // <-- THÊM KHỐI ELSE IF NÀY
            // Student chỉ xem được lịch học và lớp học của mình (ví dụ)
            tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule");
            tabbedPane.addTab("My Classes", null, classPanel, "View My Classes");
            // TODO: Thêm tab xem điểm

            // Vô hiệu hóa tất cả các nút chỉnh sửa/thêm/xóa
            setPanelControlsEnabled(false); // Gọi hàm helper mới
        }
        // Add configurations for other roles if needed

        // Refresh the initially selected tab after configuration
//        Component selectedComponent = tabbedPane.getSelectedComponent();
//        if (selectedComponent instanceof StudentPanel) ((StudentPanel) selectedComponent).refreshTable();
//        else if (selectedComponent instanceof TeacherPanel) ((TeacherPanel) selectedComponent).refreshTable();
        // Add refreshes for other panel types as needed for the default view
    }


    // --- THÊM PHƯƠNG THỨC HELPER NÀY VÀO MainView.java ---
    private void setPanelControlsEnabled(boolean isAdmin) {
        // Mặc định vô hiệu hóa các nút nhạy cảm nếu không phải Admin
        // Sau đó có thể tùy chỉnh thêm cho Teacher nếu cần

        // Student Panel
        if (studentPanel != null) {
            studentPanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào StudentPanel
        }
        // Teacher Panel
        if (teacherPanel != null) {
            teacherPanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào TeacherPanel
        }
        // Course Panel
        if (coursePanel != null) {
            coursePanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào CoursePanel
        }
        // Room Panel
        if (roomPanel != null) {
            roomPanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào RoomPanel
        }
        // Class Panel
        if (classPanel != null) {
            classPanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào ClassPanel
            // Teacher có thể cần Enroll/Unenroll? Tùy chỉnh nếu cần
            // classPanel.setEnrollmentControlsEnabled(isAdmin || isTeacher);
        }
        // Schedule Panel
        if (schedulePanel != null) {
            schedulePanel.setAdminControlsEnabled(isAdmin); // Thêm phương thức này vào SchedulePanel
        }
        // ... Thêm cho các panel khác nếu có ...
    }

    public void refreshSelectedTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            // Gọi refresh tương ứng dựa trên loại panel
            if (selectedComponent instanceof StudentPanel) {
                ((StudentPanel) selectedComponent).refreshTable();
            } else if (selectedComponent instanceof TeacherPanel) {
                ((TeacherPanel) selectedComponent).refreshTable();
            } else if (selectedComponent instanceof CoursePanel) {
                ((CoursePanel) selectedComponent).refreshTable();
            } else if (selectedComponent instanceof RoomPanel) {
                ((RoomPanel) selectedComponent).refreshTable();
            } else if (selectedComponent instanceof ClassPanel) {
                ((ClassPanel) selectedComponent).refreshTable();
            } else if (selectedComponent instanceof SchedulePanel) {
                ((SchedulePanel) selectedComponent).refreshScheduleView();
            }
            // Thêm else if cho các panel khác nếu có
        }
    }
}