package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;

public interface ITeacherDAO {

    /**
     * Retrieves a teacher by their unique ID.
     * @param id The ID of the teacher.
     * @return The Teacher object if found, null otherwise.
     */
    Teacher getById(int id);

    /**
     * Finds teachers whose specialization contains the given search string (case-insensitive).
     * @param specialization The specialization to search for.
     * @return A List of matching Teacher objects. The list is empty if no matches are found.
     */
    List<Teacher> findBySpecialization(String specialization);

    /**
     * Retrieves all teachers from the data source.
     * @return A List of all Teacher objects. The list is empty if no teachers are found.
     */
    List<Teacher> getAll();

    /**
     * Adds a new teacher to the data source. Implementations should handle ID generation.
     * @param teacher The Teacher object to add. Must not be null.
     * @throws DataAccessException if a data storage error occurs.
     * @throws IllegalArgumentException if the teacher object is invalid.
     */
    void add(Teacher teacher) throws DataAccessException;

    /**
     * Updates an existing teacher in the data source. The teacher is identified by their ID.
     * @param teacher The Teacher object with updated information. Must not be null.
     * @throws DataAccessException if the teacher to update is not found or if a data storage error occurs.
     * @throws IllegalArgumentException if the teacher object is invalid.
     */
    void update(Teacher teacher) throws DataAccessException;

    /**
     * Deletes a teacher from the data source based on their ID.
     * Consider checking if the teacher is assigned to classes/schedules before deletion.
     * @param id The ID of the teacher to delete.
     * @throws DataAccessException if a data storage error occurs or if deletion constraints are violated.
     */
    void delete(int id) throws DataAccessException;
    /**
     * Xóa nhiều giáo viên khỏi nguồn dữ liệu dựa trên danh sách ID.
     * @param ids Danh sách các ID của giáo viên cần xóa.
     * @return Số lượng bản ghi đã thực sự bị xóa.
     * @throws DataAccessException Nếu có lỗi xảy ra trong quá trình truy cập hoặc lưu dữ liệu.
     */
    int deleteMultiple(List<Integer> ids) throws DataAccessException;
}