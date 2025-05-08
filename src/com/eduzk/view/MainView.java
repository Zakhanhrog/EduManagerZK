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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.eduzk.view.panels.LogsPanel;
import com.eduzk.view.panels.EducationPanel;
import com.eduzk.controller.EducationController;


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
    private AccountsPanel accountsPanel;
    private JPanel statusBar;
    private LogsPanel logsPanel;
    private HelpPanel helpPanel;
    private EducationPanel educationPanel;
    private EducationController educationController;

    public LogsPanel getLogsPanel() {
        return this.logsPanel;
    }
    public MainView(MainController mainController) {
        this.mainController = mainController;
        initComponents();
        setupLayout();
        createMenuBar();
        setupWindowListener();
        setupTabChangeListener();
        configureWindow();
        updateStatusBarAppearance();

    }
    private void setupTabChangeListener() {
        if (tabbedPane == null) return;
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e.getSource() instanceof JTabbedPane) {
                    JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
                    int selectedIndex = sourceTabbedPane.getSelectedIndex();

                    if (selectedIndex != -1) {
                        Component selectedComponent = sourceTabbedPane.getComponentAt(selectedIndex);
                        if (selectedComponent instanceof AccountsPanel) {
                            System.out.println("MainView: AccountsPanel selected, calling refreshTable...");
                            ((AccountsPanel) selectedComponent).refreshTable();
                        }
                    }
                }
            }
        });
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
        accountsPanel = new AccountsPanel(null);
        logsPanel = new LogsPanel();
        helpPanel = new HelpPanel();
        educationPanel = new EducationPanel();
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
        JMenu helpMenu = new JMenu("Version");
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
        Role currentUserRole = (mainController != null && mainController.getLoggedInUser() != null)
                ? mainController.getLoggedInUser().getRole()
                : null;

        if (currentUserRole == null) {
            UIUtils.showErrorMessage(this, "Error", "Cannot determine user role for export.");
            return;
        }
        List<String> optionsList = new ArrayList<>();
        if (currentUserRole == Role.ADMIN) {
            optionsList.add(MainController.EXPORT_STUDENTS);
            optionsList.add(MainController.EXPORT_TEACHERS);
            optionsList.add(MainController.EXPORT_COURSES);
            optionsList.add(MainController.EXPORT_ROOMS);
            optionsList.add(MainController.EXPORT_CLASSES);
            optionsList.add(MainController.EXPORT_SCHEDULE);
        } else if (currentUserRole == Role.TEACHER) {
            optionsList.add(MainController.EXPORT_SCHEDULE);
            optionsList.add(MainController.EXPORT_CLASSES);
            optionsList.add(MainController.EXPORT_STUDENTS);
        }
        if (optionsList.isEmpty()) {
            UIUtils.showInfoMessage(this, "Export Not Available", "No export options available for your role.");
            return;
        }

        String[] exportOptions = optionsList.toArray(new String[0]);
        String selectedOption = (String) JOptionPane.showInputDialog(
                this,
                "Select data to export:",
                "Export to Excel",
                JOptionPane.PLAIN_MESSAGE,
                null,
                exportOptions,
                exportOptions[0]
        );

        if (selectedOption != null) {
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

                System.out.println("Export requested for: " + selectedOption + " to file: " + fileToSave.getAbsolutePath());
                mainController.exportDataToExcel(selectedOption, fileToSave, -1);
            } else {
                System.out.println("Export save cancelled by user.");
            }
        } else {
            System.out.println("Export type selection cancelled by user.");
        }
    }

    private void performLogout() {
        if (UIUtils.showConfirmDialog(this, "Logout", "Are you sure you want to logout?")) {
            System.out.println("Logout confirmed by user.");
            if (mainController != null) {
                mainController.logout();
            } else {
                System.err.println("MainView Error: mainController is null during logout!");
            }
            this.dispose();
        }else {
            System.out.println("Logout cancelled by user.");
        }
    }


    private void setupWindowListener() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainController.exitApplication();
            }
        });
    }

    private void configureWindow() {
        setMinimumSize(new Dimension(800, 600));
        setSize(1024, 768);
        setLocationRelativeTo(null);
    }

    public void setControllers(StudentController sc, TeacherController tc, CourseController cc,
                               RoomController rc, EduClassController ecc, ScheduleController schc,UserController uc, LogController lc) {
        studentPanel.setController(sc);
        teacherPanel.setController(tc);
        coursePanel.setController(cc);
        roomPanel.setController(rc);
        classPanel.setController(ecc);
        schedulePanel.setController(schc);
        studentPanel.refreshTable();
        accountsPanel.setController(uc);
        logsPanel.setController(lc);
        educationPanel.setMainController(this.mainController);

        if (sc != null) sc.setStudentPanel(studentPanel);
        if (tc != null) tc.setTeacherPanel(teacherPanel);
        if (cc != null) cc.setCoursePanel(coursePanel);
        if (rc != null) rc.setRoomPanel(roomPanel);
        if (ecc != null) ecc.setClassPanel(classPanel);
        if (schc != null) schc.setSchedulePanel(schedulePanel);
        if (uc != null) uc.setAccountsPanel(accountsPanel);
        if (lc != null) lc.setLogsPanel(logsPanel);
    }

    public void configureViewForUser(User user) {
        Role currentUserRole = user.getRole();
        if (user == null) {
            UIUtils.showErrorMessage(this, "Error", "No logged in user found. Exiting.");
            System.exit(1);
        }

        String windowTitle = "EduZakhanh - Welcome, " + user.getDisplayName() + " (" + user.getRole().getDisplayName() + ")";
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
        Icon accountsIcon = loadTabSVGICon("/icons/accounts.svg");
        Icon logsIcon = loadTabSVGICon("/icons/logs.svg");
        Icon helpIcon = loadTabSVGICon("/icons/help.svg");
        Icon educationIcon = loadTabSVGICon("/icons/education.svg"); // Nhớ tạo icon này

        // --- Thêm tab và đặt component tùy chỉnh ---
        boolean isAdmin = (user.getRole() == Role.ADMIN);
        int tabIndex = 0;

        if (isAdmin) {
            tabbedPane.addTab(null, null, schedulePanel, "Manage Class Schedules");
            JPanel scheduleTabComp = createTabComponent("Schedule", scheduleIcon);
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "Manage Classes & Enrollment");
            JPanel classesTabComp = createTabComponent("Classes", classesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);

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

            tabbedPane.addTab(null, null, educationPanel, "Học tập");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Education", educationIcon));

            JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
            separator.setPreferredSize(new Dimension(5, 20));
            tabbedPane.addTab("", null);
            JPanel separatorPanel = new JPanel();
            separatorPanel.setOpaque(false);
            separatorPanel.add(separator);
            tabbedPane.setTabComponentAt(tabIndex, separatorPanel);
            tabbedPane.setEnabledAt(tabIndex++, false);

            tabbedPane.addTab(null, null, accountsPanel, "Manage User Accounts");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Accounts", accountsIcon));

            tabbedPane.addTab(null, null, logsPanel, "View Action Logs");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Logs", logsIcon));

        } else if (user.getRole() == Role.TEACHER) {
            tabbedPane.addTab(null, null, schedulePanel, "View My Schedule");
            JPanel scheduleTabComp = createTabComponent("My Schedule", scheduleIcon);
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "View My Classes & Students");
            JPanel classesTabComp = createTabComponent("My Classes", classesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);

            tabbedPane.addTab(null, null, studentPanel, "View Students in My Classes");
            JPanel studentsTabComp = createTabComponent("My Students", studentsIcon);
            tabbedPane.setTabComponentAt(tabIndex++, studentsTabComp);

            tabbedPane.addTab(null, null, coursePanel, "View Courses");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Courses", coursesIcon));

            tabbedPane.addTab(null, null, educationPanel, "Manage Grades & Assignments");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Education", educationIcon));

        } else if (user.getRole() == Role.STUDENT) {
            tabbedPane.addTab(null, null, schedulePanel, "View My Schedule");
            JPanel scheduleTabComp = createTabComponent("My Schedule", scheduleIcon);
            tabbedPane.setTabComponentAt(tabIndex++, scheduleTabComp);

            tabbedPane.addTab(null, null, classPanel, "View My Classes");
            JPanel classesTabComp = createTabComponent("My Classes", classesIcon);
            tabbedPane.setTabComponentAt(tabIndex++, classesTabComp);

            tabbedPane.addTab(null, null, coursePanel, "View Courses");
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Courses", coursesIcon));

            tabbedPane.addTab(null, null, educationPanel, "View Learning Results and Assignments");// tooltip
            tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Learning Results", educationIcon));

        }

        tabbedPane.addTab(null, null, helpPanel, "Get Help and Information");
        tabbedPane.setTabComponentAt(tabIndex++, createTabComponent("Help", helpIcon));
        setPanelControlsForRole(currentUserRole);
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private void setPanelControlsForRole(Role userRole) {
        if (studentPanel != null) {
            studentPanel.configureControlsForRole(userRole);
        }
        if (teacherPanel != null) {
            teacherPanel.setAdminControlsEnabled(userRole == Role.ADMIN);
        }
        if (coursePanel != null) {
            coursePanel.configureControlsForRole(userRole);
        }
        if (roomPanel != null) {
            roomPanel.setAdminControlsEnabled(userRole == Role.ADMIN);
        }
        if (classPanel != null) {
            classPanel.setAdminControlsEnabled(userRole == Role.ADMIN);
        }
        if (schedulePanel != null) {
            schedulePanel.setAdminControlsEnabled(userRole == Role.ADMIN);
        }
        if (educationPanel != null) {
            educationPanel.configureControlsForRole(userRole);
        }
        if (accountsPanel != null) {
            accountsPanel.configureControlsForRole(userRole);
        }
        if (logsPanel != null) {
            logsPanel.configureControlsForRole(userRole);
        }

    }

    public void refreshSelectedTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
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
            }else if (selectedComponent instanceof AccountsPanel) {
                ((AccountsPanel) selectedComponent).refreshTable();
            }else if (selectedComponent instanceof LogsPanel) {
                ((LogsPanel) selectedComponent).refreshTable();
            }

        }
    }

    @Override
    public void setTitle(String title) {
        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            System.setProperty("apple.awt.application.name", title);
        }
    }
    public void setStatusText(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? "" : text);
        }
    }
    private Icon loadTabSVGICon(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                return new FlatSVGIcon(iconUrl);
            } else {
                System.err.println("Warning: Tab SVG icon resource not found at: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading/parsing SVG tab icon from path: " + path + " - " + e.getMessage());
            return null;
        }
    }
    private JPanel createTabComponent(final String title, final Icon icon) {
        JPanel tabComponent = new JPanel();
        tabComponent.setLayout(new BoxLayout(tabComponent, BoxLayout.Y_AXIS));
        tabComponent.setOpaque(false);

        // Label chứa Icon
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setOpaque(false);

        // Label chứa Text
        JLabel textLabel = new JLabel(title);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textLabel.setOpaque(false);
        Font tabFont = UIManager.getFont("TabbedPane.font");
        if (tabFont != null) {
            textLabel.setFont(tabFont);
        }
        Color tabForeground = UIManager.getColor("TabbedPane.foreground");
        if (tabForeground != null) {
            textLabel.setForeground(tabForeground);
        }


        // Thêm icon vào panel
        tabComponent.add(iconLabel);

        // Thêm khoảng cách nhỏ giữa icon và text
        tabComponent.add(Box.createRigidArea(new Dimension(0, 3)));

        // Thêm text vào panel
        tabComponent.add(textLabel);

        // Thêm một chút padding xung quanh để không bị sát viền tab
        tabComponent.setBorder(new EmptyBorder(4, 0, 2, 0));

        return tabComponent;
    }
    public void refreshAccountsPanelData() {
        if (accountsPanel != null && accountsPanel.isShowing()) {
            System.out.println("MainView: Requesting AccountsPanel refresh...");
            accountsPanel.refreshTable();
        } else if (accountsPanel != null) {
            System.out.println("MainView: AccountsPanel exists but is not showing, skipping refresh request.");
        } else {
            System.err.println("MainView: accountsPanel is null, cannot refresh.");
        }
    }
    public void setEducationController(EducationController ec) {
        this.educationController = ec;
        if (this.educationPanel != null && this.educationController != null && mainController != null) {
            this.educationPanel.setController(this.educationController, mainController.getUserRole());
        }
    }

}