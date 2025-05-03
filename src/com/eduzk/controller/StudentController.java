package com.eduzk.controller;

import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.entities.Student;
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
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

public class StudentController {
    private final IStudentDAO studentDAO;
    private final User currentUser;
    private final IEduClassDAO eduClassDAO;
    private StudentPanel studentPanel; // Reference to the view panel
    private final IUserDAO userDAO;

    public StudentController(IStudentDAO studentDAO, IEduClassDAO eduClassDAO, IUserDAO userDAO, User currentUser) {
        this.studentDAO = studentDAO;
        this.currentUser = currentUser;
        this.eduClassDAO = eduClassDAO;
        this.userDAO = userDAO;

    }

    public void setStudentPanel(StudentPanel studentPanel) {
        this.studentPanel = studentPanel;
    }

    public List<Student> getAllStudents() {
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser); // Gọi hàm helper
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
            return Collections.emptyList(); // Return empty list on error
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

        try {
            studentDAO.add(student);
            if (student.getStudentId() > 0) {
                String defaultUsername = student.getPhone();
                String defaultPassword = "123456";

                // Tạo đối tượng User mới
                User newUser = new User();
                newUser.setUsername(defaultUsername);
                newUser.setPassword(defaultPassword);
                newUser.setRole(Role.STUDENT);
                newUser.setActive(true);
                newUser.setStudentId(student.getStudentId());
                newUser.setTeacherId(null);

                // Thêm User vào userDAO (có xử lý lỗi)
                try {
                    // Kiểm tra trùng username (SĐT) VÀ trùng studentId trước khi thêm
                    if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                        throw new DataAccessException("Phone number '" + newUser.getUsername() + "' is already registered as a username.");
                    }
                    if (userDAO.findByStudentId(newUser.getStudentId()).isPresent()) {
                        throw new DataAccessException("An account for student ID " + newUser.getStudentId() + " already exists.");
                    }

                    userDAO.add(newUser);
                    System.out.println("Automatically created User account for Student ID: " + student.getStudentId() + " with username (phone): " + defaultUsername);

                    if (studentPanel != null) {
                        studentPanel.refreshTable();
                        UIUtils.showInfoMessage(studentPanel, "Success", "Student and linked User account added successfully.");
                    }
                    return true;

                } catch (DataAccessException e) {
                    System.err.println("!!! FAILED to add User account for Student ID " + student.getStudentId() + " !!! DAO Error: " + e.getMessage());
                    e.printStackTrace();
                    UIUtils.showWarningMessage(studentPanel, "User Creation Failed", "Student added, but failed to create linked user account:\n" + e.getMessage());
                    if (studentPanel != null) studentPanel.refreshTable();
                    return false;
                } catch (Exception ex) {
                    System.err.println("!!! UNEXPECTED ERROR adding User account for Student ID " + student.getStudentId() + " !!! Error: " + ex.getMessage());
                    ex.printStackTrace();
                    UIUtils.showErrorMessage(studentPanel, "Unexpected Error", "An unexpected error occurred while creating the user account.");
                    if (studentPanel != null) studentPanel.refreshTable();
                    return false;
                }
            } else {
                System.err.println("Could not get Student ID after adding student. User account not created.");
                UIUtils.showWarningMessage(studentPanel, "User Creation Failed", "Student added, but could not get ID to create linked user account.");
                if (studentPanel != null) studentPanel.refreshTable();
                return false;
            }

        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to add student: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudent(Student student) {
        if (student == null || student.getStudentId() <= 0 || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Invalid student data for update.");
            return false;
        }
        try {
            studentDAO.update(student);
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

            SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                private int successCount = 0;
                private int errorCount = 0;
                private int processedCount = 0;

                @Override
                protected List<String> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    FileInputStream fis = null;
                    Workbook workbook = null;
                    try {
                        fis = new FileInputStream(selectedFile);
                        workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0);
                        Iterator<Row> rowIterator = sheet.iterator();
                        if (rowIterator.hasNext()) rowIterator.next(); // Skip header

                        int rowNum = 1;
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            processedCount++;
                            Student addedStudent = null;
                            boolean studentAddSuccess = false;
                            boolean userAddSuccess = false;
                            try {
                                // --- 1. ĐỌC DỮ LIỆU STUDENT TỪ EXCEL ---
                                String fullName = getStringCellValue(row.getCell(0));
                                LocalDate dob = getDateCellValue(row.getCell(1));
                                String gender = getStringCellValue(row.getCell(2));
                                String address = getStringCellValue(row.getCell(3));
                                String parentName = getStringCellValue(row.getCell(4));
                                String phone = getStringCellValue(row.getCell(5)); // Dùng làm username
                                String email = getStringCellValue(row.getCell(6));

                                // --- 2. VALIDATION DỮ LIỆU STUDENT ---
                                if (!ValidationUtils.isNotEmpty(fullName)) { throw new IllegalArgumentException("Full Name required."); }
                                if (!ValidationUtils.isNotEmpty(phone) || !ValidationUtils.isValidPhoneNumber(phone)) {
                                    throw new IllegalArgumentException("A valid Phone Number required (used as username).");
                                }
                                // Thêm validation email nếu muốn
                                if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) {
                                     System.err.println("Warning Row " + rowNum + ": Invalid email format.");
                                }

                                // --- 3. TẠO VÀ THÊM STUDENT ---
                                Student newStudent = new Student();
                                newStudent.setFullName(fullName.trim());
                                newStudent.setDateOfBirth(dob);
                                newStudent.setGender(gender != null ? gender.trim() : null);
                                newStudent.setAddress(address != null ? address.trim() : null);
                                newStudent.setParentName(parentName != null ? parentName.trim() : null);
                                newStudent.setPhone(phone.trim()); // Lưu SĐT đã trim
                                newStudent.setEmail(email != null ? email.trim() : null);

                                studentDAO.add(newStudent); // Thêm Student
                                addedStudent = newStudent;  // Lưu lại
                                studentAddSuccess = true;

                                // --- 4. TẠO USER TƯƠNG ỨNG (NẾU THÊM STUDENT THÀNH CÔNG) ---
                                if (addedStudent != null && addedStudent.getStudentId() > 0) {
                                    String defaultUsername = addedStudent.getPhone(); // Đã validate ở trên
                                    String defaultPassword = "123456";

                                    User newUser = new User();
                                    newUser.setUsername(defaultUsername);
                                    newUser.setPassword(defaultPassword);
                                    newUser.setRole(Role.STUDENT);
                                    newUser.setActive(true); // Mặc định active
                                    newUser.setStudentId(addedStudent.getStudentId());
                                    newUser.setTeacherId(null);

                                    // Thêm User vào DAO (có xử lý lỗi trùng)
                                    try {
                                        // Kiểm tra trùng username (SĐT) VÀ trùng studentId
                                        if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                                            throw new DataAccessException("Phone number '" + newUser.getUsername() + "' is already registered as username.");
                                        }
                                        if (userDAO.findByStudentId(newUser.getStudentId()).isPresent()) {
                                            throw new DataAccessException("Account for student ID " + newUser.getStudentId() + " already exists.");
                                        }

                                        userDAO.add(newUser);
                                        userAddSuccess = true;
                                        System.out.println("Import - Row " + rowNum + ": Added User for Student ID: " + addedStudent.getStudentId());
                                    } catch (DataAccessException e) {
                                        // Lỗi khi thêm User, ghi nhận nhưng tiếp tục
                                        String userErrorMsg = "Row " + rowNum + ": Student added, but FAILED User creation - " + e.getMessage();
                                        System.err.println(userErrorMsg);
                                        errors.add(userErrorMsg);
                                        // Không tăng successCount, lỗi này sẽ được tính vào errorCount chung
                                    } catch (Exception ex){
                                        String userErrorMsg = "Row " + rowNum + ": Student added, but UNEXPECTED ERROR creating User - " + ex.getMessage();
                                        System.err.println(userErrorMsg);
                                        errors.add(userErrorMsg);
                                    }
                                } else {
                                    throw new IllegalStateException("Could not retrieve Student ID after adding. Linked User not created.");
                                }

                                // Chỉ tăng successCount nếu cả Student và User thành công
                                if (studentAddSuccess && userAddSuccess) {
                                    successCount++;
                                } else {
                                    errorCount++; // Tăng error nếu có lỗi ở Student hoặc User
                                }

                            } catch (Exception rowEx) {
                                // Lỗi khi xử lý dòng (đọc, validation, add Student, add User)
                                String errorMsg = "Row " + rowNum + ": Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                errorCount++;
                                // Không tăng successCount
                            }
                        } // end while
                    } finally {
                        // ... (Đóng workbook, fis) ...
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                    return errors;
                }

                @Override
                protected void done() {
                    try {
                        List<String> errors = get();
                        if (studentPanel != null) {
                            studentPanel.refreshTable();
                            studentPanel.setAllButtonsEnabled(true);
                        }
                        // Hiển thị thông báo kết quả
                        String message = "Import finished.\nSuccessfully imported: " + successCount + " students.\nErrors: " + errorCount;
                        if (errorCount > 0) {
                            message += "\n\nFirst few errors:\n" + String.join("\n", errors.stream().limit(5).toArray(String[]::new)); // Hiển thị tối đa 5 lỗi đầu
                            UIUtils.showWarningMessage(studentPanel, "Import Partially Successful", message);
                        } else {
                            UIUtils.showInfoMessage(studentPanel, "Import Successful", message);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        UIUtils.showErrorMessage(studentPanel, "Import Error", "An error occurred during import: " + e.getMessage());
                        if (studentPanel != null) {
                            // studentPanel.showLoadingState(false);
                            studentPanel.setAllButtonsEnabled(true);
                        }
                    }
                }
            };
            worker.execute();
        }
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
        try {
            System.out.println("StudentController: Calling DAO to delete students with IDs: " + idsToDelete);
            int deletedCount = studentDAO.deleteByIds(idsToDelete); // Gọi hàm DAO mới
            System.out.println("StudentController: DAO reported " + deletedCount + " students deleted.");

            if (deletedCount > 0) {
                if (studentPanel != null) {
                    studentPanel.refreshTable();
                }
                UIUtils.showInfoMessage(studentPanel, "Deletion Successful", deletedCount + " student(s) deleted successfully.");
                return true;
            } else {
                UIUtils.showWarningMessage(studentPanel, "Deletion Info", "No matching students found for deletion.");
                return false;
            }
        } catch (DataAccessException e) {
            System.err.println("Error deleting multiple students: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(studentPanel, "Deletion Error", "Failed to delete students: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error during multiple deletion: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(studentPanel, "Unexpected Error", "An unexpected error occurred during deletion.");
            return false;
        }
    }
}