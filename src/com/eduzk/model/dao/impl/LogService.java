package com.eduzk.model.service; // Hoặc com.eduzk.model.dao.impl

import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.exceptions.DataAccessException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogService {

    private final String dataFilePath;
    private final List<LogEntry> logList;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int MAX_LOG_ENTRIES = 5000; // Giới hạn số lượng log để tránh file quá lớn (tùy chọn)

    public LogService(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.logList = new ArrayList<>();
        loadLogs();
    }

    @SuppressWarnings("unchecked")
    private void loadLogs() {
        lock.writeLock().lock();
        try {
            File file = new File(dataFilePath);
            if (!file.exists() || file.length() == 0) {
                logList.clear();
                return; // Không cần tạo file trống nếu không có
            }
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                Object readObject = ois.readObject();
                if (readObject instanceof List) {
                    logList.clear();
                    logList.addAll((List<LogEntry>) readObject);
                    System.out.println("LogService: Loaded " + logList.size() + " log entries.");
                } else {
                    System.err.println("Log data file is corrupted: " + dataFilePath);
                    logList.clear(); // Xóa list nếu file lỗi
                }
            }
        } catch (EOFException e) {
            logList.clear(); // File trống hoặc kết thúc đột ngột
            System.out.println("LogService: Log file is empty or EOF reached.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading log data: " + e.getMessage());
            logList.clear(); // Xóa list nếu lỗi đọc
            // Không nên ném DataAccessException ở đây để không chặn khởi động
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveLogs() {
        lock.writeLock().lock();
        try {
            // Optional: Trim logs if exceeds limit before saving
            trimLogList();

            File file = new File(dataFilePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                // Ghi bản sao để tránh ConcurrentModificationException nếu có luồng khác đọc
                oos.writeObject(new ArrayList<>(this.logList));
            }
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR saving log data to file: " + dataFilePath + " - " + e.getMessage());
            // Không ném lỗi ở đây để không ảnh hưởng hoạt động chính
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Optional: Method to limit log size
    private void trimLogList() {
        if (logList.size() > MAX_LOG_ENTRIES) {
            // Remove the oldest entries
            int removeCount = logList.size() - MAX_LOG_ENTRIES;
            // Remove from the beginning of the list
            logList.subList(0, removeCount).clear();
            System.out.println("LogService: Trimmed log list, removed " + removeCount + " oldest entries.");
        }
    }


    /**
     * Thêm một mục log mới và lưu vào file.
     * @param entry LogEntry cần thêm.
     */
    public void addLogEntry(LogEntry entry) {
        if (entry == null) return;
        lock.writeLock().lock();
        try {
            logList.add(entry); // Thêm vào cuối
            saveLogs(); // Lưu lại toàn bộ danh sách (bao gồm cả trim nếu có)
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy tất cả các mục log đã lưu.
     * @return Danh sách LogEntry (bản sao).
     */
    public List<LogEntry> getAllLogs() {
        lock.readLock().lock();
        try {
            // Trả về bản sao để không bị sửa đổi từ bên ngoài
            // Sắp xếp theo thời gian mới nhất trước? (tùy chọn)
            List<LogEntry> sortedLogs = new ArrayList<>(this.logList);
            Collections.reverse(sortedLogs); // Đảo ngược để mới nhất lên đầu
            return sortedLogs;
            // Hoặc return new ArrayList<>(this.logList); // Giữ nguyên thứ tự cũ
        } finally {
            lock.readLock().unlock();
        }
    }
}