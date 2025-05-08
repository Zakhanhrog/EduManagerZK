package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Student;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;

public interface IStudentDAO {
    Student getById(int id);
    List<Student> findByName(String name);
    List<Student> getAll();
    void add(Student student) throws DataAccessException;
    void update(Student student) throws DataAccessException;
    void delete(int id) throws DataAccessException;
    Optional<Student> findByPhone(String phone);
    int deleteByIds(List<Integer> ids) throws DataAccessException;
    List<Student> getStudentsByClassId(int classId) throws DataAccessException;
}