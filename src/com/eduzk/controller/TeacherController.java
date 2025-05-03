package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.TeacherPanel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import com.eduzk.view.MainView;

public class TeacherController {

    private final ITeacherDAO teacherDAO;
    private final User currentUser;
    private final IUserDAO userDAO;
    private TeacherPanel teacherPanel;
    private MainView mainView;

    public TeacherController(ITeacherDAO teacherDAO, IUserDAO userDAO, User currentUser) {
        this.teacherDAO = teacherDAO;
        this.currentUser = currentUser;
        this.userDAO = userDAO;
    }
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }

    public void setTeacherPanel(TeacherPanel teacherPanel) {
        this.teacherPanel = teacherPanel;
    }

    public List<Teacher> getAllTeachers() {
        try {
            return teacherDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading teachers: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to load teacher data.");
            return Collections.emptyList();
        }
    }

    public List<Teacher> searchTeachersBySpecialization(String specialization) {
        if (!ValidationUtils.isNotEmpty(specialization)) {
            return getAllTeachers(); // Return all if search is empty
        }
        try {
            return teacherDAO.findBySpecialization(specialization);
        } catch (DataAccessException e) {
            System.err.println("Error searching teachers: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to search teachers.");
            return Collections.emptyList();
        }
    }

    public boolean addTeacher(Teacher teacher) {
        if (teacher == null || !ValidationUtils.isNotEmpty(teacher.getFullName())) {
            UIUtils.showWarningMessage(teacherPanel, "Validation Error", "Teacher name cannot be empty.");
            return false;
        }

        try {
            teacherDAO.add(teacher);
            boolean userCreatedSuccessfully = false;
            if (teacher.getTeacherId() > 0) {
                // Tạo username/password mặc định (Cần quy tắc rõ ràng)
                String defaultUsername = teacher.getEmail(); //Dùng email làm username
                String defaultPassword = "123456";
                User newUser = new User();
                newUser.setUsername(defaultUsername);
                newUser.setPassword(defaultPassword);
                newUser.setRole(Role.TEACHER);
                newUser.setActive(teacher.isActive());
                newUser.setTeacherId(teacher.getTeacherId());
                newUser.setStudentId(null);

                try {
                    System.out.println("Attempting to add User: " + newUser.getUsername() + " for Teacher ID: " + teacher.getTeacherId());
                    userDAO.add(newUser);
                    System.out.println("Successfully added User account for Teacher ID: " + teacher.getTeacherId());
                } catch (DataAccessException e) {
                    System.err.println("!!! FAILED to add User account for Teacher ID " + teacher.getTeacherId() + " !!! DAO Error: " + e.getMessage());
                    e.printStackTrace();
                    UIUtils.showWarningMessage(teacherPanel, "User Creation Failed", "Teacher added, but failed to create linked user account:\n" + e.getMessage());
                } catch (Exception ex) {
                    System.err.println("!!! UNEXPECTED ERROR adding User account for Teacher ID " + teacher.getTeacherId() + " !!! Error: " + ex.getMessage());
                    ex.printStackTrace();
                    UIUtils.showErrorMessage(teacherPanel, "Unexpected Error", "An unexpected error occurred while creating the user account.");
                }

            } else {
                System.err.println("Could not get Teacher ID after adding teacher. User account not created.");
                UIUtils.showWarningMessage(teacherPanel, "User Creation Failed", "Teacher added, but could not get ID to create linked user account.");
            }

            if (teacherPanel != null) {
                teacherPanel.refreshTable();
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to add teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTeacher(Teacher teacher) {
        if (teacher == null || teacher.getTeacherId() <= 0 || !ValidationUtils.isNotEmpty(teacher.getFullName())) {
            UIUtils.showWarningMessage(teacherPanel, "Validation Error", "Invalid teacher data for update.");
            return false;
        }
        // Add more validation...

        try {
            teacherDAO.update(teacher);
            if (teacherPanel != null) {
                teacherPanel.refreshTable();
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to update teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTeacher(int teacherId) {
        if (teacherId <= 0) {
            UIUtils.showWarningMessage(teacherPanel, "Error", "Invalid teacher ID for deletion.");
            return false;
        }
        // Confirmation dialog in View layer
        try {
            teacherDAO.delete(teacherId);
            if (teacherPanel != null) {
                teacherPanel.refreshTable();
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Handle specific errors, e.g., teacher assigned to classes
            System.err.println("Error deleting teacher: " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to delete teacher: " + e.getMessage());
            return false;
        }
    }

    public Teacher getTeacherById(int teacherId) {
        if (teacherId <= 0) return null;
        try {
            return teacherDAO.getById(teacherId);
        } catch (DataAccessException e) {
            System.err.println("Error getting teacher by ID: " + e.getMessage());
            return null;
        }
    }
    public boolean deleteMultipleTeachers(List<Integer> teacherIdsToDelete) {
        if (teacherIdsToDelete == null || teacherIdsToDelete.isEmpty()) { /*...*/ return false; }
        if (teacherDAO == null || userDAO == null) { /* Báo lỗi DAO null */ return false; } // Cần cả userDAO

        System.out.println("Attempting to delete multiple teachers using DAO: " + teacherIdsToDelete);

        try {
            // 1. Xóa các bản ghi Teacher
            int deletedTeacherCount = teacherDAO.deleteMultiple(teacherIdsToDelete);
            System.out.println("Deletion process finished via DAO. Actual deleted teacher count: " + deletedTeacherCount);

            int deletedUserCount = 0;
            if (deletedTeacherCount > 0) {
                // --- 2. XÓA CÁC USER LIÊN KẾT ---
                System.out.println("TeacherController: Attempting to delete linked user accounts...");
                for (Integer teacherId : teacherIdsToDelete) { // Lặp qua các ID đã yêu cầu xóa
                    try {
                        // *** TÌM USER BẰNG TEACHER ID ***
                        Optional<User> userOpt = userDAO.findByTeacherId(teacherId); // Gọi hàm DAO mới
                        if (userOpt.isPresent()) {
                            User userToDelete = userOpt.get();
                            System.out.println("Found linked user for teacher ID " + teacherId + ": User ID " + userToDelete.getUserId() + ", Username: " + userToDelete.getUsername());
                            userDAO.delete(userToDelete.getUserId()); // Xóa User bằng userId
                            System.out.println("Deleted linked user ID: " + userToDelete.getUserId());
                            deletedUserCount++;
                        } else {
                            System.out.println("No linked user found for teacher ID " + teacherId);
                        }
                    } catch (DataAccessException e) {

                    }
                    catch (Exception ex) {

                    }
                }
                System.out.println("TeacherController: Finished linked user deletion attempts. Deleted " + deletedUserCount + " user account(s).");
                // --- KẾT THÚC XÓA USER ---
            }

            // --- 3. REFRESH VÀ THÔNG BÁO ---
            if (deletedTeacherCount > 0 || deletedUserCount > 0) {
                if (teacherPanel != null) {
                    teacherPanel.refreshTable();
                }
                if (deletedUserCount > 0 && mainView != null) {
                    mainView.refreshAccountsPanelData(); // Gọi refresh AccountsPanel
                }
                UIUtils.showInfoMessage(teacherPanel, "Deletion Successful",
                        deletedTeacherCount + " teacher(s) and " + deletedUserCount + " linked user account(s) deleted successfully.");
                return true;
            } else {
                UIUtils.showWarningMessage(teacherPanel, "Deletion Info", "No matching teachers found or no linked users to delete.");
                return false;
            }

        } catch (DataAccessException e) {
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }
    public void importTeachersFromExcel() {
        if (teacherPanel == null) {
            System.err.println("TeacherPanel is null.");
            return;
        }
        if (!isCurrentUserAdmin()) {
            UIUtils.showErrorMessage(teacherPanel, "Permission Denied", "Only administrators can import teacher data.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Teacher Excel File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(teacherPanel);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Importing teachers from: " + selectedFile.getAbsolutePath());

            teacherPanel.setAllButtonsEnabled(false);

            SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                private int successCount = 0;
                private int errorCount = 0;
                private int processedCount = 0;
                private boolean anyUserAdded = false;

                @Override
                protected List<String> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    FileInputStream fis = null;
                    Workbook workbook = null;
                    try {
                        fis = new FileInputStream(selectedFile);
                        workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

                        Iterator<Row> rowIterator = sheet.iterator();
                        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua header

                        int rowNum = 1; // Bắt đầu từ dòng 2 (sau header)
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            processedCount++; // Tăng số dòng đã xử lý

                            Teacher addedTeacher = null; // Lưu lại teacher vừa thêm để lấy ID
                            boolean teacherAddSuccess = false;
                            boolean userAddSuccess = false;

                            try {
                                // --- 1. ĐỌC DỮ LIỆU TEACHER TỪ EXCEL ---
                                String fullName = getStringCellValue(row.getCell(0));
                                LocalDate dob = getDateCellValue(row.getCell(1));
                                String gender = getStringCellValue(row.getCell(2));
                                String specialization = getStringCellValue(row.getCell(3));
                                String phone = getStringCellValue(row.getCell(4));
                                String email = getStringCellValue(row.getCell(5)); // Dùng làm username
                                Boolean active = getBooleanCellValue(row.getCell(6));
                                if (active == null) active = true;

                                // --- 2. VALIDATION DỮ LIỆU TEACHER ---
                                if (!ValidationUtils.isNotEmpty(fullName)) {
                                    throw new IllegalArgumentException("Full Name is required.");
                                }
                                // **QUAN TRỌNG:** Email là bắt buộc để tạo username
                                if (!ValidationUtils.isNotEmpty(email) || !ValidationUtils.isValidEmail(email)) {
                                    throw new IllegalArgumentException("A valid Email is required (used as username).");
                                }
                                if (ValidationUtils.isNotEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) {
                                    throw new IllegalArgumentException("Invalid phone number format.");
                                }
                                // Thêm validation khác nếu cần

                                // --- 3. TẠO VÀ THÊM TEACHER ---
                                Teacher newTeacher = new Teacher();
                                newTeacher.setFullName(fullName.trim());
                                newTeacher.setDateOfBirth(dob);
                                newTeacher.setGender(gender != null ? gender.trim() : null);
                                newTeacher.setSpecialization(specialization != null ? specialization.trim() : null);
                                newTeacher.setPhone(phone != null ? phone.trim() : null);
                                newTeacher.setEmail(email.trim()); // Lưu email đã trim
                                newTeacher.setActive(active);

                                teacherDAO.add(newTeacher); // Thêm Teacher vào DAO
                                addedTeacher = newTeacher; // Lưu lại để lấy ID
                                teacherAddSuccess = true; // Đánh dấu Teacher đã thêm thành công

                                // --- 4. TẠO USER TƯƠNG ỨNG (NẾU THÊM TEACHER THÀNH CÔNG) ---
                                if (addedTeacher != null && addedTeacher.getTeacherId() > 0) {
                                    String defaultUsername = addedTeacher.getEmail(); // Đã validate ở trên
                                    String defaultPassword = "123456"; // Mật khẩu mặc định

                                    // Tạo đối tượng User
                                    User newUser = new User();
                                    newUser.setUsername(defaultUsername);
                                    newUser.setPassword(defaultPassword);
                                    newUser.setRole(Role.TEACHER);
                                    newUser.setActive(addedTeacher.isActive()); // Đồng bộ trạng thái
                                    newUser.setTeacherId(addedTeacher.getTeacherId()); // Liên kết ID
                                    newUser.setStudentId(null);

                                    // **Kiểm tra Username trùng trước khi thêm User**
                                    if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                                        throw new DataAccessException("Username '" + newUser.getUsername() + "' (from email) already exists for another user account.");
                                    }

                                    // Thêm User vào DAO
                                    userDAO.add(newUser);
                                    userAddSuccess = true; // Đánh dấu User đã thêm thành công
                                    System.out.println("Import - Row " + rowNum + ": Added User for Teacher ID: " + addedTeacher.getTeacherId());

                                } else {
                                    // Lỗi không mong muốn: Không lấy được ID sau khi thêm Teacher
                                    throw new IllegalStateException("Could not retrieve Teacher ID after adding. Linked User cannot be created.");
                                }

                                // Chỉ tăng successCount nếu cả hai đều thành công
                                if (teacherAddSuccess && userAddSuccess) {
                                    successCount++;
                                }

                            } catch (Exception rowEx) {
                                // Lỗi xảy ra khi xử lý dòng này (lỗi đọc, validation, thêm Teacher, hoặc thêm User)
                                String errorMsg = "Row " + rowNum + ": Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                errorCount++;
                                // Không tăng successCount nếu có lỗi
                            }
                        } // end while
                    } finally {
                        // Đóng tài nguyên (Giữ nguyên)
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                    return errors; // Trả về danh sách lỗi
                } // end doInBackground

                @Override
                protected void done() {
                    try {
                        List<String> errors = get(); // Lấy danh sách lỗi từ background task
                        if (teacherPanel != null) {
                            teacherPanel.refreshTable(); // Làm mới bảng
                            teacherPanel.setAllButtonsEnabled(true); // Bật lại nút
                        }
                        if (anyUserAdded && mainView != null) {
                            mainView.refreshAccountsPanelData();
                        }

                        // Xây dựng thông báo kết quả chi tiết hơn
                        StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append("Import finished.\n");
                        messageBuilder.append("Total rows processed (excluding header): ").append(processedCount).append("\n");
                        messageBuilder.append("Successfully imported (Teacher + User): ").append(successCount).append("\n");
                        messageBuilder.append("Errors encountered: ").append(errorCount);

                        if (errorCount > 0) {
                            messageBuilder.append("\n\nFirst few errors:\n");
                            // Giới hạn số lỗi hiển thị để tránh dialog quá dài
                            errors.stream().limit(10).forEach(err -> messageBuilder.append(err).append("\n"));
                            if (errors.size() > 10) {
                                messageBuilder.append("... (and more errors, check console log for details)\n");
                            }
                            // Hiển thị cảnh báo nếu có lỗi
                            UIUtils.showWarningMessage(teacherPanel, "Import Partially Successful", messageBuilder.toString());
                        } else {
                            // Hiển thị thông báo thành công nếu không có lỗi
                            UIUtils.showInfoMessage(teacherPanel, "Import Successful", messageBuilder.toString());
                        }

                    } catch (Exception e) {
                        // Lỗi xảy ra trong quá trình thực thi SwingWorker hoặc lấy kết quả
                        e.printStackTrace();
                        UIUtils.showErrorMessage(teacherPanel, "Import Error", "An unexpected error occurred during the import process: " + e.getMessage());
                        if (teacherPanel != null) {
                            teacherPanel.setAllButtonsEnabled(true); // Đảm bảo bật lại nút nếu có lỗi
                        }
                    }
                }
            };

            worker.execute();
        }
    }

    private boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }

    // --- CÁC HÀM HELPER ĐỌC EXCEL (Copy từ StudentController hoặc BaseController nếu có) ---
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case STRING: return cell.getStringCellValue();
                case NUMERIC: return String.valueOf((long)cell.getNumericCellValue()); // Cẩn thận với số thập phân/số 0
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                case FORMULA: return cell.getStringCellValue(); // Thử lấy giá trị đã tính
                default: return null;
            }
        } catch (Exception e) { return null; } // Trả về null nếu lỗi đọc ô
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                java.util.Date javaDate = cell.getDateCellValue();
                return javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return DateUtils.parseDate(cell.getStringCellValue()); // Thử parse từ String
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    // Hàm đọc giá trị Boolean từ ô (có thể là TRUE/FALSE hoặc số 1/0)
    private Boolean getBooleanCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue() != 0; // Coi 0 là false, khác 0 là true
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim().toLowerCase();
                if (val.equals("true") || val.equals("1") || val.equals("yes") || val.equals("active")) return true;
                if (val.equals("false") || val.equals("0") || val.equals("no") || val.equals("inactive")) return false;
            }
        } catch (Exception e) {

        }
        return null;
    }

}