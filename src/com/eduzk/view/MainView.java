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
import java.net.URL;
import javax.swing.border.EmptyBorder;
import com.formdev.flatlaf.extras.FlatSVGIcon;

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

        if (sc != null) sc.setStudentPanel(studentPanel);
        if (tc != null) tc.setTeacherPanel(teacherPanel);
        if (cc != null) cc.setCoursePanel(coursePanel);
        if (rc != null) rc.setRoomPanel(roomPanel);
        if (ecc != null) ecc.setClassPanel(classPanel);
        if (schc != null) schc.setSchedulePanel(schedulePanel);
    }

    // Configure visible tabs based on user role
    public void configureViewForUser(User user) {
        if (user == null) {
            UIUtils.showErrorMessage(this, "Error", "No logged in user found. Exiting.");
            System.exit(1);
        }

        String windowTitle = "EduZakhanh - Welcome, " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")";
        setTitle(windowTitle);
        setStatusText("Logged in as: " + user.getRole().getDisplayName());

        tabbedPane.removeAll();

        // --- Load các SVG icon ---
        Icon scheduleIcon = loadTabSVGICon("/icons/schedule.svg");
        Icon classesIcon = loadTabSVGICon("/icons/classes.svg");
        Icon studentsIcon = loadTabSVGICon("/icons/students.svg");
        Icon teachersIcon = loadTabSVGICon("/icons/teachers.svg");
        Icon coursesIcon = loadTabSVGICon("/icons/courses.svg");
        Icon roomsIcon = loadTabSVGICon("/icons/rooms.svg");

        // --- Thêm tab và đặt component tùy chỉnh (Logic giữ nguyên) ---
        boolean isAdmin = (user.getRole() == Role.ADMIN);
        int tabIndex = 0;

        if (isAdmin) {
            tabbedPane.addTab(null, null, schedulePanel, "Manage Class Schedules");
            JPanel scheduleTabComp = createTabComponent("Schedule", scheduleIcon); // Truyền SVG Icon
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "Manage Classes & Enrollment");
            JPanel classesTabComp = createTabComponent("Classes", classesIcon);     // Truyền SVG Icon
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);

            // ... Làm tương tự cho Students, Teachers, Courses, Rooms ...
            tabbedPane.addTab(null, null, studentPanel, "Manage Students");
            JPanel studentsTabComp = createTabComponent("Students", studentsIcon);
            tabbedPane.setTabComponentAt(tabIndex++, studentsTabComp);

            tabbedPane.addTab(null, null, teacherPanel, "Manage Teachers");
            JPanel teachersTabComp = createTabComponent("Teachers", teachersIcon);
            tabbedPane.setTabComponentAt(tabIndex++, teachersTabComp);

            tabbedPane.addTab(null, null, coursePanel, "Manage Courses");
            JPanel coursesTabComp = createTabComponent("Courses", coursesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, coursesTabComp);

            tabbedPane.addTab(null, null, roomPanel, "Manage Rooms");
            JPanel roomsTabComp = createTabComponent("Rooms", roomsIcon);
            tabbedPane.setTabComponentAt(tabIndex++, roomsTabComp);


        } else if (user.getRole() == Role.TEACHER) {
            // ... Làm tương tự cho các tab của Teacher ...
            tabbedPane.addTab(null, null, schedulePanel, "View My Schedule");
            JPanel scheduleTabComp = createTabComponent("My Schedule", scheduleIcon);
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "View My Classes & Students");
            JPanel classesTabComp = createTabComponent("My Classes", classesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);

            tabbedPane.addTab(null, null, studentPanel, "View Students in My Classes");
            JPanel studentsTabComp = createTabComponent("My Students", studentsIcon);
            tabbedPane.setTabComponentAt(tabIndex++, studentsTabComp);

        } else if (user.getRole() == Role.STUDENT) {
            // ... Làm tương tự cho các tab của Student ...
            tabbedPane.addTab(null, null, schedulePanel, "View My Schedule");
            JPanel scheduleTabComp = createTabComponent("My Schedule", scheduleIcon);
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "View My Classes");
            JPanel classesTabComp = createTabComponent("My Classes", classesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);
        }

        setPanelControlsEnabled(isAdmin);

        tabbedPane.revalidate();
        tabbedPane.repaint();
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
    private Icon loadTabSVGICon(String path) { // Đổi tên để rõ ràng hơn (tùy chọn)
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                // *** SỬA Ở ĐÂY: Tạo FlatSVGIcon thay vì ImageIcon ***
                // Có thể đặt kích thước mặc định nhỏ hơn nếu SVG gốc quá lớn
                // Ví dụ: return new FlatSVGIcon(iconUrl).derive(16, 16);
                return new FlatSVGIcon(iconUrl); // Load với kích thước gốc của SVG (hoặc theo viewBox)
            } else {
                System.err.println("Warning: Tab SVG icon resource not found at: " + path);
                return null;
            }
        } catch (Exception e) {
            // Lỗi này có thể xảy ra nếu file SVG không hợp lệ hoặc jsvg có vấn đề
            System.err.println("Error loading/parsing SVG tab icon from path: " + path + " - " + e.getMessage());
            // e.printStackTrace(); // Bỏ comment dòng này để xem chi tiết lỗi nếu cần
            return null; // Trả về null khi có lỗi
        }
    }
    private JPanel createTabComponent(final String title, final Icon icon) {
        // Panel chính cho tab, dùng BoxLayout để xếp dọc
        JPanel tabComponent = new JPanel();
        tabComponent.setLayout(new BoxLayout(tabComponent, BoxLayout.Y_AXIS)); // Xếp dọc
        tabComponent.setOpaque(false); // QUAN TRỌNG: Để nền tab của LookAndFeel hiển thị qua

        // Label chứa Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa icon theo chiều ngang
        iconLabel.setOpaque(false);

        // Label chứa Text
        JLabel textLabel = new JLabel(title);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa text theo chiều ngang
        textLabel.setOpaque(false);
        // Đặt font và màu giống như tab mặc định (tùy chọn, nhưng nên làm)
        Font tabFont = UIManager.getFont("TabbedPane.font");
        if (tabFont != null) {
            // Có thể làm font nhỏ hơn một chút nếu cần
            // textLabel.setFont(tabFont.deriveFont(tabFont.getSize2D() - 1f));
            textLabel.setFont(tabFont);
        }
        Color tabForeground = UIManager.getColor("TabbedPane.foreground");
        if (tabForeground != null) {
            textLabel.setForeground(tabForeground);
        }


        // Thêm icon vào panel
        tabComponent.add(iconLabel);

        // Thêm khoảng cách nhỏ giữa icon và text
        tabComponent.add(Box.createRigidArea(new Dimension(0, 3))); // 3 pixel dọc

        // Thêm text vào panel
        tabComponent.add(textLabel);

        // Thêm một chút padding xung quanh để không bị sát viền tab
        tabComponent.setBorder(new EmptyBorder(4, 0, 2, 0)); // top, left, bottom, right

        return tabComponent;
    }
}