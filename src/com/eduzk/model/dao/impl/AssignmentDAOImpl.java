// src/com/eduzk/model/dao/impl/AssignmentDAOImpl.java
package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IAssignmentDAO;
import com.eduzk.model.entities.Assignment;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssignmentDAOImpl extends BaseDAO<Assignment> implements IAssignmentDAO {

    private static final String DATA_FILE_PATH = "data/assignments.dat"; // Path to store assignment data
    private final IdGenerator idGenerator;

    public AssignmentDAOImpl(IdGenerator idGenerator) {
        super(DATA_FILE_PATH);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null for AssignmentDAOImpl");
        }
        this.idGenerator = idGenerator;
        System.out.println("AssignmentDAOImpl initialized. Data loaded from: " + DATA_FILE_PATH);
    }

    @Override
    public Assignment getById(int id) throws DataAccessException {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(a -> a.getAssignmentId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Assignment> findByClassId(int classId) throws DataAccessException {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(a -> a.getEduClassId() == classId)
                    .sorted(Comparator.comparing(Assignment::getDueDateTime, Comparator.nullsLast(Comparator.naturalOrder())) // Sort by due date
                            .thenComparing(Assignment::getCreatedAt)) // Then by creation date
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // getAll() is inherited from BaseDAO

    @Override
    public void add(Assignment assignment) throws DataAccessException {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment to add cannot be null.");
        }
        if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title cannot be empty.");
        }
        if (assignment.getEduClassId() <= 0) {
            throw new IllegalArgumentException("Assignment must be associated with a valid Class ID.");
        }

        lock.writeLock().lock();
        try {
            assignment.setAssignmentId(idGenerator.getNextAssignmentId()); // Assign new ID
            assignment.touch(); // Set create/update time (touch sets update time)
            this.dataList.add(assignment);
            saveData();
            System.out.println("Added new assignment: ID=" + assignment.getAssignmentId() + ", Title=" + assignment.getTitle());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Assignment assignment) throws DataAccessException {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment to update cannot be null.");
        }
        if (assignment.getAssignmentId() <= 0) {
            throw new IllegalArgumentException("Assignment to update must have a valid ID.");
        }
        if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title cannot be empty.");
        }
        if (assignment.getEduClassId() <= 0) {
            throw new IllegalArgumentException("Assignment must be associated with a valid Class ID.");
        }

        lock.writeLock().lock();
        try {
            Optional<Assignment> existingOpt = dataList.stream()
                    .filter(a -> a.getAssignmentId() == assignment.getAssignmentId())
                    .findFirst();

            if (existingOpt.isPresent()) {
                Assignment existing = existingOpt.get();
                // Update fields (keeping original createdAt)
                existing.setEduClassId(assignment.getEduClassId());
                existing.setTitle(assignment.getTitle());
                existing.setDescription(assignment.getDescription());
                existing.setDueDateTime(assignment.getDueDateTime());
                existing.touch(); // Update the 'updatedAt' timestamp
                saveData();
                System.out.println("Updated assignment: ID=" + assignment.getAssignmentId());
            } else {
                throw new DataAccessException("Assignment with ID " + assignment.getAssignmentId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(a -> a.getAssignmentId() == id);
            if (removed) {
                saveData();
                System.out.println("Deleted assignment: ID=" + id);
            } else {
                throw new DataAccessException("Assignment with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteByClassId(int classId) throws DataAccessException {
        lock.writeLock().lock();
        try {
            long initialSize = dataList.size();
            boolean changed = dataList.removeIf(a -> a.getEduClassId() == classId);
            if (changed) {
                saveData();
                long finalSize = dataList.size();
                System.out.println("Deleted " + (initialSize - finalSize) + " assignments for class ID: " + classId);
            } else {
                System.out.println("No assignments found to delete for class ID: " + classId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}