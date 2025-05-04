package com.eduzk.view.panels;

import javax.swing.*;
import java.awt.*;

public class HelpPanel extends JPanel {

    private JTextArea helpTextArea;
    private JScrollPane scrollPane;

    public HelpPanel() {
        setLayout(new BorderLayout());
        initComponents();
        setupLayout();
        loadHelpContent();
    }

    private void initComponents() {
        helpTextArea = new JTextArea();
        helpTextArea.setEditable(false);
        helpTextArea.setLineWrap(true);
        helpTextArea.setWrapStyleWord(true); // Xuống dòng theo từ
        helpTextArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        helpTextArea.setMargin(new Insets(10, 10, 10, 10));

        scrollPane = new JScrollPane(helpTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Không cần scroll ngang
    }

    private void setupLayout() {
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadHelpContent() {
        // --- Ví dụ nội dung trợ giúp ---
        // Bạn có thể tải nội dung này từ file .txt hoặc .html nếu muốn
        String helpText = """
                EduZakhanh - Help & Information
                ==================================

                Welcome to the EduZakhanh Educational Management System!

                General Navigation:
                -------------------
                - Use the tabs at the top to switch between different management sections (Schedule, Classes, Students, etc.).
                - Most sections display data in tables. You can often sort data by clicking on column headers.
                - Buttons for adding, editing, or deleting items are usually located above or below the tables.

                Specific Sections (Admin View):
                ---------------------------------
                *   Schedule: View, add, edit, and delete class schedules within a selected date range. Avoid scheduling conflicts (overlapping times for the same teacher, room, or class).
                *   Classes: Manage educational classes. Assign courses and teachers. Enroll or remove students. Set class capacity.
                *   Students: Add, edit, view, and delete student profiles. Student phone numbers are used for registration.
                *   Teachers: Add, edit, view, and delete teacher profiles. Teacher IDs are used for registration.
                *   Courses: Manage the courses offered.
                *   Rooms: Manage classroom details like number, building, capacity, and type.
                *   Accounts: View and manage user accounts (Teachers, Students). Passwords can be reset here.
                *   Logs: View a history of actions performed within the system.

                Specific Sections (Teacher View):
                ---------------------------------
                *   My Schedule: View your teaching schedule.
                *   My Classes: View the classes you are assigned to and the students enrolled.
                *   My Students: Add, edit, view, and delete student profiles in your classes.
                *   Courses: View available courses.

                Specific Sections (Student View):
                ---------------------------------
                *   My Schedule: View your personal class schedule.
                *   My Classes: View the classes you are enrolled in.
                *   Courses: View available courses.

                Exporting Data:
                ---------------
                - Use the 'Export' -> 'Export to Excel...' menu item to save data from various sections to an Excel file (permissions vary by role).

                Themes:
                -------
                - Use the 'View' -> 'Themes' menu to change the application's appearance.

                Need Further Assistance?
                -------------------------
                - Please contact the system administrator zakhanh.
                """;

        helpTextArea.setText(helpText);
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    // Có thể thêm các phương thức khác nếu cần (ví dụ: tìm kiếm trong nội dung help)
}