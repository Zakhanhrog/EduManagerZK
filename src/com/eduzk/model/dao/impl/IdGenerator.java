package com.eduzk.model.dao.impl;

import com.eduzk.model.exceptions.DataAccessException;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class IdGenerator {

    private final String idFilePath;
    private final Map<String, Integer> nextIdMap;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String USER_ID_KEY = "user";
    private static final String STUDENT_ID_KEY = "student";
    private static final String TEACHER_ID_KEY = "teacher";
    private static final String COURSE_ID_KEY = "course";
    private static final String ROOM_ID_KEY = "room";
    private static final String EDUCLASS_ID_KEY = "educlass";
    private static final String SCHEDULE_ID_KEY = "schedule";
    private static final String ACADEMIC_RECORD_ID_KEY = "academic_record";
    private static final String ASSIGNMENT_ID_KEY = "assignment";

    public IdGenerator(String idFilePath) {
        this.idFilePath = idFilePath;
        this.nextIdMap = new ConcurrentHashMap<>(); // Use concurrent map for potential future multi-threading
        loadNextIds();
        initializeDefaultKeys();
    }

    private void loadNextIds() {
        lock.lock();
        try {
            File file = new File(idFilePath);
            if (!file.exists()) {
                initializeDefaultKeys();
                saveNextIds(); // Create the file with defaults
                return;
            }

            Properties properties = new Properties();
            try (InputStream input = new FileInputStream(idFilePath)) {
                properties.load(input);
                for (String key : properties.stringPropertyNames()) {
                    try {
                        int id = Integer.parseInt(properties.getProperty(key));
                        nextIdMap.put(key, id);
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format for key '" + key + "' in " + idFilePath);
                        // Optionally initialize with 1 if parsing fails
                        nextIdMap.putIfAbsent(key, 1);
                    }
                }
            }
        } catch (IOException e) {
            // Log the error but try to proceed with defaults / in-memory values
            System.err.println("Error loading ID file: " + idFilePath + " - " + e.getMessage());
            initializeDefaultKeys(); // Ensure defaults are set if loading fails
        } finally {
            lock.unlock();
        }
    }

    private void initializeDefaultKeys() {
        // Initialize default keys if they are missing after loading
        nextIdMap.putIfAbsent(USER_ID_KEY, 1);
        nextIdMap.putIfAbsent(STUDENT_ID_KEY, 1);
        nextIdMap.putIfAbsent(TEACHER_ID_KEY, 1);
        nextIdMap.putIfAbsent(COURSE_ID_KEY, 1);
        nextIdMap.putIfAbsent(ROOM_ID_KEY, 1);
        nextIdMap.putIfAbsent(EDUCLASS_ID_KEY, 1);
        nextIdMap.putIfAbsent(SCHEDULE_ID_KEY, 1);
        nextIdMap.putIfAbsent(ACADEMIC_RECORD_ID_KEY, 1);
        nextIdMap.putIfAbsent(ASSIGNMENT_ID_KEY, 1);
    }


    private void saveNextIds() {
        lock.lock();
        try {
            Properties properties = new Properties();
            for (Map.Entry<String, Integer> entry : nextIdMap.entrySet()) {
                properties.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }

            File file = new File(idFilePath);
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataAccessException("Could not create directory for ID file: " + parentDir.getAbsolutePath());
                }
            }

            try (OutputStream output = new FileOutputStream(idFilePath)) {
                properties.store(output, "Next Available IDs for EduHub Entities");
            }
        } catch (IOException e) {
            throw new DataAccessException("Error saving ID file: " + idFilePath, e);
        } finally {
            lock.unlock();
        }
    }
    public int getNextAcademicRecordId() {
        return getNextId(ACADEMIC_RECORD_ID_KEY);
    }

    public int getNextUserId() {
        return getNextId(USER_ID_KEY);
    }

    public int getNextStudentId() {
        return getNextId(STUDENT_ID_KEY);
    }

    public int getNextTeacherId() {
        return getNextId(TEACHER_ID_KEY);
    }

    public int getNextCourseId() {
        return getNextId(COURSE_ID_KEY);
    }

    public int getNextRoomId() {
        return getNextId(ROOM_ID_KEY);
    }

    public int getNextEduClassId() {
        return getNextId(EDUCLASS_ID_KEY);
    }

    public int getNextScheduleId() {
        return getNextId(SCHEDULE_ID_KEY);
    }

    public int getNextAssignmentId() {
        return getNextId(ASSIGNMENT_ID_KEY);
    }

    private int getNextId(String key) {
        lock.lock();
        try {
            int nextId = nextIdMap.getOrDefault(key, 1);
            nextIdMap.put(key, nextId + 1);
            saveNextIds();
            return nextId;
        } finally {
            lock.unlock();
        }
    }
}