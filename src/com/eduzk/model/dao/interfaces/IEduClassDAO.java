package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.EduClass;
import com.eduzk.model.exceptions.DataAccessException;


import java.util.List;

public interface IEduClassDAO {

    /**
     * Retrieves an educational class by its unique ID.
     * @param id The ID of the class.
     * @return The EduClass object if found, null otherwise.
     */
    EduClass getById(int id);

    /**
     * Finds all classes associated with a specific course ID.
     * @param courseId The ID of the course.
     * @return A List of matching EduClass objects. The list is empty if no matches are found.
     */
    List<EduClass> findByCourseId(int courseId);

    /**
     * Finds all classes primarily taught by a specific teacher ID.
     * @param teacherId The ID of the primary teacher.
     * @return A List of matching EduClass objects. The list is empty if no matches are found.
     */
    List<EduClass> findByTeacherId(int teacherId);

    /**
     * Finds all classes in which a specific student is enrolled.
     * @param studentId The ID of the student.
     * @return A List of matching EduClass objects. The list is empty if the student is not enrolled in any class.
     */
    List<EduClass> findByStudentId(int studentId);

    /**
     * Retrieves all educational classes from the data source.
     * @return A List of all EduClass objects. The list is empty if no classes are found.
     */
    List<EduClass> getAll();

    /**
     * Adds a new educational class to the data source. Implementations should handle ID generation.
     * @param eduClass The EduClass object to add. Must not be null and have valid properties (name, course, teacher, capacity).
     * @throws DataAccessException if a data storage error occurs.
     * @throws IllegalArgumentException if the eduClass object is invalid.
     */
    void add(EduClass eduClass) throws DataAccessException;

    /**
     * Updates an existing educational class in the data source. The class is identified by its ID.
     * @param eduClass The EduClass object with updated information. Must not be null and have valid properties.
     *              Cannot update max capacity to be less than current enrollment.
     * @throws DataAccessException if the class to update is not found, if capacity constraint is violated, or if a data storage error occurs.
     * @throws IllegalArgumentException if the eduClass object is invalid.
     */
    void update(EduClass eduClass) throws DataAccessException;

    /**
     * Deletes an educational class from the data source based on its ID.
     * Implementations should typically prevent deletion if the class has enrolled students or associated schedules.
     * @param id The ID of the class to delete.
     * @throws DataAccessException if deletion constraints are violated (e.g., class not empty) or if a data storage error occurs.
     */
    void delete(int id) throws DataAccessException;

    /**
     * Adds a student to a specific class.
     * @param classId The ID of the class to add the student to.
     * @param studentId The ID of the student to add.
     * @throws DataAccessException if the class is not found, the class is full, or the student is already enrolled.
     */
    void addStudentToClass(int classId, int studentId) throws DataAccessException;

    /**
     * Removes a student from a specific class.
     * @param classId The ID of the class to remove the student from.
     * @param studentId The ID of the student to remove.
     * @throws DataAccessException if the class is not found or if a data storage error occurs. (May not throw if student wasn't enrolled).
     */
    void removeStudentFromClass(int classId, int studentId) throws DataAccessException;
}