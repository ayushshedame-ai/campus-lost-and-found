package com.campus.lostfound.service;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.LostItem;

/**
 * Default matching algorithm. A pair scores up to 100 points:
 * <ul>
 *   <li>30 - same category</li>
 *   <li>40 - words shared by the item names/descriptions</li>
 *   <li>15 - same (or overlapping) location</li>
 *   <li>15 - found shortly after the item was lost</li>
 * </ul>
 */
public class KeywordMatchStrategy implements MatchStrategy {

    private static final Set<String> STOP_WORDS = new HashSet<String>(Arrays.asList(
            "a", "an", "the", "of", "and", "with", "in", "on", "at", "my", "is", "it", "to",
            "for", "near", "has", "have", "was", "very", "some", "inside", "contains"));

    @Override
    public int score(LostItem lost, FoundItem found) {
        int score = 0;
        if (lost.getCategory() == found.getCategory()) {
            score += 30;
        }
        score += keywordScore(lost, found);
        score += locationScore(lost.getLocation(), found.getLocation());
        score += dateScore(lost, found);
        return Math.min(100, score);
    }

    private int keywordScore(Item lost, Item found) {
        Set<String> lostWords = tokens(lost.getName() + " " + lost.getDescription());
        Set<String> foundWords = tokens(found.getName() + " " + found.getDescription());
        if (lostWords.isEmpty() || foundWords.isEmpty()) {
            return 0;
        }
        Set<String> common = new HashSet<String>(lostWords);
        common.retainAll(foundWords); // set intersection
        int smaller = Math.min(lostWords.size(), foundWords.size());
        return (int) Math.round(40.0 * common.size() / smaller);
    }

    private int locationScore(String lostLocation, String foundLocation) {
        String a = lostLocation.toLowerCase();
        String b = foundLocation.toLowerCase();
        if (a.equals(b)) {
            return 15;
        }
        if (a.contains(b) || b.contains(a)) {
            return 10;
        }
        return 0;
    }

    private int dateScore(Item lost, Item found) {
        long days = ChronoUnit.DAYS.between(lost.getDate(), found.getDate());
        if (days < 0) {
            return 0; // cannot be found before it was lost
        }
        if (days <= 3) {
            return 15;
        }
        if (days <= 7) {
            return 10;
        }
        return days <= 30 ? 5 : 0;
    }

    private Set<String> tokens(String text) {
        Set<String> words = new HashSet<String>();
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }
}
