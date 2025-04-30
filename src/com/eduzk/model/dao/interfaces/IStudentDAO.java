package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Student;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface IStudentDAO {

    /**
     * Retrieves a student by their unique ID.
     * @param id The ID of the student.
     * @return The Student object if found, null otherwise.
     */
    Student getById(int id);

    /**
     * Finds students whose full name contains the given search string (case-insensitive).
     * @param name The partial or full name to search for.
     * @return A List of matching Student objects. The list is empty if no matches are found.
     */
    List<Student> findByName(String name);

    /**
     * Retrieves all students from the data source.
     * @return A List of all Student objects. The list is empty if no students are found.
     */
    List<Student> getAll();

    /**
     * Adds a new student to the data source. Implementations should handle ID generation.
     * @param student The Student object to add. Must not be null.
     * @throws DataAccessException if a data storage error occurs (e.g., duplicate check fails).
     * @throws IllegalArgumentException if the student object is invalid.
     */
    void add(Student student) throws DataAccessException;

    /**
     * Updates an existing student in the data source. The student is identified by their ID.
     * @param student The Student object with updated information. Must not be null.
     * @throws DataAccessException if the student to update is not found or if a data storage error occurs.
     * @throws IllegalArgumentException if the student object is invalid.
     */
    void update(Student student) throws DataAccessException;

    /**
     * Deletes a student from the data source based on their ID.
     * Consider implications like removing the student from enrolled classes.
     * @param id The ID of the student to delete.
     * @throws DataAccessException if a data storage error occurs.
     */
    void delete(int id) throws DataAccessException;

    /**
     * Tìm kiếm học sinh dựa trên số điện thoại.
     * Giả định số điện thoại là duy nhất hoặc chỉ lấy người đầu tiên tìm thấy.
     * @param phone Số điện thoại cần tìm.
     * @return Optional chứa Student nếu tìm thấy, ngược lại là Optional rỗng.
     */
    Optional<Student> findByPhone(String phone);
}