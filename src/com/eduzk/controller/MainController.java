package com.eduzk.controller; // Hoặc com.eduhub

// --- Import các lớp cần thiết ---
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.utils.UIUtils;        // Dùng để hiển thị thông báo
import com.eduzk.view.MainView;        // Liên kết với View chính
import com.eduzk.model.dao.impl.*;   // Import các lớp DAO implementation
import com.eduzk.model.dao.interfaces.*; // Import các interface DAO (cần cho StudentController)

import org.apache.poi.ss.usermodel.*; // Cell, Row, Sheet, Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Cụ thể cho .xlsx
import java.io.FileOutputStream;      // Để ghi file
import java.io.IOException;           // Để bắt lỗi IO
import java.io.File;                // Để làm việc với đối tượng File
import java.util.List;              // Để làm việc với danh sách dữ liệu
import com.eduzk.model.entities.*;   // Import các entities

import javax.swing.JOptionPane;       // Dùng để hiển thị lỗi nghiêm trọng

/**
 * Controller chính quản lý luồng chính của ứng dụng sau khi đăng nhập.
 * Khởi tạo các DAO và Controller con.
 * Liên kết với MainView.
 * (Phiên bản này sử dụng đường dẫn tương đối cho thư mục dữ liệu)
 */
public class MainController {

    // Thông tin người dùng đang đăng nhập
    private final User loggedInUser;
    // Tham chiếu đến cửa sổ chính
    private MainView mainView;

    // Các Controller con cho từng module chức năng
    private StudentController studentController;
    private TeacherController teacherController;
    private CourseController courseController;
    private RoomController roomController;
    private EduClassController eduClassController;
    private ScheduleController scheduleController;
    // Thêm UserController nếu cần chức năng quản lý người dùng từ Admin

    // --- Định nghĩa đường dẫn tương đối đến các file dữ liệu ---
    // Giả định thư mục 'data' nằm cùng cấp với nơi ứng dụng được chạy
    private static final String DATA_DIR = "data/";
    private static final String ID_FILE = DATA_DIR + "next_ids.dat";
    private static final String USERS_FILE = DATA_DIR + "users.dat";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String TEACHERS_FILE = DATA_DIR + "teachers.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    private static final String ROOMS_FILE = DATA_DIR + "rooms.dat";
    private static final String EDUCLASSES_FILE = DATA_DIR + "educlasses.dat";
    private static final String SCHEDULES_FILE = DATA_DIR + "schedules.dat";
    // --- Kết thúc định nghĩa đường dẫn ---

