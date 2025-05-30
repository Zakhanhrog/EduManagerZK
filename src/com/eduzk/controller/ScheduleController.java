package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.*;
import com.eduzk.model.entities.*;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.exceptions.ScheduleConflictException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.MainView;
import com.eduzk.view.panels.SchedulePanel;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.DateUtils;
import java.time.LocalDateTime;

public class ScheduleController {

    private final IScheduleDAO scheduleDAO;
    private final IEduClassDAO eduClassDAO;
    private final ITeacherDAO teacherDAO;
    private final IRoomDAO roomDAO;
    private final LogService logService;
    private SchedulePanel schedulePanel;
    private final User currentUser;
    private MainView mainView;

    public ScheduleController(IScheduleDAO scheduleDAO, IEduClassDAO eduClassDAO, ITeacherDAO teacherDAO, IRoomDAO roomDAO, User currentUser, LogService logService) {
        this.scheduleDAO = scheduleDAO;
        this.eduClassDAO = eduClassDAO;
        this.teacherDAO = teacherDAO;
        this.roomDAO = roomDAO;
        this.currentUser = currentUser;
        this.logService = logService;
    }
    public void setSchedulePanel(SchedulePanel schedulePanel) {
        this.schedulePanel = schedulePanel;
    }
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }
    public List<Schedule> getSchedulesByDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            UIUtils.showWarningMessage(schedulePanel,"Invalid Range", "Please provide a valid date range.");
            return Collections.emptyList();
        }
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser);
                if (teacherId > 0) {
                    System.out.println("ScheduleController: Filtering schedule for Teacher ID: " + teacherId);
                    return scheduleDAO.findByTeacherId(teacherId, start, end);
                } else {
                    System.err.println("ScheduleController: Could not determine Teacher ID for logged in user. Returning empty schedule.");
                    return Collections.emptyList();
                }
            } else {
                System.out.println("ScheduleController: Getting all schedules in range for Admin/Other.");
                return scheduleDAO.findByDateRange(start, end);
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading schedules: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Error", "Failed to load schedule data.");
            return Collections.emptyList();
        }
    }
    private int getTeacherIdForUser(User user) {
        if (user != null && user.getRole() == Role.TEACHER && user.getTeacherId() != null) {
            return user.getTeacherId();
        }
        return -1;
    }
    private int getStudentIdForUser(User user) {
        if (user != null && user.getRole() == Role.STUDENT && user.getStudentId() != null) {
            return user.getStudentId();
        }
        return -1;
    }

    public List<EduClass> getAllClassesForSelection() {
        try {
            return eduClassDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading classes for schedule selection: " + e.getMessage());
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
            System.err.println("Error loading active teachers for schedule selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Room> getAllRoomsForSelection() {
        try {
            List<Room> allRooms = roomDAO.getAll();
            if (allRooms != null) {
                return allRooms.stream()
                        .filter(Room::isAvailable)
                        .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } catch (DataAccessException e) {
            System.err.println("Error loading available rooms for schedule selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Teacher getTeacherById(int id) {
        if (this.teacherDAO == null) {
            System.err.println("ScheduleController Error: teacherDAO is null!");
            return null;
        }
        try {
            return teacherDAO.getById(id);
        } catch (Exception e) {
            System.err.println("Error getting teacher by ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    private Room getRoomById(int id) {
        if (this.roomDAO == null) {
            System.err.println("ScheduleController Error: roomDAO is null!");
            return null;
        }
        try {
            return roomDAO.getById(id);
        } catch (Exception e) {
            System.err.println("Error getting room by ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    public boolean addSchedule(Schedule schedule) {
        if (schedule == null || schedule.getClassId() <= 0 || schedule.getTeacherId() <= 0 || schedule.getRoomId() <= 0 ||
                !ValidationUtils.isValidDate(schedule.getDate()) ||
                !ValidationUtils.isValidTimeRange(schedule.getStartTime(), schedule.getEndTime()))
        {
            UIUtils.showWarningMessage(schedulePanel, "Validation Error", "Please select Class, Teacher, Room and provide a valid Date and Time range.");
            return false;
        }
        Teacher selectedTeacher = getTeacherById(schedule.getTeacherId());
        Room selectedRoom = getRoomById(schedule.getRoomId());

        if (selectedTeacher == null || selectedRoom == null) {
            UIUtils.showErrorMessage(schedulePanel, "Error", "Selected Teacher or Room not found.");
            return false;
        }
        if (!selectedTeacher.isActive()) {
            UIUtils.showWarningMessage(schedulePanel, "Validation Error", "Cannot schedule inactive teacher '" + selectedTeacher.getFullName() + "'.");
            return false;
        }
        if (!selectedRoom.isAvailable()) {
            UIUtils.showWarningMessage(schedulePanel, "Validation Error", "Cannot schedule in unavailable room '" + selectedRoom.getRoomNumber() + "'.");
            return false;
        }

        try {
            scheduleDAO.add(schedule);
            writeAddLog("Added Schedule", schedule);
            if (schedulePanel != null) {
                schedulePanel.refreshScheduleView();
                UIUtils.showInfoMessage(schedulePanel, "Success", "Schedule added successfully.");
            }
            return true;
        } catch (ScheduleConflictException e) {
            System.err.println("Schedule Conflict: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Schedule Conflict", e.getMessage());
            return false;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding schedule: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Error", "Failed to add schedule: " + e.getMessage());
            return false;
        }
    }

    public boolean updateSchedule(Schedule schedule) {
        if (schedule == null || schedule.getScheduleId() <= 0 || schedule.getClassId() <= 0 || schedule.getTeacherId() <= 0 || schedule.getRoomId() <= 0 ||
                !ValidationUtils.isValidDate(schedule.getDate()) ||
                !ValidationUtils.isValidTimeRange(schedule.getStartTime(), schedule.getEndTime()))
        {
            UIUtils.showWarningMessage(schedulePanel, "Validation Error", "Invalid schedule data for update.");
            return false;
        }
        Teacher selectedTeacher = getTeacherById(schedule.getTeacherId());
        Room selectedRoom = getRoomById(schedule.getRoomId());

        if (selectedTeacher == null || selectedRoom == null) { return false; }
        if (!selectedTeacher.isActive()) { return false; }
        if (!selectedRoom.isAvailable()) { return false; }

        try {
            scheduleDAO.update(schedule);
            writeUpdateLog("Updated Schedule", schedule);
            if (schedulePanel != null) {
                schedulePanel.refreshScheduleView();
                UIUtils.showInfoMessage(schedulePanel, "Success", "Schedule updated successfully.");
            }
            return true;
        } catch (ScheduleConflictException e) {
            System.err.println("Schedule Conflict on update: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Schedule Conflict", e.getMessage());
            return false;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating schedule: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Error", "Failed to update schedule: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            UIUtils.showWarningMessage(schedulePanel, "Error", "Invalid schedule ID for deletion.");
            return false;
        }
        Schedule scheduleToDelete = null;
        String scheduleInfoForLog = "ID: " + scheduleId;
        try {
            scheduleToDelete = scheduleDAO.getById(scheduleId);
            if(scheduleToDelete != null) {
                scheduleInfoForLog = String.format("ID: %d, Date: %s, Time: %s-%s, ClassID: %d, TeacherID: %d, RoomID: %d",
                        scheduleId,
                        DateUtils.formatDate(scheduleToDelete.getDate()),
                        DateUtils.formatTime(scheduleToDelete.getStartTime()),
                        DateUtils.formatTime(scheduleToDelete.getEndTime()),
                        scheduleToDelete.getClassId(),
                        scheduleToDelete.getTeacherId(),
                        scheduleToDelete.getRoomId());
            }
        } catch (DataAccessException e) {
            System.err.println("Error finding schedule to delete (for logging): " + e.getMessage());
        }
        try {
            scheduleDAO.delete(scheduleId);
            writeDeleteLog("Deleted Schedule", scheduleInfoForLog);
            if (schedulePanel != null) {
                schedulePanel.refreshScheduleView();
                UIUtils.showInfoMessage(schedulePanel, "Success", "Schedule deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error deleting schedule: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Error", "Failed to delete schedule: " + e.getMessage());
            return false;
        }
    }

    public String getClassNameById(int classId) {
        EduClass ec = eduClassDAO.getById(classId);
        return ec != null ? ec.getClassName() : "Unknown Class";
    }

    public String getTeacherNameById(int teacherId) {
        Teacher t = teacherDAO.getById(teacherId);
        return t != null ? t.getFullName() : "Unknown Teacher";
    }

    public String getRoomNameById(int roomId) {
        Room r = roomDAO.getById(roomId);
        return r != null ? r.toString() : "Unknown Room";
    }


    public Schedule getScheduleById(int scheduleId) {
        if (scheduleId <= 0) return null;
        try {
            return scheduleDAO.getById(scheduleId);
        } catch (DataAccessException e) {
            System.err.println("Error getting schedule by ID: " + e.getMessage());
            return null;
        }
    }

    public List<Schedule> getAllSchedules() {
        System.out.println("ScheduleController: getAllSchedules() called.");
        if (scheduleDAO == null) {
            System.err.println("ScheduleController Error: scheduleDAO is null!");
            UIUtils.showErrorMessage(null, "Error", "Schedule data access is not available.");
            return Collections.emptyList();
        }
        try {
            return scheduleDAO.getAllSchedules();
        } catch (DataAccessException e) {
            System.err.println("Error loading all schedules from DAO: " + e.getMessage());
            if (mainView != null) {
                UIUtils.showErrorMessage(mainView, "Error Loading Data", "Failed to load all schedule data.\n" + e.getMessage());
            } else {
                e.printStackTrace();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Unexpected error in getAllSchedules: " + e.getMessage());
            e.printStackTrace();
            if (mainView != null) {
                UIUtils.showErrorMessage(mainView, "Unexpected Error", "An unexpected error occurred while loading all schedules.");
            }
            return Collections.emptyList();
        }
    }
    private void writeAddLog(String action, Schedule schedule) {
        String details = String.format("ID: %d, Date: %s, Time: %s-%s, ClassID: %d, TeacherID: %d, RoomID: %d",
                schedule.getScheduleId(),
                DateUtils.formatDate(schedule.getDate()),
                DateUtils.formatTime(schedule.getStartTime()),
                DateUtils.formatTime(schedule.getEndTime()),
                schedule.getClassId(),
                schedule.getTeacherId(),
                schedule.getRoomId());
        writeLog(action, details);
    }

    private void writeUpdateLog(String action, Schedule schedule) {
        String details = String.format("ID: %d, Date: %s, Time: %s-%s, ClassID: %d, TeacherID: %d, RoomID: %d",
                schedule.getScheduleId(),
                DateUtils.formatDate(schedule.getDate()),
                DateUtils.formatTime(schedule.getStartTime()),
                DateUtils.formatTime(schedule.getEndTime()),
                schedule.getClassId(),
                schedule.getTeacherId(),
                schedule.getRoomId());
        writeLog(action, details);
    }

    private void writeDeleteLog(String action, String details) {
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
                System.out.println("Log written: " + log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: " + action + " - " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }
}