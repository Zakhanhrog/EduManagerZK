package com.eduzk.model.dao.impl;

import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.dao.interfaces.LogEventListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.event.EventListenerList; // <<< IMPORT EventListenerList

public class LogService {

    private final String dataFilePath;
    private final List<LogEntry> logList;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int MAX_LOG_ENTRIES = 5000;

    // <<< THÊM BIẾN QUẢN LÝ LISTENER >>>
    private final EventListenerList listenerList = new EventListenerList();

    public LogService(String dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.logList = new ArrayList<>();
        loadLogs();
    }

    // <<< THÊM PHƯƠNG THỨC ĐĂNG KÝ LISTENER >>>
    public void addLogEventListener(LogEventListener listener) {
        listenerList.add(LogEventListener.class, listener);
        System.out.println("LogService: Listener registered - " + listener.getClass().getName());
    }

    // <<< THÊM PHƯƠNG THỨC HỦY ĐĂNG KÝ LISTENER >>>
    public void removeLogEventListener(LogEventListener listener) {
        listenerList.remove(LogEventListener.class, listener);
        System.out.println("LogService: Listener unregistered - " + listener.getClass().getName());
    }

    // <<< THÊM PHƯƠNG THỨC THÔNG BÁO CHO LISTENER >>>
    protected void fireLogAdded(LogEntry newLogEntry) {
        Object[] listeners = listenerList.getListenerList();
        // Duyệt ngược để an toàn nếu listener tự hủy đăng ký
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LogEventListener.class) {
                System.out.println("LogService: Notifying listener " + listeners[i + 1].getClass().getName());
                try {
                    // Gọi phương thức logAdded trên listener
                    ((LogEventListener) listeners[i + 1]).logAdded(newLogEntry);
                } catch (Exception e) {
                    // Bắt lỗi nếu listener ném ra exception để không ảnh hưởng đến các listener khác
                    System.err.println("LogService Error: Listener " + listeners[i + 1].getClass().getName() + " threw an exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        System.out.println("LogService: Finished notifying listeners.");
    }


    // --- Các phương thức loadLogs, saveLogs, trimLogList giữ nguyên ---
    @SuppressWarnings("unchecked")
    private void loadLogs() {
        lock.writeLock().lock();
        try {
            File file = new File(dataFilePath);
            if (!file.exists() || file.length() == 0) {
                logList.clear();
                return;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                Object readObject = ois.readObject();
                if (readObject instanceof List) {
                    logList.clear();
                    logList.addAll((List<LogEntry>) readObject);
                    System.out.println("LogService: Loaded " + logList.size() + " log entries.");
                } else {
                    System.err.println("Log data file is corrupted: " + dataFilePath);
                    logList.clear();
                }
            }
        } catch (EOFException e) {
            logList.clear();
            System.out.println("LogService: Log file is empty or EOF reached.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading log data: " + e.getMessage());
            logList.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveLogs() {
        lock.writeLock().lock(); // Đã có lock ở đây
        boolean success = false;
        try {
            trimLogList(); // Trim trước khi lưu

            File file = new File(dataFilePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                // Ghi bản sao để an toàn hơn
                oos.writeObject(new ArrayList<>(this.logList));
                success = true; // Đánh dấu lưu thành công
                System.out.println("LogService: Logs saved successfully.");
            }
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR saving log data to file: " + dataFilePath + " - " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
            if (success) {
            }
        }
    }

    private void trimLogList() {
        if (logList.size() > MAX_LOG_ENTRIES) {
            int removeCount = logList.size() - MAX_LOG_ENTRIES;
            logList.subList(0, removeCount).clear();
            System.out.println("LogService: Trimmed log list, removed " + removeCount + " oldest entries.");
        }
    }

    /**
     * Thêm một mục log mới, lưu vào file và thông báo cho listeners.
     * @param entry LogEntry cần thêm.
     */
    public void addLogEntry(LogEntry entry) {
        if (entry == null) return;
        boolean added = false; // Biến để kiểm tra log có thực sự được thêm không
        lock.writeLock().lock();
        try {
            logList.add(entry); // Thêm vào cuối
            added = true; // Đánh dấu đã thêm vào list
            System.out.println("LogService: Log entry added to internal list.");
        } finally {
            lock.writeLock().unlock();
        }

        // Chỉ lưu và thông báo nếu đã thêm thành công vào list
        if (added) {
            saveLogs(); // Lưu lại toàn bộ danh sách
            // Thông báo sau khi lưu (hoặc ngay sau khi thêm vào list tùy logic mong muốn)
            System.out.println("LogService: Preparing to notify listeners about new log entry.");
            fireLogAdded(entry); // <<< GỌI THÔNG BÁO SAU KHI THÊM VÀ LƯU
        }
    }

    /**
     * Lấy tất cả các mục log đã lưu.
     * @return Danh sách LogEntry (bản sao, mới nhất lên đầu).
     */
    public List<LogEntry> getAllLogs() {
        lock.readLock().lock();
        try {
            List<LogEntry> sortedLogs = new ArrayList<>(this.logList);
            Collections.reverse(sortedLogs); // Đảo ngược để mới nhất lên đầu
            return sortedLogs;
        } finally {
            lock.readLock().unlock();
        }
    }
}