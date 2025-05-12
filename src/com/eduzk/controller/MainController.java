package com.eduzk.controller;

import java.io.IOException;
import javax.swing.*;
import com.eduzk.model.dao.impl.AssignmentDAOImpl;
import com.eduzk.model.dao.impl.IdGenerator;
import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.entities.Role;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.MainView;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.File;
import java.util.List;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.model.dao.interfaces.IAcademicRecordDAO;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainController {
    public static final String EXPORT_STUDENTS = "Student List";
    public static final String EXPORT_TEACHERS = "Teacher List";
    public static final String EXPORT_COURSES = "Course List";
    public static final String EXPORT_ROOMS = "Room List";
    public static final String EXPORT_CLASSES = "Class List (Basic Info)";
    public static final String EXPORT_SCHEDULE = "Schedule (Current View. All)";
    public static final String EXPORT_GRADES = "Student Grades";
    private final User loggedInUser;
    private MainView mainView;
    private final AuthController authController;
    private StudentController studentController;
    private TeacherController teacherController;
    private CourseController courseController;
    private RoomController roomController;
    private EduClassController eduClassController;
    private ScheduleController scheduleController;
    private UserController userController;
    private LogController logController;
    private EducationController educationController;
    private final IAcademicRecordDAO recordDAO;
    private final IAssignmentDAO assignmentDAO;
    private final IdGenerator idGenerator;

    public MainController(User loggedInUser,
                          AuthController authController,
                          IUserDAO userDAO,
                          IStudentDAO studentDAO,
                          ITeacherDAO teacherDAO,
                          ICourseDAO courseDAO,
                          IRoomDAO roomDAO,
                          IEduClassDAO eduClassDAO,
                          IScheduleDAO scheduleDAO,
                          LogService logService,
                          IAcademicRecordDAO recordDAO,
                          IdGenerator idGenerator)
    {
        if (recordDAO == null) {
            throw new IllegalArgumentException("AcademicRecordDAO cannot be null in MainController");
        }

        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null in MainController");
        }
        if (loggedInUser == null) {
            throw new IllegalArgumentException("LoggedInUser cannot be null in MainController");
        }
        if (authController == null) {
            throw new IllegalArgumentException("AuthController cannot be null in MainController");
        }
        this.recordDAO = recordDAO;
        this.idGenerator = idGenerator;
        this.loggedInUser = loggedInUser;
        this.authController = authController;

        try {
            this.assignmentDAO = new AssignmentDAOImpl(this.idGenerator);
        } catch (Exception e) {
            System.err.println("!!! CRITICAL ERROR INITIALIZING AssignmentDAO !!!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to initialize Assignment Data Access Object.\nError: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            throw new RuntimeException("Failed to initialize AssignmentDAO", e);
        }

        EduClassController tempEduClassController = null;
        try {
            tempEduClassController = new EduClassController(eduClassDAO, courseDAO, teacherDAO, studentDAO, loggedInUser, logService);
        } catch (Exception e) {
            System.err.println("!!! CRITICAL ERROR INITIALIZING EduClassController !!!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to initialize EduClassController.\nError: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }


        initializeControllers(
                userDAO, studentDAO, teacherDAO, courseDAO, roomDAO, eduClassDAO,
                scheduleDAO, logService, recordDAO,
                this.assignmentDAO,
                tempEduClassController
        );
    }

    private void initializeControllers(
            IUserDAO userDAO, IStudentDAO studentDAO, ITeacherDAO teacherDAO,
            ICourseDAO courseDAO, IRoomDAO roomDAO, IEduClassDAO eduClassDAO,
            IScheduleDAO scheduleDAO, LogService logService,
            IAcademicRecordDAO recordDAO,
            IAssignmentDAO assignmentDAO,
            EduClassController eduClassController
    )
    {
        try {
            this.eduClassController = eduClassController;
            studentController = new StudentController(studentDAO, eduClassDAO, userDAO, loggedInUser, logService);
            teacherController = new TeacherController(teacherDAO, userDAO, loggedInUser, logService);
            courseController = new CourseController(courseDAO, loggedInUser, logService);
            roomController = new RoomController(roomDAO, loggedInUser, logService);
            scheduleController = new ScheduleController(scheduleDAO, eduClassDAO, teacherDAO, roomDAO, loggedInUser, logService);
            userController = new UserController(userDAO, loggedInUser, logService);
            logController = new LogController(logService, loggedInUser);
            educationController = new EducationController(
                    loggedInUser,
                    recordDAO,
                    eduClassDAO,
                    studentDAO,
                    logService,
                    eduClassController,
                    assignmentDAO
            );

            System.out.println("Child Controllers initialized successfully.");
        } catch (Exception e) {
            System.err.println("!!! CRITICAL ERROR INITIALIZING CHILD CONTROLLERS !!!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to initialize application components.\nPlease check the console log or contact support.\nError: " + e.getMessage(),
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void setMainView(MainView mainView) {
        this.mainView = mainView;
        if (this.mainView == null) {
            System.err.println("Error: MainView is null in setMainView!");
            return;
        }

        mainView.setControllers(
                studentController,
                teacherController,
                courseController,
                roomController,
                eduClassController,
                scheduleController,
                userController,
                logController
        );

        mainView.setEducationController(this.educationController);

        if (studentController != null) studentController.setMainView(mainView);
        if (teacherController != null) teacherController.setMainView(mainView);
        if (scheduleController != null) scheduleController.setMainView(mainView);
        if (logController != null && mainView.getLogsPanel() != null) logController.setLogsPanel(mainView.getLogsPanel());


        mainView.configureViewForUser(loggedInUser);
        mainView.refreshSelectedTab();
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public Role getUserRole() {
        return loggedInUser != null ? loggedInUser.getRole() : null;
    }

    public void exitApplication() {
        if (UIUtils.showConfirmDialog(mainView, "Exit Confirmation", "Are you sure you want to exit EduManager?")) {
            System.out.println("Exiting application...");
            if (this.logController != null) this.logController.cleanupListener();
            if (this.educationController != null) this.educationController.cleanup();

            System.exit(0);
        }
    }

    public void showAboutDialog() {
        if (mainView != null) {
            UIUtils.showInfoMessage(mainView,
                    "About EduManager",
                    "EduManager - Educational Management System\nVersion 1.1.0 (Devoloper zakhanh)");
        } else {
            System.err.println("MainController Warning: mainView is null when trying to show About dialog.");
            JOptionPane.showMessageDialog(null,
                    "EduManager - Educational Management System\nVersion 1.1.0 (Devoloper zakhanh)",
                    "About EduManager",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void logout() {
        System.out.println("MainController: logout() called.");

        if (this.logController != null) this.logController.cleanupListener();
        if (this.educationController != null) this.educationController.cleanup();

        if (this.authController != null) {
            System.out.println("MainController: Calling authController.logout() and showLoginView()...");
            this.authController.logout();
            if (this.mainView != null) {
                this.mainView.dispose();
            }
            this.authController.showLoginView();
        } else {
            System.err.println("MainController Error: authController is null! Cannot logout properly.");
            JOptionPane.showMessageDialog(null, "Logout failed due to an internal error.", "Logout Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void requestExcelExport(String exportType, int associatedId) {
        Role currentUserRole = getUserRole();
        if (currentUserRole == Role.STUDENT && exportType.equals(EXPORT_GRADES)) {
            UIUtils.showErrorMessage(mainView, "Permission Denied", "Students cannot export grade lists.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Excel File - " + exportType);
        String suggestedFileName = exportType.replace(" ", "_").replace("/", "")
                + (associatedId > 0 ? "_ID" + associatedId : "")
                + ".xlsx";
        fileChooser.setSelectedFile(new File(suggestedFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int userSelection = fileChooser.showSaveDialog(mainView);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(filePath + ".xlsx");
            }
            System.out.println("Export requested for: " + exportType + (associatedId > 0 ? " (ID: "+associatedId+")" : "") + " to file: " + fileToSave.getAbsolutePath());
            exportDataToExcel(exportType, fileToSave, associatedId);
        } else {
            System.out.println("Export save cancelled by user.");
        }
    }

    public void exportDataToExcel(String dataType, File outputFile, int associatedId) {
        System.out.println("Starting Excel export for: " + dataType + ", Associated ID: " + associatedId);

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
        try {
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
                case EXPORT_GRADES:
                    if (associatedId <= 0) {
                        throw new IllegalArgumentException("Class ID is required for exporting grades.");
                    }
                    exportGradeData(sheet, associatedId);
                    break;
                default:
                    System.err.println("Unsupported data type for export: " + dataType);
                    UIUtils.showWarningMessage(mainView, "Export Failed", "Data type '" + dataType + "' is not supported for export.");
                    return;
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
                System.out.println("Excel file exported successfully to: " + outputFile.getAbsolutePath());
                UIUtils.showInfoMessage(mainView, "Export Successful", "Data exported successfully to:\n" + outputFile.getName());
            } catch (IOException e) {
                System.err.println("Error writing Excel file: " + e.getMessage());
                e.printStackTrace();
                UIUtils.showErrorMessage(mainView, "Export Error", "Could not write to file:\n" + outputFile.getName() + "\nError: " + e.getMessage());
            }

        } catch (SecurityException secEx) {
            System.err.println("Permission denied during export: " + secEx.getMessage());
            UIUtils.showErrorMessage(mainView, "Permission Denied", "You do not have permission to export this data type.");
        } catch (Exception e) {
            System.err.println("Error during Excel export process: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(mainView, "Export Error", "An unexpected error occurred during export.\nError: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException ioex) {
                    System.err.println("Error closing workbook: " + ioex.getMessage());
                }
            }
        }
    }

    private void exportStudentData(Sheet sheet) {
        System.out.println("Exporting Student data...");
        if (studentController == null) { System.err.println("StudentController is null!"); return; }

        List<Student> students = studentController.getAllStudents();
        if (students == null || students.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No student data to export."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Full Name", "Date of Birth", "Gender", "Phone", "Email", "Parent"};
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

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

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Full Name", "Specialization", "Phone", "Email", "DOB", "Active"};
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for(int i=0; i<headers.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

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
            else dobCell.setCellValue("");
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
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for(int i=0; i<headers.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

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
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for(int i=0; i<headers.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

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
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for(int i=0; i<headers.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

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
        List<Schedule> schedules = scheduleController.getAllSchedules();

        if (schedules == null || schedules.isEmpty()) { UIUtils.showInfoMessage(mainView, "Export Info", "No schedule data to export."); return; }

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Date", "Start Time", "End Time", "Class Name", "Teacher Name", "Room Name"};
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for(int i=0; i<headers.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for(Schedule schedule : schedules) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(schedule.getScheduleId());
            Cell dateCell = row.createCell(1);
            if (schedule.getDate() != null) dateCell.setCellValue(schedule.getDate().toString()); else dateCell.setCellValue("");
            Cell startCell = row.createCell(2);
            if (schedule.getStartTime() != null) startCell.setCellValue(schedule.getStartTime().toString()); else startCell.setCellValue("");
            Cell endCell = row.createCell(3);
            if (schedule.getEndTime() != null) endCell.setCellValue(schedule.getEndTime().toString()); else endCell.setCellValue("");
            row.createCell(4).setCellValue(scheduleController.getClassNameById(schedule.getClassId()));
            row.createCell(5).setCellValue(scheduleController.getTeacherNameById(schedule.getTeacherId()));
            row.createCell(6).setCellValue(scheduleController.getRoomNameById(schedule.getRoomId()));
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void exportGradeData(Sheet sheet, int classId) {
        System.out.println("Exporting Grade data for class ID: " + classId);
        if (educationController == null) { System.err.println("EducationController is null!"); return; }

        Object[][] data = educationController.getGradeDataForExport(classId);
        if (data == null || data.length == 0) {
            UIUtils.showInfoMessage(mainView, "Export Info", "No grade data to export for this class.");
            return;
        }

        String[] headers = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật", "TB KHTN", "TB KHXH", "TB môn học", "Hạnh kiểm"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        CellStyle doubleStyle = sheet.getWorkbook().createCellStyle();
        DataFormat format = sheet.getWorkbook().createDataFormat();
        doubleStyle.setDataFormat(format.getFormat("0.00"));

        CellStyle integerStyle = sheet.getWorkbook().createCellStyle();
        integerStyle.setDataFormat(format.getFormat("0"));

        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                Object value = rowData[i];

                if (i == 0) {
                    if (value instanceof Integer) cell.setCellValue(((Integer) value).intValue());
                    else if (value != null) cell.setCellValue(value.toString());
                    cell.setCellStyle(integerStyle);
                } else if (value instanceof Number && !(value instanceof Integer)) {
                    cell.setCellValue(((Number) value).doubleValue());
                    cell.setCellStyle(doubleStyle);
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                } else {
                    cell.setCellValue("");
                }
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        System.out.println("Grade data prepared for export.");
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
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