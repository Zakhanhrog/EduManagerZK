package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.AcademicRecord;
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
    /**
     * Xóa nhiều học sinh dựa trên danh sách ID được cung cấp.
     * @param ids Danh sách các ID của học sinh cần xóa.
     * @return Số lượng bản ghi đã thực sự bị xóa.
     * @throws DataAccessException Nếu có lỗi xảy ra trong quá trình xóa.
     */
    int deleteByIds(List<Integer> ids) throws DataAccessException;
    List<Student> getStudentsByClassId(int classId) throws DataAccessException;
}