package com.eduzk.model.dao.interfaces; // Hoặc package phù hợp khác

import com.eduzk.model.entities.LogEntry;
import java.util.EventListener;

public interface LogEventListener extends EventListener {
    /**
     * Được gọi khi một LogEntry mới được thêm thành công vào LogService.
     * @param newLogEntry LogEntry vừa được thêm.
     */
    void logAdded(LogEntry newLogEntry);
}