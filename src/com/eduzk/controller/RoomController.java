package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.IRoomDAO;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Room;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.panels.RoomPanel;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class RoomController {

    private final IRoomDAO roomDAO;
    private final User currentUser;
    private final LogService logService;
    private RoomPanel roomPanel;

    public RoomController(IRoomDAO roomDAO, User currentUser, LogService logService) {
        this.roomDAO = roomDAO;
        this.currentUser = currentUser;
        this.logService = logService;
    }

    public void setRoomPanel(RoomPanel roomPanel) {
        this.roomPanel = roomPanel;
    }

    public List<Room> getAllRooms() {
        try {
            return roomDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading rooms: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to load room data.");
            return Collections.emptyList();
        }
    }

    public List<Room> searchRoomsByCapacity(int minCapacity) {
        if (minCapacity <= 0) {
            return getAllRooms();
        }
        try {
            return roomDAO.findByCapacity(minCapacity);
        } catch (DataAccessException e) {
            System.err.println("Error searching rooms: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to search rooms by capacity.");
            return Collections.emptyList();
        }
    }

    public boolean addRoom(Room room) {
        if (room == null || !ValidationUtils.isNotEmpty(room.getRoomNumber()) || room.getCapacity() <= 0) {
            UIUtils.showWarningMessage(roomPanel, "Validation Error", "Room number cannot be empty and capacity must be positive.");
            return false;
        }

        try {
            roomDAO.add(room);

            writeAddLog("Added Room", room);

            if (roomPanel != null) {
                roomPanel.refreshTable();
                UIUtils.showInfoMessage(roomPanel, "Success", "Room added successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error adding room: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to add room: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRoom(Room room) {
        if (room == null || room.getRoomId() <= 0 || !ValidationUtils.isNotEmpty(room.getRoomNumber()) || room.getCapacity() <= 0) {
            UIUtils.showWarningMessage(roomPanel, "Validation Error", "Invalid room data for update.");
            return false;
        }

        try {
            roomDAO.update(room);

            writeUpdateLog("Updated Room", room);

            if (roomPanel != null) {
                roomPanel.refreshTable();
                UIUtils.showInfoMessage(roomPanel, "Success", "Room updated successfully.");
            }
            return true;
        } catch (DataAccessException | IllegalArgumentException e) {
            System.err.println("Error updating room: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to update room: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRoom(int roomId) {
        if (roomId <= 0) {
            UIUtils.showWarningMessage(roomPanel, "Error", "Invalid room ID for deletion.");
            return false;
        }
        Room roomToDelete = null;
        String roomInfoForLog = "ID: " + roomId;
        try {
            roomToDelete = roomDAO.getById(roomId);
            if(roomToDelete != null){
                roomInfoForLog = "ID: " + roomId + ", Number: " + roomToDelete.getRoomNumber() + ", Building: " + roomToDelete.getBuilding();
            }
        } catch (DataAccessException e) {
            System.err.println("Error finding room to delete (for logging): " + e.getMessage());
        }

        try {
            roomDAO.delete(roomId);

            writeDeleteLog("Deleted Room", roomInfoForLog);

            if (roomPanel != null) {
                roomPanel.refreshTable();
                UIUtils.showInfoMessage(roomPanel, "Success", "Room deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            System.err.println("Error deleting room: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to delete room: " + e.getMessage());
            return false;
        }
    }

    public Room getRoomById(int roomId) {
        if (roomId <= 0) return null;
        try {
            return roomDAO.getById(roomId);
        } catch (DataAccessException e) {
            System.err.println("Error getting room by ID: " + e.getMessage());
            return null;
        }
    }

    private void writeAddLog(String action, Room room) {
        String details = String.format("ID: %d, Number: %s, Building: %s, Capacity: %d, Type: %s, Available: %b",
                room.getRoomId(), room.getRoomNumber(), room.getBuilding(),
                room.getCapacity(), room.getType(), room.isAvailable());
        writeLog(action, details);
    }

    private void writeUpdateLog(String action, Room room) {
        String details = String.format("ID: %d, Number: %s, Building: %s, Capacity: %d, Type: %s, Available: %b",
                room.getRoomId(), room.getRoomNumber(), room.getBuilding(),
                room.getCapacity(), room.getType(), room.isAvailable());
        writeLog(action, details);
    }

    private void writeDeleteLog(String action, String details) {
        writeLog(action, details);
    }

    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(),
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: " + action + " - " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

}