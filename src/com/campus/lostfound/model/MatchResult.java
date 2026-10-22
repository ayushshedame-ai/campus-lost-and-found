package com.campus.lostfound.model;

/**
 * A suggested pairing of one lost report and one found report together with
 * the similarity score (0-100). Sorted so that the best match comes first.
 */
public class MatchResult implements Comparable<MatchResult> {

    private final LostItem lostItem;
    private final FoundItem foundItem;
    private final int score;

    public MatchResult(LostItem lostItem, FoundItem foundItem, int score) {
        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.score = score;
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public FoundItem getFoundItem() {
        return foundItem;
    }

    public int getScore() {
        return score;
    }

    /** Higher score first. */
    @Override
    public int compareTo(MatchResult other) {
        return Integer.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s \"%s\" (found at %s)  Match score: %d%%",
                lostItem.getId(), foundItem.getId(), foundItem.getName(),
                foundItem.getLocation(), score);
    }
}
