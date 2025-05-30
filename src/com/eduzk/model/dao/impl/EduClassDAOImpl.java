package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IEduClassDAO;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.eduzk.utils.ValidationUtils;

public class EduClassDAOImpl extends BaseDAO<EduClass> implements IEduClassDAO {

    private final IdGenerator idGenerator;

    public EduClassDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null in EduClassDAOImpl");
        }
        this.idGenerator = idGenerator;
    }

    @Override
    public EduClass getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(eduClass -> eduClass.getClassId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<EduClass> findByCourseId(int courseId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(eduClass -> eduClass.getCourse() != null && eduClass.getCourse().getCourseId() == courseId)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<EduClass> findByTeacherId(int teacherId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(eduClass -> eduClass.getPrimaryTeacher() != null && eduClass.getPrimaryTeacher().getTeacherId() == teacherId)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<EduClass> findByStudentId(int studentId) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(eduClass -> eduClass.getStudentIds().contains(studentId))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public void add(EduClass eduClass) {
        if (eduClass == null) {
            throw new IllegalArgumentException("EduClass cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(eduClass.getClassName())) {
            throw new IllegalArgumentException("Class name cannot be empty.");
        }
        if (eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null) {
            throw new IllegalArgumentException("Course and Primary Teacher must be assigned.");
        }
        if (eduClass.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive.");
        }
        eduClass.setClassId(idGenerator.getNextEduClassId());
        lock.writeLock().lock();
        try {
            this.dataList.add(eduClass);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(EduClass eduClass) {
        if (eduClass == null) {
            throw new IllegalArgumentException("EduClass cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(eduClass.getClassName())) {
            throw new IllegalArgumentException("Class name cannot be empty.");
        }
        if (eduClass.getCourse() == null || eduClass.getPrimaryTeacher() == null) {
            throw new IllegalArgumentException("Course and Primary Teacher must be assigned.");
        }
        if (eduClass.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive.");
        }
        if (eduClass.getCurrentEnrollment() > eduClass.getMaxCapacity()) {
            throw new DataAccessException("Cannot update class. New maximum capacity ("
                    + eduClass.getMaxCapacity() + ") is less than current enrollment ("
                    + eduClass.getCurrentEnrollment() + ").");
        }
        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getClassId() == eduClass.getClassId()) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                dataList.set(index, eduClass);
                saveData();
            } else {
                throw new DataAccessException("EduClass with ID " + eduClass.getClassId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            EduClass classToDelete = getById(id);
            if (classToDelete != null && classToDelete.getCurrentEnrollment() > 0) {
                throw new DataAccessException("Cannot delete class with ID " + id + ". It still has enrolled students.");
            }
            boolean removed = dataList.removeIf(eduClass -> eduClass.getClassId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: EduClass with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addStudentToClass(int classId, int studentId) {
        lock.writeLock().lock();
        try {
            EduClass eduClass = getByIdInternal(classId);
            if (eduClass == null) {
                throw new DataAccessException("EduClass with ID " + classId + " not found.");
            }
            if (eduClass.getCurrentEnrollment() >= eduClass.getMaxCapacity()) {
                throw new DataAccessException("Cannot add student. Class '" + eduClass.getClassName() + "' is full.");
            }
            if (eduClass.getStudentIds().contains(studentId)) {
                System.err.println("Warning: Student with ID " + studentId + " is already enrolled in class ID " + classId);
                return;
            }
            eduClass.addStudentId(studentId);
            update(eduClass);

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeStudentFromClass(int classId, int studentId) {
        lock.writeLock().lock();
        try {
            EduClass eduClass = getByIdInternal(classId);
            if (eduClass == null) {
                System.err.println("Warning: EduClass with ID " + classId + " not found when trying to remove student.");
                return;
            }
            if (!eduClass.getStudentIds().contains(studentId)) {
                System.err.println("Warning: Student with ID " + studentId + " is not enrolled in class ID " + classId);
                return;
            }
            eduClass.removeStudentId(studentId);
            update(eduClass);

        } finally {
            lock.writeLock().unlock();
        }
    }

    private EduClass getByIdInternal(int id) {
        return dataList.stream()
                .filter(eduClass -> eduClass.getClassId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public int addStudentsToClass(int classId, List<Integer> studentIds) throws DataAccessException {
        if (studentIds == null || studentIds.isEmpty()) return 0;
        lock.writeLock().lock();
        int addedCount = 0;
        try {
            EduClass eduClass = getByIdInternal(classId);
            if (eduClass == null) throw new DataAccessException("EduClass with ID " + classId + " not found.");
            int currentEnrollment = eduClass.getCurrentEnrollment();
            int maxCapacity = eduClass.getMaxCapacity();
            int availableSpots = maxCapacity - currentEnrollment;

            List<Integer> studentsToAddActually = new ArrayList<>();
            for (Integer studentId : studentIds) {
                if (studentId != null && studentId > 0 && !eduClass.getStudentIds().contains(studentId)) {
                    if(studentsToAddActually.size() < availableSpots) {
                        studentsToAddActually.add(studentId);
                    } else {
                        System.err.println("Warning: Cannot add student ID " + studentId + " to class ID " + classId + ". Class is full.");
                    }
                } else {
                    System.err.println("Warning: Skipping student ID " + studentId + " (already enrolled or invalid) for class ID " + classId);
                }
            }
            if (!studentsToAddActually.isEmpty()) {
                for(Integer idToAdd : studentsToAddActually) {
                    eduClass.addStudentId(idToAdd);
                }
                addedCount = studentsToAddActually.size();
                this.update(eduClass);
                System.out.println("DAO: Updated class " + classId + " after adding " + addedCount + " students.");
            }
            return addedCount;
        } catch (DataAccessException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessException("Unexpected error adding students to class.", e);
        }finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int removeStudentsFromClass(int classId, List<Integer> studentIds) throws DataAccessException {
        if (studentIds == null || studentIds.isEmpty()) return 0;
        lock.writeLock().lock();
        int actualRemovedCount = 0;
        boolean changed = false;
        try {
            EduClass eduClass = getByIdInternal(classId);
            if (eduClass == null) {
            }
            List<Integer> idsToRemove = new ArrayList<>(studentIds);
            int initialSize = eduClass.getStudentIds().size();

            for(Integer idToRemove : idsToRemove) {
                if(eduClass.removeStudentId(idToRemove)) {
                    changed = true;
                    actualRemovedCount++;
                }
            }
            if (changed) {
                this.update(eduClass);
                System.out.println("DAO: Updated class " + classId + " after removing " + actualRemovedCount + " students.");
            }
            return actualRemovedCount;

        } catch (DataAccessException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e){
            throw new DataAccessException("Unexpected error removing students from class.", e);
        }finally {
            lock.writeLock().unlock();
        }
    }

}