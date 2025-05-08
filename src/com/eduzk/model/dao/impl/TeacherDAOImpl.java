package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.ITeacherDAO;
import com.eduzk.model.entities.Teacher;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TeacherDAOImpl extends BaseDAO<Teacher> implements ITeacherDAO {

    private final IdGenerator idGenerator;

    public TeacherDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null in TeacherDAOImpl");
        }
        this.idGenerator = idGenerator;
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
    @Override
    public int deleteMultiple(List<Integer> ids) throws DataAccessException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int initialSize;
        int finalSize;

        lock.writeLock().lock();
        try {
            initialSize = dataList.size();
            System.out.println("TeacherDAOImpl.deleteMultiple: Initial size = " + initialSize + ", attempting to remove IDs: " + ids);
            boolean removed = dataList.removeIf(teacher -> ids.contains(teacher.getTeacherId()));

            finalSize = dataList.size();
            int deletedCount = initialSize - finalSize;
            System.out.println("TeacherDAOImpl.deleteMultiple: Removed " + deletedCount + " items. Final size = " + finalSize);

            if (deletedCount > 0) {
                System.out.println("TeacherDAOImpl.deleteMultiple: Saving data after deletion...");
                saveData();
                System.out.println("TeacherDAOImpl.deleteMultiple: Data saved.");
            } else {
                System.out.println("TeacherDAOImpl.deleteMultiple: No items were removed matching the IDs.");
            }

            return deletedCount;

        } catch (Exception e) {
            System.err.println("Error during multiple delete operation: " + e.getMessage());
            e.printStackTrace();
            throw new DataAccessException("Error deleting multiple teachers.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}