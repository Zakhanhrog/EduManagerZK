package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.EducationPanel;
import com.eduzk.model.dao.interfaces.ClassListChangeListener;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

public class EducationController implements ClassListChangeListener {

    private final User currentUser;
    private final IAcademicRecordDAO recordDAO;
    private final IEduClassDAO classDAO;
    private final IStudentDAO studentDAO;
    private final LogService logService;
    private EduClassController eduClassControllerRef;

    private EducationPanel educationPanel;

    private List<AcademicRecord> currentDisplayedRecords;
    private List<Student> currentDisplayedStudents;
    private int currentSelectedClassId = -1;

    public EducationController(
            User currentUser,
            IAcademicRecordDAO recordDAO,
            IEduClassDAO classDAO,
            IStudentDAO studentDAO,
            LogService logService,
            EduClassController eduClassController) {

        this.currentUser = currentUser;
        this.recordDAO = recordDAO;
        this.classDAO = classDAO;
        this.studentDAO = studentDAO;
        this.logService = logService;
        this.currentDisplayedRecords = new ArrayList<>();
        this.currentDisplayedStudents = new ArrayList<>();

        this.eduClassControllerRef = eduClassController;
        if (this.eduClassControllerRef != null) {
            this.eduClassControllerRef.addClassListChangeListener(this);
            System.out.println("EducationController registered for ClassList changes.");
        } else {
            System.err.println("Warning: EduClassController is null in EducationController constructor, cannot listen for class list changes.");
        }
    }

    public void setEducationPanel(EducationPanel educationPanel) {
        this.educationPanel = educationPanel;
    }

