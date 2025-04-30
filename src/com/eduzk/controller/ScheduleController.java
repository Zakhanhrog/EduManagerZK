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
import com.eduzk.view.panels.SchedulePanel; // To update the panel's table/view

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class ScheduleController {

    private final IScheduleDAO scheduleDAO;
    private final IEduClassDAO eduClassDAO; // Needed for class selection/info
    private final ITeacherDAO teacherDAO;   // Needed for teacher selection/info
    private final IRoomDAO roomDAO;         // Needed for room selection/info
    private SchedulePanel schedulePanel;
    private final User currentUser;
    private MainView mainView;

    public ScheduleController(IScheduleDAO scheduleDAO, IEduClassDAO eduClassDAO, ITeacherDAO teacherDAO, IRoomDAO roomDAO, User currentUser) {
        this.scheduleDAO = scheduleDAO;
        this.eduClassDAO = eduClassDAO;
        this.teacherDAO = teacherDAO;
        this.roomDAO = roomDAO;
        this.currentUser = currentUser;
    }

    public void setSchedulePanel(SchedulePanel schedulePanel) {
        this.schedulePanel = schedulePanel;
    }
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }

    public List<Schedule> getSchedulesByDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            // Default to today or a week? Handle invalid range.
            UIUtils.showWarningMessage(schedulePanel,"Invalid Range", "Please provide a valid date range.");
            return Collections.emptyList();
        }
        try {
            if (currentUser != null && currentUser.getRole() == Role.TEACHER) {
                int teacherId = getTeacherIdForUser(currentUser); // Gọi hàm helper
                if (teacherId > 0) {
                    System.out.println("ScheduleController: Filtering schedule for Teacher ID: " + teacherId);
                    // Gọi DAO để lọc theo teacherId và khoảng ngày
                    return scheduleDAO.findByTeacherId(teacherId, start, end);
                } else {
                    System.err.println("ScheduleController: Could not determine Teacher ID for logged in user. Returning empty schedule.");
                    return Collections.emptyList();
                }
            } else { // Admin hoặc vai trò khác (hoặc chưa đăng nhập)
                System.out.println("ScheduleController: Getting all schedules in range for Admin/Other.");
                // Lấy tất cả schedule trong khoảng ngày
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
            // return teacherDAO.getAll().stream().filter(Teacher::isActive).collect(Collectors.toList());
            return teacherDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading teachers for schedule selection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Room> getAllRoomsForSelection() {
        try {
            // return roomDAO.getAll().stream().filter(Room::isAvailable).collect(Collectors.toList());
            return roomDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading rooms for schedule selection: " + e.getMessage());
            return Collections.emptyList();
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

        try {
            // Fetch related entities to potentially display better conflict messages if needed, although DAO handles the check
            // EduClass eduClass = eduClassDAO.getById(schedule.getClassId());
            // Teacher teacher = teacherDAO.getById(schedule.getTeacherId());
            // Room room = roomDAO.getById(schedule.getRoomId());
            // Could add checks here e.g., if room capacity < class capacity

            scheduleDAO.add(schedule);
            if (schedulePanel != null) {
                schedulePanel.refreshScheduleView(); // Assumes SchedulePanel has this method
                UIUtils.showInfoMessage(schedulePanel, "Success", "Schedule added successfully.");
            }
            return true;
        } catch (ScheduleConflictException e) {
            System.err.println("Schedule Conflict: " + e.getMessage());
            UIUtils.showErrorMessage(schedulePanel, "Schedule Conflict", e.getMessage()); // Show specific conflict message
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

        try {
            scheduleDAO.update(schedule);
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
        // Confirmation dialog in View layer
        try {
            scheduleDAO.delete(scheduleId);
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

    // --- Helper methods to get entity names for display (avoids passing full objects everywhere) ---
    // These can be used by the SchedulePanel/Dialog to display names instead of just IDs.

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
        // Use the room's toString() or a specific format
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
    /**
     * Lấy danh sách TẤT CẢ các lịch trình từ DAO.
     * Được sử dụng cho chức năng export hoặc các chức năng cần xem toàn bộ lịch sử.
     * @return Danh sách tất cả Schedule, hoặc danh sách rỗng nếu có lỗi hoặc không có dữ liệu.
     */
    public List<Schedule> getAllSchedules() {
        System.out.println("ScheduleController: getAllSchedules() called.");
        if (scheduleDAO == null) {
            System.err.println("ScheduleController Error: scheduleDAO is null!");
            // Có thể hiển thị lỗi UI ở đây không? Tùy ngữ cảnh gọi hàm này
            // UIUtils.showErrorMessage(null, "Error", "Schedule data access is not available.");
            return Collections.emptyList();
        }
        try {
            // Gọi phương thức mới trong IScheduleDAO
            return scheduleDAO.getAllSchedules();
        } catch (DataAccessException e) {
            System.err.println("Error loading all schedules from DAO: " + e.getMessage());
            // Hiển thị lỗi cho người dùng (giả sử mainView có thể truy cập)
            // Cần cẩn thận nếu hàm này được gọi từ luồng nền (ví dụ: export)
            if (mainView != null) { // Kiểm tra mainView tồn tại (cần thêm getter/setter nếu chưa có)
                UIUtils.showErrorMessage(mainView, "Error Loading Data", "Failed to load all schedule data.\n" + e.getMessage());
            } else {
                // Hoặc chỉ log lỗi nếu không có context UI
                e.printStackTrace();
            }
            return Collections.emptyList(); // Trả về danh sách rỗng khi có lỗi
        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác
            System.err.println("Unexpected error in getAllSchedules: " + e.getMessage());
            e.printStackTrace();
            if (mainView != null) {
                UIUtils.showErrorMessage(mainView, "Unexpected Error", "An unexpected error occurred while loading all schedules.");
            }
            return Collections.emptyList();
        }
    }
}