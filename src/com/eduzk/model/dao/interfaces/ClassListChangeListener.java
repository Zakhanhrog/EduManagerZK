package com.eduzk.model.dao.interfaces;

import java.util.EventListener;

public interface ClassListChangeListener extends EventListener {
    /**
     * Được gọi khi danh sách lớp học có thể đã thay đổi (thêm, sửa, xóa).
     */
    void classListChanged();
}