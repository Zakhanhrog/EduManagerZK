package com.eduzk.model.dao.impl;

import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.exceptions.ScheduleConflictException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BaseDAO<T extends Serializable> {

    protected final String dataFilePath;
    protected final List<T> dataList;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected BaseDAO(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.dataList = new ArrayList<>();
        loadData();
    }

    @SuppressWarnings("unchecked")
    protected void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File(dataFilePath);
            if (!file.exists() || file.length() == 0) {
                this.dataList.clear();
                if (!file.exists()) {
                    try {
                        File parentDir = file.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs();
                        }
                        file.createNewFile();
                    } catch (IOException createEx) {
                        System.err.println("Warning: Could not create data file on initial load: " + dataFilePath + " - " + createEx.getMessage());
                    }
                }
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                Object readObject = ois.readObject();
                if (readObject instanceof List) {
                    this.dataList.clear();
                    this.dataList.addAll((List<T>) readObject);
                } else {
                    throw new DataAccessException("Data file does not contain a valid List: " + dataFilePath);
                }
            }
        } catch (EOFException e) {
            this.dataList.clear();
        } catch (FileNotFoundException e) {
            System.err.println("DEBUG: BaseDAO.loadData - Gặp FileNotFoundException (lỗi logic?): " + dataFilePath);
            this.dataList.clear();
        } catch (IOException | ClassNotFoundException e) {
            this.dataList.clear();
            throw new DataAccessException("Error loading data from file: " + dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void saveData() {
        lock.writeLock().lock();
        try {
            File file = new File(dataFilePath);

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataAccessException("Could not create directory for data file: " + parentDir.getAbsolutePath());
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                oos.writeObject(new ArrayList<>(this.dataList));
            }
        } catch (IOException e) {
            throw new DataAccessException("Error saving data to file: " + dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<T> getAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(this.dataList);
        } finally {
            lock.readLock().unlock();
        }
    }
    public void add(T item) throws ScheduleConflictException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add a null item.");
        }
        lock.writeLock().lock();
        try {
            this.dataList.add(item);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

}