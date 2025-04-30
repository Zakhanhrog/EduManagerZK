package com.eduzk.controller; // Hoặc com.eduhub

// --- Import các lớp cần thiết ---
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.utils.UIUtils;        // Dùng để hiển thị thông báo
import com.eduzk.view.MainView;        // Liên kết với View chính
import com.eduzk.model.dao.impl.*;   // Import các lớp DAO implementation
import com.eduzk.model.dao.interfaces.*; // Import các interface DAO (cần cho StudentController)

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
}