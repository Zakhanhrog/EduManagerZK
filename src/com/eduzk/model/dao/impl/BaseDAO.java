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
        System.out.println("DEBUG: BaseDAO.loadData() được gọi cho file: [" + dataFilePath + "]");
        lock.writeLock().lock();
        try {
            File file = new File(dataFilePath);

            String absolutePath = "N/A";
            try {
                absolutePath = file.getAbsolutePath(); // Lấy đường dẫn tuyệt đối để rõ ràng hơn
            } catch (Exception e) { /* Bỏ qua lỗi nếu không lấy được path */ }
            System.out.println("DEBUG: BaseDAO.loadData - Kiểm tra file: [" + absolutePath + "]");
            System.out.println("DEBUG: BaseDAO.loadData - File tồn tại? " + file.exists());
            System.out.println("DEBUG: BaseDAO.loadData - Độ dài file: " + file.length());

            if (!file.exists() || file.length() == 0) {
                System.out.println("DEBUG: BaseDAO.loadData - File không tồn tại hoặc rỗng. Xóa dataList.");
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
                System.out.println("DEBUG: BaseDAO.loadData - Đọc dữ liệu thành công. Size: " + this.dataList.size());
            }
        } catch (EOFException e) {
            System.out.println("DEBUG: BaseDAO.loadData - Gặp EOFException (file có thể rỗng). Xóa dataList.");
            this.dataList.clear();
        } catch (FileNotFoundException e) {
            System.err.println("DEBUG: BaseDAO.loadData - Gặp FileNotFoundException (lỗi logic?): " + dataFilePath);
            this.dataList.clear();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("DEBUG: BaseDAO.loadData - Gặp lỗi IO/ClassNotFound. Xóa dataList. Lỗi: " + e.getMessage());
            this.dataList.clear();
            throw new DataAccessException("Error loading data from file: " + dataFilePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void saveData() {
        System.out.println("DEBUG: BaseDAO.saveData() được gọi cho file: [" + dataFilePath + "]");
        lock.writeLock().lock(); // Use write lock for saving
        try {
            File file = new File(dataFilePath);

            String absolutePath = "N/A";
            try {
                absolutePath = file.getAbsolutePath(); // Lấy đường dẫn tuyệt đối
            } catch (Exception e) { /* Bỏ qua lỗi */ }
            System.out.println("DEBUG: BaseDAO.saveData - Chuẩn bị ghi vào file: [" + absolutePath + "]");
            System.out.println("DEBUG: BaseDAO.saveData - Số lượng bản ghi sẽ ghi: " + this.dataList.size());

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new DataAccessException("Could not create directory for data file: " + parentDir.getAbsolutePath());
                }
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                oos.writeObject(new ArrayList<>(this.dataList));
                System.out.println("DEBUG: BaseDAO.saveData - Ghi dữ liệu thành công.");
            }
        } catch (IOException e) {
            System.err.println("DEBUG: BaseDAO.saveData - Gặp lỗi IO khi ghi file: " + e.getMessage());
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