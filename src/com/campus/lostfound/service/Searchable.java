package com.campus.lostfound.service;

import java.util.List;

/**
 * Anything that can be searched by a keyword.
 */
public interface Searchable<T> {

    List<T> search(String keyword);
}
