package com.eduzk.model.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private int scheduleId;
    private int classId;
    private int teacherId;
    private int roomId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

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
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean overlaps(Schedule other) {
        if (other == null || !this.date.equals(other.date)) {
            return false;
        }
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