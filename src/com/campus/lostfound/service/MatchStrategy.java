package com.campus.lostfound.service;

import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.LostItem;

/**
 * Interface for the matching algorithm. Different strategies can be plugged
 * into the service (polymorphism) without changing any other code.
 */
public interface MatchStrategy {

    /** Similarity between a lost report and a found report, from 0 (no match) to 100 (perfect). */
    int score(LostItem lost, FoundItem found);
}
