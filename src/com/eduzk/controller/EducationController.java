package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import com.eduzk.model.entities.Role;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.EducationPanel;
import com.eduzk.model.dao.interfaces.ClassListChangeListener;
import com.eduzk.view.dialogs.AssignmentDialog;
import javax.swing.*;
import java.awt.Frame;
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
    private final IAssignmentDAO assignmentDAO;
    private EduClassController eduClassControllerRef;
    private EducationPanel educationPanel;
    private List<AcademicRecord> currentDisplayedRecords;
    private List<Student> currentDisplayedStudents;
    private int currentSelectedClassId = -1;
    private List<AcademicRecord> academicRecordsForSelectedClass;
    private List<Student> studentsForSelectedClass;

    public EducationController(
            User currentUser,
            IAcademicRecordDAO recordDAO,
            IEduClassDAO classDAO,
            IStudentDAO studentDAO,
            LogService logService,
            EduClassController eduClassController,
            IAssignmentDAO assignmentDAO) {
        if (currentUser == null) throw new IllegalArgumentException("CurrentUser cannot be null");
        if (recordDAO == null) throw new IllegalArgumentException("AcademicRecordDAO cannot be null");
        if (classDAO == null) throw new IllegalArgumentException("EduClassDAO cannot be null");
        if (studentDAO == null) throw new IllegalArgumentException("StudentDAO cannot be null");
        if (logService == null) throw new IllegalArgumentException("LogService cannot be null");
        if (assignmentDAO == null) throw new IllegalArgumentException("AssignmentDAO cannot be null");
        if (eduClassController == null) System.err.println("Warning: EduClassController is null in EducationController constructor, cannot listen for class list changes.");

        this.currentUser = currentUser;
        this.classDAO = classDAO;
        this.recordDAO = recordDAO;
        this.studentDAO = studentDAO;
        this.logService = logService;
        this.assignmentDAO = assignmentDAO;
        this.currentDisplayedRecords = new ArrayList<>();
        this.currentDisplayedStudents = new ArrayList<>();
        this.eduClassControllerRef = eduClassController;
        this.academicRecordsForSelectedClass = new ArrayList<>();
        this.studentsForSelectedClass = new ArrayList<>();

        if (this.eduClassControllerRef != null) {
            this.eduClassControllerRef.addClassListChangeListener(this);
            System.out.println("EducationController registered for ClassList changes.");
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
                case TEACHER:
                    classes = classDAO.getAll();
                    break;
                case STUDENT:
                default:
                    classes = Collections.emptyList();
                    break;
            }
            if (classes != null) {
                classes.sort(Comparator.comparing(EduClass::getClassName, String.CASE_INSENSITIVE_ORDER));
            } else {
                classes = Collections.emptyList();
            }
            return classes;

        } catch (DataAccessException e) {
            System.err.println("Error getting classes for user: " + currentUser.getUsername() + " - " + e.getMessage());
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
        System.out.println("[Controller] Loading data for class ID: " + classId);

        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null) {
                clearCurrentData();
                if (educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                System.err.println("Class not found for grade loading: " + classId);
                return;
            }
            this.studentsForSelectedClass = studentDAO.getStudentsByClassId(classId);
            if (this.studentsForSelectedClass != null) {
                this.studentsForSelectedClass.sort(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER));
            } else {
                this.studentsForSelectedClass = new ArrayList<>();
            }
            System.out.println("[Controller] Fetched Students count: " + this.studentsForSelectedClass.size());

            this.academicRecordsForSelectedClass = this.studentsForSelectedClass.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());
            System.out.println("[Controller] Generated/Fetched Academic Records count: " + this.academicRecordsForSelectedClass.size());

            this.currentDisplayedStudents = this.studentsForSelectedClass;
            this.currentDisplayedRecords = this.academicRecordsForSelectedClass;

            if (educationPanel != null) {
                System.out.println("[Controller] Calling educationPanel.updateTableData with " +
                        this.studentsForSelectedClass.size() + " students and " +
                        this.academicRecordsForSelectedClass.size() + " records.");
                educationPanel.updateTableData(this.studentsForSelectedClass, this.academicRecordsForSelectedClass);
                String classNameLog = selectedClass.getClassName();
                writeLog("Viewed Grades", "Viewed grade data for class: " + classNameLog + " (ID: " + classId + ")");
            } else {
                System.err.println("[Controller] Error: educationPanel is null!");
            }
        } catch (DataAccessException e) {
            System.err.println("[Controller] DataAccessException in loadDataForClass: " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Data Load Error", "Failed to load student or grade data for the selected class.");
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
        } catch (Exception ex) {
            System.err.println("[Controller] Unexpected Exception in loadDataForClass: " + ex.getMessage());
            ex.printStackTrace();
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
            UIUtils.showErrorMessage(educationPanel, "Unexpected Error", "An unexpected error occurred while loading data.");
        }
    }

    public void loadDataForCurrentStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateStudentInfoDisplay(null, null);
                educationPanel.updateAchievementCertificateDisplay(null, null, "Chưa có thông tin sinh viên.");
                educationPanel.updateTableDataForStudent((Student) null, Collections.emptyList());
            }
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("EducationController: Loading data for current student ID: " + studentId);

        try {
            Student currentStudent = studentDAO.getById(studentId);
            if (currentStudent == null) {
                System.err.println("Error: Student profile not found for current user ID: " + studentId);
                clearCurrentData();
                if (educationPanel != null) {
                    educationPanel.updateStudentInfoDisplay(null, null);
                    educationPanel.updateAchievementCertificateDisplay(null, null, "Không tìm thấy hồ sơ sinh viên.");
                    educationPanel.updateTableDataForStudent((Student) null, Collections.emptyList());
                }
                UIUtils.showErrorMessage(null, "Error", "Could not find your student profile.");
                return;
            }

            EduClass studentPrimaryClass = null;
            List<EduClass> studentClasses = classDAO.findByStudentId(studentId);
            if (studentClasses != null && !studentClasses.isEmpty()) {
                studentPrimaryClass = studentClasses.get(0);
            }

            List<AcademicRecord> allStudentRecords = recordDAO.findAllByStudentId(studentId);
            if (allStudentRecords == null) {
                allStudentRecords = new ArrayList<>();
            }

            AcademicRecord recordForAchievementAndTable = null;
            if (!allStudentRecords.isEmpty()) {
                if (studentPrimaryClass != null) {
                    final int primaryClassId = studentPrimaryClass.getClassId();
                    Optional<AcademicRecord> foundRecord = allStudentRecords.stream()
                            .filter(r -> r != null && r.getClassId() == primaryClassId)
                            .findFirst();
                    recordForAchievementAndTable = foundRecord.orElse(allStudentRecords.get(0));
                } else {
                    recordForAchievementAndTable = allStudentRecords.get(0);
                }
            }

            if (educationPanel != null) {
                educationPanel.updateStudentInfoDisplay(currentStudent, studentPrimaryClass);

                String studentNameForCert = currentStudent.getFullName();
                String studentClassInfoForCert = (studentPrimaryClass != null) ? studentPrimaryClass.getClassName() : "Chưa có thông tin lớp";
                String achievementText = "Chưa có dữ liệu để xếp loại.";

                if (recordForAchievementAndTable != null) {
                    try {
                        achievementText = recordForAchievementAndTable.getAchievementTitle();
                        if (achievementText != null && achievementText.toLowerCase().startsWith("học lực: ")) {
                            achievementText = achievementText.substring("học lực: ".length()).trim();
                        }
                        if (achievementText == null || achievementText.trim().isEmpty() ||
                                achievementText.equalsIgnoreCase("(Không có học lực nổi bật)") ||
                                achievementText.contains("Không đủ điều kiện xét") ||
                                achievementText.contains("Chưa xếp loại") ||
                                achievementText.contains("Chưa đủ thông tin")) {
                            achievementText = "(Chưa có thành tích nổi bật đáng ghi nhận)";
                        }
                    } catch (Exception calcEx) {
                        System.err.println("Error calculating achievement title: " + calcEx.getMessage());
                        achievementText = "(Lỗi khi tính toán thành tích)";
                    }
                } else {
                    achievementText = "(Chưa có bản ghi điểm để xét thành tích)";
                }
                educationPanel.updateAchievementCertificateDisplay(studentNameForCert, studentClassInfoForCert, achievementText);

                List<AcademicRecord> recordsForTableDisplay = (recordForAchievementAndTable != null) ?
                        List.of(recordForAchievementAndTable) :
                        Collections.emptyList();
                educationPanel.updateTableDataForStudent(currentStudent, recordsForTableDisplay);

                writeLog("Viewed Info & Results", "Student viewed their own info, grades and achievement.");
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for student " + studentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(null, "Data Load Error", "Failed to load your academic records.");
            clearCurrentData();
            if (educationPanel != null) {
                educationPanel.updateStudentInfoDisplay(null, null);
                educationPanel.updateAchievementCertificateDisplay(null, null, "Lỗi tải dữ liệu.");
                educationPanel.updateTableDataForStudent((Student) null, Collections.emptyList());
            }
        }
    }

    private AcademicRecord findOrCreateRecordForStudent(int studentId, int classId) {
        try {
            Optional<AcademicRecord> existingRecord = recordDAO.findByStudentAndClass(studentId, classId);
            return existingRecord.orElseGet(() -> {
                System.out.println("Creating new in-memory AcademicRecord for student " + studentId + " in class " + classId);
                return new AcademicRecord(studentId, classId);
            });
        } catch (DataAccessException e) {
            System.err.println("Error finding/creating record for student " + studentId + " in class " + classId + ": " + e.getMessage());
            return new AcademicRecord(studentId, classId);
        }
    }

    public void updateRecordInMemory(int rowIndex, String subjectKey, Object value) {
        if (!canCurrentUserEditGrades()) {
            System.out.println("Permission denied for grade update attempt.");
            return;
        }
        if (rowIndex < 0 || currentDisplayedRecords == null || rowIndex >= currentDisplayedRecords.size()) {
            System.err.println("Invalid row index for grade update: " + rowIndex);
            return;
        }
        AcademicRecord record = currentDisplayedRecords.get(rowIndex);
        Object oldValue = null;
        boolean changed = false;
        Object validatedValue = value;
        try {
            if ("Hạnh kiểm".equals(subjectKey)) {
                oldValue = record.getConductRating();
                if (value instanceof ConductRating) {
                    if (!Objects.equals(oldValue, value)){
                        record.setConductRating((ConductRating) value);
                        validatedValue = value;
                        changed = true;
                    }
                } else if (value == null && oldValue != null) {
                    record.setConductRating(null);
                    validatedValue = null;
                    changed = true;
                } else if (value != null) {
                    throw new IllegalArgumentException("Invalid value type for Conduct.");
                }
            } else if ("Nghệ thuật".equals(subjectKey)) {
                oldValue = record.getArtStatus();
                if (value instanceof ArtStatus) {
                    if (!Objects.equals(oldValue, value)) {
                        record.setArtStatus((ArtStatus) value);
                        validatedValue = value;
                        changed = true;
                    }
                } else if (value == null && oldValue != null) {
                    record.setArtStatus(null);
                    validatedValue = null;
                    changed = true;
                } else if (value != null) {
                    throw new IllegalArgumentException("Invalid value type for Art Status.");
                }
            } else {
                oldValue = record.getGrade(subjectKey);
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
                            throw new IllegalArgumentException("Invalid number format: '" + strVal + "'");
                        }
                    }
                } else if (value != null) {
                    throw new IllegalArgumentException("Invalid data type for grade.");
                }

                if (grade != null && (grade < 0.0 || grade > 10.0)) {
                    throw new IllegalArgumentException("Grade must be between 0.0 and 10.0.");
                }

                if (!Objects.equals(oldValue, grade)) {
                    record.setGrade(subjectKey, grade);
                    validatedValue = grade;
                    changed = true;
                }
            }

            if (changed) {
                if (educationPanel != null) {
                    educationPanel.updateCalculatedValues(rowIndex, record);
                    educationPanel.markChangesPending(true);
                }
                writeLog("Edited Grade/Conduct", "Edited data for Student ID: " + record.getStudentId() + " (Item: " + subjectKey + ", New Value: " + validatedValue +")");
            }

        } catch (IllegalArgumentException e) {
            UIUtils.showWarningMessage(educationPanel,"Validation Error", e.getMessage());
            if(educationPanel != null) {
                System.out.println("Validation error for " + subjectKey + ". Reverting cell visual to old value: " + oldValue);
                educationPanel.updateSpecificCellValue(rowIndex, subjectKey, oldValue);
            }
        }
    }

    public void saveAllChanges() {
        if (!canCurrentUserEditGrades()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to save grade changes.");
            return;
        }
        if (currentDisplayedRecords == null || currentDisplayedRecords.isEmpty() || currentSelectedClassId <= 0) {
            UIUtils.showInfoMessage(educationPanel, "No Data", "There is no grade data or class selected to save.");
            return;
        }
        if (!UIUtils.showConfirmDialog(educationPanel, "Confirm Save", "Save all grade and conduct changes for the current class?")) {
            return;
        }
        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        String currentClassName = "ID " + currentSelectedClassId;
        try {
            EduClass cls = classDAO.getById(currentSelectedClassId);
            if (cls != null) currentClassName = cls.getClassName();
        } catch (DataAccessException dae) { }

        System.out.println("Attempting to save " + currentDisplayedRecords.size() + " academic records for class: " + currentClassName);

        List<AcademicRecord> recordsToSave = new ArrayList<>(currentDisplayedRecords);

        for (AcademicRecord record : recordsToSave) {
            try {
                if (record.getStudentId() > 0 && record.getClassId() > 0) {
                    recordDAO.addOrUpdate(record);
                    successCount++;
                } else {
                    System.err.println("Skipping save for invalid record (missing studentId or classId): " + record);
                    errorCount++;
                    errorMessages.add("Skipped saving record for unknown student/class.");
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
        String logDetails = String.format("Saved %d/%d records for Class '%s' (ID %d). Errors: %d",
                successCount, recordsToSave.size(), currentClassName, currentSelectedClassId, errorCount);
        writeLog("Saved Grades", logDetails);

        if (errorCount > 0) {
            String errorDetail = String.join("\n", errorMessages.subList(0, Math.min(errorMessages.size(), 5)));
            UIUtils.showWarningMessage(educationPanel, "Save Partially Successful",
                    String.format("Successfully saved %d records.\nFailed to save %d records.\n\nFirst few errors:\n%s",
                            successCount, errorCount, errorDetail));
        } else {
            UIUtils.showInfoMessage(educationPanel, "Save Successful", "All changes saved successfully.");
            if (educationPanel != null) {
                educationPanel.markChangesPending(false);
            }
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

    public boolean canCurrentUserEditGrades() {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ADMIN) return true;

        if (currentUser.getRole() == Role.TEACHER) {
            return true;
        }
        return false;
    }

    public boolean canCurrentUserManageAssignments() {
        if (currentUser == null) return false;
        if (currentUser.getRole() == Role.ADMIN) return true;
        if (currentUser.getRole() == Role.TEACHER) {
            return true;
        }
        return false;
    }

    public Object[][] getGradeDataForExport(int classId) {
        if (currentUser.getRole() == Role.STUDENT) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "Export function not available for your role.");
            return new Object[0][0];
        }
        List<Student> studentsForExport;
        List<AcademicRecord> recordsForExport;
        String className = "Class_ID_" + classId;

        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null || selectedClass.getStudentIds() == null || selectedClass.getStudentIds().isEmpty()) {
                UIUtils.showInfoMessage(educationPanel, "Export Info", "Selected class not found or has no students.");
                return new Object[0][0];
            }
            className = selectedClass.getClassName().replaceAll("[^a-zA-Z0-9_]", "_");
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
            System.err.println("Error preparing grade export data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Export Error", "Could not load data for export: " + e.getMessage());
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
        if (educationPanel != null && currentUser != null &&
                (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.TEACHER)) {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Requesting EducationPanel to reload class tree and assignment combo box.");
                educationPanel.reloadClassTree();
            });
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
        if (this.currentDisplayedStudents != null) {
            this.currentDisplayedStudents.clear();
        }
        if (this.currentDisplayedRecords != null) {
            this.currentDisplayedRecords.clear();
        }
    }

    public void clearSelectedClass() {
        System.out.println("EducationController: Clearing selected class context for grades.");
        this.currentSelectedClassId = -1;
        clearCurrentData();
    }

    public void loadAssignmentsForClass(int classId) {
        if (currentUser.getRole() == Role.STUDENT) {
            if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
            return;
        }
        System.out.println("EducationController: Loading assignments for class ID: " + classId);
        try {
            List<Assignment> assignments = assignmentDAO.findByClassId(classId);
            if (educationPanel != null) {
                educationPanel.displayAssignments(assignments);
                EduClass cls = classDAO.getById(classId);
                String className = (cls != null) ? cls.getClassName() : "ID " + classId;
                writeLog("Viewed Assignments", "Viewed assignments for class: " + className);
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading assignments for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Load Error", "Failed to load assignments for the selected class.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList());
            }
        }
    }

    public void handleAddAssignment(EduClass selectedClass) {
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to add assignments.");
            return;
        }
        if (selectedClass == null) {
            UIUtils.showWarningMessage(educationPanel, "Error", "Cannot add assignment without a selected class.");
            return;
        }
        AssignmentDialog dialog = new AssignmentDialog(
                (Frame) SwingUtilities.getWindowAncestor(educationPanel),
                "Add New Assignment for " + selectedClass.getClassName(),
                selectedClass,
                null
        );
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            Assignment newAssignment = dialog.getAssignmentData();
            newAssignment.setEduClassId(selectedClass.getClassId());

            try {
                assignmentDAO.add(newAssignment);
                UIUtils.showInfoMessage(educationPanel, "Success", "Assignment '" + newAssignment.getTitle() + "' added successfully.");
                loadAssignmentsForClass(selectedClass.getClassId());
                writeLog("Added Assignment", "Added new assignment '" + newAssignment.getTitle() + "' for class: " + selectedClass.getClassName());
            } catch (DataAccessException | IllegalArgumentException e) {
                System.err.println("Error adding assignment: " + e.getMessage());
                UIUtils.showErrorMessage(educationPanel, "Error", "Failed to add assignment: " + e.getMessage());
            }
        } else {
            System.out.println("Add assignment cancelled by user.");
        }
    }

    public void handleEditAssignment(EduClass selectedClass, int assignmentId) {
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to edit assignments.");
            return;
        }
        if (selectedClass == null) {
            UIUtils.showWarningMessage(educationPanel, "Error", "Cannot edit assignment without a selected class context.");
            return;
        }

        try {
            Assignment assignmentToEdit = assignmentDAO.getById(assignmentId);
            if (assignmentToEdit == null) {
                UIUtils.showErrorMessage(educationPanel, "Error", "Assignment with ID " + assignmentId + " not found.");
                loadAssignmentsForClass(selectedClass.getClassId());
                return;
            }

            if (assignmentToEdit.getEduClassId() != selectedClass.getClassId()) {
                System.err.println("Mismatch: Attempt to edit assignment ID " + assignmentId + " (Class "+assignmentToEdit.getEduClassId()+") from context of class "+selectedClass.getClassId());
                UIUtils.showErrorMessage(educationPanel, "Error", "Selected assignment does not belong to the currently selected class ("+selectedClass.getClassName()+").");
                loadAssignmentsForClass(selectedClass.getClassId());
                return;
            }
            if (assignmentToEdit.isOverdue()) {
                UIUtils.showWarningMessage(educationPanel, "Bài tập đã quá hạn",
                        "Bài tập '" + assignmentToEdit.getTitle() + "' đã quá hạn nộp.\n" +
                                "Bạn không thể chỉnh sửa. Chỉ có thể xem hoặc xóa.");
                return;
            }

            AssignmentDialog dialog = new AssignmentDialog(
                    (Frame) SwingUtilities.getWindowAncestor(educationPanel),
                    "Edit Assignment '" + assignmentToEdit.getTitle() + "'",
                    selectedClass,
                    assignmentToEdit
            );
            dialog.setVisible(true);

            if (dialog.isSaved()) {
                Assignment updatedAssignment = dialog.getAssignmentData();

                try {
                    assignmentDAO.update(updatedAssignment);
                    UIUtils.showInfoMessage(educationPanel, "Success", "Assignment '" + updatedAssignment.getTitle() + "' updated successfully.");
                    loadAssignmentsForClass(selectedClass.getClassId());
                    writeLog("Edited Assignment", "Edited assignment ID: " + updatedAssignment.getAssignmentId() + ", Title: " + updatedAssignment.getTitle());
                } catch (DataAccessException | IllegalArgumentException e) {
                    System.err.println("Error updating assignment: " + e.getMessage());
                    UIUtils.showErrorMessage(educationPanel, "Error", "Failed to update assignment: " + e.getMessage());
                }
            } else {
                System.out.println("Edit assignment cancelled by user.");
            }

        } catch (DataAccessException e) {
            System.err.println("Error retrieving assignment for edit (ID: " + assignmentId + "): " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Error", "Could not load assignment data for editing.");
        }
    }

    public void handleDeleteAssignment(int assignmentId) {
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to delete assignments.");
            return;
        }

        try {
            Assignment assignmentToDelete = assignmentDAO.getById(assignmentId);
            String title = "ID " + assignmentId;
            int classId = -1;
            if (assignmentToDelete != null) {
                title = "'" + assignmentToDelete.getTitle() + "'";
                classId = assignmentToDelete.getEduClassId();
            } else {
                System.err.println("Attempting to delete assignment ID " + assignmentId + " which was not found.");
            }

            assignmentDAO.delete(assignmentId);
            UIUtils.showInfoMessage(educationPanel, "Success", "Assignment " + title + " deleted successfully.");

            if (educationPanel != null && classId > 0) {
                EduClass currentComboClass = educationPanel.getSelectedAssignmentClass();
                if (currentComboClass != null && currentComboClass.getClassId() == classId) {
                    loadAssignmentsForClass(classId);
                } else if (currentComboClass != null){
                    loadAssignmentsForClass(currentComboClass.getClassId());
                }
            }
            writeLog("Deleted Assignment", "Deleted assignment: " + title + " (ID: " + assignmentId + ")");

        } catch (DataAccessException e) {
            System.err.println("Error deleting assignment ID " + assignmentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Error", "Failed to delete assignment: " + e.getMessage());
            if (educationPanel != null) {
                EduClass currentComboClass = educationPanel.getSelectedAssignmentClass();
                if (currentComboClass != null) {
                    loadAssignmentsForClass(currentComboClass.getClassId());
                }
            }
        }
    }
    public AcademicRecord getAcademicRecordAt(int rowIndex) {
        if (currentDisplayedRecords != null && rowIndex >= 0 && rowIndex < currentDisplayedRecords.size()) {
            return currentDisplayedRecords.get(rowIndex);
        }
        System.err.println("getAcademicRecordAt: Invalid rowIndex (" + rowIndex + ") or null/empty record list.");
        return null;
    }
    public void loadAssignmentsForStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("EducationController: Loading assignments for current student ID: " + studentId);
        try {
            List<EduClass> studentClasses = classDAO.findByStudentId(studentId);
            if (studentClasses == null || studentClasses.isEmpty()) {
                System.err.println("Student " + studentId + " is not enrolled in any class.");
                if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
                return;
            }
            int studentClassId = studentClasses.get(0).getClassId();
            String studentClassName = studentClasses.get(0).getClassName();

            System.out.println("Student " + studentId + " belongs to class ID: " + studentClassId + " (" + studentClassName + ")");


            List<Assignment> assignments = assignmentDAO.findByClassId(studentClassId);

            if (educationPanel != null) {
                educationPanel.displayAssignments(assignments);
                writeLog("Viewed Assignments", "Student viewed assignments for their class: " + studentClassName);
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading assignments for student " + studentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Load Error", "Failed to load assignments for your class.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList());
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Error: Could not determine class for student " + studentId + " from DAO results.");
            UIUtils.showErrorMessage(educationPanel, "Error", "Could not determine your class to load assignments.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList());
            }
        }

    }
    public boolean isAssignmentOverdue(int assignmentId) {
        try {
            Assignment assignment = assignmentDAO.getById(assignmentId);
            if (assignment != null) {
                return assignment.isOverdue();
            }
        } catch (DataAccessException e) {
            System.err.println("Error checking if assignment " + assignmentId + " is overdue: " + e.getMessage());
        }
        return true;
    }
    public void loadAchievementsForClass(int classId) {
        System.out.println("Controller: Loading achievements for class ID: " + classId);
        if (educationPanel == null || (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.TEACHER)) {
            return;
        }

        List<Object[]> achievementDisplayData = new ArrayList<>();
        int stt = 1;

        try {
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null || selectedClass.getStudentIds() == null || selectedClass.getStudentIds().isEmpty()) {
                System.out.println("No students in class " + classId + " to load achievements for.");
                educationPanel.displayAllStudentAchievements(achievementDisplayData);
                return;
            }

            List<Student> studentsInClass = selectedClass.getStudentIds().stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName))
                    .collect(Collectors.toList());

            for (Student student : studentsInClass) {
                Optional<AcademicRecord> recordOpt = recordDAO.findByStudentAndClass(student.getStudentId(), classId);
                String achievement = "(Chưa có dữ liệu để xét)";
                if (recordOpt.isPresent()) {
                    AcademicRecord actualRecord = recordOpt.get();
                    boolean hasAnyGrade = false;
                    String[] subjectsToCheckForData = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"};
                    for (String subject : subjectsToCheckForData) {
                        if (actualRecord.getGrade(subject) != null) {
                            hasAnyGrade = true;
                            break;
                        }
                    }
                    if (hasAnyGrade) {
                        try {
                            achievement = actualRecord.getAchievementTitle();
                        } catch (Exception e) {
                            System.err.println("Error calculating achievement for student " + student.getStudentId() + " in class " + classId + ": " + e.getMessage());
                            achievement = "Lỗi tính danh hiệu";
                        }
                    } else {
                        achievement = "Chưa nhập điểm";
                    }
                }
                achievementDisplayData.add(new Object[]{
                        stt++,
                        selectedClass.getClassName(),
                        student.getFullName(),
                        achievement
                });
            }
            educationPanel.displayAllStudentAchievements(achievementDisplayData);
            writeLog("Viewed Class Achievements", "Viewed achievements for class: " + selectedClass.getClassName());

        } catch (DataAccessException e) {
            System.err.println("Error loading data for class achievements (Class ID: " + classId + "): " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Data Load Error", "Failed to load student achievement data for class.");
            educationPanel.displayAllStudentAchievements(new ArrayList<>());
        }
    }
    public void clearAllSubjectGradesForCurrentClass() {
        if (currentSelectedClassId <= 0) {
            UIUtils.showWarningMessage(educationPanel, "Lỗi", "Chưa có lớp nào được chọn.");
            return;
        }
        if (!canCurrentUserEditGrades()) {
            UIUtils.showErrorMessage(educationPanel, "Không có quyền", "Bạn không có quyền chỉnh sửa điểm cho lớp này.");
            return;
        }

        System.out.println("Controller: User requested to clear subject grades for class ID: " + currentSelectedClassId);

        List<AcademicRecord> recordsToClear = getCurrentlyLoadedRecordsForSelectedClass();

        if (recordsToClear == null || recordsToClear.isEmpty()) {
            UIUtils.showInfoMessage(educationPanel, "Không có dữ liệu", "Không có dữ liệu điểm để xóa cho lớp này.");
            return;
        }

        boolean actualChangeMade = false;
        for (AcademicRecord record : recordsToClear) {
            if (record.clearSubjectGrades()) {
                actualChangeMade = true;
            }
        }

        if (actualChangeMade) {
            List<Student> studentsOfClass = getStudentsForSelectedClass();
            if (educationPanel != null && studentsOfClass != null) {
                educationPanel.updateTableData(studentsOfClass, recordsToClear);
                educationPanel.markChangesPending(true);
                UIUtils.showInfoMessage(educationPanel, "Điểm Đã Xóa (Trong Bộ Nhớ)",
                        "Điểm các môn học đã được xóa. Nhấn 'Lưu Thay Đổi' để áp dụng vĩnh viễn.");
            }
            writeLog("Prepared Clear Grades", "Prepared to clear subject grades for Class ID: " + currentSelectedClassId);

        } else {
            UIUtils.showInfoMessage(educationPanel, "Không Thay Đổi", "Không có điểm môn học nào để xóa (có thể đã trống).");
        }
    }

    private List<AcademicRecord> getCurrentlyLoadedRecordsForSelectedClass() {
        if (currentSelectedClassId > 0 && academicRecordsForSelectedClass != null) {
            return academicRecordsForSelectedClass;
        }
        return Collections.emptyList();
    }

    private List<Student> getStudentsForSelectedClass() {
        if (currentSelectedClassId > 0 && studentDAO != null) {
            try {
                return studentDAO.getStudentsByClassId(currentSelectedClassId);
            } catch (DataAccessException e) {
                System.err.println("Error getting students for class " + currentSelectedClassId + ": " + e.getMessage());
            }
        }
        return Collections.emptyList();
    }

}