package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Schedule;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.exceptions.ScheduleConflictException;

import java.time.LocalDate;
import java.util.List;

public interface IScheduleDAO {

    /**
     * Retrieves a schedule entry by its unique ID.
     * @param id The ID of the schedule entry.
     * @return The Schedule object if found, null otherwise.
     */
    Schedule getById(int id);

    /**
     * Finds all schedule entries within a given date range (inclusive).
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return A List of Schedule objects within the range, sorted by date and time. Empty if none found.
     */
    List<Schedule> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Finds all schedule entries for a specific class ID.
     * @param classId The ID of the EduClass.
     * @return A List of Schedule objects for the class, sorted by date and time. Empty if none found.
     */
    List<Schedule> findByClassId(int classId);

    /**
     * Finds all schedule entries for a specific teacher within a date range.
     * @param teacherId The ID of the teacher.
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return A List of Schedule objects for the teacher in the range, sorted by date and time. Empty if none found.
     */
    List<Schedule> findByTeacherId(int teacherId, LocalDate startDate, LocalDate endDate);

    /**
     * Finds all schedule entries for a specific room within a date range.
     * @param roomId The ID of the room.
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return A List of Schedule objects for the room in the range, sorted by date and time. Empty if none found.
     */
    List<Schedule> findByRoomId(int roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves all schedule entries from the data source.
     * @return A List of all Schedule objects. The list is empty if no schedules are found.
     */
    List<Schedule> getAll();

    /**
     * Adds a new schedule entry to the data source. Implementations must handle ID generation and conflict checking.
     * @param schedule The Schedule object to add. Must not be null and have valid properties.
     * @throws ScheduleConflictException if the new schedule conflicts with an existing one (teacher or room).
     * @throws DataAccessException if a data storage error occurs.
     * @throws IllegalArgumentException if the schedule object is invalid.
     */
    void add(Schedule schedule) throws ScheduleConflictException, DataAccessException;

    /**
     * Updates an existing schedule entry in the data source. The entry is identified by its ID.
     * Implementations must handle conflict checking for the updated details.
     * @param schedule The Schedule object with updated information. Must not be null and have valid properties.
     * @throws ScheduleConflictException if the updated schedule conflicts with another existing one (teacher or room).
     * @throws DataAccessException if the schedule to update is not found or if a data storage error occurs.
     * @throws IllegalArgumentException if the schedule object is invalid.
     */
    void update(Schedule schedule) throws ScheduleConflictException, DataAccessException;

    /**
     * Deletes a schedule entry from the data source based on its ID.
     * @param id The ID of the schedule entry to delete.
     * @throws DataAccessException if a data storage error occurs.
     */
    void delete(int id) throws DataAccessException;
}