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
    protected final ReadWriteLock lock = new ReentrantReadWriteLock(); // For thread safety on dataList access

    protected BaseDAO(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.dataList = new ArrayList<>();
        loadData();
    }

    @SuppressWarnings("unchecked")
    protected void loadData() {
        lock.writeLock().lock(); // Use write lock for initial load/modification
        try {
            File file = new File(dataFilePath);
            if (!file.exists() || file.length() == 0) {
                // File doesn't exist or is empty, start with an empty list
                this.dataList.clear();
                // Optionally, create the file here if it doesn't exist to avoid errors on first save
                if (!file.exists()) {
                    try {
                        File parentDir = file.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs();
                        }
                        file.createNewFile();
                    } catch (IOException createEx) {
                        System.err.println("Warning: Could not create data file on initial load: " + dataFilePath + " - " + createEx.getMessage());
                        // Continue with empty list in memory
                    }
                }
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                Object readObject = ois.readObject();
                if (readObject instanceof List) {
                    // Clear existing list before loading new data
                    this.dataList.clear();
                    // Need to ensure type safety here, though ObjectInputStream gives List<?>
                    // This cast is potentially unsafe if the file contains wrong data type.
                    this.dataList.addAll((List<T>) readObject);
                } else {
                    throw new DataAccessException("Data file does not contain a valid List: " + dataFilePath);
                }
            }
        } catch (EOFException e) {
            // This is expected if the file exists but is empty
            this.dataList.clear();
        } catch (FileNotFoundException e) {
            // Should generally be handled by the check above, but good practice to catch
            System.err.println("Data file not found during load (should have been handled): " + dataFilePath);
            this.dataList.clear();
        } catch (IOException | ClassNotFoundException e) {
            // Wrap other IO or ClassNotFound errors in DataAccessException
            this.dataList.clear(); // Clear list on error to prevent using corrupted data
            throw new DataAccessException("Error loading data from file: " + dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void saveData() {
        lock.writeLock().lock(); // Use write lock for saving
        try {
            File file = new File(dataFilePath);
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataAccessException("Could not create directory for data file: " + parentDir.getAbsolutePath());
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                // Create a new ArrayList to avoid potential ConcurrentModificationException
                // if the list is modified elsewhere while writing (though lock should prevent this)
                oos.writeObject(new ArrayList<>(this.dataList));
            }
        } catch (IOException e) {
            throw new DataAccessException("Error saving data to file: " + dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Provide read-only access to the data list using a read lock
    public List<T> getAll() {
        lock.readLock().lock();
        try {
            // Return a copy to prevent external modification of the internal list
            return new ArrayList<>(this.dataList);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Basic implementation for getting by ID, assuming T has a getId() method (requires reflection or abstract method)
    // For simplicity here, we'll require subclasses to implement getById efficiently.
    // public abstract T getById(int id);

    // Example add operation (subclasses might override or add specific logic)
    public void add(T item) throws ScheduleConflictException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add a null item.");
        }
        lock.writeLock().lock();
        try {
            // Optional: Check for duplicates before adding if necessary
            // if (dataList.contains(item)) {
            //     throw new DataAccessException("Item already exists.");
            // }
            this.dataList.add(item);
            saveData(); // Persist after adding
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Example update operation - requires identifying the item to update
    // Subclasses must implement the logic to find and replace the item, often by ID.
    // public abstract void update(T item);


    // Example delete operation - requires identifying the item to delete
    // Subclasses must implement the logic to find and remove the item, often by ID.
    // public abstract void delete(int id);

}