package com.eduzk.model.dao.interfaces;

import com.eduzk.model.entities.Room;
import com.eduzk.model.exceptions.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface IRoomDAO {

    /**
     * Retrieves a room by its unique ID.
     * @param id The ID of the room.
     * @return The Room object if found, null otherwise.
     */
    Room getById(int id);

    /**
     * Finds a room by its unique room number (case-insensitive). Assumes room numbers are globally unique.
     * @param roomNumber The room number to search for.
     * @return An Optional containing the Room if found, otherwise empty.
     */
    Optional<Room> findByRoomNumber(String roomNumber);

    /**
     * Finds rooms with a capacity greater than or equal to the specified minimum capacity.
     * @param minCapacity The minimum capacity required.
     * @return A List of matching Room objects. The list is empty if no matches are found.
     */
    List<Room> findByCapacity(int minCapacity);

    /**
     * Retrieves all rooms from the data source.
     * @return A List of all Room objects. The list is empty if no rooms are found.
     */
    List<Room> getAll();

    /**
     * Adds a new room to the data source. Implementations should handle ID generation.
     * @param room The Room object to add. Must not be null and have a valid number/capacity.
     * @throws DataAccessException if a room with the same number already exists or if a data storage error occurs.
     * @throws IllegalArgumentException if the room object is invalid.
     */
    void add(Room room) throws DataAccessException;

    /**
     * Updates an existing room in the data source. The room is identified by its ID.
     * @param room The Room object with updated information. Must not be null and have a valid number/capacity.
     * @throws DataAccessException if the room to update is not found, if the updated number conflicts, or if a data storage error occurs.
     * @throws IllegalArgumentException if the room object is invalid.
     */
    void update(Room room) throws DataAccessException;

    /**
     * Deletes a room from the data source based on its ID.
     * Consider checking if the room is used in any Schedule before deletion.
     * @param id The ID of the room to delete.
     * @throws DataAccessException if a data storage error occurs or if deletion constraints are violated.
     */
    void delete(int id) throws DataAccessException;
}