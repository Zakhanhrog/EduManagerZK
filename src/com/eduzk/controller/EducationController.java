package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
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
    private final IAssignmentDAO assignmentDAO; // DAO for assignments
    private EduClassController eduClassControllerRef; // Reference for listener
    private EducationPanel educationPanel; // Reference to the UI panel
    private List<AcademicRecord> currentDisplayedRecords; // Cache for grade records
    private List<Student> currentDisplayedStudents; // Cache for students in grade view
    private int currentSelectedClassId = -1; // ID of the class selected for grade view

    // Updated Constructor to include IAssignmentDAO
    public EducationController(
            User currentUser,
            IAcademicRecordDAO recordDAO,
            IEduClassDAO classDAO,
            IStudentDAO studentDAO,
            LogService logService,
            EduClassController eduClassController,
            IAssignmentDAO assignmentDAO) {
        if (recordDAO == null) {
            throw new IllegalArgumentException("AcademicRecordDAO parameter cannot be null");
        }
        // Null checks for essential dependencies
        if (currentUser == null) throw new IllegalArgumentException("CurrentUser cannot be null");
        if (recordDAO == null) throw new IllegalArgumentException("AcademicRecordDAO cannot be null");
        if (classDAO == null) throw new IllegalArgumentException("EduClassDAO cannot be null");
        if (studentDAO == null) throw new IllegalArgumentException("StudentDAO cannot be null");
        if (logService == null) throw new IllegalArgumentException("LogService cannot be null");
        if (assignmentDAO == null) throw new IllegalArgumentException("AssignmentDAO cannot be null"); // Check new DAO
        if (eduClassController == null) System.err.println("Warning: EduClassController is null in EducationController constructor, cannot listen for class list changes.");


        this.currentUser = currentUser;
        this.classDAO = classDAO;
        this.recordDAO = recordDAO;
        this.studentDAO = studentDAO;
        this.logService = logService;
        this.assignmentDAO = assignmentDAO; // Assign the new DAO
        this.currentDisplayedRecords = new ArrayList<>();
        this.currentDisplayedStudents = new ArrayList<>();
        this.eduClassControllerRef = eduClassController;

        // Register as a listener for class list changes if controller ref is valid
        if (this.eduClassControllerRef != null) {
            this.eduClassControllerRef.addClassListChangeListener(this);
            System.out.println("EducationController registered for ClassList changes.");
        }
    }

    // Sets the reference to the UI panel this controller manages
    public void setEducationPanel(EducationPanel educationPanel) {
        this.educationPanel = educationPanel;
    }

    // Gets the list of classes relevant to the current user (based on role)
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

    // Loads grade data for a specific class selected in the "Results & reviews" tree
    public void loadDataForClass(int classId) {
        // Students don't use this method, their data is loaded differently
        if (currentUser.getRole() == Role.STUDENT || classId <= 0) {
            clearCurrentData(); // Clear any previous data
            if (educationPanel != null) {
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
            return;
        }

        this.currentSelectedClassId = classId; // Store the selected class ID for grades
        System.out.println("EducationController: Loading grade data for class ID: " + classId);

        try {
            // Fetch the selected class details
            EduClass selectedClass = classDAO.getById(classId);
            if (selectedClass == null) {
                clearCurrentData();
                if (educationPanel != null) {
                    educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                }
                System.err.println("Class not found for grade loading: " + classId);
                UIUtils.showWarningMessage(educationPanel, "Not Found", "Selected class could not be found.");
                return;
            }

            // Get the list of student IDs enrolled in this class
            List<Integer> studentIds = selectedClass.getStudentIds();
            if (studentIds == null || studentIds.isEmpty()) {
                clearCurrentData();
                if (educationPanel != null) {
                    educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                }
                System.out.println("Class has no students enrolled: " + selectedClass.getClassName());
                // Optionally show info message: UIUtils.showInfoMessage(educationPanel, "Info", "This class has no students enrolled.");
                return;
            }

            // Fetch student details for the enrolled students
            this.currentDisplayedStudents = studentIds.stream()
                    .map(studentDAO::getById) // Fetch each student by ID
                    .filter(Objects::nonNull) // Filter out any null results (if student deleted)
                    .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER)) // Sort by name
                    .collect(Collectors.toList());

            // Fetch or create academic records for these students in this class
            this.currentDisplayedRecords = this.currentDisplayedStudents.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());

            // Update the UI panel with the loaded data
            if (educationPanel != null) {
                educationPanel.updateTableData(this.currentDisplayedStudents, this.currentDisplayedRecords);
                writeLog("Viewed Grades", "Viewed grade data for class: " + selectedClass.getClassName() + " (ID: " + classId + ")");
            }

        } catch (DataAccessException e) {
            System.err.println("Error loading grade data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Data Load Error", "Failed to load student or grade data for the selected class.");
            clearCurrentData(); // Clear cache on error
            if (educationPanel != null) {
                // Show empty table on error
                educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            }
        }
    }

    // Loads grade data specifically for the currently logged-in student
    public void loadDataForCurrentStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            clearCurrentData();
            if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("EducationController: Loading grade data for current student ID: " + studentId);
        try {
            // Get the student object for the current user
            Student currentStudent = studentDAO.getById(studentId);
            if (currentStudent == null) {
                System.err.println("Error: Student profile not found for current user ID: " + studentId);
                clearCurrentData();
                if(educationPanel != null) educationPanel.updateTableData(Collections.emptyList(), Collections.emptyList());
                UIUtils.showErrorMessage(null, "Error", "Could not find your student profile."); // Show error (parent might be null)
                return;
            }

            // Fetch all academic records for this student across all classes
            // Note: The UI currently might only display one row based on updateTableDataForStudent implementation.
            // Adjust UI or this logic if student needs to see records from multiple classes.
            this.currentDisplayedRecords = recordDAO.findAllByStudentId(studentId);
            this.currentDisplayedStudents = List.of(currentStudent); // List contains only the current student
            this.currentSelectedClassId = -1; // No specific class selected in this context

            // Update the panel specifically for the student view
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

    // Helper to find an existing AcademicRecord or create a new default one if not found
    private AcademicRecord findOrCreateRecordForStudent(int studentId, int classId) {
        try {
            Optional<AcademicRecord> existingRecord = recordDAO.findByStudentAndClass(studentId, classId);
            // If record exists, return it. Otherwise, create a new default record (unsaved).
            return existingRecord.orElseGet(() -> {
                System.out.println("Creating new in-memory AcademicRecord for student " + studentId + " in class " + classId);
                return new AcademicRecord(studentId, classId);
            });
        } catch (DataAccessException e) {
            // Log error but return a new object to avoid crashing the UI loading process
            System.err.println("Error finding/creating record for student " + studentId + " in class " + classId + ": " + e.getMessage());
            return new AcademicRecord(studentId, classId); // Return new object on error
        }
    }

    // Updates the AcademicRecord in memory when a cell in the grade table is edited
    public void updateRecordInMemory(int rowIndex, String subjectKey, Object value) {
        // Check permission before allowing update
        if (!canCurrentUserEditGrades()) {
            // Silently return or show a message if needed
            System.out.println("Permission denied for grade update attempt.");
            return;
        }
        // Validate row index
        if (rowIndex < 0 || currentDisplayedRecords == null || rowIndex >= currentDisplayedRecords.size()) {
            System.err.println("Invalid row index for grade update: " + rowIndex);
            return;
        }

        AcademicRecord record = currentDisplayedRecords.get(rowIndex);
        Object oldValue = null;
        boolean changed = false;
        Object validatedValue = value; // Store the value after potential validation/parsing

        try {
            // Handle different types of updates (Conduct, Art, Grade)
            if ("Hạnh kiểm".equals(subjectKey)) { // <--- THAY ĐỔI TỪ CONDUCT_KEY
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
                // THAY ĐỔI Ở ĐÂY: Sử dụng chuỗi trực tiếp
            } else if ("Nghệ thuật".equals(subjectKey)) { // <--- THAY ĐỔI TỪ ART_KEY
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
            } else { // Handle numeric grades
                oldValue = record.getGrade(subjectKey);
                Double grade = null;
                if (value instanceof Number) {
                    grade = ((Number) value).doubleValue();
                } else if (value instanceof String) { // Try parsing from String input
                    String strVal = ((String) value).trim();
                    if (strVal.isEmpty()) {
                        grade = null; // Treat empty string as null grade
                    } else {
                        try {
                            grade = Double.parseDouble(strVal);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid number format: '" + strVal + "'");
                        }
                    }
                } else if (value != null) { // Reject other non-null types
                    throw new IllegalArgumentException("Invalid data type for grade.");
                }

                // Validate grade range (0-10)
                if (grade != null && (grade < 0.0 || grade > 10.0)) {
                    throw new IllegalArgumentException("Grade must be between 0.0 and 10.0.");
                }

                // Check if the value actually changed
                if (!Objects.equals(oldValue, grade)) {
                    record.setGrade(subjectKey, grade);
                    validatedValue = grade;
                    changed = true;
                }
            }

            // If a change occurred, update UI and mark pending changes
            if (changed) {
                if (educationPanel != null) {
                    // Update the specific cell visually (optional, table might update itself)
                    // educationPanel.updateSpecificCellValue(rowIndex, subjectKey, validatedValue);

                    // Recalculate and update average columns in the table
                    educationPanel.updateCalculatedValues(rowIndex, record);
                    // Mark that there are unsaved changes
                    educationPanel.markChangesPending(true);
                }
                // Log the edit action (consider logging details carefully)
                writeLog("Edited Grade/Conduct", "Edited data for Student ID: " + record.getStudentId() + " (Item: " + subjectKey + ", New Value: " + validatedValue +")");
            }

        } catch (IllegalArgumentException e) {
            // Handle validation errors (e.g., invalid number, out of range)
            UIUtils.showWarningMessage(educationPanel,"Validation Error", e.getMessage());
            // Revert visual representation in the table cell if validation fails
            if(educationPanel != null) {
                System.out.println("Validation error for " + subjectKey + ". Reverting cell visual to old value: " + oldValue);
                // Update cell back to old value visually
                educationPanel.updateSpecificCellValue(rowIndex, subjectKey, oldValue);
                // Optional: Refresh the cell rendering
                // educationPanel.refreshTableCell(rowIndex, subjectKey);
            }
        }
    }

    // Saves all pending changes made in the grade editor
    public void saveAllChanges() {
        // Check permissions first
        if (!canCurrentUserEditGrades()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to save grade changes.");
            return;
        }
        // Check if there's data to save
        if (currentDisplayedRecords == null || currentDisplayedRecords.isEmpty() || currentSelectedClassId <= 0) {
            UIUtils.showInfoMessage(educationPanel, "No Data", "There is no grade data or class selected to save.");
            return;
        }
        // Confirm before saving
        if (!UIUtils.showConfirmDialog(educationPanel, "Confirm Save", "Save all grade and conduct changes for the current class?")) {
            return; // User cancelled
        }

        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();
        String currentClassName = "ID " + currentSelectedClassId; // Fallback name
        try {
            EduClass cls = classDAO.getById(currentSelectedClassId);
            if (cls != null) currentClassName = cls.getClassName();
        } catch (DataAccessException dae) { /* Ignore, use ID */ }


        System.out.println("Attempting to save " + currentDisplayedRecords.size() + " academic records for class: " + currentClassName);

        // Create a copy to avoid ConcurrentModificationException if underlying list changes during iteration
        List<AcademicRecord> recordsToSave = new ArrayList<>(currentDisplayedRecords);

        for (AcademicRecord record : recordsToSave) {
            try {
                // Basic validation before attempting save
                if (record.getStudentId() > 0 && record.getClassId() > 0) {
                    // Use addOrUpdate which handles both insert and update scenarios
                    recordDAO.addOrUpdate(record);
                    successCount++;
                } else {
                    // This shouldn't happen if data loading/creation is correct
                    System.err.println("Skipping save for invalid record (missing studentId or classId): " + record);
                    errorCount++; // Count as error
                    errorMessages.add("Skipped saving record for unknown student/class.");
                }
            } catch (DataAccessException | IllegalArgumentException e) {
                // Catch DAO errors or validation errors during save
                errorCount++;
                String studentName = getStudentNameById(record.getStudentId()); // Get student name for error message
                String msg = "Failed to save record for " + studentName + " (ID: " + record.getStudentId() + "): " + e.getMessage();
                System.err.println(msg);
                errorMessages.add(msg);
            } catch (Exception e) {
                // Catch unexpected runtime errors
                errorCount++;
                String studentName = getStudentNameById(record.getStudentId());
                String msg = "Unexpected error saving record for " + studentName + " (ID: " + record.getStudentId() + "): " + e.getMessage();
                System.err.println(msg);
                errorMessages.add(msg);
                e.printStackTrace(); // Print stack trace for debugging
            }
        }

        System.out.println("Save operation complete. Success: " + successCount + ", Errors: " + errorCount);

        // Log the overall result
        String logDetails = String.format("Saved %d/%d records for Class '%s' (ID %d). Errors: %d",
                successCount, recordsToSave.size(), currentClassName, currentSelectedClassId, errorCount);
        writeLog("Saved Grades", logDetails);

        // Provide feedback to the user
        if (errorCount > 0) {
            String errorDetail = String.join("\n", errorMessages.subList(0, Math.min(errorMessages.size(), 5))); // Show first few errors
            UIUtils.showWarningMessage(educationPanel, "Save Partially Successful",
                    String.format("Successfully saved %d records.\nFailed to save %d records.\n\nFirst few errors:\n%s",
                            successCount, errorCount, errorDetail));
            // Do NOT reset pending changes if there were errors, user might need to retry/fix
        } else {
            UIUtils.showInfoMessage(educationPanel, "Save Successful", "All changes saved successfully.");
            // Reset pending changes flag only on full success
            if (educationPanel != null) {
                educationPanel.markChangesPending(false);
                // Optionally turn off editing mode on successful save
                // educationPanel.setEditingMode(false); // Requires public setEditingMode or calling via markChangesPending
            }
        }
    }

    // Helper to get student name by ID, used for logging/error messages
    public String getStudentNameById(int studentId) {
        // Check cache first
        if (currentDisplayedStudents != null) {
            for (Student s : currentDisplayedStudents) {
                if (s.getStudentId() == studentId) {
                    return s.getFullName();
                }
            }
        }
        // If not in cache, query DAO
        try {
            Student student = studentDAO.getById(studentId);
            return (student != null) ? student.getFullName() : "Student [" + studentId + "]";
        } catch (DataAccessException e) {
            System.err.println("Error getting student name for ID " + studentId + ": " + e.getMessage());
            return "Error [" + studentId + "]"; // Return error indicator
        }
    }

    // Checks if the current user has permission to edit grades for the selected class
    public boolean canCurrentUserEditGrades() {
        if (currentUser == null) return false;
        // Admin can always edit
        if (currentUser.getRole() == Role.ADMIN) return true;

        // Teacher can edit if it's their class
        if (currentUser.getRole() == Role.TEACHER) {
            return true;
        }
        // Students and other roles cannot edit grades
        return false;
    }


    // Checks if the current user has permission to manage assignments for the selected class
    public boolean canCurrentUserManageAssignments() {
        if (currentUser == null) return false;
        // Admin can always manage
        if (currentUser.getRole() == Role.ADMIN) return true;
        // Teacher can manage if it's their class (check based on assignment combo box)
        if (currentUser.getRole() == Role.TEACHER) {
            return true;
        }
        // Students and other roles cannot manage assignments
        return false;
    }


    // Prepares grade data in a 2D array format suitable for Excel export
    public Object[][] getGradeDataForExport(int classId) {
        // Students cannot export this data
        if (currentUser.getRole() == Role.STUDENT) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "Export function not available for your role.");
            return new Object[0][0]; // Return empty array
        }

        List<Student> studentsForExport;
        List<AcademicRecord> recordsForExport;
        String className = "Class_ID_" + classId; // Default filename part

        try {
            // Fetch class and student data needed for the export
            EduClass selectedClass = classDAO.getById(classId);
            // Basic validation
            if (selectedClass == null || selectedClass.getStudentIds() == null || selectedClass.getStudentIds().isEmpty()) {
                UIUtils.showInfoMessage(educationPanel, "Export Info", "Selected class not found or has no students.");
                return new Object[0][0];
            }
            className = selectedClass.getClassName().replaceAll("[^a-zA-Z0-9_]", "_"); // Sanitize class name for filename
            List<Integer> studentIds = selectedClass.getStudentIds();

            // Fetch and sort students
            studentsForExport = studentIds.stream()
                    .map(studentDAO::getById)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            // Fetch or create records (should exist if loaded previously)
            recordsForExport = studentsForExport.stream()
                    .map(student -> findOrCreateRecordForStudent(student.getStudentId(), classId))
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            System.err.println("Error preparing grade export data for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Export Error", "Could not load data for export: " + e.getMessage());
            return new Object[0][0];
        }

        // Define headers matching the table structure
        String[] columnHeaders = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật", "TB KHTN", "TB KHXH", "TB môn học", "Hạnh kiểm"};
        Object[][] exportData = new Object[studentsForExport.size()][columnHeaders.length];

        // Populate the 2D array
        for (int i = 0; i < studentsForExport.size(); i++) {
            Student student = studentsForExport.get(i);
            AcademicRecord record = recordsForExport.get(i);

            exportData[i][0] = i + 1; // STT (1-based index)
            exportData[i][1] = student.getFullName();
            exportData[i][2] = record.getGrade("Toán"); // Use Double or null
            exportData[i][3] = record.getGrade("Văn");
            exportData[i][4] = record.getGrade("Anh");
            exportData[i][5] = record.getGrade("Lí");
            exportData[i][6] = record.getGrade("Hoá");
            exportData[i][7] = record.getGrade("Sinh");
            exportData[i][8] = record.getGrade("Sử");
            exportData[i][9] = record.getGrade("Địa");
            exportData[i][10] = record.getGrade("GDCD");
            // Convert enums to string for export, handle nulls
            exportData[i][11] = (record.getArtStatus() != null) ? record.getArtStatus().toString() : "";
            // Calculated averages (already Doubles)
            exportData[i][12] = record.calculateAvgNaturalSciences();
            exportData[i][13] = record.calculateAvgSocialSciences();
            exportData[i][14] = record.calculateAvgOverallSubjects();
            // Convert enum to string, handle nulls
            exportData[i][15] = (record.getConductRating() != null) ? record.getConductRating().toString() : "";
        }

        // Log the export action
        writeLog("Exported Grades", "Prepared grade data for export for class: " + className + " (ID: " + classId + ")");
        return exportData;
    }

    // Returns the ID of the class currently selected in the grade view context
    public int getCurrentSelectedClassId() {
        return currentSelectedClassId;
    }

    // Helper method to write log entries consistently
    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(), // Or getUsername()
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
            } catch (Exception e) {
                // Log failure to log, but don't crash the application
                System.err.println("!!! Failed to write log entry: Action=" + action + ", Details=" + details + " - Error: " + e.getMessage());
            }
        } else {
            // This indicates a potential initialization issue
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

    // Implementation of ClassListChangeListener interface method
    @Override
    public void classListChanged() {
        System.out.println("EducationController received classListChanged notification.");
        // Reload the class tree in the EducationPanel if it's visible and user is Admin/Teacher
        if (educationPanel != null && currentUser != null &&
                (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.TEACHER)) {
            // Ensure UI updates happen on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                System.out.println("Requesting EducationPanel to reload class tree and assignment combo box.");
                educationPanel.reloadClassTree(); // This method now also updates the combo box
            });
        }
    }

    // Cleanup method, e.g., to unregister listeners when the view/controller is disposed
    public void cleanup() {
        // Unregister from class list changes
        if (this.eduClassControllerRef != null) {
            this.eduClassControllerRef.removeClassListChangeListener(this);
            System.out.println("EducationController unregistered from ClassList changes.");
        }
        // Add any other cleanup logic here if needed
    }

    // Clears the cached data for grades
    private void clearCurrentData() {
        this.currentSelectedClassId = -1;
        if (this.currentDisplayedStudents != null) {
            this.currentDisplayedStudents.clear();
        }
        if (this.currentDisplayedRecords != null) {
            this.currentDisplayedRecords.clear();
        }
    }

    // Specifically clears the context related to grade viewing
    public void clearSelectedClass() {
        System.out.println("EducationController: Clearing selected class context for grades.");
        this.currentSelectedClassId = -1; // Reset grade context class ID
        // Optionally clear cached student/record data related to grades
        clearCurrentData();
    }


    // --- New Methods for Assignment Management ---

    /**
     * Loads assignments for a specific class ID and tells the panel to display them.
     * @param classId The ID of the class whose assignments are to be loaded.
     */
    public void loadAssignmentsForClass(int classId) {
        // Basic validation or role check if needed (e.g., students might not see this)
        if (currentUser.getRole() == Role.STUDENT) {
            if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
            return;
        }
        System.out.println("EducationController: Loading assignments for class ID: " + classId);
        try {
            // Fetch assignments from the DAO
            List<Assignment> assignments = assignmentDAO.findByClassId(classId);
            // Update the UI panel
            if (educationPanel != null) {
                educationPanel.displayAssignments(assignments);
                // Log viewing action
                EduClass cls = classDAO.getById(classId);
                String className = (cls != null) ? cls.getClassName() : "ID " + classId;
                writeLog("Viewed Assignments", "Viewed assignments for class: " + className);
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading assignments for class " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Load Error", "Failed to load assignments for the selected class.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList()); // Show empty list on error
            }
        }
    }

    /**
     * Handles the request to add a new assignment for a given class.
     * Opens the AssignmentDialog.
     * @param selectedClass The EduClass object for which the assignment is being added.
     */
    public void handleAddAssignment(EduClass selectedClass) {
        // Check permission
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to add assignments.");
            return;
        }
        // Ensure a class context is provided
        if (selectedClass == null) {
            UIUtils.showWarningMessage(educationPanel, "Error", "Cannot add assignment without a selected class.");
            return;
        }

        // Create and show the dialog
        AssignmentDialog dialog = new AssignmentDialog(
                (Frame) SwingUtilities.getWindowAncestor(educationPanel), // Get parent frame
                "Add New Assignment for " + selectedClass.getClassName(),
                selectedClass, // Pass class info to dialog
                null // Pass null because it's a new assignment
        );
        dialog.setVisible(true); // Show modally

        // Process result if dialog was saved
        if (dialog.isSaved()) {
            Assignment newAssignment = dialog.getAssignmentData();
            newAssignment.setEduClassId(selectedClass.getClassId()); // Ensure class ID is set

            try {
                // Add the assignment via DAO
                assignmentDAO.add(newAssignment);
                UIUtils.showInfoMessage(educationPanel, "Success", "Assignment '" + newAssignment.getTitle() + "' added successfully.");
                // Refresh the assignment list in the UI for the current class
                loadAssignmentsForClass(selectedClass.getClassId());
                // Log the action
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
        // Check permission
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to edit assignments.");
            return;
        }
        // Ensure class context
        if (selectedClass == null) {
            UIUtils.showWarningMessage(educationPanel, "Error", "Cannot edit assignment without a selected class context.");
            return;
        }

        try {
            // Fetch the assignment to edit
            Assignment assignmentToEdit = assignmentDAO.getById(assignmentId);
            if (assignmentToEdit == null) {
                UIUtils.showErrorMessage(educationPanel, "Error", "Assignment with ID " + assignmentId + " not found.");
                // Refresh list in case it was deleted elsewhere
                loadAssignmentsForClass(selectedClass.getClassId());
                return;
            }

            // Security/Consistency check: Ensure the assignment belongs to the selected class
            if (assignmentToEdit.getEduClassId() != selectedClass.getClassId()) {
                System.err.println("Mismatch: Attempt to edit assignment ID " + assignmentId + " (Class "+assignmentToEdit.getEduClassId()+") from context of class "+selectedClass.getClassId());
                UIUtils.showErrorMessage(educationPanel, "Error", "Selected assignment does not belong to the currently selected class ("+selectedClass.getClassName()+").");
                // Refresh list for the *selected* class
                loadAssignmentsForClass(selectedClass.getClassId());
                return;
            }

            // Create and show the dialog for editing
            AssignmentDialog dialog = new AssignmentDialog(
                    (Frame) SwingUtilities.getWindowAncestor(educationPanel),
                    "Edit Assignment '" + assignmentToEdit.getTitle() + "'",
                    selectedClass, // Pass class info
                    assignmentToEdit // Pass the assignment to edit
            );
            dialog.setVisible(true);

            // Process result if dialog was saved
            if (dialog.isSaved()) {
                Assignment updatedAssignment = dialog.getAssignmentData();
                // Note: ID and ClassID should remain the same from the dialog's perspective here

                try {
                    // Update the assignment via DAO
                    assignmentDAO.update(updatedAssignment);
                    UIUtils.showInfoMessage(educationPanel, "Success", "Assignment '" + updatedAssignment.getTitle() + "' updated successfully.");
                    // Refresh the assignment list in the UI
                    loadAssignmentsForClass(selectedClass.getClassId());
                    // Log the action
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

    /**
     * Handles the request to delete an assignment.
     * @param assignmentId The ID of the assignment to delete.
     */
    public void handleDeleteAssignment(int assignmentId) {
        // Check permission
        if (!canCurrentUserManageAssignments()) {
            UIUtils.showErrorMessage(educationPanel, "Permission Denied", "You do not have permission to delete assignments.");
            return;
        }

        try {
            // Get assignment details before deleting (for logging/confirmation)
            Assignment assignmentToDelete = assignmentDAO.getById(assignmentId);
            String title = "ID " + assignmentId; // Fallback title
            int classId = -1;
            if (assignmentToDelete != null) {
                title = "'" + assignmentToDelete.getTitle() + "'";
                classId = assignmentToDelete.getEduClassId();
            } else {
                // Assignment might already be deleted? Show warning maybe?
                System.err.println("Attempting to delete assignment ID " + assignmentId + " which was not found.");
                // Optionally show error and stop:
                // UIUtils.showErrorMessage(educationPanel, "Not Found", "Assignment with ID " + assignmentId + " not found.");
                // return;
            }


            // No need for confirmation here as it's done in the Panel's action listener

            // Delete the assignment via DAO
            assignmentDAO.delete(assignmentId);
            UIUtils.showInfoMessage(educationPanel, "Success", "Assignment " + title + " deleted successfully.");

            // Refresh the assignment list for the class that was being viewed
            if (educationPanel != null && classId > 0) {
                EduClass currentComboClass = educationPanel.getSelectedAssignmentClass();
                if (currentComboClass != null && currentComboClass.getClassId() == classId) {
                    loadAssignmentsForClass(classId);
                } else if (currentComboClass != null){
                    // If the deleted assignment was from a different class than selected,
                    // still refresh the *selected* class's list just in case.
                    loadAssignmentsForClass(currentComboClass.getClassId());
                }
            }
            // Log the action
            writeLog("Deleted Assignment", "Deleted assignment: " + title + " (ID: " + assignmentId + ")");

        } catch (DataAccessException e) {
            System.err.println("Error deleting assignment ID " + assignmentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Error", "Failed to delete assignment: " + e.getMessage());
            // Optional: Refresh list even on error to ensure UI consistency
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
        return null; // Trả về null nếu không hợp lệ
    }
    public void loadAssignmentsForStudent() {
        if (currentUser == null || currentUser.getRole() != Role.STUDENT || currentUser.getStudentId() == null) {
            if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
            return;
        }

        int studentId = currentUser.getStudentId();
        System.out.println("EducationController: Loading assignments for current student ID: " + studentId);
        try {
            // Cách 2: Tìm lớp học chứa học sinh này (phổ biến hơn)
            List<EduClass> studentClasses = classDAO.findByStudentId(studentId); // Giả sử có phương thức này trong IEduClassDAO
            if (studentClasses == null || studentClasses.isEmpty()) {
                System.err.println("Student " + studentId + " is not enrolled in any class.");
                if (educationPanel != null) educationPanel.displayAssignments(Collections.emptyList());
                return;
            }
            // Lấy ID của lớp đầu tiên tìm thấy (giả định học sinh chỉ thuộc 1 lớp trong context này)
            int studentClassId = studentClasses.get(0).getClassId();
            String studentClassName = studentClasses.get(0).getClassName(); // Lấy tên lớp để log

            System.out.println("Student " + studentId + " belongs to class ID: " + studentClassId + " (" + studentClassName + ")");
            // --- Kết thúc lấy Class ID ---


            // Fetch assignments từ DAO cho lớp đó
            List<Assignment> assignments = assignmentDAO.findByClassId(studentClassId);

            // Update the UI panel
            if (educationPanel != null) {
                educationPanel.displayAssignments(assignments);
                writeLog("Viewed Assignments", "Student viewed assignments for their class: " + studentClassName);
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading assignments for student " + studentId + ": " + e.getMessage());
            UIUtils.showErrorMessage(educationPanel, "Load Error", "Failed to load assignments for your class.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList()); // Show empty list on error
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Error: Could not determine class for student " + studentId + " from DAO results.");
            UIUtils.showErrorMessage(educationPanel, "Error", "Could not determine your class to load assignments.");
            if (educationPanel != null) {
                educationPanel.displayAssignments(Collections.emptyList());
            }
        }
    }
}