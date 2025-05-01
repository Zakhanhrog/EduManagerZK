package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.entities.Student;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StudentDAOImpl extends BaseDAO<Student> implements IStudentDAO {

    private final IdGenerator idGenerator;

    public StudentDAOImpl(String dataFilePath, String idFilePath) {
        super(dataFilePath);
        this.idGenerator = new IdGenerator(idFilePath);
    }

    @Override
    public Student getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(student -> student.getStudentId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }
        student.setStudentId(idGenerator.getNextStudentId());

        lock.writeLock().lock();
        try {
            // Optional: Add checks for duplicate emails or phone numbers if needed
            this.dataList.add(student);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }
        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getStudentId() == student.getStudentId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Optional: Add checks for duplicate emails/phones before update if necessary
                dataList.set(index, student);
                saveData();
            } else {
                throw new DataAccessException("Student with ID " + student.getStudentId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(student -> student.getStudentId() == id);
            if (removed) {
                saveData();
                // Consider removing student from any enrolled classes (EduClassDAO responsibility?)
            } else {
                System.err.println("Warning: Student with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Student> findByName(String name) {
        lock.readLock().lock();
        try {
            String lowerCaseName = name.toLowerCase();
            return dataList.stream()
                    .filter(student -> student.getFullName().toLowerCase().contains(lowerCaseName))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public Optional<Student> findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Optional.empty();
        }
        String trimmedPhone = phone.trim(); // Chỉ tìm số đã chuẩn hóa (nếu cần)
        lock.readLock().lock();
        try {
            return dataList.stream()
                    // So sánh trực tiếp hoặc chuẩn hóa cả 2 SĐT trước khi so sánh nếu cần
                    .filter(student -> trimmedPhone.equals(student.getPhone())) // Giả sử phone trong Student đã được chuẩn hóa
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public int deleteByIds(List<Integer> ids) throws DataAccessException {
        if (ids == null || ids.isEmpty()) {
            return 0; // Không có gì để xóa
        }
        // Dùng Set để kiểm tra ID hiệu quả hơn trong removeIf
        Set<Integer> idsToDeleteSet = new HashSet<>(ids);
        int initialSize;
        int removedCount = 0;

        lock.writeLock().lock(); // Khóa để ghi
        try {
            initialSize = dataList.size();
            // Sử dụng removeIf để xóa các phần tử khớp với ID trong Set
            boolean changed = dataList.removeIf(student -> idsToDeleteSet.contains(student.getStudentId()));

            if (changed) {
                removedCount = initialSize - dataList.size(); // Tính số lượng đã xóa
                System.out.println("Deleted " + removedCount + " students with IDs: " + ids);
                saveData(); // Chỉ lưu lại file nếu có thay đổi
            } else {
                System.out.println("No students found matching IDs for deletion: " + ids);
            }
        } catch (Exception e) {
            // Gói lại lỗi nếu cần
            throw new DataAccessException("Error deleting multiple students.", e);
        } finally {
            lock.writeLock().unlock();
        }
        return removedCount; // Trả về số lượng đã xóa
    }

    // getAll() is inherited from BaseDAO
}