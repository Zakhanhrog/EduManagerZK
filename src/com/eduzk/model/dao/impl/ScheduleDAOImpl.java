package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IScheduleDAO;
import com.eduzk.model.entities.Schedule;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.exceptions.ScheduleConflictException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.DateUtils;
import java.util.Optional;

public class ScheduleDAOImpl extends BaseDAO<Schedule> implements IScheduleDAO {

    private final IdGenerator idGenerator;

    public ScheduleDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null");
        }
        this.idGenerator = idGenerator;
    }

    @Override
    public Schedule getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(schedule -> schedule.getScheduleId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Schedule> findByDateRange(LocalDate startDate, LocalDate endDate) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(schedule -> !schedule.getDate().isBefore(startDate) && !schedule.getDate().isAfter(endDate))
                    .sorted((s1, s2) -> { // Sort by date then time for better readability
                        int dateComp = s1.getDate().compareTo(s2.getDate());
                        if (dateComp == 0) {
                            return s1.getStartTime().compareTo(s2.getStartTime());
                        }
                        return dateComp;
                    })
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Schedule> findByClassId(int classId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(schedule -> schedule.getClassId() == classId)
                    .sorted((s1, s2) -> s1.getDate().compareTo(s2.getDate()) != 0 ? s1.getDate().compareTo(s2.getDate()) : s1.getStartTime().compareTo(s2.getStartTime()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Schedule> findByTeacherId(int teacherId, LocalDate startDate, LocalDate endDate) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(schedule -> schedule.getTeacherId() == teacherId &&
                            !schedule.getDate().isBefore(startDate) &&
                            !schedule.getDate().isAfter(endDate))
                    .sorted((s1, s2) -> s1.getDate().compareTo(s2.getDate()) != 0 ? s1.getDate().compareTo(s2.getDate()) : s1.getStartTime().compareTo(s2.getStartTime()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Schedule> findByRoomId(int roomId, LocalDate startDate, LocalDate endDate) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(schedule -> schedule.getRoomId() == roomId &&
                            !schedule.getDate().isBefore(startDate) &&
                            !schedule.getDate().isAfter(endDate))
                    .sorted((s1, s2) -> s1.getDate().compareTo(s2.getDate()) != 0 ? s1.getDate().compareTo(s2.getDate()) : s1.getStartTime().compareTo(s2.getStartTime()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Schedule schedule) throws ScheduleConflictException {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null.");
        }
        if (!ValidationUtils.isValidDate(schedule.getDate()) ||
                !ValidationUtils.isValidTimeRange(schedule.getStartTime(), schedule.getEndTime())) {
            throw new IllegalArgumentException("Invalid date or time range for schedule.");
        }

        schedule.setScheduleId(idGenerator.getNextScheduleId());

        lock.writeLock().lock();
        try {
            checkForConflicts(schedule);
            this.dataList.add(schedule);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Schedule schedule) throws ScheduleConflictException {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null.");
        }
        if (!ValidationUtils.isValidDate(schedule.getDate()) ||
                !ValidationUtils.isValidTimeRange(schedule.getStartTime(), schedule.getEndTime())) {
            throw new IllegalArgumentException("Invalid date or time range for schedule.");
        }

        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getScheduleId() == schedule.getScheduleId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                checkForConflicts(schedule);
                dataList.set(index, schedule);
                saveData();
            } else {
                throw new DataAccessException("Schedule with ID " + schedule.getScheduleId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(schedule -> schedule.getScheduleId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: Schedule with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public List<Schedule> getAllSchedules() throws DataAccessException {
        System.out.println("ScheduleDAOImpl: getAllSchedules() called, delegating to BaseDAO.getAll()...");
        try {
            return super.getAll();
        } catch (Exception e) {
            System.err.println("Error in ScheduleDAOImpl.getAllSchedules while calling super.getAll(): " + e.getMessage());
            throw new DataAccessException("Failed to retrieve all schedules from BaseDAO.", e);
        }
    }

    private void checkForConflicts(Schedule newSchedule) throws ScheduleConflictException {
        LocalDate date = newSchedule.getDate();
        LocalTime startTime = newSchedule.getStartTime();
        LocalTime endTime = newSchedule.getEndTime();
        int teacherId = newSchedule.getTeacherId();
        int roomId = newSchedule.getRoomId();
        int scheduleId = newSchedule.getScheduleId();

        List<Schedule> potentialConflicts = dataList.stream()
                .filter(existing -> existing.getScheduleId() != scheduleId &&
                        existing.getDate().equals(date) &&
                        existing.overlaps(newSchedule))
                .collect(Collectors.toList());

        Optional<Schedule> teacherConflict = potentialConflicts.stream()
                .filter(existing -> existing.getTeacherId() == teacherId)
                .findFirst();
        if (teacherConflict.isPresent()) {
            throw new ScheduleConflictException(
                    String.format("Teacher conflict: Teacher ID %d is already scheduled from %s to %s on %s (Schedule ID: %d).",
                            teacherId,
                            DateUtils.formatTime(teacherConflict.get().getStartTime()),
                            DateUtils.formatTime(teacherConflict.get().getEndTime()),
                            DateUtils.formatDate(date),
                            teacherConflict.get().getScheduleId())
            );
        }

        Optional<Schedule> roomConflict = potentialConflicts.stream()
                .filter(existing -> existing.getRoomId() == roomId)
                .findFirst();
        if (roomConflict.isPresent()) {
            throw new ScheduleConflictException(
                    String.format("Room conflict: Room ID %d is already booked from %s to %s on %s (Schedule ID: %d).",
                            roomId,
                            DateUtils.formatTime(roomConflict.get().getStartTime()),
                            DateUtils.formatTime(roomConflict.get().getEndTime()),
                            DateUtils.formatDate(date),
                            roomConflict.get().getScheduleId())
            );
        }
    }
}