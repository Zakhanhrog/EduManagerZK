package com.eduzk.controller;

import com.eduzk.model.entities.*;
import com.eduzk.model.dao.interfaces.ITeacherDAO;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.PasswordUtils;

import com.eduzk.view.MainView;

public class TeacherController {

    private final ITeacherDAO teacherDAO;
    private final User currentUser;
    private final IUserDAO userDAO;
    private final LogService logService;
    private TeacherPanel teacherPanel;
    private MainView mainView;

    public TeacherController(ITeacherDAO teacherDAO, IUserDAO userDAO, User currentUser, LogService logService) {
        this.teacherDAO = teacherDAO;
        this.currentUser = currentUser;
        this.userDAO = userDAO;
        this.logService = logService;
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
            return getAllTeachers();
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
        if (!ValidationUtils.isNotEmpty(teacher.getEmail()) || !ValidationUtils.isValidEmail(teacher.getEmail())) {
            UIUtils.showWarningMessage(teacherPanel, "Validation Error", "A valid Email is required (used as username).");
            return false;
        }

        boolean userCreatedSuccessfully = false;
        try {
            teacherDAO.add(teacher);
            System.out.println("Teacher added with ID: " + teacher.getTeacherId());

            if (teacher.getTeacherId() > 0) {
                String defaultUsername = teacher.getEmail();
                String defaultPassword = "123456";
                User newUser = new User();
                newUser.setUsername(defaultUsername);
                String hashedPassword = PasswordUtils.hashPassword(defaultPassword);
                newUser.setPassword(hashedPassword);
                newUser.setRole(Role.TEACHER);
                newUser.setActive(teacher.isActive());
                newUser.setTeacherId(teacher.getTeacherId());
                newUser.setStudentId(null);

                newUser.setRequiresPasswordChange(true);

                try {
                    if (userDAO.findByUsername(newUser.getUsername()).isPresent()) {
                        throw new DataAccessException("Username (Email) '" + newUser.getUsername() + "' already exists for another user account.");
                    }
                    userDAO.add(newUser);
                    userCreatedSuccessfully = true;
                    System.out.println("Successfully created User account for Teacher ID: " + teacher.getTeacherId());
                    writeAddLog("Added Teacher & User", teacher);

                } catch (DataAccessException | IllegalArgumentException e) {
                    System.err.println("!!! FAILED to add User account for Teacher ID " + teacher.getTeacherId() + " !!! DAO Error: " + e.getMessage());
                    UIUtils.showWarningMessage(teacherPanel, "User Creation Failed", "Teacher added, but failed to create linked user account:\n" + e.getMessage());
                    writeAddLog("Added Teacher (User Failed)", teacher);
                } catch (Exception ex) {
                    System.err.println("!!! UNEXPECTED ERROR adding User account for Teacher ID " + teacher.getTeacherId() + " !!! Error: " + ex.getMessage());
                    ex.printStackTrace();
                    UIUtils.showErrorMessage(teacherPanel, "Unexpected Error", "An unexpected error occurred while creating the user account.");
                    writeAddLog("Added Teacher (User Error)", teacher);
                }
            } else {
                System.err.println("Could not get Teacher ID after adding teacher. User account not created.");
                UIUtils.showWarningMessage(teacherPanel, "User Creation Failed", "Teacher added, but could not get ID to create linked user account.");
                writeAddLog("Added Teacher (ID Error)", teacher);
            }

            if (teacherPanel != null) {
                teacherPanel.refreshTable();
            }
            if (userCreatedSuccessfully && mainView != null) {
                mainView.refreshAccountsPanelData();
            }
            if(userCreatedSuccessfully) {
                UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher and linked User account added successfully.");
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
        try {
            teacherDAO.update(teacher);
            writeUpdateLog("Updated Teacher", teacher);
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

        boolean teacherDeleted = false;
        boolean userDeleted = false;
        User linkedUser = null;

        try {
            Optional<User> userOpt = userDAO.findByTeacherId(teacherId);
            if (userOpt.isPresent()) {
                linkedUser = userOpt.get();
                System.out.println("Found linked User " + linkedUser.getUsername() + " for Teacher ID " + teacherId);
            } else {
                System.out.println("No linked User found for Teacher ID " + teacherId);
            }

            teacherDAO.delete(teacherId);
            teacherDeleted = true;
            System.out.println("Deleted Teacher ID: " + teacherId);

            if (linkedUser != null) {
                try {
                    userDAO.delete(linkedUser.getUserId());
                    userDeleted = true;
                    System.out.println("Deleted linked User ID: " + linkedUser.getUserId());
                } catch (DataAccessException e) {
                    System.err.println("Error deleting linked user for teacher ID " + teacherId + ": " + e.getMessage());
                }
            }

            String userDetail = linkedUser != null ? ("Linked User: " + linkedUser.getUsername() + "(ID:"+linkedUser.getUserId()+")" + (userDeleted ? " - Deleted" : " - Deletion Failed")) : "No Linked User";
            writeDeleteLog("Deleted Teacher", "ID: " + teacherId + " | " + userDetail);


            if (teacherPanel != null) teacherPanel.refreshTable();
            if (userDeleted && mainView != null) mainView.refreshAccountsPanelData();
            UIUtils.showInfoMessage(teacherPanel, "Success", "Teacher" + (userDeleted ? " and linked User account" : "") + " deleted successfully.");
            return true;

        } catch (DataAccessException e) {
            System.err.println("Error deleting teacher or linked user for ID " + teacherId + ": " + e.getMessage());
            UIUtils.showErrorMessage(teacherPanel, "Error", "Failed to delete teacher: " + e.getMessage());
            if (teacherPanel != null) teacherPanel.refreshTable();
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error during teacher deletion for ID " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(teacherPanel, "Unexpected Error", "An unexpected error occurred during deletion.");
            if (teacherPanel != null) teacherPanel.refreshTable();
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
        if (teacherIdsToDelete == null || teacherIdsToDelete.isEmpty()) { return false; }
        if (teacherDAO == null || userDAO == null) { return false; }

        System.out.println("Attempting to delete multiple teachers: " + teacherIdsToDelete);
        int deletedTeacherCount = 0;
        int deletedUserCount = 0;
        List<String> finalLogDetails = new ArrayList<>();

        try {
            for (Integer teacherId : teacherIdsToDelete) {
                boolean teacherDeleted = false;
                boolean userDeleted = false;
                User linkedUser = null;
                String teacherName = "ID: " + teacherId;

                try {
                    Teacher teacher = teacherDAO.getById(teacherId);
                    if (teacher != null) teacherName = teacher.getFullName() + " (ID: " + teacherId + ")";

                    Optional<User> userOpt = userDAO.findByTeacherId(teacherId);
                    if (userOpt.isPresent()) linkedUser = userOpt.get();

                    teacherDAO.delete(teacherId);
                    teacherDeleted = true;
                    deletedTeacherCount++;

                    if (linkedUser != null) {
                        userDAO.delete(linkedUser.getUserId());
                        userDeleted = true;
                        deletedUserCount++;
                    }
                    finalLogDetails.add(teacherName + (userDeleted ? " + Linked User" : " (No/Failed Linked User)"));

                } catch (DataAccessException e) {
                    System.err.println("Error deleting teacher/user for ID " + teacherId + ": " + e.getMessage());
                    finalLogDetails.add(teacherName + " - FAILED: " + e.getMessage());
                } catch (Exception ex) {
                    System.err.println("Unexpected error deleting teacher/user for ID " + teacherId + ": " + ex.getMessage());
                    finalLogDetails.add(teacherName + " - FAILED (Unexpected)");
                    ex.printStackTrace();
                }
            }

            if (!finalLogDetails.isEmpty()) {
                writeDeleteLog("Deleted Multiple Teachers", String.join("; ", finalLogDetails));
            }

            if (deletedTeacherCount > 0 || deletedUserCount > 0) {
                if (teacherPanel != null) teacherPanel.refreshTable();
                if (deletedUserCount > 0 && mainView != null) mainView.refreshAccountsPanelData();
                UIUtils.showInfoMessage(teacherPanel, "Deletion Attempt Finished",
                        "Attempted to delete " + teacherIdsToDelete.size() + " teacher(s). \n" +
                                "Successfully deleted Teachers: " + deletedTeacherCount + "\n" +
                                "Successfully deleted Linked Users: " + deletedUserCount + "\n" +
                                "(Check console log for individual errors if any)");
                return true;
            } else {
                UIUtils.showWarningMessage(teacherPanel, "Deletion Info", "No teachers were deleted (may not exist or errors occurred).");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Unexpected error during multiple teacher deletion process: " + e.getMessage());
            e.printStackTrace();
            if (teacherPanel != null) {
                UIUtils.showErrorMessage(teacherPanel, "Unexpected Error", "An unexpected error occurred during deletion.");
                teacherPanel.refreshTable();
            }
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
                @Override
                protected Map<String, Object> doInBackground() throws Exception {
                    List<String> errors = new ArrayList<>();
                    List<Teacher> validTeachersToImport = new ArrayList<>();
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
                        if (rowIterator.hasNext()) rowIterator.next();

                        int rowNum = 1;
                        while (rowIterator.hasNext()) {
                            Row row = rowIterator.next();
                            rowNum++;
                            processedCount++;

                            try {
                                String fullName = getStringCellValue(row.getCell(0));
                                LocalDate dob = getDateCellValue(row.getCell(1));
                                String gender = getStringCellValue(row.getCell(2));
                                String specialization = getStringCellValue(row.getCell(3));
                                String phone = getStringCellValue(row.getCell(4));
                                String email = getStringCellValue(row.getCell(5));
                                Boolean active = getBooleanCellValue(row.getCell(6));
                                if (active == null) active = true;

                                if (!ValidationUtils.isNotEmpty(fullName)) throw new IllegalArgumentException("Full Name required.");
                                if (!ValidationUtils.isNotEmpty(email) || !ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Valid Email required (used as username).");
                                if (ValidationUtils.isNotEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) throw new IllegalArgumentException("Invalid phone number format.");
                                if (userDAO.findByUsername(email.trim()).isPresent()){
                                    throw new DataAccessException("Username (Email) '" + email.trim() + "' already exists.");
                                }

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
                                String defaultPassword = "123456";
                                String hashedPassword = PasswordUtils.hashPassword(defaultPassword);
                                tempUser.setPassword(hashedPassword);
                                tempUser.setRole(Role.TEACHER);
                                tempUser.setActive(active);
                                tempUser.setStudentId(null);
                                tempUser.setRequiresPasswordChange(true);
                                validTeachersToImport.add(tempTeacher);
                                validUsersToCreate.add(tempUser);

                            } catch (Exception rowEx) {
                                String errorMsg = "Row " + rowNum + ": Validation/Read Error - " + rowEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                                validationErrorCount++;
                            }
                        }

                        int actualTeacherSuccessCount = 0;
                        int actualUserSuccessCount = 0;

                        for (int i = 0; i < validTeachersToImport.size(); i++) {
                            Teacher teacherToAdd = validTeachersToImport.get(i);
                            User userToCreate = validUsersToCreate.get(i);
                            boolean teacherAdded = false;
                            boolean userAdded = false;
                            String currentProcessingInfo = " (Teacher: " + teacherToAdd.getFullName() + ", User: " + userToCreate.getUsername() + ")";

                            try {
                                teacherDAO.add(teacherToAdd);
                                if (teacherToAdd.getTeacherId() > 0) {
                                    teacherAdded = true;
                                    userToCreate.setTeacherId(teacherToAdd.getTeacherId());
                                    if (userDAO.findByTeacherId(userToCreate.getTeacherId()).isPresent()){
                                        throw new DataAccessException("Account for Teacher ID " + userToCreate.getTeacherId() + " already exists.");
                                    }
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
                                String errorMsg = "Import Save Error" + currentProcessingInfo + ": " + addEx.getMessage();
                                System.err.println(errorMsg);
                                errors.add(errorMsg);
                            }
                        }

                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("errors", errors);
                        resultData.put("processedCount", processedCount);
                        resultData.put("teacherSuccessCount", actualTeacherSuccessCount);
                        resultData.put("userSuccessCount", actualUserSuccessCount);
                        resultData.put("validationErrorCount", validationErrorCount);

                        return resultData;

                    } finally {
                        if (workbook != null) try { workbook.close(); } catch (IOException ignored) {}
                        if (fis != null) try { fis.close(); } catch (IOException ignored) {}
                    }
                }

                @Override
                protected void done() {
                    try {
                        Map<String, Object> resultData = get();
                        List<String> errors = (List<String>) resultData.get("errors");
                        int finalProcessedCount = (int) resultData.get("processedCount");
                        int finalTeacherSuccessCount = (int) resultData.get("teacherSuccessCount");
                        int finalUserSuccessCount = (int) resultData.get("userSuccessCount");
                        int finalValidationErrorCount = (int) resultData.get("validationErrorCount");
                        int totalErrors = errors != null ? errors.size() : 0;
                        int saveErrors = totalErrors - finalValidationErrorCount;

                        if (teacherPanel != null) {
                            System.out.println("Import done. Refreshing TeacherPanel...");
                            teacherPanel.refreshTable();
                            teacherPanel.setAllButtonsEnabled(true);
                        }
                        if (finalUserSuccessCount > 0 && mainView != null) {
                            System.out.println("Import done. Refreshing AccountsPanel...");
                            mainView.refreshAccountsPanelData();
                        }
                        if (finalTeacherSuccessCount > 0 || totalErrors > 0) {
                            String logDetails = String.format("Processed: %d, Teachers Added: %d, Users Created: %d, Validation Errors: %d, Save Errors: %d",
                                    finalProcessedCount,
                                    finalTeacherSuccessCount,
                                    finalUserSuccessCount,
                                    finalValidationErrorCount,
                                    saveErrors);
                            writeGeneralLog("Imported Teachers", logDetails);
                        }

                        StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append("Import finished.\n");
                        messageBuilder.append("Total rows processed (excluding header): ").append(finalProcessedCount).append("\n");
                        messageBuilder.append("Successfully imported Teachers: ").append(finalTeacherSuccessCount).append("\n");
                        messageBuilder.append("Successfully created User accounts: ").append(finalUserSuccessCount).append("\n");
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

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case STRING: return cell.getStringCellValue();
                case NUMERIC: return String.valueOf((long)cell.getNumericCellValue());
                case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                case FORMULA: return cell.getStringCellValue();
                default: return null;
            }
        } catch (Exception e) { return null; }
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                java.util.Date javaDate = cell.getDateCellValue();
                return javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                return DateUtils.parseDate(cell.getStringCellValue());
            }
        } catch (Exception e) { }
        return null;
    }

    private Boolean getBooleanCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.BOOLEAN) {
                return cell.getBooleanCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue() != 0;
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim().toLowerCase();
                if (val.equals("true") || val.equals("1") || val.equals("yes") || val.equals("active")) return true;
                if (val.equals("false") || val.equals("0") || val.equals("no") || val.equals("inactive")) return false;
            }
        } catch (Exception e) {

        }
        return null;
    }
    private void writeAddLog(String action, Teacher teacher) {
        writeLog(action, "ID: " + teacher.getTeacherId() + ", Name: " + teacher.getFullName() + ", Email: " + teacher.getEmail());
    }
    private void writeUpdateLog(String action, Teacher teacher) {
        writeLog(action, "ID: " + teacher.getTeacherId() + ", Name: " + teacher.getFullName());
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
                        currentUser.getDisplayName(),
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