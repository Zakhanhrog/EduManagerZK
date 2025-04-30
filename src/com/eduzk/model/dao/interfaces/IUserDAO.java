package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException; // Optional, if methods throw specific exceptions

import java.util.List;
import java.util.Optional;

public interface IUserDAO {

    /**
     * Finds a user by their unique username (case-insensitive).
     * @param username The username to search for.
     * @return An Optional containing the User if found, otherwise empty.
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves a user by their unique ID.
     * @param id The ID of the user.
     * @return The User object if found, null otherwise.
     */
    User getById(int id);

    /**
     * Retrieves all users from the data source.
     * @return A List of all User objects. The list is empty if no users are found.
     */
    List<User> getAll();

    /**
     * Adds a new user to the data source. Implementations should handle ID generation.
     * @param user The User object to add. Must not be null.
     * @throws DataAccessException if a user with the same username already exists or if a data storage error occurs.
     * @throws IllegalArgumentException if the user object is invalid (e.g., null).
     */
    void add(User user) throws DataAccessException;

    /**
     * Updates an existing user in the data source. The user is identified by its ID.
     * @param user The User object with updated information. Must not be null.
     * @throws DataAccessException if the user to update is not found, if the updated username conflicts with another user, or if a data storage error occurs.
     * @throws IllegalArgumentException if the user object is invalid (e.g., null).
     */
    void update(User user) throws DataAccessException;

    /**
     * Deletes a user from the data source based on their ID.
     * @param id The ID of the user to delete.
     * @throws DataAccessException if a data storage error occurs. (Implementations might choose not to throw if the ID doesn't exist).
     */
    void delete(int id) throws DataAccessException;
    /**
     * Tìm kiếm tài khoản User dựa trên studentId liên kết.
     * Chỉ nên có tối đa 1 User có Role STUDENT cho mỗi studentId.
     * @param studentId ID của học sinh cần tìm tài khoản User.
     * @return Optional chứa User nếu tìm thấy, ngược lại là Optional rỗng.
     */
    Optional<User> findByStudentId(int studentId);
}