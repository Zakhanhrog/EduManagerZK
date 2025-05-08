package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.AcademicRecord;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;

public interface IAcademicRecordDAO {
    Optional<AcademicRecord> findByStudentAndClass(int studentId, int classId);
    List<AcademicRecord> findAllByStudentId(int studentId);
    void addOrUpdate(AcademicRecord record) throws DataAccessException;
    void delete(int recordId) throws DataAccessException;
}