// src/com/eduzk/model/dao/interfaces/IAssignmentDAO.java
package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Assignment;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;

public interface IAssignmentDAO {

    /**
     * Retrieves an assignment by its unique ID.
     * @param id The ID of the assignment.
     * @return The Assignment object if found, null otherwise.
     * @throws DataAccessException if a data storage error occurs.
     */
    Assignment getById(int id) throws DataAccessException;

    /**
     * Finds all assignments associated with a specific educational class ID.
     * @param classId The ID of the educational class.
     * @return A List of matching Assignment objects, sorted potentially by due date or creation date. Empty if no matches are found.
     * @throws DataAccessException if a data storage error occurs.
     */
    List<Assignment> findByClassId(int classId) throws DataAccessException;

    /**
     * Retrieves all assignments from the data source. Use with caution on large datasets.
     * @return A List of all Assignment objects.
     * @throws DataAccessException if a data storage error occurs.
     */
    List<Assignment> getAll() throws DataAccessException;

    /**
     * Adds a new assignment to the data source. Implementations should handle ID generation and setting creation/update timestamps.
     * @param assignment The Assignment object to add. Must not be null and have valid properties (title, classId).
     * @throws DataAccessException if a data storage error occurs.
     * @throws IllegalArgumentException if the assignment object is invalid.
     */
    void add(Assignment assignment) throws DataAccessException;

    /**
     * Updates an existing assignment in the data source. The assignment is identified by its ID. Update timestamp should be set.
     * @param assignment The Assignment object with updated information. Must not be null.
     * @throws DataAccessException if the assignment to update is not found or if a data storage error occurs.
     * @throws IllegalArgumentException if the assignment object is invalid.
     */
    void update(Assignment assignment) throws DataAccessException;

    /**
     * Deletes an assignment from the data source based on its ID.
     * @param id The ID of the assignment to delete.
     * @throws DataAccessException if the assignment is not found or if a data storage error occurs.
     */
    void delete(int id) throws DataAccessException;

    /**
     * Deletes all assignments associated with a specific class ID.
     * Used typically when a class is deleted.
     * @param classId The ID of the class whose assignments should be deleted.
     * @throws DataAccessException if a data storage error occurs.
     */
    void deleteByClassId(int classId) throws DataAccessException;
}