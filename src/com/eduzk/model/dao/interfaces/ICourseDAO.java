package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Course;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface ICourseDAO {

    /**
     * Retrieves a course by its unique ID.
     * @param id The ID of the course.
     * @return The Course object if found, null otherwise.
     */
    Course getById(int id);

    /**
     * Finds a course by its unique code (case-insensitive).
     * @param courseCode The course code to search for.
     * @return An Optional containing the Course if found, otherwise empty.
     */
    Optional<Course> findByCode(String courseCode);

    /**
     * Finds courses whose name contains the given search string (case-insensitive).
     * @param name The partial or full name to search for.
     * @return A List of matching Course objects. The list is empty if no matches are found.
     */
    List<Course> findByName(String name);

    /**
     * Retrieves all courses from the data source.
     * @return A List of all Course objects. The list is empty if no courses are found.
     */
    List<Course> getAll();

    /**
     * Adds a new course to the data source. Implementations should handle ID generation.
     * @param course The Course object to add. Must not be null and have a valid code/name.
     * @throws DataAccessException if a course with the same code already exists or if a data storage error occurs.
     * @throws IllegalArgumentException if the course object is invalid.
     */
    void add(Course course) throws DataAccessException;

    /**
     * Updates an existing course in the data source. The course is identified by its ID.
     * @param course The Course object with updated information. Must not be null and have a valid code/name.
     * @throws DataAccessException if the course to update is not found, if the updated code conflicts, or if a data storage error occurs.
     * @throws IllegalArgumentException if the course object is invalid.
     */
    void update(Course course) throws DataAccessException;

    /**
     * Deletes a course from the data source based on its ID.
     * Consider checking if the course is used in any EduClass before deletion.
     * @param id The ID of the course to delete.
     * @throws DataAccessException if a data storage error occurs or if deletion constraints are violated.
     */
    void delete(int id) throws DataAccessException;
}