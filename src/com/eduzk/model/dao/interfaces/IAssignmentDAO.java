package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Assignment;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;

public interface IAssignmentDAO {

    Assignment getById(int id) throws DataAccessException;

    List<Assignment> findByClassId(int classId) throws DataAccessException;

    List<Assignment> getAll() throws DataAccessException;

    void add(Assignment assignment) throws DataAccessException;

    void update(Assignment assignment) throws DataAccessException;

    void delete(int id) throws DataAccessException;

}