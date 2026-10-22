package com.campus.lostfound.storage;

import java.util.Collection;
import java.util.List;

import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.Item;

/**
 * Interface for saving and loading reports. The service only depends on this
 * interface, so the file-based implementation could be replaced by a database
 * without touching the rest of the program.
 */
public interface ItemRepository {

    /** Writes all items, replacing the previous contents. */
    void saveAll(Collection<Item> items) throws StorageException;

    /** Reads every stored item (empty list if nothing has been saved yet). */
    List<Item> loadAll() throws StorageException;
}
