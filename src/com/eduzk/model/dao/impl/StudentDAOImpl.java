package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.dao.interfaces.IStudentDAO;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Student;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.*;
import java.util.stream.Collectors;

public class StudentDAOImpl extends BaseDAO<Student> implements IStudentDAO {

    private final IdGenerator idGenerator;
    private final IEduClassDAO eduClassDAO; // <<<< THÊM BIẾN NÀY

    public StudentDAOImpl(String dataFilePath, IdGenerator idGenerator, IEduClassDAO eduClassDAO) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null in StudentDAOImpl");
        }
        this.idGenerator = idGenerator;
        this.eduClassDAO = eduClassDAO;

    }
    @Override
    public List<Student> getStudentsByClassId(int classId) throws DataAccessException {
        if (eduClassDAO == null) {
            throw new DataAccessException("EduClassDAO is not available in StudentDAOImpl");
        }
        EduClass eduClass = eduClassDAO.getById(classId);
        if (eduClass != null && eduClass.getStudentIds() != null && !eduClass.getStudentIds().isEmpty()) {
            List<Integer> studentIds = eduClass.getStudentIds();
            return studentIds.stream()
                    .map(this::getById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
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
        String trimmedPhone = phone.trim();
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(student -> trimmedPhone.equals(student.getPhone()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int deleteByIds(List<Integer> ids) throws DataAccessException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Set<Integer> idsToDeleteSet = new HashSet<>(ids);
        int initialSize;
        int removedCount = 0;

        lock.writeLock().lock();
        try {
            initialSize = dataList.size();
            boolean changed = dataList.removeIf(student -> idsToDeleteSet.contains(student.getStudentId()));

            if (changed) {
                removedCount = initialSize - dataList.size();
                System.out.println("Deleted " + removedCount + " students with IDs: " + ids);
                saveData();
            } else {
                System.out.println("No students found matching IDs for deletion: " + ids);
            }
        } catch (Exception e) {
            throw new DataAccessException("Error deleting multiple students.", e);
        } finally {
            lock.writeLock().unlock();
        }
        return removedCount;
    }


}