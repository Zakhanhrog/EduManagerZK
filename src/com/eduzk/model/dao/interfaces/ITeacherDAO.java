package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;

public interface ITeacherDAO {

    Teacher getById(int id);

    List<Teacher> findBySpecialization(String specialization);

    List<Teacher> getAll();

    void add(Teacher teacher) throws DataAccessException;

    void update(Teacher teacher) throws DataAccessException;

    void delete(int id) throws DataAccessException;

    int deleteMultiple(List<Integer> ids) throws DataAccessException;
}