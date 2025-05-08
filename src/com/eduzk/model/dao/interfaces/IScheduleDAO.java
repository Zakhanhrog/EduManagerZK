package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Schedule;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.exceptions.ScheduleConflictException;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

public interface IScheduleDAO {

    Schedule getById(int id);

    List<Schedule> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByClassId(int classId);

    List<Schedule> findByTeacherId(int teacherId, LocalDate startDate, LocalDate endDate);

    List<Schedule> findByRoomId(int roomId, LocalDate startDate, LocalDate endDate);

    List<Schedule> getAll();

    void add(Schedule schedule) throws ScheduleConflictException, DataAccessException;

    void update(Schedule schedule) throws ScheduleConflictException, DataAccessException;

    void delete(int id) throws DataAccessException;

    List<Schedule> getAllSchedules() throws DataAccessException;
}