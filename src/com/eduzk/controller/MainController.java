package com.eduzk.controller;

import com.eduzk.model.entities.*;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.MainView;
import com.eduzk.model.dao.impl.*;
import com.eduzk.model.dao.interfaces.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collections;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import java.awt.Component;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainController {
    public static final String EXPORT_STUDENTS = "Student List";
    public static final String EXPORT_TEACHERS = "Teacher List";
    public static final String EXPORT_COURSES = "Course List";
    public static final String EXPORT_ROOMS = "Room List";
    public static final String EXPORT_CLASSES = "Class List (Basic Info)";
    public static final String EXPORT_SCHEDULE = "Schedule (Current View. All)";

    private final User loggedInUser;
    private MainView mainView;

    private final AuthController authController;
    private StudentController studentController;
    private TeacherController teacherController;
    private CourseController courseController;
    private RoomController roomController;
    private EduClassController eduClassController;
    private ScheduleController scheduleController;

    private static final String DATA_DIR = "data/";
    private static final String ID_FILE = DATA_DIR + "next_ids.dat";
    private static final String USERS_FILE = DATA_DIR + "users.dat";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String TEACHERS_FILE = DATA_DIR + "teachers.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    private static final String ROOMS_FILE = DATA_DIR + "rooms.dat";
    private static final String EDUCLASSES_FILE = DATA_DIR + "educlasses.dat";
    private static final String SCHEDULES_FILE = DATA_DIR + "schedules.dat";

    public MainController(User loggedInUser, AuthController authController) {
        if (loggedInUser == null) {
            System.exit(1);
        }
        if (authController == null) { throw new IllegalArgumentException("AuthController cannot be null in MainController"); }
        this.loggedInUser = loggedInUser;
        this.authController = authController;
        initializeDAOsAndControllers();
    }

    public void setMainView(MainView mainView) {
        this.mainView = mainView;
        if (this.mainView == null) {
            System.err.println("Error: MainView is null in setMainView!");
            return;
        }

        // 1. Truyền các controller con đã được khởi tạo vào MainView
        mainView.setControllers(
                studentController,
                teacherController,
                courseController,
                roomController,
                eduClassController,
                scheduleController
        );
        if (scheduleController != null) { // Kiểm tra null trước khi gọi setter
            scheduleController.setMainView(mainView); // <-- THÊM DÒNG NÀY
        }

        // 2. Yêu cầu MainView tự cấu hình giao diện (tab, nút) dựa trên vai trò user
        mainView.configureViewForUser(loggedInUser);

        // 3. Yêu cầu MainView làm mới dữ liệu cho tab đang được chọn (tab mặc định)
        mainView.refreshSelectedTab();
    }

    private void initializeDAOsAndControllers() {
        try {
            UserDAOImpl userDAO = new UserDAOImpl(USERS_FILE, ID_FILE); // Cần cho việc lấy thông tin user khác nếu là Admin
            StudentDAOImpl studentDAO = new StudentDAOImpl(STUDENTS_FILE, ID_FILE);
            TeacherDAOImpl teacherDAO = new TeacherDAOImpl(TEACHERS_FILE, ID_FILE);
            CourseDAOImpl courseDAO = new CourseDAOImpl(COURSES_FILE, ID_FILE);
            RoomDAOImpl roomDAO = new RoomDAOImpl(ROOMS_FILE, ID_FILE);
            EduClassDAOImpl eduClassDAO = new EduClassDAOImpl(EDUCLASSES_FILE, ID_FILE);
            ScheduleDAOImpl scheduleDAO = new ScheduleDAOImpl(SCHEDULES_FILE, ID_FILE);
            studentController = new StudentController(studentDAO, eduClassDAO, userDAO, loggedInUser);// <-- Thêm userDAO
            teacherController = new TeacherController(teacherDAO, loggedInUser);
            courseController = new CourseController(courseDAO, loggedInUser);
            roomController = new RoomController(roomDAO, loggedInUser);
            eduClassController = new EduClassController(eduClassDAO, courseDAO, teacherDAO, studentDAO, loggedInUser);
            scheduleController = new ScheduleController(scheduleDAO, eduClassDAO, teacherDAO, roomDAO, loggedInUser);

            System.out.println("DAOs and Controllers initialized successfully.");

        } catch (Exception e) {
            System.err.println("!!! CRITICAL ERROR DURING DAO/CONTROLLER INITIALIZATION !!!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "A critical error occurred while initializing application components.\nError: " + e.getMessage(),
                    "Initialization Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public Role getUserRole() {
        return loggedInUser != null ? loggedInUser.getRole() : null; // Trả về null nếu không có user
    }

    public void exitApplication() {
        if (UIUtils.showConfirmDialog(mainView, "Exit Confirmation", "Are you sure you want to exit EduManager?")) {
            System.out.println("Exiting application...");
            System.exit(0);
        }
    }

    /** Hiển thị hộp thoại "About". */
    public void showAboutDialog() {
        if (mainView != null) {
            UIUtils.showInfoMessage(mainView, "About EduManager", "EduManager - Educational Management System\nVersion 1.0 (Basic)");
        }
        if (this.authController != null) {
            System.out.println("MainController: Calling authController.showLoginView()...");
            this.authController.showLoginView();
        } else {
            System.err.println("MainController Error: authController is null, cannot show login view!");
        }
    }

    /** Xử lý yêu cầu đăng xuất (logic cơ bản). */
    public void logout() {
        System.out.println("MainController: logout() called.");
        if (this.authController != null) {
            System.out.println("MainController: Calling authController.showLoginView()...");
            this.authController.showLoginView();
        } else {
            System.err.println("MainController Error: authController is null! Cannot show login view.");
            JOptionPane.showMessageDialog(null, "Logout failed due to an internal error.", "Logout Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public StudentController getStudentController() { return studentController; }
    public TeacherController getTeacherController() { return teacherController; }
    public CourseController getCourseController() { return courseController; }
    public RoomController getRoomController() { return roomController; }
    public EduClassController getEduClassController() { return eduClassController; }
    public ScheduleController getScheduleController() { return scheduleController; }

    public void exportDataToExcel(String dataType, File outputFile) {
        System.out.println("Starting Excel export for: " + dataType + " by user role: " + getUserRole()); // Log thêm role

        Role userRole = getUserRole();
        if (userRole == null) {
            UIUtils.showErrorMessage(mainView, "Permission Denied", "Cannot verify user permissions.");
            return;
        }
        if (userRole == Role.STUDENT) {
            UIUtils.showErrorMessage(mainView, "Permission Denied", "Export function is not available for your role.");
            return;
        }
        Workbook workbook = null;
        try  {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(dataType);

            switch (dataType) {
                case EXPORT_STUDENTS:
                    exportStudentData(sheet);
                    break;
                case EXPORT_TEACHERS:
                    if (userRole != Role.ADMIN) throw new SecurityException("Permission denied for exporting teacher list.");
                    exportTeacherData(sheet);
                    break;
                case EXPORT_COURSES:
                    if (userRole != Role.ADMIN) throw new SecurityException("Permission denied for exporting course list.");
                    exportCourseData(sheet);
                    break;
                case EXPORT_ROOMS:
                    if (userRole != Role.ADMIN) throw new SecurityException("Permission denied for exporting room list.");
                    exportRoomData(sheet);
                    break;
                case EXPORT_CLASSES:
                    exportClassData(sheet);
                    break;
                case EXPORT_SCHEDULE:
                    exportScheduleData(sheet);
                    break;
                default:
                    System.err.println("Unsupported data type for export: " + dataType);
                    UIUtils.showWarningMessage(mainView, "Export Failed", "Data type '" + dataType + "' is not supported for export yet.");
                    return;
            }

            // --- Ghi Workbook ra file ---
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
                System.out.println("Excel file exported successfully to: " + outputFile.getAbsolutePath());
                UIUtils.showInfoMessage(mainView, "Export Successful", "Data exported successfully to:\n" + outputFile.getName());
            } catch (IOException e) {
                System.err.println("Error writing Excel file: " + e.getMessage());
                e.printStackTrace();
                UIUtils.showErrorMessage(mainView, "Export Error", "Could not write to file:\n" + outputFile.getName() + "\nError: " + e.getMessage());
            }

        } catch (SecurityException secEx) { // Bắt lỗi phân quyền
            System.err.println("Permission denied during export: " + secEx.getMessage());
            UIUtils.showErrorMessage(mainView, "Permission Denied", "You do not have permission to export this data type.");
        } catch (Exception e) { // Bắt lỗi chung khi tạo Workbook hoặc lấy dữ liệu
            System.err.println("Error during Excel export process: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(mainView, "Export Error", "An unexpected error occurred during export.\nError: " + e.getMessage());
        }
        finally {
            // Đóng workbook
            if (workbook != null) {
                try { workbook.close(); } catch (IOException e) { /* Ignore close error */ }
            }
        }
    }

    // --- CÁC PHƯƠNG THỨC HELPER ĐỂ XUẤT TỪNG LOẠI DỮ LIỆU ---
    private void exportStudentData(Sheet sheet) {
        System.out.println("Exporting Student data...");
        if (studentController == null) { System.err.println("StudentController is null!"); return; }

        List<Student> students = studentController.getAllStudents();
        if (students == null || students.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No student data to export."); return; }

        // Tạo hàng tiêu đề
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Full Name", "Date of Birth", "Gender", "Phone", "Email", "Parent"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Ghi dữ liệu từng học viên
        int rowNum = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getStudentId());
            row.createCell(1).setCellValue(student.getFullName());
            Cell dobCell = row.createCell(2);
            if (student.getDateOfBirth() != null) {
                dobCell.setCellValue(student.getDateOfBirth().toString());
            } else {
                dobCell.setCellValue("");
            }
            row.createCell(3).setCellValue(student.getGender());
            row.createCell(4).setCellValue(student.getPhone());
            row.createCell(5).setCellValue(student.getEmail());
            row.createCell(6).setCellValue(student.getParentName());
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void exportTeacherData(Sheet sheet) {
        System.out.println("Exporting Teacher data...");
        if (teacherController == null) { System.err.println("TeacherController is null!"); return; }
        List<Teacher> teachers = teacherController.getAllTeachers();
        if (teachers == null || teachers.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No teacher data to export."); return; }

        // Tạo header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Full Name", "Specialization", "Phone", "Email", "DOB", "Active"};
        for(int i=0; i<headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        // Ghi data
        int rowNum = 1;
        for(Teacher teacher : teachers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(teacher.getTeacherId());
            row.createCell(1).setCellValue(teacher.getFullName());
            row.createCell(2).setCellValue(teacher.getSpecialization());
            row.createCell(3).setCellValue(teacher.getPhone());
            row.createCell(4).setCellValue(teacher.getEmail());
            Cell dobCell = row.createCell(5);
            if (teacher.getDateOfBirth() != null) dobCell.setCellValue(teacher.getDateOfBirth().toString());
            row.createCell(6).setCellValue(teacher.isActive());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportCourseData(Sheet sheet) {
        System.out.println("Exporting Course data...");
        if (courseController == null) { System.err.println("CourseController is null!"); return; }
        List<Course> courses = courseController.getAllCourses();
        if (courses == null || courses.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No course data to export."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Code", "Name", "Level", "Credits", "Description"};
        for(int i=0; i<headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        int rowNum = 1;
        for(Course course : courses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(course.getCourseId());
            row.createCell(1).setCellValue(course.getCourseCode());
            row.createCell(2).setCellValue(course.getCourseName());
            row.createCell(3).setCellValue(course.getLevel());
            row.createCell(4).setCellValue(course.getCredits());
            row.createCell(5).setCellValue(course.getDescription());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportRoomData(Sheet sheet) {
        System.out.println("Exporting Room data...");
        if (roomController == null) { System.err.println("RoomController is null!"); return; }
        List<Room> rooms = roomController.getAllRooms();
        if (rooms == null || rooms.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No room data to export."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Number", "Building", "Capacity", "Type", "Available"};
        for(int i=0; i<headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        int rowNum = 1;
        for(Room room : rooms) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(room.getRoomId());
            row.createCell(1).setCellValue(room.getRoomNumber());
            row.createCell(2).setCellValue(room.getBuilding());
            row.createCell(3).setCellValue(room.getCapacity());
            row.createCell(4).setCellValue(room.getType());
            row.createCell(5).setCellValue(room.isAvailable());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportClassData(Sheet sheet) {
        System.out.println("Exporting Class (basic info) data...");
        if (eduClassController == null) { System.err.println("EduClassController is null!"); return; }
        List<EduClass> classes = eduClassController.getAllEduClasses();
        if (classes == null || classes.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No class data to export."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Class Name", "Course Code", "Course Name", "Teacher Name", "Year", "Semester", "Max Capacity", "Enrolled Count"};
        for(int i=0; i<headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        int rowNum = 1;
        for(EduClass eduClass : classes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(eduClass.getClassId());
            row.createCell(1).setCellValue(eduClass.getClassName());
            row.createCell(2).setCellValue(eduClass.getCourse() != null ? eduClass.getCourse().getCourseCode() : "N/A");
            row.createCell(3).setCellValue(eduClass.getCourse() != null ? eduClass.getCourse().getCourseName() : "N/A");
            row.createCell(4).setCellValue(eduClass.getPrimaryTeacher() != null ? eduClass.getPrimaryTeacher().getFullName() : "N/A");
            row.createCell(5).setCellValue(eduClass.getAcademicYear());
            row.createCell(6).setCellValue(eduClass.getSemester());
            row.createCell(7).setCellValue(eduClass.getMaxCapacity());
            row.createCell(8).setCellValue(eduClass.getCurrentEnrollment());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportScheduleData(Sheet sheet) {
        System.out.println("Exporting Schedule data...");
        if (scheduleController == null) { System.err.println("ScheduleController is null!"); return; }
        List<Schedule> schedules = null;
        boolean useDateRange = false;

        // Lay tat ca khong lay theo ngay
        if (!useDateRange) {
            System.out.println("Exporting ALL schedules...");
            schedules = scheduleController.getAllSchedules();
        }

        if (schedules == null || schedules.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No schedule data to export (for the selected range/all)."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Date", "Start Time", "End Time", "Class Name", "Teacher Name", "Room Name"};
        for(int i=0; i<headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        int rowNum = 1;
        for(Schedule schedule : schedules) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(schedule.getScheduleId());
            Cell dateCell = row.createCell(1);
            if (schedule.getDate() != null) dateCell.setCellValue(schedule.getDate().toString());
            Cell startCell = row.createCell(2);
            if (schedule.getStartTime() != null) startCell.setCellValue(schedule.getStartTime().toString());
            Cell endCell = row.createCell(3);
            if (schedule.getEndTime() != null) endCell.setCellValue(schedule.getEndTime().toString());
            row.createCell(4).setCellValue(scheduleController.getClassNameById(schedule.getClassId()));
            row.createCell(5).setCellValue(scheduleController.getTeacherNameById(schedule.getTeacherId()));
            row.createCell(6).setCellValue(scheduleController.getRoomNameById(schedule.getRoomId()));
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    public void setLookAndFeel(String lafClassName) {
        try {
            UIManager.setLookAndFeel(lafClassName);
            if (mainView != null) {
                SwingUtilities.updateComponentTreeUI(mainView);
                System.out.println("Look and Feel updated to: " + lafClassName);
            }
        } catch (Exception e) {
            System.err.println("Failed to set Look and Feel to: " + lafClassName);
            e.printStackTrace();
            UIUtils.showErrorMessage(mainView, "Theme Error", "Could not apply the selected theme.");
        }
    }
}