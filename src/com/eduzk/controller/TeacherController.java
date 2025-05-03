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
import java.util.Map;
import java.util.HashMap;

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

            SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
                /*private int successCount = 0;
                private int errorCount = 0;
                private int processedCount = 0;
                private boolean anyUserAdded = false;*/

                @Override
                protected Map<String, Object> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    List<Teacher> validTeachersToImport = new ArrayList<>(); // Danh sách Teacher hợp lệ
                    List<User> validUsersToCreate = new ArrayList<>();       // Danh sách User tương ứng

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
                                // --- 1. ĐỌC & VALIDATE DỮ LIỆU ---
                                String fullName = getStringCellValue(row.getCell(0));
                                LocalDate dob = getDateCellValue(row.getCell(1));
                                String gender = getStringCellValue(row.getCell(2));
                                String specialization = getStringCellValue(row.getCell(3));
                                String phone = getStringCellValue(row.getCell(4));
                                String email = getStringCellValue(row.getCell(5)); // Dùng làm username
                                Boolean active = getBooleanCellValue(row.getCell(6));
                                if (active == null) active = true;

                                // --- Validation ---
                                if (!ValidationUtils.isNotEmpty(fullName)) throw new IllegalArgumentException("Full Name required.");
                                if (!ValidationUtils.isNotEmpty(email) || !ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Valid Email required (used as username).");
                                if (ValidationUtils.isNotEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) throw new IllegalArgumentException("Invalid phone number format.");
                                // **Kiểm tra trùng username (email) ngay bây giờ**
                                if (userDAO.findByUsername(email.trim()).isPresent()){
                                    throw new DataAccessException("Username (Email) '" + email.trim() + "' already exists.");
                                }

                                // --- 2. TẠO OBJECTS NHƯNG CHƯA LƯU ---
                                Teacher tempTeacher = new Teacher();
                                tempTeacher.setFullName(fullName.trim());
                                tempTeacher.setDateOfBirth(dob);
                                tempTeacher.setGender(gender != null ? gender.trim() : null);
                                tempTeacher.setSpecialization(specialization != null ? specialization.trim() : null);
                                tempTeacher.setPhone(phone != null ? phone.trim() : null);
                                tempTeacher.setEmail(email.trim());
                                tempTeacher.setActive(active);

                                User tempUser = new User();
                                tempUser.setUsername(email.trim());
                                tempUser.setPassword("123456"); // Mật khẩu mặc định
                                tempUser.setRole(Role.TEACHER);
                                tempUser.setActive(active);
                                tempUser.setStudentId(null);
                                // teacherId sẽ được set sau khi Teacher được lưu và có ID

                                // Thêm vào danh sách tạm nếu hợp lệ
                                validTeachersToImport.add(tempTeacher);
                                validUsersToCreate.add(tempUser); // User tạm thời chưa có teacherId

                            } catch (Exception rowEx) {
                                // Lỗi validation hoặc đọc dòng
                                String errorMsg = "Row " + rowNum + ": Validation/Read Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                validationErrorCount++;
                            }
                        } // end while

                        // --- 3. THỰC HIỆN LƯU VÀO DAO (SAU KHI ĐỌC HẾT FILE) ---
                        int actualTeacherSuccessCount = 0;
                        int actualUserSuccessCount = 0;

                        for (int i = 0; i < validTeachersToImport.size(); i++) {
                            Teacher teacherToAdd = validTeachersToImport.get(i);
                            User userToCreate = validUsersToCreate.get(i);
                            boolean teacherAdded = false;
                            boolean userAdded = false;
                            String currentProcessingInfo = " (Teacher: " + teacherToAdd.getFullName() + ", User: " + userToCreate.getUsername() + ")";

                            try {
                                // Thêm Teacher trước để lấy ID
                                teacherDAO.add(teacherToAdd);
                                if (teacherToAdd.getTeacherId() > 0) {
                                    teacherAdded = true;
                                    // Liên kết teacherId vào User
                                    userToCreate.setTeacherId(teacherToAdd.getTeacherId());

                                    // Thêm User (không cần kiểm tra trùng username nữa vì đã làm ở trên)
                                    userDAO.add(userToCreate);
                                    userAdded = true;
                                } else {
                                    throw new DataAccessException("Failed to get generated ID for teacher." + currentProcessingInfo);
                                }

                                if(teacherAdded && userAdded){
                                    actualTeacherSuccessCount++;
                                    actualUserSuccessCount++;
                                }

                            } catch (Exception addEx) {
                                // Lỗi khi thêm Teacher hoặc User vào DAO
                                String errorMsg = "Import Save Error" + currentProcessingInfo + ": " + addEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                            }
                        } // end for loop saving

                        // Chuẩn bị kết quả trả về
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("errors", errors);
                        resultData.put("processedCount", processedCount);
                        resultData.put("teacherSuccessCount", actualTeacherSuccessCount);
                        resultData.put("userSuccessCount", actualUserSuccessCount);
                        resultData.put("validationErrorCount", validationErrorCount);

                        return resultData;

                    } finally {
                        // Đóng tài nguyên
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                }

                @Override
                protected void done() {
                    try {
                        // Lấy kết quả từ background task
                        Map<String, Object> resultData = get();
                        List<String> errors = (List<String>) resultData.get("errors");
                        int finalProcessedCount = (int) resultData.get("processedCount");
                        int finalTeacherSuccessCount = (int) resultData.get("teacherSuccessCount");
                        int finalUserSuccessCount = (int) resultData.get("userSuccessCount"); // Số user thực sự được tạo
                        int finalValidationErrorCount = (int) resultData.get("validationErrorCount");
                        int totalErrors = errors.size(); // Bao gồm cả lỗi validation và lỗi lưu

                        // *** LUÔN GỌI REFRESH ĐỂ LẤY DỮ LIỆU MỚI NHẤT TỪ DAO ***
                        if (teacherPanel != null) {
                            System.out.println("Import done. Refreshing TeacherPanel...");
                            teacherPanel.refreshTable(); // Panel sẽ tự lấy dữ liệu đúng từ DAO
                            teacherPanel.setAllButtonsEnabled(true);
                        }

                        // Refresh AccountsPanel nếu có User được thêm thành công
                        if (finalUserSuccessCount > 0 && mainView != null) {
                            System.out.println("Import done. Refreshing AccountsPanel...");
                            mainView.refreshAccountsPanelData();
                        }

                        // Xây dựng thông báo kết quả chi tiết
                        StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append("Import finished.\n");
                        messageBuilder.append("Total rows processed (excluding header): ").append(finalProcessedCount).append("\n");
                        messageBuilder.append("Successfully imported Teachers: ").append(finalTeacherSuccessCount).append("\n");
                        messageBuilder.append("Successfully created User accounts: ").append(finalUserSuccessCount).append("\n");
                        int saveErrors = totalErrors - finalValidationErrorCount;
                        messageBuilder.append("Validation/Read errors: ").append(finalValidationErrorCount).append("\n");
                        messageBuilder.append("Save errors (Teacher or User): ").append(saveErrors);


                        if (totalErrors > 0) {
                            messageBuilder.append("\n\nFirst few errors:\n");
                            errors.stream().limit(10).forEach(err -> messageBuilder.append(err).append("\n"));
                            if (errors.size() > 10) {
                                messageBuilder.append("... (check console log for all errors)\n");
                            }
                            UIUtils.showWarningMessage(teacherPanel, "Import Partially Successful", messageBuilder.toString());
                        } else {
                            UIUtils.showInfoMessage(teacherPanel, "Import Successful", messageBuilder.toString());
                        }

                    } catch (Exception e) {
                        // Lỗi trong quá trình thực thi SwingWorker
                        e.printStackTrace();
                        UIUtils.showErrorMessage(teacherPanel, "Import Error", "An unexpected error occurred during the import process: " + e.getMessage());
                        if (teacherPanel != null) {
                            teacherPanel.setAllButtonsEnabled(true);
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