    /**
     * Constructor chính, nhận User đã đăng nhập và khởi tạo các thành phần.
     * @param loggedInUser Đối tượng User chứa thông tin người đăng nhập.
     */
    public MainController(User loggedInUser) {
        if (loggedInUser == null) {
            // Xử lý lỗi nếu không có user đăng nhập (không nên xảy ra)
            System.err.println("CRITICAL: MainController initialized with null user!");
            JOptionPane.showMessageDialog(null, "Login information is missing. Application cannot continue.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        this.loggedInUser = loggedInUser;
        // Khởi tạo ngay lập tức các DAO và Controller con
        initializeDAOsAndControllers();
    }

    /**
     * Thiết lập tham chiếu đến MainView và cấu hình nó.
     * Được gọi từ bên ngoài (thường là AuthController) sau khi MainController được tạo.
     * @param mainView Instance của cửa sổ chính.
     */
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
        if (this.mainView == null) {
            System.err.println("Error: MainView is null in setMainView!");
            return; // Không thể tiếp tục nếu view là null
        }

        // 1. Truyền các controller con đã được khởi tạo vào MainView
        mainView.setControllers(
                studentController,
                teacherController,
                courseController,
                roomController,
                eduClassController,
                scheduleController
                // Truyền các controller khác nếu có
        );
        if (scheduleController != null) { // Kiểm tra null trước khi gọi setter
            scheduleController.setMainView(mainView); // <-- THÊM DÒNG NÀY
        }

        // 2. Yêu cầu MainView tự cấu hình giao diện (tab, nút) dựa trên vai trò user
        mainView.configureViewForUser(loggedInUser);

        // 3. Yêu cầu MainView làm mới dữ liệu cho tab đang được chọn (tab mặc định)
        mainView.refreshSelectedTab();
    }

    /**
     * Khởi tạo tất cả các đối tượng DAO và Controller con cần thiết cho ứng dụng.
     * Sử dụng các đường dẫn file tương đối đã định nghĩa.
     */
    private void initializeDAOsAndControllers() {
        try {
            // Khởi tạo các DAO Implementation
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

            // Khởi tạo các controller khác nếu cần (ví dụ: UserController cho Admin)

            System.out.println("DAOs and Controllers initialized successfully.");

        } catch (Exception e) {
            // Lỗi nghiêm trọng trong quá trình khởi tạo DAO/Controller
            System.err.println("!!! CRITICAL ERROR DURING DAO/CONTROLLER INITIALIZATION !!!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "A critical error occurred while initializing application components.\nError: " + e.getMessage(),
                    "Initialization Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Thoát nếu không khởi tạo được
        }
    }

    // --- Các phương thức tiện ích và xử lý sự kiện chung ---

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public Role getUserRole() {
        return loggedInUser != null ? loggedInUser.getRole() : null; // Trả về null nếu không có user
    }

    /** Xử lý yêu cầu thoát ứng dụng (thường gọi từ MainView). */
    public void exitApplication() {
        if (UIUtils.showConfirmDialog(mainView, "Exit Confirmation", "Are you sure you want to exit EduManager?")) {
            System.out.println("Exiting application...");
            // Có thể thêm các hành động dọn dẹp ở đây nếu cần (ví dụ: đóng kết nối DB)
            System.exit(0); // Kết thúc tiến trình Java
        }
    }

    /** Hiển thị hộp thoại "About". */
    public void showAboutDialog() {
        if (mainView != null) {
            UIUtils.showInfoMessage(mainView, "About EduManager", "EduManager - Educational Management System\nVersion 1.0 (Basic)");
        }
    }

    /** Xử lý yêu cầu đăng xuất (logic cơ bản). */
    public void logout() {
        System.out.println("Logout initiated in MainController.");
        // Logic đầy đủ:
        // 1. Đóng MainView hiện tại.
        // 2. Gọi lại AuthController để hiển thị LoginView mới.
        // Cần cơ chế liên lạc giữa các controller hoặc quay lại App.java.
        // Tạm thời chỉ in ra thông báo. Việc đóng cửa sổ đã được xử lý trong MainView.performLogout().
    }

    // --- Getters cho các Controller con (để MainView có thể truyền chúng đi nếu cần) ---
    public StudentController getStudentController() { return studentController; }
    public TeacherController getTeacherController() { return teacherController; }
    public CourseController getCourseController() { return courseController; }
    public RoomController getRoomController() { return roomController; }
    public EduClassController getEduClassController() { return eduClassController; }
    public ScheduleController getScheduleController() { return scheduleController; }

    // --- THÊM PHƯƠNG THỨC MỚI NÀY ---
    /**
     * Xử lý yêu cầu xuất dữ liệu ra file Excel.
     * Gọi các Controller con để lấy dữ liệu và sử dụng Apache POI để ghi file.
     * @param dataType Loại dữ liệu cần xuất (chuỗi từ JComboBox trong MainView).
     * @param outputFile Đối tượng File đại diện cho file Excel cần lưu.
     */
    public void exportDataToExcel(String dataType, File outputFile) {
        // Sử dụng try-with-resources để đảm bảo Workbook được đóng đúng cách
        // Tạo một Workbook mới (định dạng .xlsx)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(dataType); // Tạo sheet với tên là loại dữ liệu

            // --- Logic lấy dữ liệu và ghi vào sheet dựa trên dataType ---
            switch (dataType) {
                case "Student List":
                    exportStudentData(sheet);
                    break;
                case "Teacher List":
                    exportTeacherData(sheet);
                    break;
                case "Course List":
                    exportCourseData(sheet);
                    break;
                case "Room List":
                    exportRoomData(sheet);
                    break;
                case "Class List (Basic Info)":
                    exportClassData(sheet);
                    break;
                case "Schedule (Current View/All)":
                    exportScheduleData(sheet); // Cần lấy dữ liệu phù hợp
                    break;
                // Thêm các case khác nếu cần
                default:
                    System.err.println("Unsupported data type for export: " + dataType);
                    UIUtils.showWarningMessage(mainView, "Export Failed", "Data type '" + dataType + "' is not supported for export yet.");
                    return; // Thoát nếu không hỗ trợ
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

        } catch (Exception e) { // Bắt lỗi chung khi tạo Workbook hoặc lấy dữ liệu
            System.err.println("Error during Excel export process: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(mainView, "Export Error", "An unexpected error occurred during export.\nError: " + e.getMessage());
        }
    }

    // --- CÁC PHƯƠNG THỨC HELPER ĐỂ XUẤT TỪNG LOẠI DỮ LIỆU ---
    // Bạn cần tự viết code chi tiết cho các phương thức này sử dụng Apache POI

    private void exportStudentData(Sheet sheet) {
        System.out.println("Exporting Student data...");
        if (studentController == null) { System.err.println("StudentController is null!"); return; }

        List<Student> students = studentController.getAllStudents(); // Lấy danh sách student
        if (students == null || students.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No student data to export."); return; }

        // Tạo hàng tiêu đề
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Full Name", "Date of Birth", "Gender", "Phone", "Email", "Parent"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            // (Tùy chọn: Định dạng tiêu đề - bold, background)
        }

        // Ghi dữ liệu từng học viên
        int rowNum = 1;
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(student.getStudentId());
            row.createCell(1).setCellValue(student.getFullName());
            // Xử lý ngày tháng (cần định dạng)
            Cell dobCell = row.createCell(2);
            if (student.getDateOfBirth() != null) {
                dobCell.setCellValue(student.getDateOfBirth().toString()); // Hoặc dùng DateUtils.formatDate
                // (Tùy chọn: Định dạng cell là Date trong Excel)
            } else {
                dobCell.setCellValue("");
            }
            row.createCell(3).setCellValue(student.getGender());
            row.createCell(4).setCellValue(student.getPhone());
            row.createCell(5).setCellValue(student.getEmail());
            row.createCell(6).setCellValue(student.getParentName());
        }

        // (Tùy chọn: Tự động điều chỉnh độ rộng cột)
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
            row.createCell(6).setCellValue(teacher.isActive()); // Excel sẽ hiển thị TRUE/FALSE
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

        // LỰA CHỌN 2: Nếu không lấy được theo ngày hoặc muốn lấy tất cả
        if (!useDateRange) { // Hoặc if (true) nếu luôn muốn lấy tất cả
            System.out.println("Exporting ALL schedules...");
            schedules = scheduleController.getAllSchedules(); // <-- Gọi hàm mới
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
            // Dùng helper từ ScheduleController để lấy tên
            row.createCell(4).setCellValue(scheduleController.getClassNameById(schedule.getClassId()));
            row.createCell(5).setCellValue(scheduleController.getTeacherNameById(schedule.getTeacherId()));
            row.createCell(6).setCellValue(scheduleController.getRoomNameById(schedule.getRoomId()));
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }
}