package com.eduzk.controller;

import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils; // For showing messages
import com.eduzk.view.panels.StudentPanel; // To update the panel's table
import com.eduzk.model.dao.interfaces.IUserDAO; // <-- THÊM IMPORT
import com.eduzk.model.entities.User;
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

                    // --- PHẦN LOGIC PHỨC TẠP ---
                    // Kiểm tra xem eduClassDAO đã được inject chưa
                    if (this.eduClassDAO == null) { // Giả sử tên biến là eduClassDAO
                        System.err.println("StudentController: EduClassDAO is required for filtering students but is null!");
                        return Collections.emptyList();
                    }

                    try {
                        // 1. Lấy các lớp của giáo viên
                        List<EduClass> teacherClasses = eduClassDAO.findByTeacherId(teacherId);
                        if (teacherClasses.isEmpty()) {
                            return Collections.emptyList(); // Giáo viên không có lớp nào
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
                        // Tạm thời dùng getById lặp lại (kém hiệu quả)
                        // Nên tối ưu bằng cách thêm findByIds vào StudentDAO sau này
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
            } else { // Admin hoặc vai trò khác
                System.out.println("StudentController: Getting all students for Admin/Other.");
                // Lấy tất cả học viên
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
        // Basic validation (more specific validation might happen in the dialog/panel before calling this)
        if (student == null || !ValidationUtils.isNotEmpty(student.getFullName())) {
            UIUtils.showWarningMessage(studentPanel, "Validation Error", "Student name cannot be empty.");
            return false;
        }
        // Add more validation for other fields as needed (DOB, contact, etc.)

        try {
            studentDAO.add(student);
            // Optionally refresh the view if the panel is set
            if (studentPanel != null) {
                System.out.println("StudentController: Add successful, refreshing panel...");
                studentPanel.refreshTable(); // Assumes StudentPanel has this method
                UIUtils.showInfoMessage(studentPanel, "Success", "Student added successfully.");
            }
            return true;
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
        // Add more validation...

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

    public boolean deleteStudent(int studentId) {
        if (studentId <= 0) {
            UIUtils.showWarningMessage(studentPanel, "Error", "Invalid student ID for deletion.");
            return false;
        }
        // Confirmation dialog should be shown in the View layer before calling this
        try {
            studentDAO.delete(studentId);
            if (studentPanel != null) {
                studentPanel.refreshTable();
                UIUtils.showInfoMessage(studentPanel, "Success", "Student deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Potentially handle specific errors, e.g., student enrolled in classes
            System.err.println("Error deleting student: " + e.getMessage());
            UIUtils.showErrorMessage(studentPanel, "Error", "Failed to delete student: " + e.getMessage());
            return false;
        }
    }

    public Student getStudentById(int studentId) {
        if (studentId <= 0) return null;
        try {
            return studentDAO.getById(studentId);
        } catch (DataAccessException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
            // Don't usually show UI message for simple get, just log error
            return null;
        }
    }
    public String getPasswordForStudent(int studentId) {
        if (userDAO == null) return null; // Chưa inject DAO
        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            // !!! TRẢ VỀ PLAIN TEXT - RỦI RO BẢO MẬT !!!
            return userOpt.get().getPassword();
        }
        return null; // Không tìm thấy User liên kết
    }
    // --- THÊM PHƯƠNG THỨC CẬP NHẬT PASSWORD ---
    public boolean updatePasswordForStudent(int studentId, String newPassword) {
        if (userDAO == null) { System.err.println("UserDAO not injected in StudentController!"); return false; }
        if (!ValidationUtils.isValidPassword(newPassword)) { UIUtils.showWarningMessage(studentPanel, "Error", "New password is too short."); return false;}

        Optional<User> userOpt = userDAO.findByStudentId(studentId);
        if (userOpt.isPresent()) {
            User userToUpdate = userOpt.get();
            // !!! CẬP NHẬT PLAIN TEXT - RỦI RO BẢO MẬT !!!
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
        if (!isCurrentUserAdmin()) {
            UIUtils.showErrorMessage(studentPanel, "Permission Denied", "Only administrators can import students data.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Student Excel File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        fileChooser.setAcceptAllFileFilterUsed(false); // Chỉ chấp nhận .xlsx

        int result = fileChooser.showOpenDialog(studentPanel); // Hiển thị dialog mở file

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Importing students from: " + selectedFile.getAbsolutePath());

            // --- Sử dụng SwingWorker để đọc và import trên luồng nền ---
            // Hiển thị trạng thái chờ trên panel (ví dụ: thay đổi text của một label)
            // studentPanel.showLoadingState(true); // Cần thêm hàm này vào StudentPanel

            // Vô hiệu hóa các nút trong khi import
            studentPanel.setAllButtonsEnabled(false); // Cần thêm hàm này vào StudentPanel


            SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                private int successCount = 0;
                private int errorCount = 0;

                @Override
                protected List<String> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    FileInputStream fis = null;
                    Workbook workbook = null;
                    try {
                        fis = new FileInputStream(selectedFile);
                        workbook = new XSSFWorkbook(fis); // Mở file .xlsx
                        Sheet sheet = workbook.getSheetAt(0); // Giả sử dữ liệu ở sheet đầu tiên

                        Iterator<Row> rowIterator = sheet.iterator();

                        // Bỏ qua hàng tiêu đề (giả sử hàng đầu tiên là header)
                        if (rowIterator.hasNext()) {
                            rowIterator.next();
                        }

                        int rowNum = 1; // Bắt đầu từ hàng 1 (sau header)
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            try {
                                // --- Đọc dữ liệu từ các ô ---
                                // Quan trọng: Thứ tự cột phải khớp với file Excel mẫu
                                // Cần xử lý cẩn thận kiểu dữ liệu và ô trống (null)
                                String fullName = getStringCellValue(row.getCell(0)); // Cột 0: Full Name
                                LocalDate dob = getDateCellValue(row.getCell(1));    // Cột 1: Date of Birth
                                String gender = getStringCellValue(row.getCell(2));   // Cột 2: Gender
                                String address = getStringCellValue(row.getCell(3));  // Cột 3: Address
                                String parentName = getStringCellValue(row.getCell(4));// Cột 4: Parent Name
                                String phone = getStringCellValue(row.getCell(5));    // Cột 5: Phone
                                String email = getStringCellValue(row.getCell(6));    // Cột 6: Email

                                // --- Validation cơ bản ---
                                if (fullName == null || fullName.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
                                    throw new IllegalArgumentException("Full Name and Phone are required.");
                                }
                                // Thêm validation khác nếu cần (SĐT, Email hợp lệ,...)

                                // --- Tạo đối tượng Student ---
                                Student newStudent = new Student();
                                newStudent.setFullName(fullName.trim());
                                newStudent.setDateOfBirth(dob);
                                newStudent.setGender(gender != null ? gender.trim() : null);
                                newStudent.setAddress(address != null ? address.trim() : null);
                                newStudent.setParentName(parentName != null ? parentName.trim() : null);
                                newStudent.setPhone(phone.trim()); // SĐT là username
                                newStudent.setEmail(email != null ? email.trim() : null);

                                // --- Gọi DAO để thêm ---
                                // studentDAO.add sẽ tự xử lý ID
                                studentDAO.add(newStudent);
                                successCount++;

                            } catch (Exception rowEx) {
                                // Ghi lại lỗi cho hàng này
                                String errorMsg = "Row " + rowNum + ": Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                errorCount++;
                            }
                        } // Kết thúc while

                    } finally {
                        // Đóng workbook và input stream
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                    return errors; // Trả về danh sách lỗi
                }

                @Override
                protected void done() {
                    try {
                        List<String> errors = get(); // Lấy danh sách lỗi từ doInBackground
                        // --- Cập nhật giao diện trên EDT ---
                        if (studentPanel != null) {
                            studentPanel.refreshTable(); // Làm mới bảng
                            // studentPanel.showLoadingState(false); // Tắt trạng thái chờ
                            studentPanel.setAllButtonsEnabled(true); // Bật lại các nút
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
                        // Lỗi xảy ra trong quá trình import hoặc khi get() kết quả
                        e.printStackTrace();
                        UIUtils.showErrorMessage(studentPanel, "Import Error", "An error occurred during import: " + e.getMessage());
                        if (studentPanel != null) {
                            // studentPanel.showLoadingState(false);
                            studentPanel.setAllButtonsEnabled(true);
                        }
                    }
                }
            };
            worker.execute(); // Bắt đầu import nền
        }
    }

    // --- HELPER ĐỌC GIÁ TRỊ TỪ Ô EXCEL (Cần thêm vào StudentController) ---

    // Lấy giá trị String từ ô, xử lý ô null hoặc không phải String
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC: // Nếu cột số nhưng muốn đọc thành String
                // Có thể cần DataFormatter để đọc chính xác số hiển thị
                // Tạm thời chuyển trực tiếp, có thể mất số 0 ở đầu
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: // Nếu là công thức, thử lấy kết quả đã tính
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
                    // Ô số nhưng không phải định dạng ngày tháng -> không đọc
                    return null;
                }
            } else if (cell.getCellType() == CellType.STRING) {
                // Thử parse ngày từ String nếu người dùng nhập text
                return DateUtils.parseDate(cell.getStringCellValue()); // Cần DateUtils.parseDate chuẩn
            }
        } catch (Exception e) {
            System.err.println("Error parsing date from cell: " + e.getMessage());
            return null; // Trả về null nếu lỗi
        }
        return null;
    }

    public boolean deleteStudents(List<Integer> idsToDelete) {
        if (idsToDelete == null || idsToDelete.isEmpty()) {
            UIUtils.showWarningMessage(studentPanel, "Error", "No student IDs provided for deletion.");
            return false;
        }
        // Không cần Confirmation Dialog ở đây vì nó đã được thực hiện ở Panel

        try {
            System.out.println("StudentController: Calling DAO to delete students with IDs: " + idsToDelete);
            int deletedCount = studentDAO.deleteByIds(idsToDelete); // Gọi hàm DAO mới
            System.out.println("StudentController: DAO reported " + deletedCount + " students deleted.");

            if (deletedCount > 0) {
                // Quan trọng: Gọi refresh sau khi xóa thành công
                if (studentPanel != null) {
                    studentPanel.refreshTable();
                }
                UIUtils.showInfoMessage(studentPanel, "Deletion Successful", deletedCount + " student(s) deleted successfully.");
                return true;
            } else {
                // Không có student nào bị xóa (có thể ID không tồn tại)
                UIUtils.showWarningMessage(studentPanel, "Deletion Info", "No matching students found for deletion.");
                return false; // Coi như không thành công nếu không xóa được gì
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