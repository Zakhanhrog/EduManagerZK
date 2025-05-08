package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.entities.Role;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.ClassPanel;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.model.dao.interfaces.ClassListChangeListener;
import javax.swing.event.EventListenerList;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class EduClassController {

    private final IEduClassDAO eduClassDAO;
    private final ICourseDAO courseDAO;
    private final ITeacherDAO teacherDAO;
    private final IStudentDAO studentDAO;

    private ClassPanel classPanel;

    private final LogService logService;
    private final User currentUser;

    private final EventListenerList listenerList = new EventListenerList();


    public EduClassController(
            IEduClassDAO eduClassDAO,
            ICourseDAO courseDAO,
            ITeacherDAO teacherDAO,
            IStudentDAO studentDAO,
            User currentUser,
            LogService logService) {
        this.eduClassDAO = eduClassDAO;
        this.courseDAO = courseDAO;
        this.teacherDAO = teacherDAO;
        this.studentDAO = studentDAO;
        this.currentUser = currentUser;
        this.logService = logService;
    }

    public void setClassPanel(ClassPanel classPanel) {
        this.classPanel = classPanel;
    }

    public List<EduClass> getAllEduClasses() {
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser);
                System.out.println("EduClassController.getAllEduClasses: Filtering for Teacher ID: " + teacherId);
                if (teacherId > 0) {
                    return eduClassDAO.findByTeacherId(teacherId);
                } else {
                    System.err.println("EduClassController: Could not determine Teacher ID for logged in user. Returning empty class list.");
                    return Collections.emptyList();
                }
            } else {
                System.out.println("EduClassController: Getting all classes for Admin/Other.");
                return eduClassDAO.getAll();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading classes: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to load class data.");
            return Collections.emptyList();
        }
    }

    private int getTeacherIdForUser(User user) {
        if (user != null && user.getRole() == Role.TEACHER && user.getTeacherId() != null) {
            System.out.println("EduClassController.getTeacherIdForUser: Found Teacher ID = " + user.getTeacherId());
            return user.getTeacherId();
        }
        System.err.println("EduClassController.getTeacherIdForUser: Could not get valid Teacher ID for user: " + (user != null ? user.getUsername() : "null"));
        return -1;
    }

    public List<Course> getAllCoursesForSelection() {
        try {
            return courseDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading courses for selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Teacher> getAllTeachersForSelection() {
        try {
            List<Teacher> allTeachers = teacherDAO.getAll();
            if (allTeachers != null) {
                return allTeachers.stream()
                        .filter(Teacher::isActive)
                        .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading active teachers for selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Student> getEnrolledStudents(int classId) {
        if (classId <= 0) return Collections.emptyList();
        try {
            EduClass eduClass = eduClassDAO.getById(classId);
            if (eduClass != null) {
                List<Integer> studentIds = eduClass.getStudentIds();
                if (studentIds == null || studentIds.isEmpty()) return Collections.emptyList();
                return studentIds.stream()
                        .map(studentDAO::getById)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (DataAccessException e) {
            System.err.println("Error loading enrolled students for class ID " + classId + ": " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to load enrolled students.");
            return Collections.emptyList();
        }
    }

    public List<Student> getAvailableStudentsForEnrollment(int classId) {
        try {
            List<Student> allStudents = studentDAO.getAll();
            if (allStudents == null) return Collections.emptyList();

            if (classId <= 0) return allStudents;

            EduClass currentClass = eduClassDAO.getById(classId);
            if (currentClass != null) {
                List<Integer> enrolledIds = currentClass.getStudentIds();
                if (enrolledIds == null || enrolledIds.isEmpty()) return allStudents;

                Set<Integer> enrolledIdSet = new HashSet<>(enrolledIds);
                return allStudents.stream()
                        .filter(s -> !enrolledIdSet.contains(s.getStudentId()))
                        .collect(Collectors.toList());
            }
            return allStudents;
        } catch (DataAccessException e) {
            System.err.println("Error loading available students: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean addEduClass(EduClass eduClass) {
        if (eduClass == null || !ValidationUtils.isNotEmpty(eduClass.getClassName()) ||
                eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null || eduClass.getMaxCapacity() <= 0) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Class name, course, teacher, and positive capacity are required.");
            return false;
        }
        Teacher selectedTeacher = eduClass.getPrimaryTeacher();
        if (!selectedTeacher.isActive()) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Cannot assign inactive teacher '" + selectedTeacher.getFullName() + "' to the class.");
            return false;
        }

        try {
            eduClassDAO.add(eduClass);
            writeAddLog("Added Class", eduClass);
            fireClassListChanged();
            if (classPanel != null) {
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Class added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to add class: " + e.getMessage());
            return false;
        }
    }

    public boolean updateEduClass(EduClass eduClass) {
        if (eduClass == null || eduClass.getClassId() <= 0 || !ValidationUtils.isNotEmpty(eduClass.getClassName()) ||
                eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null || eduClass.getMaxCapacity() <= 0) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Invalid class data for update.");
            return false;
        }
        Teacher selectedTeacher = eduClass.getPrimaryTeacher();
        if (!selectedTeacher.isActive()) {
            UIUtils.showWarningMessage(classPanel, "Validation Error", "Cannot assign inactive teacher '" + selectedTeacher.getFullName() + "' to the class.");
            return false;
        }

        try {
            EduClass existingClass = eduClassDAO.getById(eduClass.getClassId());
            if (existingClass != null && existingClass.getCurrentEnrollment() > eduClass.getMaxCapacity()) {
                UIUtils.showErrorMessage(classPanel, "Error", "Cannot update class. New maximum capacity ("
                        + eduClass.getMaxCapacity() + ") is less than current enrollment ("
                        + existingClass.getCurrentEnrollment() + ").");
                return false;
            }

            eduClassDAO.update(eduClass);
            writeUpdateLog("Updated Class", eduClass);
            fireClassListChanged();
            if (classPanel != null) {
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Class updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to update class: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEduClass(int classId) {
        if (classId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class ID for deletion.");
            return false;
        }

        EduClass classToDelete = null;
        String classInfoForLog = "ID: " + classId;

        try {
            classToDelete = eduClassDAO.getById(classId);
            if (classToDelete != null) {
                classInfoForLog = "ID: " + classId + ", Name: " + classToDelete.getClassName();
                if (classToDelete.getCurrentEnrollment() > 0) {
                    UIUtils.showErrorMessage(classPanel, "Deletion Failed", "Cannot delete class '" + classToDelete.getClassName() + "'. There are still students enrolled.");
                    return false;
                }
            } else {
                UIUtils.showWarningMessage(classPanel, "Not Found", "Class with ID " + classId + " not found for deletion.");
                return false;
            }


            eduClassDAO.delete(classId);
            writeDeleteLog("Deleted Class", classInfoForLog);
            fireClassListChanged();
            if (classPanel != null) {
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Class deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error deleting class: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Error", "Failed to delete class: " + e.getMessage());
            return false;
        }
    }

    public boolean enrollStudent(int classId, int studentId) {
        if (classId <= 0 || studentId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student ID for enrollment.");
            return false;
        }
        try {
            eduClassDAO.addStudentToClass(classId, studentId);
            writeEnrollmentLog("Enrolled Student", classId, List.of(studentId));
            if(classPanel != null) {
                classPanel.refreshStudentListForSelectedClass();
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Student enrolled successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error enrolling student: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Enrollment Failed", "Failed to enroll student: " + e.getMessage());
            return false;
        }
    }

    public boolean unenrollStudent(int classId, int studentId) {
        if (classId <= 0 || studentId <= 0) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student ID for unenrolling.");
            return false;
        }
        try {
            eduClassDAO.removeStudentFromClass(classId, studentId);
            writeEnrollmentLog("Unenrolled Student", classId, List.of(studentId));
            if(classPanel != null) {
                classPanel.refreshStudentListForSelectedClass();
                classPanel.refreshTable();
                UIUtils.showInfoMessage(classPanel, "Success", "Student unenrolled successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error unenrolling student: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Unenrollment Failed", "Failed to unenroll student: " + e.getMessage());
            return false;
        }
    }

    public EduClass getEduClassById(int classId) {
        if (classId <= 0) return null;
        try {
            return eduClassDAO.getById(classId);
        } catch (DataAccessException e) {
            System.err.println("Error getting class by ID: " + e.getMessage());
            return null;
        }
    }

    public boolean enrollStudents(int classId, List<Integer> studentIds) {
        if (classId <= 0 || studentIds == null || studentIds.isEmpty()) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student IDs for enrollment.");
            return false;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            EduClass eduClass = eduClassDAO.getById(classId);
            if (eduClass == null) {
                throw new DataAccessException("EduClass with ID " + classId + " not found.");
            }

            int currentEnrollment = eduClass.getCurrentEnrollment();
            int maxCapacity = eduClass.getMaxCapacity();
            int availableSpots = maxCapacity - currentEnrollment;

            if (studentIds.size() > availableSpots) {
                UIUtils.showWarningMessage(classPanel, "Capacity Exceeded",
                        "Cannot enroll " + studentIds.size() + " students. Only " +
                                availableSpots + " spot(s) remaining in class '" + eduClass.getClassName() + "'.");
                return false;
            }

            try {
                int addedCount = eduClassDAO.addStudentsToClass(classId, studentIds);
                successCount = addedCount;
                if (addedCount != studentIds.size()) {
                    errors.add("Some students might not have been enrolled (e.g., already exist or class full).");
                }
            } catch (DataAccessException e) {
                errors.add("Failed to enroll students: " + e.getMessage());
                System.err.println("Error enrolling multiple students: " + e.getMessage());
            }
            if (successCount > 0) {
                writeEnrollmentLog("Enrolled Multiple Students", classId, studentIds.subList(0, successCount));
            }
            if(classPanel != null) {
                classPanel.refreshStudentListForSelectedClass();
                classPanel.refreshTable();
            }
            if (errors.isEmpty()) {
                UIUtils.showInfoMessage(classPanel, "Success", successCount + " student(s) enrolled successfully.");
            } else {
                String errorMsg = successCount + " student(s) enrolled. \nErrors encountered:\n" + String.join("\n", errors);
                UIUtils.showWarningMessage(classPanel, "Partial Success/Errors", errorMsg);
            }
            return successCount > 0;

        } catch (DataAccessException e) {
            System.err.println("Error preparing enrollment: " + e.getMessage());
            UIUtils.showErrorMessage(classPanel, "Enrollment Failed", "Failed to prepare enrollment: " + e.getMessage());
            return false;
        }
    }

    public boolean unenrollStudents(int classId, List<Integer> studentIds) {
        if (classId <= 0 || studentIds == null || studentIds.isEmpty()) {
            UIUtils.showWarningMessage(classPanel, "Error", "Invalid class or student IDs for unenrolling.");
            return false;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();
        boolean performedUnenroll = false;
        try {
            int removedCount = eduClassDAO.removeStudentsFromClass(classId, studentIds);
            successCount = removedCount;
            performedUnenroll = true;
            if (removedCount != studentIds.size()) {
                errors.add("Some students might not have been unenrolled (e.g., not found in class).");
            }
        } catch (DataAccessException e) {
            errors.add("Failed to unenroll students: " + e.getMessage());
            System.err.println("Error unenrolling multiple students: " + e.getMessage());
        }
        if(performedUnenroll) {
            writeEnrollmentLog("Unenrolled Multiple Students", classId, studentIds);
        }
        if(classPanel != null) {
            classPanel.refreshStudentListForSelectedClass();
            classPanel.refreshTable();
        }
        if (errors.isEmpty()) {
            UIUtils.showInfoMessage(classPanel, "Success", successCount + " student(s) unenrolled successfully.");
        } else {
            String errorMsg = successCount + " student(s) unenrolled. \nErrors encountered:\n" + String.join("\n", errors);
            UIUtils.showWarningMessage(classPanel, "Partial Success/Errors", errorMsg);
        }
        return successCount > 0;
    }

    private void writeAddLog(String action, EduClass eduClass) {
        String details = String.format("ID: %d, Name: %s, CourseID: %d, TeacherID: %d, Cap: %d",
                eduClass.getClassId(), eduClass.getClassName(),
                eduClass.getCourse() != null ? eduClass.getCourse().getCourseId() : -1,
                eduClass.getPrimaryTeacher() != null ? eduClass.getPrimaryTeacher().getTeacherId() : -1,
                eduClass.getMaxCapacity());
        writeLog(action, details);
    }

    private void writeUpdateLog(String action, EduClass eduClass) {
        String details = String.format("ID: %d, Name: %s, CourseID: %d, TeacherID: %d, Cap: %d",
                eduClass.getClassId(), eduClass.getClassName(),
                eduClass.getCourse() != null ? eduClass.getCourse().getCourseId() : -1,
                eduClass.getPrimaryTeacher() != null ? eduClass.getPrimaryTeacher().getTeacherId() : -1,
                eduClass.getMaxCapacity());
        writeLog(action, details);
    }

    private void writeDeleteLog(String action, String details) {
        writeLog(action, details);
    }

    private void writeEnrollmentLog(String action, int classId, List<Integer> studentIds) {
        String details = String.format("ClassID: %d, StudentIDs: %s",
                classId,
                studentIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
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

    public void addClassListChangeListener(ClassListChangeListener listener) {
        listenerList.add(ClassListChangeListener.class, listener);
        System.out.println("EduClassController: Listener added: " + listener.getClass().getName());
    }

    public void removeClassListChangeListener(ClassListChangeListener listener) {
        listenerList.remove(ClassListChangeListener.class, listener);
        System.out.println("EduClassController: Listener removed: " + listener.getClass().getName());
    }

    protected void fireClassListChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ClassListChangeListener.class) {
                System.out.println("EduClassController: Notifying ClassListChangeListener.");
                ((ClassListChangeListener) listeners[i + 1]).classListChanged();
            }
        }
    }
}