package com.eduzk.view;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import com.eduzk.controller.*;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import javax.swing.border.Border;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

public class MainView extends JFrame {

    private final MainController mainController;
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    private StudentPanel studentPanel;
    private TeacherPanel teacherPanel;
    private CoursePanel coursePanel;
    private RoomPanel roomPanel;
    private ClassPanel classPanel;
    private SchedulePanel schedulePanel;
    private JPanel statusBar;

    public MainView(MainController mainController) {
        this.mainController = mainController;
        initComponents();
        setupLayout();
        createMenuBar();
        setupWindowListener();
        configureWindow();
        updateStatusBarAppearance();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        statusLabel = new JLabel("Ready.");
        studentPanel = new StudentPanel(null);
        teacherPanel = new TeacherPanel(null);
        coursePanel = new CoursePanel(null);
        roomPanel = new RoomPanel(null);
        classPanel = new ClassPanel(null);
        schedulePanel = new SchedulePanel(null);
        statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        statusBar.add(statusLabel);
    }

    private void setupLayout() {
        setTitle("EduZakhanh - Educational Management");
        setLayout(new BorderLayout());
        tabbedPane.addTab("Students", null, studentPanel, "Manage Students");
        tabbedPane.addTab("Teachers", null, teacherPanel, "Manage Teachers");
        tabbedPane.addTab("Courses", null, coursePanel, "Manage Courses");
        tabbedPane.addTab("Rooms", null, roomPanel, "Manage Rooms");
        tabbedPane.addTab("Classes", null, classPanel, "Manage Classes & Enrollment");
        tabbedPane.addTab("Schedule", null, schedulePanel, "Manage Class Schedules");
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // Status Bar
        /*JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);*/


    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("Main");
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
        exportExcelItem.addActionListener(e -> showExportExcelDialog());
        exportMenu.add(exportExcelItem);
        menuBar.add(exportMenu);

        // Themes
        JMenu viewMenu = new JMenu("View");
        JMenu themeMenu = new JMenu("Themes");
        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButtonMenuItem lightThemeItem = createThemeMenuItem("Light", FlatLightLaf.class.getName(), themeGroup);
        JRadioButtonMenuItem darkThemeItem = createThemeMenuItem("Dark", FlatDarkLaf.class.getName(), themeGroup);
        JRadioButtonMenuItem intellijThemeItem = createThemeMenuItem("IntelliJ Light", FlatIntelliJLaf.class.getName(), themeGroup);
        JRadioButtonMenuItem macLightThemeItem = createThemeMenuItem("Mac Light", FlatMacLightLaf.class.getName(), themeGroup);
        JRadioButtonMenuItem macDarkThemeItem = createThemeMenuItem("Mac Dark", FlatMacDarkLaf.class.getName(), themeGroup);

        themeMenu.add(lightThemeItem);
        themeMenu.add(darkThemeItem);
        themeMenu.add(intellijThemeItem);
        themeMenu.add(macLightThemeItem);
        themeMenu.add(macDarkThemeItem);
        viewMenu.add(themeMenu);
        menuBar.add(viewMenu);

        String currentLaf = UIManager.getLookAndFeel().getClass().getName();
        for (MenuElement element : themeMenu.getPopupMenu().getSubElements()) {
            if (element instanceof JRadioButtonMenuItem) {
                JRadioButtonMenuItem item = (JRadioButtonMenuItem) element;
                if (item.getActionCommand() != null && item.getActionCommand().equals(currentLaf)) {
                    item.setSelected(true);
                }
            }
        }

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About EduHub");
        aboutItem.addActionListener(e -> mainController.showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JRadioButtonMenuItem createThemeMenuItem(String text, String lafClassName, ButtonGroup group) {
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(text);
        menuItem.setActionCommand(lafClassName);
        menuItem.addActionListener(e -> {
            if (mainController != null) {
                mainController.setLookAndFeel(lafClassName);
                updateStatusBarAppearance();
            }
        });
        group.add(menuItem);
        return menuItem;
    }

    private void updateStatusBarAppearance() {
        if (statusBar != null) {
            Color borderColor = UIManager.getColor("Component.borderColor");
            if (borderColor == null) {
                borderColor = Color.LIGHT_GRAY;
            }
            Border topBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor);
            statusBar.setBorder(topBorder);
            statusBar.revalidate();
            statusBar.repaint();
        }
    }

    private void showExportExcelDialog() {
        // --- LẤY VAI TRÒ USER HIỆN TẠI ---
        Role currentUserRole = (mainController != null && mainController.getLoggedInUser() != null)
                ? mainController.getLoggedInUser().getRole()
                : null; // Hoặc Role mặc định nếu không có user? (Không nên xảy ra)

        if (currentUserRole == null) {
            UIUtils.showErrorMessage(this, "Error", "Cannot determine user role for export.");
            return;
        }
        // --- TẠO DANH SÁCH TÙY CHỌN DỰA TRÊN ROLE ---
        List<String> optionsList = new ArrayList<>();
        if (currentUserRole == Role.ADMIN) {
            // Admin có tất cả các quyền export
            optionsList.add(MainController.EXPORT_STUDENTS); // Dùng hằng số đã tạo
            optionsList.add(MainController.EXPORT_TEACHERS);
            optionsList.add(MainController.EXPORT_COURSES);
            optionsList.add(MainController.EXPORT_ROOMS);
            optionsList.add(MainController.EXPORT_CLASSES);
            optionsList.add(MainController.EXPORT_SCHEDULE);
        } else if (currentUserRole == Role.TEACHER) {
            // Teacher chỉ có quyền export dữ liệu liên quan đến họ
            optionsList.add(MainController.EXPORT_SCHEDULE); // Lịch của tôi
            optionsList.add(MainController.EXPORT_CLASSES); // Lớp của tôi
            optionsList.add(MainController.EXPORT_STUDENTS); // Học viên trong lớp của tôi
            // Không thêm Teacher List, Course List, Room List
        }
        if (optionsList.isEmpty()) {
            UIUtils.showInfoMessage(this, "Export Not Available", "No export options available for your role.");
            return;
        }

        // Chuyển List thành Array để dùng cho JOptionPane
        String[] exportOptions = optionsList.toArray(new String[0]);

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
            String suggestedFileName = selectedOption.replace(" ", "_") + ".xlsx";
            fileChooser.setSelectedFile(new File(suggestedFileName));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
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
            System.out.println("Logout confirmed by user.");
            // 1. Gọi phương thức logout của MainController
            if (mainController != null) {
                mainController.logout();
            } else {
                System.err.println("MainView Error: mainController is null during logout!");
            }
            // 2. Đóng cửa sổ MainView hiện tại
            this.dispose();
        }else {
            System.out.println("Logout cancelled by user.");
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
        studentPanel.refreshTable();

        if (sc != null) sc.setStudentPanel(studentPanel); // <-- Dòng quan trọng
        if (tc != null) tc.setTeacherPanel(teacherPanel); // <-- Làm tương tự cho Teacher
        if (cc != null) cc.setCoursePanel(coursePanel);   // <-- Làm tương tự cho Course
        if (rc != null) rc.setRoomPanel(roomPanel);       // <-- Làm tương tự cho Room
        if (ecc != null) ecc.setClassPanel(classPanel);     // <-- Làm tương tự cho Class
        if (schc != null) schc.setSchedulePanel(schedulePanel); // <-- Làm tương tự cho Schedule
    }

    // Configure visible tabs based on user role
    public void configureViewForUser(User user) {
        if (user == null) {
            UIUtils.showErrorMessage(this, "Error", "No logged in user found. Exiting.");
            System.exit(1);
            String windowTitle = "EduZakhanh - Welcome, " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")";
            setTitle(windowTitle); // Gọi setTitle đã override (tự động cập nhật menu bar Mac)
            setStatusText("Logged in as: " + user.getRole().getDisplayName()); // Gọi hàm setStatusText mới

            tabbedPane.removeAll();
            // Biến isAdmin để dễ đọc hơn
            boolean isAdmin = (user.getRole() == Role.ADMIN);

            if (isAdmin) {
                tabbedPane.addTab("Schedule", null, schedulePanel, "Manage Class Schedules");
                tabbedPane.addTab("Classes", null, classPanel, "Manage Classes & Enrollment");
                tabbedPane.addTab("Students", null, studentPanel, "Manage Students");
                tabbedPane.addTab("Teachers", null, teacherPanel, "Manage Teachers");
                tabbedPane.addTab("Courses", null, coursePanel, "Manage Courses");
                tabbedPane.addTab("Rooms", null, roomPanel, "Manage Rooms");
            } else if (user.getRole() == Role.TEACHER) {
                tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule");
                tabbedPane.addTab("My Classes", null, classPanel, "View My Classes & Students");
                tabbedPane.addTab("My Students", null, studentPanel, "View Students in My Classes");
            } else if (user.getRole() == Role.STUDENT) {
                tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule");
                tabbedPane.addTab("My Classes", null, classPanel, "View My Classes");
            }

            // Gọi setPanelControlsEnabled dựa trên vai trò
            setPanelControlsEnabled(isAdmin);
        }

        setTitle("EduZakhanh - Welcome, " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")");
        statusLabel.setText("Logged in as: " + user.getRole().getDisplayName());
            tabbedPane.removeAll();
        if (user.getRole() == Role.ADMIN) {
            tabbedPane.addTab("Schedule", null, schedulePanel, "Manage Class Schedules");
            tabbedPane.addTab("Classes", null, classPanel, "Manage Classes & Enrollment");
            tabbedPane.addTab("Students", null, studentPanel, "Manage Students");
            tabbedPane.addTab("Teachers", null, teacherPanel, "Manage Teachers");
            tabbedPane.addTab("Courses", null, coursePanel, "Manage Courses");
            tabbedPane.addTab("Rooms", null, roomPanel, "Manage Rooms");
        } else if (user.getRole() == Role.TEACHER) {
            tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule"); // SchedulePanel needs to filter by teacher
            tabbedPane.addTab("My Classes", null, classPanel, "View My Classes & Students"); // ClassPanel needs filter/mode
            tabbedPane.addTab("My Students", null, studentPanel, "View Students in My Classes"); // StudentPanel needs filter/mode
        }else if (user.getRole() == Role.STUDENT) {
            tabbedPane.addTab("My Schedule", null, schedulePanel, "View My Schedule");
            tabbedPane.addTab("My Classes", null, classPanel, "View My Classes");

            setPanelControlsEnabled(false);
        }
    }


    // --- THÊM PHƯƠNG THỨC HELPER NÀY VÀO MainView.java ---
    private void setPanelControlsEnabled(boolean isAdmin) {

        // Student Panel
        if (studentPanel != null) {
            studentPanel.setAdminControlsEnabled(isAdmin);
        }
        // Teacher Panel
        if (teacherPanel != null) {
            teacherPanel.setAdminControlsEnabled(isAdmin);
        }
        // Course Panel
        if (coursePanel != null) {
            coursePanel.setAdminControlsEnabled(isAdmin);
        }
        // Room Panel
        if (roomPanel != null) {
            roomPanel.setAdminControlsEnabled(isAdmin);
        }
        // Class Panel
        if (classPanel != null) {
            classPanel.setAdminControlsEnabled(isAdmin);
        }
        // Schedule Panel
        if (schedulePanel != null) {
            schedulePanel.setAdminControlsEnabled(isAdmin);
        }
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
        }
    }
    @Override
    public void setTitle(String title) {
        super.setTitle(title); // Gọi hàm gốc để đặt tiêu đề cửa sổ
        // Cập nhật tên ứng dụng trên thanh menu của macOS nếu đang chạy trên Mac
        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            System.setProperty("apple.awt.application.name", title);
        }
    }
    public void setStatusText(String text) {
        if (statusLabel != null) { // Kiểm tra null
            statusLabel.setText(text == null ? "" : text); // Xử lý text null
        }
    }
}