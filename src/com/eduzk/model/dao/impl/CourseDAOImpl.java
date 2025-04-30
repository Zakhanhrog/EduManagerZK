package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.ICourseDAO;
import com.eduzk.model.entities.Course;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.eduzk.utils.ValidationUtils;

public class CourseDAOImpl extends BaseDAO<Course> implements ICourseDAO {

    private final IdGenerator idGenerator;

    public CourseDAOImpl(String dataFilePath, String idFilePath) {
        super(dataFilePath);
        this.idGenerator = new IdGenerator(idFilePath);
    }

    @Override
    public Course getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(course -> course.getCourseId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Course> findByCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(course -> courseCode.equalsIgnoreCase(course.getCourseCode()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Course> findByName(String name) {
        lock.readLock().lock();
        try {
            String lowerCaseName = name.toLowerCase();
            return dataList.stream()
                    .filter(course -> course.getCourseName().toLowerCase().contains(lowerCaseName))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(course.getCourseCode()) || !ValidationUtils.isNotEmpty(course.getCourseName())) {
            throw new IllegalArgumentException("Course code and name cannot be empty.");
        }

        course.setCourseId(idGenerator.getNextCourseId());

        lock.writeLock().lock();
        try {
            // Check for duplicate course code (case-insensitive)
            boolean codeExists = dataList.stream()
                    .anyMatch(existing -> existing.getCourseCode().equalsIgnoreCase(course.getCourseCode()));
            if (codeExists) {
                throw new DataAccessException("Course with code '" + course.getCourseCode() + "' already exists.");
            }

            this.dataList.add(course);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(course.getCourseCode()) || !ValidationUtils.isNotEmpty(course.getCourseName())) {
            throw new IllegalArgumentException("Course code and name cannot be empty.");
        }

        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getCourseId() == course.getCourseId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // Check if the updated code conflicts with another existing course
                final int currentIndex = index;
                boolean codeConflict = dataList.stream()
                        .filter(existing -> existing.getCourseId() != course.getCourseId()) // Exclude self
                        .anyMatch(existing -> existing.getCourseCode().equalsIgnoreCase(course.getCourseCode()));

                if (codeConflict) {
                    throw new DataAccessException("Cannot update course. Code '" + course.getCourseCode() + "' is already used by another course.");
                }

                dataList.set(index, course);
                saveData();
            } else {
                throw new DataAccessException("Course with ID " + course.getCourseId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            // Check if course is used in any EduClass before deleting? (EduClassDAO responsibility?)
            // Simple delete for now.

            boolean removed = dataList.removeIf(course -> course.getCourseId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: Course with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // getAll() is inherited from BaseDAO
}