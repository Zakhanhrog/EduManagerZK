package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IAcademicRecordDAO;
import com.eduzk.model.entities.AcademicRecord;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AcademicRecordDAOImpl extends BaseDAO<AcademicRecord> implements IAcademicRecordDAO {

    private final IdGenerator idGenerator;

    public AcademicRecordDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null");
        }
        this.idGenerator = idGenerator;
    }

    @Override
    public Optional<AcademicRecord> findByStudentAndClass(int studentId, int classId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(r -> r.getStudentId() == studentId && r.getClassId() == classId)
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<AcademicRecord> findAllByClassId(int classId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(r -> r.getClassId() == classId)
                    .collect(Collectors.toList()); // Trả về bản sao
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<AcademicRecord> findAllByStudentId(int studentId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(r -> r.getStudentId() == studentId)
                    .collect(Collectors.toList()); // Trả về bản sao
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addOrUpdate(AcademicRecord record) throws DataAccessException {
        if (record == null) {
            throw new IllegalArgumentException("AcademicRecord cannot be null.");
        }
        lock.writeLock().lock();
        try {
            int recordId = record.getRecordId();
            boolean found = false;
            if (recordId > 0) { // Ưu tiên tìm theo recordId nếu có
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).getRecordId() == recordId) {
                        dataList.set(i, record); // Update theo recordId
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                Optional<AcademicRecord> existingOpt = findByStudentAndClassInternal(record.getStudentId(), record.getClassId());
                if (existingOpt.isPresent()) {
                    // Tìm thấy bản ghi cũ, cập nhật nó (giữ nguyên recordId cũ)
                    AcademicRecord existingRecord = existingOpt.get();
                    record.setRecordId(existingRecord.getRecordId()); // Đảm bảo giữ ID cũ
                    // Tìm index và cập nhật
                    for (int i = 0; i < dataList.size(); i++) {
                        if (dataList.get(i).getRecordId() == existingRecord.getRecordId()) {
                            dataList.set(i, record);
                            found = true;
                            break;
                        }
                    }
                }
            }


            if (!found) {
                record.setRecordId(idGenerator.getNextAcademicRecordId());
                dataList.add(record);
            }
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void delete(int recordId) throws DataAccessException {
        if (recordId <= 0) return;
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(r -> r.getRecordId() == recordId);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: AcademicRecord with ID " + recordId + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<AcademicRecord> findByStudentAndClassInternal(int studentId, int classId) {
        return dataList.stream()
                .filter(r -> r.getStudentId() == studentId && r.getClassId() == classId)
                .findFirst();
    }
}