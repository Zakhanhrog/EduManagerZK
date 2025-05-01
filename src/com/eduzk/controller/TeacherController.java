package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.TeacherPanel; // To update the panel's table
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TeacherController {

    private final ITeacherDAO teacherDAO;
    private final User currentUser;
    private TeacherPanel teacherPanel;

    public TeacherController(ITeacherDAO teacherDAO, User currentUser) {
        this.teacherDAO = teacherDAO;
        this.currentUser = currentUser;
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
        // Add more validation (email, phone, specialization)

        try {
            teacherDAO.add(teacher);
            if (teacherPanel != null) {
                teacherPanel.refreshTable(); // Assumes TeacherPanel has this method
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
        if (teacherIdsToDelete == null || teacherIdsToDelete.isEmpty()) {
            UIUtils.showWarningMessage(teacherPanel, "No Selection", "No teacher IDs provided for deletion.");
            return false;
        }
        if (teacherDAO == null) { System.err.println("TeacherDAO is null!"); return false; }

        System.out.println("Attempting to delete multiple teachers using DAO: " + teacherIdsToDelete);

        try {
            // Gọi phương thức DAO mới để xóa nhiều bản ghi cùng lúc
            int deletedCount = teacherDAO.deleteMultiple(teacherIdsToDelete); // <-- THAY ĐỔI CHÍNH Ở ĐÂY

            System.out.println("Deletion process finished via DAO. Actual deleted count: " + deletedCount);

            // Hiển thị thông báo dựa trên số lượng thực tế đã xóa
            if (deletedCount == teacherIdsToDelete.size()) {
                UIUtils.showInfoMessage(teacherPanel, "Deletion Successful", "Successfully deleted " + deletedCount + " teacher(s).");
            } else if (deletedCount > 0) {
                UIUtils.showWarningMessage(teacherPanel, "Deletion Partially Successful", "Deleted " + deletedCount + " out of " + teacherIdsToDelete.size() + " selected teachers. Some might not exist or could not be deleted.");
            } else {
                UIUtils.showWarningMessage(teacherPanel, "Deletion Failed", "No teachers were deleted. They might have already been removed or an error occurred.");
            }

            // Refresh bảng NGAY SAU KHI thao tác DAO hoàn tất
            if (teacherPanel != null) {
                System.out.println("Refreshing teacher table after multiple delete.");
                teacherPanel.refreshTable();
            } else {
                System.err.println("TeacherController Error: teacherPanel is null after deleteMultiple!");
            }
            return true; // Coi như thành công nếu không có Exception từ DAO

        } catch (DataAccessException e) {
            // Lỗi từ DAO khi xóa nhiều
            System.err.println("Error deleting multiple teachers: " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(teacherPanel, "Deletion Error", "An error occurred while deleting teachers: " + e.getMessage());
            // Vẫn nên refresh bảng để cập nhật những gì có thể đã bị xóa trước khi lỗi
            if (teacherPanel != null) {
                teacherPanel.refreshTable();
            }
            return false; // Báo lỗi
        } catch (Exception e) {
            // Lỗi không mong muốn khác
            System.err.println("Unexpected error during multiple teacher deletion: " + e.getMessage());
            e.printStackTrace();
            if (teacherPanel != null) {
                UIUtils.showErrorMessage(teacherPanel, "Unexpected Error", "An unexpected error occurred during deletion.");
                teacherPanel.refreshTable(); // Cố gắng refresh
            }
            return false;
        }
    }
    public void importTeachersFromExcel() {
        if (teacherPanel == null) { System.err.println("TeacherPanel is null."); return; }
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

            // Vô hiệu hóa nút khi import
            teacherPanel.setAllButtonsEnabled(false);
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
                        workbook = new XSSFWorkbook(fis);
                        Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

                        Iterator<Row> rowIterator = sheet.iterator();
                        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua header

                        int rowNum = 1;
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            try {
                                // --- ĐỌC DỮ LIỆU THEO CỘT CỦA TEACHER ---
                                // **QUAN TRỌNG:** Điều chỉnh chỉ số cột (0, 1, 2,...) cho đúng file Excel mẫu của bạn
                                String fullName = getStringCellValue(row.getCell(0));       // Cột 0: Full Name
                                LocalDate dob = getDateCellValue(row.getCell(1));         // Cột 1: Date of Birth
                                String gender = getStringCellValue(row.getCell(2));       // Cột 2: Gender
                                String specialization = getStringCellValue(row.getCell(3)); // Cột 3: Specialization
                                String phone = getStringCellValue(row.getCell(4));        // Cột 4: Phone
                                String email = getStringCellValue(row.getCell(5));        // Cột 5: Email
                                // Cột 6: Active (Có thể là TRUE/FALSE hoặc 1/0)
                                Boolean active = getBooleanCellValue(row.getCell(6));
                                if (active == null) active = true; // Mặc định là active nếu ô trống/lỗi

                                // --- Validation ---
                                if (!ValidationUtils.isNotEmpty(fullName)) {
                                    throw new IllegalArgumentException("Full Name is required.");
                                }
                                if (ValidationUtils.isNotEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) {
                                    throw new IllegalArgumentException("Invalid phone number format.");
                                }
                                if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) {
                                    throw new IllegalArgumentException("Invalid email format.");
                                }

                                // --- Tạo đối tượng Teacher ---
                                Teacher newTeacher = new Teacher();
                                newTeacher.setFullName(fullName.trim());
                                newTeacher.setDateOfBirth(dob);
                                newTeacher.setGender(gender != null ? gender.trim() : null);
                                newTeacher.setSpecialization(specialization != null ? specialization.trim() : null);
                                newTeacher.setPhone(phone != null ? phone.trim() : null);
                                newTeacher.setEmail(email != null ? email.trim() : null);
                                newTeacher.setActive(active);

                                // --- Gọi DAO thêm ---
                                teacherDAO.add(newTeacher); // Tự xử lý ID
                                successCount++;

                            } catch (Exception rowEx) {
                                String errorMsg = "Row " + rowNum + ": Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                errorCount++;
                            }
                        } // end while
                    } finally {
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                    return errors;
                }

                @Override
                protected void done() {
                    try {
                        List<String> errors = get();
                        if (teacherPanel != null) {
                            teacherPanel.refreshTable(); // Làm mới bảng
                            teacherPanel.setAllButtonsEnabled(true); // Bật lại nút
                        }

                        String message = "Import finished.\nSuccessfully imported: " + successCount + " teachers.\nErrors: " + errorCount;
                        if (errorCount > 0) {
                            message += "\n\nFirst few errors:\n" + String.join("\n", errors.stream().limit(5).toArray(String[]::new));
                            UIUtils.showWarningMessage(teacherPanel, "Import Partially Successful", message);
                        } else {
                            UIUtils.showInfoMessage(teacherPanel, "Import Successful", message);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        UIUtils.showErrorMessage(teacherPanel, "Import Error", "An error occurred: " + e.getMessage());
                        if (teacherPanel != null) {
                            teacherPanel.setAllButtonsEnabled(true);
                        }
                    }
                }
            };
            worker.execute(); // Chạy import
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
        } catch (Exception e) { /* ignore */ }
        return null; // Không xác định được
    }

}