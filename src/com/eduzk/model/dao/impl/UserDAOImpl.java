package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.Optional;
import com.eduzk.model.entities.Role;

public class UserDAOImpl extends BaseDAO<User> implements IUserDAO {

    private final IdGenerator idGenerator;

    public UserDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null");
        }
        this.idGenerator = idGenerator;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(user -> username.equalsIgnoreCase(user.getUsername()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public User getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(user -> user.getUserId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(User user) throws DataAccessException {
        if (user == null) { throw new IllegalArgumentException("User cannot be null."); }
        user.setUserId(idGenerator.getNextUserId());

        lock.writeLock().lock();
        try {
            boolean usernameExists = dataList.stream()
                    .anyMatch(existingUser -> existingUser.getUsername().equalsIgnoreCase(user.getUsername()));
            if (usernameExists) {
                throw new DataAccessException("Username '" + user.getUsername() + "' already exists.");
            }

            if (user.getRole() == Role.STUDENT && user.getStudentId() != null) {
                boolean studentIdExists = dataList.stream()
                        .anyMatch(existingUser -> existingUser.getRole() == Role.STUDENT &&
                                user.getStudentId().equals(existingUser.getStudentId()));
                if (studentIdExists) {
                    throw new DataAccessException("An account for student ID " + user.getStudentId() + " already exists.");
                }
            }

            this.dataList.add(user);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void update(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getUserId() == user.getUserId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                final int currentIndex = index;
                boolean usernameConflict = dataList.stream()
                        .filter(existingUser -> existingUser.getUserId() != user.getUserId())
                        .anyMatch(existingUser -> existingUser.getUsername().equalsIgnoreCase(user.getUsername()));

                if (usernameConflict) {
                    throw new DataAccessException("Cannot update user. Username '" + user.getUsername() + "' is already used by another user.");
                }

                dataList.set(index, user);
                saveData();
            } else {
                throw new DataAccessException("User with ID " + user.getUserId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(user -> user.getUserId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: User with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public Optional<User> findByStudentId(int studentId) {
        if (studentId <= 0) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(user -> user.getRole() == Role.STUDENT &&
                            user.getStudentId() != null &&
                            user.getStudentId() == studentId)
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public Optional<User> findByTeacherId(int teacherId) {
        if (teacherId <= 0) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(user -> user.getRole() == Role.TEACHER &&
                            user.getTeacherId() != null &&
                            user.getTeacherId().equals(teacherId))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

}