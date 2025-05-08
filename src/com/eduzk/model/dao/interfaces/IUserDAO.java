package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface IUserDAO {

    Optional<User> findByUsername(String username);

    User getById(int id);

    List<User> getAll();

    void add(User user) throws DataAccessException;

    void update(User user) throws DataAccessException;

    void delete(int id) throws DataAccessException;

    Optional<User> findByStudentId(int studentId);
    Optional<User> findByTeacherId(int teacherId);
}