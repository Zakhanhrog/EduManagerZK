package com.eduzk.controller;

import com.eduzk.model.entities.User;
import com.eduzk.model.entities.Role;
import com.eduzk.model.dao.interfaces.IRoomDAO;
import com.eduzk.model.entities.Room;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.panels.RoomPanel; // To update the panel's table

import java.util.Collections;
import java.util.List;

public class RoomController {

    private final IRoomDAO roomDAO;
    private final User currentUser;
    private RoomPanel roomPanel;

    public RoomController(IRoomDAO roomDAO, User currentUser) {
        this.roomDAO = roomDAO;
        this.currentUser = currentUser;
    }

    public void setRoomPanel(RoomPanel roomPanel) {
        this.roomPanel = roomPanel;
    }

    public List<Room> getAllRooms() {
        try {
            // Filter out unavailable rooms if needed by default, or provide filter option
            // return roomDAO.getAll().stream().filter(Room::isAvailable).collect(Collectors.toList());
            return roomDAO.getAll();
        } catch (DataAccessException e) {
            System.err.println("Error loading rooms: " + e.getMessage());
            UIUtils.showErrorMessage(roomPanel, "Error", "Failed to load room data.");
            return Collections.emptyList();
        }
    }

    public List<Room> searchRoomsByCapacity(int minCapacity) {
        if (minCapacity <= 0) {
            return getAllRooms(); // Return all if capacity is invalid/zero
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
        // Add more validation (type)

        try {
            roomDAO.add(room);
            if (roomPanel != null) {
                roomPanel.refreshTable(); // Assumes RoomPanel has this method
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
        // Add more validation...

        try {
            roomDAO.update(room);
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
        // Confirmation dialog in View layer
        try {
            roomDAO.delete(roomId);
            if (roomPanel != null) {
                roomPanel.refreshTable();
                UIUtils.showInfoMessage(roomPanel, "Success", "Room deleted successfully.");
            }
            return true;
        } catch (DataAccessException e) {
            // Handle specific errors, e.g., room used in schedules
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
}