package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.AcademicRecord;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;

public interface IAcademicRecordDAO {
    Optional<AcademicRecord> findByStudentAndClass(int studentId, int classId); // Tìm record cụ thể
    List<AcademicRecord> findAllByClassId(int classId); // Lấy tất cả record của lớp
    List<AcademicRecord> findAllByStudentId(int studentId); // Lấy tất cả record của HS (cho view HS)
    void addOrUpdate(AcademicRecord record) throws DataAccessException; // Thêm mới hoặc cập nhật nếu đã tồn tại
    void delete(int recordId) throws DataAccessException; // Xóa theo ID của record
    // Có thể thêm các phương thức tìm kiếm khác nếu cần
}