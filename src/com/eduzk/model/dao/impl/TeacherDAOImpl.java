package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TeacherDAOImpl extends BaseDAO<Teacher> implements ITeacherDAO {

    private final IdGenerator idGenerator;

    public TeacherDAOImpl(String dataFilePath, String idFilePath) {
        super(dataFilePath);
        this.idGenerator = new IdGenerator(idFilePath);
        System.out.println("TeacherDAOImpl initialized. dataList size after load: " + (dataList == null ? "null" : dataList.size()));
    }

    @Override
    public Teacher getById(int id) {
        System.out.println("TeacherDAOImpl.getById(" + id + ") called. Current dataList size: " + (dataList == null ? "null" : dataList.size()));
        lock.readLock().lock();
        try {
            Optional<Teacher> found = dataList.stream()
                    .filter(teacher -> teacher.getTeacherId() == id)
                    .findFirst();
            System.out.println("TeacherDAOImpl.getById(" + id + ") - Found: " + found.isPresent());
            return found.orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null.");
        }
        teacher.setTeacherId(idGenerator.getNextTeacherId());

        lock.writeLock().lock();
        try {
            // Optional: Check for duplicate email/phone
            this.dataList.add(teacher);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher cannot be null.");
        }
        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getTeacherId() == teacher.getTeacherId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Optional: Check for duplicate email/phone before update
                dataList.set(index, teacher);
                saveData();
            } else {
                throw new DataAccessException("Teacher with ID " + teacher.getTeacherId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            // Check if teacher is assigned to any active classes or schedules before deleting?
            // This might require calls to EduClassDAO or ScheduleDAO.
            // For simplicity now, we just delete. Add checks later if needed.

            boolean removed = dataList.removeIf(teacher -> teacher.getTeacherId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: Teacher with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Teacher> findBySpecialization(String specialization) {
        lock.readLock().lock();
        try {
            String lowerCaseSpec = specialization.toLowerCase();
            return dataList.stream()
                    .filter(teacher -> teacher.getSpecialization() != null &&
                            teacher.getSpecialization().toLowerCase().contains(lowerCaseSpec))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // getAll() is inherited from BaseDAO
}