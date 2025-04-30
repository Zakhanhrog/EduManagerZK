package com.eduzk.model.entities;

import java.io.Serializable;
import java.util.Objects;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomId;
    private String roomNumber; // E.g., "A101", "Lab B", "Meeting Room 3"
    private String building; // Optional, e.g., "Main Building", "West Wing"
    private int capacity; // Maximum number of occupants/students
    private String type; // E.g., "Classroom", "Laboratory", "Lecture Hall", "Gym"
    private boolean available; // To mark if the room is usable or under maintenance

    public Room() {
        this.available = true; // Default to available
    }

    public Room(int roomId, String roomNumber, String building, int capacity, String type) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.building = building;
        this.capacity = capacity;
        this.type = type;
        this.available = true;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return roomId == room.roomId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }

    @Override
    public String toString() {
        // Concise representation for selection lists
        return roomNumber + (building != null && !building.isEmpty() ? " (" + building + ")" : "") + " [Cap: " + capacity + "]";
        // Or a more detailed version:
        // return "Room{" +
        //        "roomId=" + roomId +
        //        ", roomNumber='" + roomNumber + '\'' +
        //        ", building='" + building + '\'' +
        //        ", capacity=" + capacity +
        //        ", type='" + type + '\'' +
        //        ", available=" + available +
        //        '}';
    }
}