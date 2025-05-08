package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.LogEntry;
import java.util.EventListener;

public interface LogEventListener extends EventListener {
    void logAdded(LogEntry newLogEntry);
}