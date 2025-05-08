package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Course;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;

public interface ICourseDAO {

    Course getById(int id);

    Optional<Course> findByCode(String courseCode);

    List<Course> findByName(String name);

    List<Course> getAll();

    void add(Course course) throws DataAccessException;

    void update(Course course) throws DataAccessException;

    void delete(int id) throws DataAccessException;
}