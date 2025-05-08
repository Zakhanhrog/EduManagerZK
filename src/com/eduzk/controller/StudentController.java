package com.eduzk.controller;

import com.eduzk.model.entities.*;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.entities.Role;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.StudentPanel;
import com.eduzk.model.dao.interfaces.IUserDAO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;
import com.eduzk.view.MainView;
import java.util.HashMap;
import java.util.Map;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.utils.PasswordUtils;

public class StudentController {
    private final IStudentDAO studentDAO;
    private final User currentUser;
    private final IEduClassDAO eduClassDAO;
    private StudentPanel studentPanel;
    private final IUserDAO userDAO;
    private MainView mainView;
    private final LogService logService;

    public StudentController(IStudentDAO studentDAO, IEduClassDAO eduClassDAO, IUserDAO userDAO, User currentUser, LogService logService) {
        this.studentDAO = studentDAO;
        this.currentUser = currentUser;
        this.eduClassDAO = eduClassDAO;
        this.userDAO = userDAO;
        this.logService = logService;
    }
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }

    public void setStudentPanel(StudentPanel studentPanel) {
        this.studentPanel = studentPanel;
    }

    public List<Student> getAllStudents() {
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser);
                if (teacherId > 0) {
                    System.out.println("StudentController: Filtering students for Teacher ID: " + teacherId);

                    if (this.eduClassDAO == null) {
                        System.err.println("StudentController: EduClassDAO is required for filtering students but is null!");
                        return Collections.emptyList();
                    }

                    try {
                        // 1. Lấy các lớp của giáo viên
                        List<EduClass> teacherClasses = eduClassDAO.findByTeacherId(teacherId);
                        if (teacherClasses.isEmpty()) {
                            return Collections.emptyList();
                        }

                        // 2. Lấy ID của tất cả học viên từ các lớp đó, loại bỏ trùng lặp
                        List<Integer> studentIds = teacherClasses.stream()
                                .flatMap(eduClass -> eduClass.getStudentIds().stream())
                                .distinct()
                                .collect(Collectors.toList());

                        if (studentIds.isEmpty()) {
                            return Collections.emptyList(); // Các lớp không có học viên nào
                        }

                        // 3. Lấy thông tin chi tiết các học viên từ StudentDAO
                        System.out.println("StudentController: Fetching details for student IDs: " + studentIds);
                        return studentIds.stream()
                                .map(studentId -> studentDAO.getById(studentId)) // Gọi getById cho từng ID
                                .filter(Objects::nonNull) // Bỏ qua nếu student không tìm thấy (dữ liệu không nhất quán?)
                                .collect(Collectors.toList());

                    } catch (DataAccessException daoEx) {
                        System.err.println("StudentController: Error accessing data while filtering students for teacher: " + daoEx.getMessage());
                        UIUtils.showErrorMessage(studentPanel, "Error", "Could not load student data for your classes.");
                        return Collections.emptyList();
                    }
                } else {
                    System.err.println("StudentController: Could not determine Teacher ID for logged in user. Returning empty student list.");
                    return Collections.emptyList();
                }
            } else {
                System.out.println("StudentController: Getting all students for Admin/Other.");
                return studentDAO.getAll();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading students: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to load student data.");
            return Collections.emptyList();
        }
    }

    private int getTeacherIdForUser(User user) {
        if (user != null && user.getRole() == Role.TEACHER && user.getTeacherId() != null) {
            return user.getTeacherId();
        }
        return -1;
    }

    public List<Student> searchStudentsByName(String name) {
        if (!ValidationUtils.isNotEmpty(name)) {
            return getAllStudents(); // Return all if search is empty
        }
        try {
            return studentDAO.findByName(name);
        } catch (DataAccessException e) {
            System.err.println("Error searching students: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to search students.");
            return Collections.emptyList();
        }
    }


    public boolean addStudent(Student student) {

        if (student == null || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Student name cannot be empty.");
            return false;
        }

        if (!ValidationUtils.isNotEmpty(student.getPhone()) || !ValidationUtils.isValidPhoneNumber(student.getPhone())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "A valid Phone Number is required (used as username).");
            return false;
        }
        boolean studentAddSuccess = false;
        boolean userCreatedSuccessfully = false;
        try {
            studentDAO.add(student);
            studentAddSuccess = true;

            if (student.getStudentId() > 0) {
                String defaultUsername = student.getPhone();
                String defaultPassword = "123456";

                // Tạo đối tượng User mới
                User newUser = new User();
                newUser.setUsername(defaultUsername);
                String hashedPassword = PasswordUtils.hashPassword(defaultPassword);
                newUser.setPassword(hashedPassword);
                newUser.setRole(Role.STUDENT);
                newUser.setActive(true);
                newUser.setStudentId(student.getStudentId());
                newUser.setTeacherId(null);
                newUser.setRequiresPasswordChange(true);

                try {
                    if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                        throw new DataAccessException("Phone number '" + newUser.getUsername() + "' is already registered as a username.");
                    }
                    if (userDAO.findByStudentId(newUser.getStudentId()).isPresent()) {
                        throw new DataAccessException("An account for student ID " + newUser.getStudentId() + " already exists.");
                    }
                    userDAO.add(newUser);
                    userCreatedSuccessfully = true;
                    System.out.println("Automatically created User account for Student ID: " + student.getStudentId() + " with username (phone): " + defaultUsername);
                    writeAddLog("Added Student", student);

                    if (studentPanel != null) {
                        studentPanel.refreshTable();
                        if (userCreatedSuccessfully) {
                            UIUtils.showInfoMessage(studentPanel, "Success", "Student and linked User account added successfully.");
                        }
                        if (userCreatedSuccessfully && mainView != null) {
                            mainView.refreshAccountsPanelData();
                        }
                        return true;
                    }

                } catch (DataAccessException e) {
                    System.err.println("!!! FAILED to add User account for Student ID " + student.getStudentId() + " !!! DAO Error: " + e.getMessage());
                    e.printStackTrace();
                    UIUtils.showWarningMessage(studentPanel, "User Creation Failed", "Student added, but failed to create linked user account:\n" + e.getMessage());
                    writeAddLog("Added Student (User Failed)", student);
                    if (studentPanel != null) studentPanel.refreshTable();
                    return true;
                } catch (Exception ex) {
                    System.err.println("!!! UNEXPECTED ERROR adding User account for Student ID " + student.getStudentId() + " !!! Error: " + ex.getMessage());
                    ex.printStackTrace();
                    UIUtils.showErrorMessage(studentPanel, "Unexpected Error", "An unexpected error occurred while creating the user account.");
                    writeAddLog("Added Student (User Error)", student);
                    if (studentPanel != null) studentPanel.refreshTable();
                    return true;
                }
            } else {
                System.err.println("Could not get Student ID after adding student. User account not created.");
                UIUtils.showWarningMessage(studentPanel, "User Creation Failed", "Student added, but could not get ID to create linked user account.");
                writeAddLog("Added Student (ID Error)", student);
                if (studentPanel != null) studentPanel.refreshTable();
                return true;
            }

        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to add student: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean updateStudent(Student student) {
        if (student == null || student.getStudentId() <= 0 || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Invalid student data for update.");
            return false;
        }
        try {
            studentDAO.update(student);
            writeUpdateLog("Updated Student", student);
            if (studentPanel != null) {
                studentPanel.refreshTable();
                UIUtils.showInfoMessage(studentPanel, "Success", "Student updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to update student: " + e.getMessage());
            return false;
        }
    }

    public Student getStudentById(int studentId) {
        if (studentId <= 0) return null;
        try {
            return studentDAO.getById(studentId);
        } catch (DataAccessException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
            return null;
        }
    }
    public String getPasswordForStudent(int studentId) {
        if (userDAO == null) return null;
        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            return userOpt.get().getPassword();
        }
        return null;
    }
    // --- THÊM PHƯƠNG THỨC CẬP NHẬT PASSWORD ---
    public boolean updatePasswordForStudent(int studentId, String newPassword) {
        if (userDAO == null) { System.err.println("UserDAO not injected in StudentController!"); return false; }
        if (!ValidationUtils.isValidPassword(newPassword)) { UIUtils.showWarningMessage(studentPanel, "Error", "New password is too short."); return false;}

        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            User userToUpdate = userOpt.get();
            userToUpdate.setPassword(newPassword);
            try {
                userDAO.update(userToUpdate);
                writeUpdateLog("Updated Student Password", userToUpdate, "For Student ID: " + studentId);
                UIUtils.showInfoMessage(studentPanel, "Success", "Password updated for student ID " + studentId);
                return true;
            } catch (DataAccessException e) {
                System.err.println("Error updating user password: " + e);
                UIUtils.showErrorMessage(studentPanel, "Error", "Could not update password: " + e.getMessage());
                return false;
            }
        } else {
            UIUtils.showWarningMessage(studentPanel, "Error", "Could not find user account for student ID " + studentId + " to update password.");
            return false;
        }
    }
    public boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }

    public void importStudentsFromExcel() {
        if (studentPanel == null) {
            System.err.println("StudentPanel is null in controller.");
            return;
        }
        if (!(currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.TEACHER)) {
            UIUtils.showErrorMessage(studentPanel, "Permission Denied", "You do not have permission to import student data.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Student Excel File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(studentPanel);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Importing students from: " + selectedFile.getAbsolutePath());
            studentPanel.setAllButtonsEnabled(false);

            SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>(){
                @Override
                protected Map<String, Object> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    List<Student> validStudentsToImport = new ArrayList<>();
                    List<User> validUsersToCreate = new ArrayList<>();

                    int processedCount = 0;
                    int validationErrorCount = 0;
                    FileInputStream fis = null;
                    Workbook workbook = null;

                    try {
                        fis = new FileInputStream(selectedFile);
                        workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0);
                        Iterator<Row> rowIterator = sheet.iterator();
                        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua header

                        int rowNum = 1;
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            processedCount++;

                            try {
                                // --- 1. ĐỌC & VALIDATE DỮ LIỆU STUDENT ---
                                String fullName = getStringCellValue(row.getCell(0));
                                LocalDate dob = getDateCellValue(row.getCell(1));
                                String gender = getStringCellValue(row.getCell(2));
                                String address = getStringCellValue(row.getCell(3));
                                String parentName = getStringCellValue(row.getCell(4));
                                String phone = getStringCellValue(row.getCell(5)); // Dùng làm username
                                String email = getStringCellValue(row.getCell(6));

                                // --- Validation ---
                                if (!ValidationUtils.isNotEmpty(fullName)) throw new IllegalArgumentException("Full Name required.");
                                // Phone là bắt buộc và hợp lệ để làm username
                                if (!ValidationUtils.isNotEmpty(phone) || !ValidationUtils.isValidPhoneNumber(phone)) {
                                    throw new IllegalArgumentException("Valid Phone Number required (used as username).");
                                }
                                // Kiểm tra trùng username (Phone) ngay bây giờ trong bảng User
                                if (userDAO.findByUsername(phone.trim()).isPresent()){
                                    throw new DataAccessException("Username (Phone) '" + phone.trim() + "' already exists.");
                                }
                                // Validation email nếu cần
                                if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) {
                                    // throw new IllegalArgumentException("Invalid email format."); // Hoặc chỉ cảnh báo
                                }

                                // --- 2. TẠO OBJECTS NHƯNG CHƯA LƯU ---
                                Student tempStudent = new Student();
                                tempStudent.setFullName(fullName.trim());
                                tempStudent.setDateOfBirth(dob);
                                tempStudent.setGender(gender != null ? gender.trim() : null);
                                tempStudent.setAddress(address != null ? address.trim() : null);
                                tempStudent.setParentName(parentName != null ? parentName.trim() : null);
                                tempStudent.setPhone(phone.trim());
                                tempStudent.setEmail(email != null ? email.trim() : null);

                                User tempUser = new User();
                                tempUser.setUsername(phone.trim()); // Dùng phone làm username
                                tempUser.setPassword("123456"); // Mật khẩu mặc định
                                tempUser.setRole(Role.STUDENT);
                                tempUser.setActive(true); // Mặc định active
                                tempUser.setTeacherId(null);
                                // studentId sẽ được set sau khi Student được lưu và có ID

                                // Thêm vào danh sách tạm nếu hợp lệ
                                validStudentsToImport.add(tempStudent);
                                validUsersToCreate.add(tempUser);

                            } catch (Exception rowEx) {
                                // Lỗi validation hoặc đọc dòng
                                String errorMsg = "Row " + rowNum + ": Validation/Read Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                validationErrorCount++;
                            }
                        } // end while

                        // --- 3. THỰC HIỆN LƯU VÀO DAO ---
                        int actualStudentSuccessCount = 0;
                        int actualUserSuccessCount = 0;

                        for (int i = 0; i < validStudentsToImport.size(); i++) {
                            Student studentToAdd = validStudentsToImport.get(i);
                            User userToCreate = validUsersToCreate.get(i);
                            boolean studentAdded = false;
                            boolean userAdded = false;
                            String currentProcessingInfo = " (Student: " + studentToAdd.getFullName() + ", User: " + userToCreate.getUsername() + ")";

                            try {
                                // Thêm Student trước để lấy ID
                                studentDAO.add(studentToAdd);
                                // *** QUAN TRỌNG: Kiểm tra xem ID có được gán không ***
                                // Logic gán ID nằm trong studentDAO.add(), cần đảm bảo nó hoạt động
                                // Giả sử sau khi add, studentToAdd.getStudentId() sẽ có giá trị
                                if (studentToAdd.getStudentId() > 0) { // Giả sử ID vẫn là int
                                    studentAdded = true;
                                    // Liên kết studentId vào User
                                    userToCreate.setStudentId(studentToAdd.getStudentId());

                                    // **Kiểm tra trùng studentId trong bảng User trước khi thêm**
                                    if (userDAO.findByStudentId(userToCreate.getStudentId()).isPresent()){
                                        throw new DataAccessException("Account for student ID " + userToCreate.getStudentId() + " already exists.");
                                    }

                                    // Thêm User
                                    userDAO.add(userToCreate);
                                    userAdded = true;

                                } else {
                                    throw new DataAccessException("Failed to get generated ID for student." + currentProcessingInfo);
                                }

                                if(studentAdded && userAdded){
                                    actualStudentSuccessCount++;
                                    actualUserSuccessCount++;
                                }

                            } catch (Exception addEx) {
                                // Lỗi khi thêm Student hoặc User vào DAO
                                String errorMsg = "Import Save Error" + currentProcessingInfo + ": " + addEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                // Lỗi này tính vào saveErrors
                            }
                        } // end for loop saving

                        // Chuẩn bị kết quả trả về
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("errors", errors);
                        resultData.put("processedCount", processedCount);
                        resultData.put("studentSuccessCount", actualStudentSuccessCount);
                        resultData.put("userSuccessCount", actualUserSuccessCount);
                        resultData.put("validationErrorCount", validationErrorCount);

                        return resultData;

                    } finally {
                        // Đóng tài nguyên
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                } // end doInBackground

                @Override
                protected void done() {
                    try {
                        // Lấy kết quả
                        Map<String, Object> resultData = get();
                        List<String> errors = (List<String>) resultData.get("errors");
                        int finalProcessedCount = (int) resultData.get("processedCount");
                        int finalStudentSuccessCount = (int) resultData.get("studentSuccessCount");
                        int finalUserSuccessCount = (int) resultData.get("userSuccessCount");
                        int finalValidationErrorCount = (int) resultData.get("validationErrorCount");
                        int totalErrors = errors != null ? errors.size() : 0;

                        // *** GỌI REFRESH TABLE CỦA PANEL ***
                        if (studentPanel != null) {
                            System.out.println("Import done. Refreshing StudentPanel...");
                            studentPanel.refreshTable(); // Panel sẽ tự lấy dữ liệu đúng từ DAO
                            studentPanel.setAllButtonsEnabled(true);
                        }

                        // Refresh AccountsPanel nếu có User được thêm thành công
                        if (finalUserSuccessCount > 0 && mainView != null) {
                            System.out.println("Import done. Refreshing AccountsPanel...");
                            mainView.refreshAccountsPanelData();
                        }

                        totalErrors = ((List<String>) resultData.get("errors")).size();
                        int saveErrors = totalErrors - finalValidationErrorCount;
                        if (finalStudentSuccessCount > 0 || totalErrors > 0) {
                            String logDetails = String.format("Processed: %d, Students Added: %d, Users Created: %d, Validation Errors: %d, Save Errors: %d",
                                    finalProcessedCount,
                                    finalStudentSuccessCount, finalUserSuccessCount,
                                    finalValidationErrorCount, saveErrors);
                            writeGeneralLog("Imported Students", logDetails);
                        }

                        // Xây dựng và hiển thị thông báo kết quả chi tiết
                        StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append("Import finished.\n");
                        messageBuilder.append("Total rows processed: ").append(finalProcessedCount).append("\n");
                        messageBuilder.append("Successfully imported Students: ").append(finalStudentSuccessCount).append("\n");
                        messageBuilder.append("Successfully created User accounts: ").append(finalUserSuccessCount).append("\n");
                        messageBuilder.append("Validation/Read errors: ").append(finalValidationErrorCount).append("\n");
                        messageBuilder.append("Save errors (Student or User): ").append(saveErrors);


                        if (totalErrors > 0) {
                            messageBuilder.append("\n\nFirst few errors:\n");
                            errors.stream().limit(10).forEach(err -> messageBuilder.append(err).append("\n"));
                            if (errors.size() > 10) messageBuilder.append("... (check console log for all errors)\n");
                            UIUtils.showWarningMessage(studentPanel, "Import Partially Successful", messageBuilder.toString());
                        } else {
                            UIUtils.showInfoMessage(studentPanel, "Import Successful", messageBuilder.toString());
                        }

                    } catch (Exception e) {
                        // Lỗi trong quá trình thực thi SwingWorker
                        e.printStackTrace();
                        UIUtils.showErrorMessage(studentPanel, "Import Error", "An unexpected error occurred: " + e.getMessage());
                        if (studentPanel != null) studentPanel.setAllButtonsEnabled(true);
                    }
                } // end done
            }; // end SwingWorker
            worker.execute();
        } // end if JFileChooser
    }

    // Lấy giá trị String từ ô, xử lý ô null hoặc không phải String
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); } catch (IllegalStateException e) { return null; } // Có thể lỗi nếu công thức trả về số
            default:
                return null;
        }
    }

    // Lấy giá trị LocalDate từ ô, xử lý ô null hoặc kiểu khác
    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date javaDate = cell.getDateCellValue();
                    return javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    return null;
                }
            } else if (cell.getCellType() == CellType.STRING) {
                return DateUtils.parseDate(cell.getStringCellValue());
            }
        } catch (Exception e) {
            System.err.println("Error parsing date from cell: " + e.getMessage());
            return null;
        }
        return null;
    }

    public boolean deleteStudents(List<Integer> idsToDelete) {
        if (idsToDelete == null || idsToDelete.isEmpty()) {
            UIUtils.showWarningMessage(studentPanel, "Error", "No student IDs provided for deletion.");
            return false;
        }
        if (studentDAO == null || userDAO == null) {
            UIUtils.showWarningMessage(studentPanel, "Error", "No student IDs provided for deletion.");
            return false;
        }
        try {
            System.out.println("StudentController: Calling DAO to delete students with IDs: " + idsToDelete);
            int deletedStudentCount = studentDAO.deleteByIds(idsToDelete);
            System.out.println("StudentController: DAO reported " + deletedStudentCount + " students deleted.");
            int deletedUserCount = 0;
            List<User> deletedUsersInfo = new ArrayList<>();

            if (deletedStudentCount > 0) {
                System.out.println("StudentController: Attempting to delete linked user accounts...");
                for (Integer studentId : idsToDelete) {
                    try {
                        Optional<User> userOpt = userDAO.findByStudentId(studentId);
                        if (userOpt.isPresent()) {
                            User userToDelete = userOpt.get();
                            deletedUsersInfo.add(userToDelete);
                            System.out.println("Found linked user for student ID " + studentId + ": User ID " + userToDelete.getUserId() + ", Username: " + userToDelete.getUsername());
                            userDAO.delete(userToDelete.getUserId());
                            System.out.println("Deleted linked user ID: " + userToDelete.getUserId());
                            deletedUserCount++;
                        } else {
                            System.out.println("No linked user found for student ID " + studentId);
                        }
                    } catch (DataAccessException e) {
                        System.err.println("Error finding/deleting user for student ID " + studentId + ": " + e.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Unexpected error processing user deletion for student ID " + studentId + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                String studentDetail = "IDs: " + idsToDelete.toString();
                String userDetail = "Linked User IDs: [" + deletedUsersInfo.stream().map(u -> String.valueOf(u.getUserId())).collect(Collectors.joining(", ")) + "]";
                writeDeleteLog("Deleted Student(s)", studentDetail + " | " + userDetail);
            }
           // --- 3. REFRESH VÀ THÔNG BÁO ---
            if (deletedStudentCount > 0 || deletedUserCount > 0) {
                if (studentPanel != null) {
                    studentPanel.refreshTable();
                }
                // Gọi refresh AccountsPanel nếu có User bị xóa
                if (deletedUserCount > 0 && mainView != null) {
                    mainView.refreshAccountsPanelData();
                }
                // Thông báo kết quả
                UIUtils.showInfoMessage(studentPanel, "Deletion Successful",
                        deletedStudentCount + " student(s) and " + deletedUserCount + " linked user account(s) deleted successfully.");
                return true;
            } else {
                UIUtils.showWarningMessage(studentPanel, "Deletion Info", "No matching students found or no linked users to delete.");
                return false;
            }
        } catch (DataAccessException e) {
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }
    private void writeAddLog(String action, Student student) {
        writeLog(action, "ID: " + student.getStudentId() + ", Name: " + student.getFullName() + ", Phone: " + student.getPhone());
    }
    private void writeUpdateLog(String action, Student student) {
        writeLog(action, "ID: " + student.getStudentId() + ", Name: " + student.getFullName());
    }
    private void writeUpdateLog(String action, User user, String additionalDetails) {
        writeLog(action, "UserID: " + user.getUserId() + ", Username: " + user.getUsername() + ". " + (additionalDetails != null ? additionalDetails : ""));
    }
    private void writeDeleteLog(String action, String details) {
        writeLog(action, details);
    }
    private void writeGeneralLog(String action, String details) {
        writeLog(action, details);
    }
    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(), // Dùng DisplayName thay vì Username
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: " + action + " - " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

}