package com.eduzk.model.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private int scheduleId;
    private int classId; // Reference to the EduClass ID
    private int teacherId; // Reference to the Teacher ID (can be different from primary)
    private int roomId; // Reference to the Room ID
    private LocalDate date; // Specific date for this session
    private LocalTime startTime;
    private LocalTime endTime;
    // Optional: Add recurrence info if needed (e.g., DayOfWeek, frequency)
    // private DayOfWeek dayOfWeek; // For recurring schedules

    public Schedule() {
    }

    public Schedule(int scheduleId, int classId, int teacherId, int roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.scheduleId = scheduleId;
        this.classId = classId;
        this.teacherId = teacherId;
        this.roomId = roomId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;

        // Basic validation
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time.");
        }
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        // Consider validation: endTime != null && startTime != null && endTime.isBefore(startTime)
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        // Consider validation: endTime != null && startTime != null && endTime.isBefore(startTime)
        this.endTime = endTime;
    }

    // Helper method to check for time overlap with another schedule on the same date
    public boolean overlaps(Schedule other) {
        if (other == null || !this.date.equals(other.date)) {
            return false; // Different dates cannot overlap
        }
        // Overlap occurs if one starts before the other ends AND one ends after the other starts
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return scheduleId == schedule.scheduleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId);
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "scheduleId=" + scheduleId +
                ", classId=" + classId +
                ", teacherId=" + teacherId +
                ", roomId=" + roomId +
                ", date=" + date +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}