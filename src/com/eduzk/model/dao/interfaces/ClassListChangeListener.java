package com.eduzk.model.dao.interfaces;

import java.util.EventListener;

public interface ClassListChangeListener extends EventListener {
    void classListChanged();
}