    public List<EduClass> getClassesForCurrentUser() {
        if (currentUser == null) {
            return Collections.emptyList();
        }
        try {
            List<EduClass> classes;
            switch (currentUser.getRole()) {
                case ADMIN:
                    classes = classDAO.getAll();
                    break;
                case TEACHER:
                    if (currentUser.getTeacherId() != null) {
                        classes = classDAO.findByTeacherId(currentUser.getTeacherId());
                    } else {
                        classes = Collections.emptyList();
                    }
                    break;
                case STUDENT:
                default:
                    classes = Collections.emptyList();
                    break;
            }
            if (classes != null) {
                classes.sort(Comparator.comparing(EduClass::getClassName, String.CASE_INSENSITIVE_ORDER));
            }
            return classes;

        } catch (DataAccessException e) {
            System.err.println("Error getting classes for user: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void loadDataForClass(int classId) {
        if (currentUser.getRole() == Role.STUDENT || classId <= 0) {
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
            return;
        }

        this.currentSelectedClassId = classId;
        System.out.println("EducationController: Loading data for class ID: " + classId);

        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null) {
                clearCurrentData();
                if (educationPanel != null) {
                    educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                }
                System.err.println("Class not found: " + classId);
                UIUtils.showWarningMessage(educationPanel, "Not Found", "Selected class could not be found.");
                return;
            }

            List<Integer> studentIds = selectedClass.getStudentIds();
            if (studentIds == null || studentIds.isEmpty()) {
                clearCurrentData();
                if (educationPanel != null) {
                    educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                }
                System.out.println("Class has no students: " + classId);
                return;
            }

            this.currentDisplayedStudents = studentIds.stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            this.currentDisplayedRecords = this.currentDisplayedStudents.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());

            if (educationPanel != null) {
                educationPanel.updateTableData(this.currentDisplayedStudents, this.currentDisplayedRecords);
                writeLog("Viewed Grades", "Viewed grade data for class: " + selectedClass.getClassName() + " (ID: " + classId + ")");
            }

        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Data Load Error", "Failed to load student or grade data for the selected class.");
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
        }
    }

    public void loadDataForCurrentStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            clearCurrentData();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("EducationController: Loading grade data for current student ID: " + studentId);
        try {
            Student currentStudent = studentDAO.getById(studentId);
            if (currentStudent == null) {
                System.err.println("Error: Student profile not found for current user ID: " + studentId);
                clearCurrentData();
                if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                UIUtils.showErrorMessage(null, "Error", "Could not find your student profile.");
                return;
            }

            this.currentDisplayedRecords = recordDAO.findAllByStudentId(studentId);
            this.currentDisplayedStudents = List.of(currentStudent);
            this.currentSelectedClassId = -1;

            if (educationPanel != null) {
                educationPanel.updateTableDataForStudent(currentStudent, this.currentDisplayedRecords);
                writeLog("Viewed Grades", "Student viewed their own grades.");
            }

        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for student " + studentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Data Load Error", "Failed to load your academic records.");
            clearCurrentData();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
        }
    }

    private AcademicRecord findOrCreateRecordForStudent(int studentId, int classId) {
        try {
            Optional<AcademicRecord> existingRecord = recordDAO.findByStudentAndClass(studentId, classId);
            return existingRecord.orElseGet(() -> new AcademicRecord(studentId, classId));
        } catch (DataAccessException e) {
            System.err.println("Error finding/creating record for student " + studentId + " in class " + classId + ": " + e.getMessage());
            return new AcademicRecord(studentId, classId);
        }
    }

    public void updateRecordInMemory(int rowIndex, String subjectKey, Object value) {
        if (!canCurrentUserEdit()) { return; }
        if (rowIndex < 0 || rowIndex >= currentDisplayedRecords.size()) { return; }

        AcademicRecord record = currentDisplayedRecords.get(rowIndex);
        Object oldValue = null; // Giữ giá trị cũ để rollback nếu cần
        if ("Hạnh kiểm".equals(subjectKey)) oldValue = record.getConductRating();
        else if ("Nghệ thuật".equals(subjectKey)) oldValue = record.getArtStatus();
        else oldValue = record.getGrade(subjectKey);

        boolean changed = false;
        Object validatedValue = value; // Lưu giá trị đã được xử lý/validate

        try {
            if ("Hạnh kiểm".equals(subjectKey)) {
                if (value instanceof ConductRating) {
                    if (!Objects.equals(oldValue, value)){
                        record.setConductRating((ConductRating) value);
                        validatedValue = value; // Giá trị hợp lệ
                        changed = true;
                    }
                } else if (value == null && oldValue != null) {
                    record.setConductRating(null);
                    validatedValue = null; // Giá trị hợp lệ (null)
                    changed = true;
                } else if (!(value instanceof ConductRating) && value != null) {
                    // Ném lỗi nếu kiểu không đúng và không phải null
                    throw new IllegalArgumentException("Invalid value for Conduct.");
                }
            } else if ("Nghệ thuật".equals(subjectKey)) {
                if (value instanceof ArtStatus) {
                    if (!Objects.equals(oldValue, value)){
                        record.setArtStatus((ArtStatus) value);
                        validatedValue = value;
                        changed = true;
                    }
                } else if (value == null && oldValue != null) {
                    record.setArtStatus(null);
                    validatedValue = null;
                    changed = true;
                } else if (!(value instanceof ArtStatus) && value != null) {
                    throw new IllegalArgumentException("Invalid value for Art Status.");
                }
            } else { // Điểm số
                Double grade = null;
                if (value instanceof Number) {
                    grade = ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    String strVal = ((String) value).trim();
                    if (strVal.isEmpty()) {
                        grade = null;
                    } else {
                        try {
                            grade = Double.parseDouble(strVal);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid number format.");
                        }
                    }
                } else if (value != null) { // Nếu không phải Number, String hoặc null -> lỗi
                    throw new IllegalArgumentException("Invalid data type for grade.");
                }

                if (grade != null && (grade < 0 || grade > 10)) {
                    throw new IllegalArgumentException("Grade must be between 0 and 10.");
                }

                if (!Objects.equals(record.getGrade(subjectKey), grade)) {
                    record.setGrade(subjectKey, grade);
                    validatedValue = grade;
                    changed = true;
                }
            }

            if (changed && educationPanel != null) {
                educationPanel.updateSpecificCellValue(rowIndex, subjectKey, validatedValue);

                educationPanel.updateCalculatedValues(rowIndex, record);
                educationPanel.markChangesPending(true);
                writeLog("Edited Grade/Conduct", "Edited data for Student ID: " + record.getStudentId() + " ... (Subject/Item: " + subjectKey + ")");
            }

        } catch (IllegalArgumentException e) {
            UIUtils.showWarningMessage(educationPanel,"Validation Error", e.getMessage());
            if(educationPanel != null) {
                System.out.println("Validation error for " + subjectKey + ". Clearing cell visual.");
                educationPanel.updateSpecificCellValue(rowIndex, subjectKey, null);
            }
        }
    }

    public void saveAllChanges() {
        if (!canCurrentUserEdit()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to save changes.");
            return;
        }
        if (currentDisplayedRecords == null || currentDisplayedRecords.isEmpty() || currentSelectedClassId <= 0) {
            UIUtils.showInfoMessage(educationPanel, "No Data", "There is no data or class selected to save.");
            return;
        }

        if (!UIUtils.showConfirmDialog(educationPanel, "Confirm Save", "Save all grade and conduct changes for the current class?")) {
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();

        System.out.println("Saving " + currentDisplayedRecords.size() + " academic records for class ID: " + currentSelectedClassId);

        List<AcademicRecord> recordsToSave = new ArrayList<>(currentDisplayedRecords);

        for (AcademicRecord record : recordsToSave) {
            try {
                if (record.getStudentId() > 0 && record.getClassId() > 0) {
                    recordDAO.addOrUpdate(record);
                    successCount++;
                } else {
                    System.err.println("Skipping save for invalid record: " + record);
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                errorCount++;
                String studentName = getStudentNameById(record.getStudentId());
                String msg = "Failed to save record for " + studentName + " (ID: " + record.getStudentId() + "): " + e.getMessage();
                System.err.println(msg);
                errorMessages.add(msg);
            } catch (Exception e) {
                errorCount++;
                String studentName = getStudentNameById(record.getStudentId());
                String msg = "Unexpected error saving record for " + studentName + " (ID: " + record.getStudentId() + "): " + e.getMessage();
                System.err.println(msg);
                errorMessages.add(msg);
                e.printStackTrace();
            }
        }

        System.out.println("Save operation complete. Success: " + successCount + ", Errors: " + errorCount);

        String logDetails = String.format("Saved %d/%d records for Class ID %d. Errors: %d",
                successCount, recordsToSave.size(), currentSelectedClassId, errorCount);
        writeLog("Saved Grades", logDetails);

        if (errorCount > 0) {
            UIUtils.showWarningMessage(educationPanel, "Save Partially Successful",
                    String.format("Successfully saved %d records.\nFailed to save %d records.\n\nSee console or logs for details.",
                            successCount, errorCount));
        } else {
            UIUtils.showInfoMessage(educationPanel, "Save Successful", "All changes saved successfully.");
        }

        if (educationPanel != null) {
            educationPanel.markChangesPending(false);
        }
    }

    public String getStudentNameById(int studentId) {
        if (currentDisplayedStudents != null) {
            for (Student s : currentDisplayedStudents) {
                if (s.getStudentId() == studentId) {
                    return s.getFullName();
                }
            }
        }
        try {
            Student student = studentDAO.getById(studentId);
            return (student != null) ? student.getFullName() : "Student [" + studentId + "]";
        } catch (DataAccessException e) {
            System.err.println("Error getting student name for ID " + studentId + ": " + e.getMessage());
            return "Error [" + studentId + "]";
        }
    }

    public boolean canCurrentUserEdit() {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ADMIN) return true;

        if (currentUser.getRole() == Role.TEACHER) {
            if (currentSelectedClassId <= 0 || currentUser.getTeacherId() == null) {
                return false;
            }
            try {
                EduClass selectedClass = classDAO.getById(currentSelectedClassId);
                return selectedClass != null &&
                        selectedClass.getPrimaryTeacher() != null &&
                        currentUser.getTeacherId().equals(selectedClass.getPrimaryTeacher().getTeacherId());
            } catch (DataAccessException e) {
                System.err.println("Error checking edit permission: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public Object[][] getGradeDataForExport(int classId) {
        if (currentUser.getRole() == Role.STUDENT) {
            return new Object[0][0];
        }

        List<Student> studentsForExport;
        List<AcademicRecord> recordsForExport;
        String className = "Class " + classId;

        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null || selectedClass.getStudentIds() == null || selectedClass.getStudentIds().isEmpty()) {
                return new Object[0][0];
            }
            className = selectedClass.getClassName();
            List<Integer> studentIds = selectedClass.getStudentIds();

            studentsForExport = studentIds.stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            recordsForExport = studentsForExport.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            System.err.println("Error preparing export data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Export Error", "Could not load data for export.");
            return new Object[0][0];
        }

        String[] columnHeaders = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật", "TB KHTN", "TB KHXH", "TB môn học", "Hạnh kiểm"};
        Object[][] exportData = new Object[studentsForExport.size()][columnHeaders.length];

        for (int i = 0; i < studentsForExport.size(); i++) {
            Student student = studentsForExport.get(i);
            AcademicRecord record = recordsForExport.get(i);

            exportData[i][0] = i + 1;
            exportData[i][1] = student.getFullName();
            exportData[i][2] = record.getGrade("Toán");
            exportData[i][3] = record.getGrade("Văn");
            exportData[i][4] = record.getGrade("Anh");
            exportData[i][5] = record.getGrade("Lí");
            exportData[i][6] = record.getGrade("Hoá");
            exportData[i][7] = record.getGrade("Sinh");
            exportData[i][8] = record.getGrade("Sử");
            exportData[i][9] = record.getGrade("Địa");
            exportData[i][10] = record.getGrade("GDCD");
            exportData[i][11] = (record.getArtStatus() != null) ? record.getArtStatus().toString() : "";
            exportData[i][12] = record.calculateAvgNaturalSciences();
            exportData[i][13] = record.calculateAvgSocialSciences();
            exportData[i][14] = record.calculateAvgOverallSubjects();
            exportData[i][15] = (record.getConductRating() != null) ? record.getConductRating().toString() : "";
        }

        writeLog("Exported Grades", "Prepared grade data for export for class: " + className + " (ID: " + classId + ")");
        return exportData;
    }

    public int getCurrentSelectedClassId() {
        return currentSelectedClassId;
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
                System.err.println("!!! Failed to write log entry: Action=" + action + ", Details=" + details + " - Error: " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

    @Override
    public void classListChanged() {
        System.out.println("EducationController received classListChanged notification.");
        if (educationPanel != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.TEACHER)) {
            SwingUtilities.invokeLater(() -> educationPanel.reloadClassList());
        }
    }

    public void cleanup() {
        if (this.eduClassControllerRef != null) {
            this.eduClassControllerRef.removeClassListChangeListener(this);
            System.out.println("EducationController unregistered from ClassList changes.");
        }
    }

    private void clearCurrentData() {
        this.currentSelectedClassId = -1;
        this.currentDisplayedStudents.clear();
        this.currentDisplayedRecords.clear();
    }
}