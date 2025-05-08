package com.eduzk.model.dao.impl;

import com.eduzk.model.dao.interfaces.IRoomDAO;
import com.eduzk.model.entities.Room;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.ValidationUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoomDAOImpl extends BaseDAO<Room> implements IRoomDAO {

    private final IdGenerator idGenerator;

    public RoomDAOImpl(String dataFilePath, IdGenerator idGenerator) {
        super(dataFilePath);
        if (idGenerator == null) {
            throw new IllegalArgumentException("IdGenerator cannot be null");
        }
        this.idGenerator = idGenerator;
    }

    @Override
    public Room getById(int id) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(room -> room.getRoomId() == id)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return Optional.empty();
        }
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(room -> roomNumber.equalsIgnoreCase(room.getRoomNumber()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Room> findByCapacity(int minCapacity) {
        lock.readLock().lock();
        try {
            return dataList.stream()
                    .filter(room -> room.getCapacity() >= minCapacity)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(room.getRoomNumber())) {
            throw new IllegalArgumentException("Room number cannot be empty.");
        }
        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive.");
        }

        room.setRoomId(idGenerator.getNextRoomId());

        lock.writeLock().lock();
        try {
            boolean numberExists = dataList.stream()
                    .anyMatch(existing -> existing.getRoomNumber().equalsIgnoreCase(room.getRoomNumber()));
            if (numberExists) {
                throw new DataAccessException("Room with number '" + room.getRoomNumber() + "' already exists.");
            }

            this.dataList.add(room);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null.");
        }
        if (!ValidationUtils.isNotEmpty(room.getRoomNumber())) {
            throw new IllegalArgumentException("Room number cannot be empty.");
        }
        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive.");
        }

        lock.writeLock().lock();
        try {
            int index = -1;
            for (int i = 0; i < dataList.size(); i++) {
                if (dataList.get(i).getRoomId() == room.getRoomId()) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                final int currentIndex = index;
                boolean numberConflict = dataList.stream()
                        .filter(existing -> existing.getRoomId() != room.getRoomId()) // Exclude self
                        .anyMatch(existing -> existing.getRoomNumber().equalsIgnoreCase(room.getRoomNumber()));

                if (numberConflict) {
                    throw new DataAccessException("Cannot update room. Number '" + room.getRoomNumber() + "' is already used by another room.");
                }

                dataList.set(index, room);
                saveData();
            } else {
                throw new DataAccessException("Room with ID " + room.getRoomId() + " not found for update.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int id) {
        lock.writeLock().lock();
        try {
            boolean removed = dataList.removeIf(room -> room.getRoomId() == id);
            if (removed) {
                saveData();
            } else {
                System.err.println("Warning: Room with ID " + id + " not found for deletion.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}