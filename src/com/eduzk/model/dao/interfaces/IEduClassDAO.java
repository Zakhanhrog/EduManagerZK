package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.EduClass;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;

public interface IEduClassDAO {

    EduClass getById(int id);

    List<EduClass> findByCourseId(int courseId);

    List<EduClass> findByTeacherId(int teacherId);

    List<EduClass> findByStudentId(int studentId);

    List<EduClass> getAll();

    void add(EduClass eduClass) throws DataAccessException;

    void update(EduClass eduClass) throws DataAccessException;

    void delete(int id) throws DataAccessException;

    void addStudentToClass(int classId, int studentId) throws DataAccessException;

    void removeStudentFromClass(int classId, int studentId) throws DataAccessException;

    int addStudentsToClass(int classId, List<Integer> studentIds) throws DataAccessException;
    int removeStudentsFromClass(int classId, List<Integer> studentIds) throws DataAccessException;
}