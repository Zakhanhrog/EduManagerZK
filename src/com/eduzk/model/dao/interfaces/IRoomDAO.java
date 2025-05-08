package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Room;
import com.eduzk.model.exceptions.DataAccessException;
import java.util.List;
import java.util.Optional;

public interface IRoomDAO {

    Room getById(int id);

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findByCapacity(int minCapacity);

    List<Room> getAll();

    void add(Room room) throws DataAccessException;

    void update(Room room) throws DataAccessException;

    void delete(int id) throws DataAccessException;
}