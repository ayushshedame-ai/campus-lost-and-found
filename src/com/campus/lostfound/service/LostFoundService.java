package com.campus.lostfound.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.campus.lostfound.exception.InvalidInputException;
import com.campus.lostfound.exception.ItemNotFoundException;
import com.campus.lostfound.exception.MatchException;
import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemCategory;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.LostItem;
import com.campus.lostfound.model.MatchResult;
import com.campus.lostfound.model.Person;
import com.campus.lostfound.storage.ItemRepository;
import com.campus.lostfound.util.FileUtil;
import com.campus.lostfound.util.Validator;

/**
 * Business logic of the Lost &amp; Found system: reporting, searching,
 * matching, claiming, statistics and report export.
 *
 * Collections used: LinkedHashMap (lookup by ID, keeps insertion order),
 * ArrayList (results), EnumMap (statistics), sorting with Comparable.
 */
public class LostFoundService implements Searchable<Item> {

    /** Pairs scoring below this value are not suggested as matches. */
    public static final int MIN_MATCH_SCORE = 50;

    private final Map<String, LostItem> lostItems = new LinkedHashMap<String, LostItem>();
    private final Map<String, FoundItem> foundItems = new LinkedHashMap<String, FoundItem>();

    private final ItemRepository repository;
    private final MatchStrategy matchStrategy;

    private int lostCounter = 0;
    private int foundCounter = 0;

    public LostFoundService(ItemRepository repository, MatchStrategy matchStrategy) throws StorageException {
        this.repository = repository;
        this.matchStrategy = matchStrategy;
        loadFromStorage();
    }

    // ------------------------------------------------------------ reporting

    public LostItem reportLost(String name, String description, ItemCategory category,
                               String location, LocalDate dateLost, Person owner)
            throws InvalidInputException, StorageException {
        String cleanName = Validator.requireNonBlank(name, "Item name");
        String cleanDescription = Validator.requireNonBlank(description, "Description");
        String cleanLocation = Validator.requireNonBlank(location, "Location");
        requireCategoryAndPerson(category, owner);
        Validator.requireNotFuture(dateLost);

        String id = String.format("L%03d", ++lostCounter);
        LostItem item = new LostItem(id, cleanName, cleanDescription, category, cleanLocation, dateLost, owner);
        lostItems.put(id, item);
        persist();
        return item;
    }

    public FoundItem reportFound(String name, String description, ItemCategory category,
                                 String location, LocalDate dateFound, Person finder, String storedAt)
            throws InvalidInputException, StorageException {
        String cleanName = Validator.requireNonBlank(name, "Item name");
        String cleanDescription = Validator.requireNonBlank(description, "Description");
        String cleanLocation = Validator.requireNonBlank(location, "Location");
        String cleanStoredAt = Validator.requireNonBlank(storedAt, "Storage location");
        requireCategoryAndPerson(category, finder);
        Validator.requireNotFuture(dateFound);

        String id = String.format("F%03d", ++foundCounter);
        FoundItem item = new FoundItem(id, cleanName, cleanDescription, category, cleanLocation,
                dateFound, finder, cleanStoredAt);
        foundItems.put(id, item);
        persist();
        return item;
    }

    private void requireCategoryAndPerson(ItemCategory category, Person person) throws InvalidInputException {
        if (category == null) {
            throw new InvalidInputException("Category must be selected.");
        }
        if (person == null) {
            throw new InvalidInputException("Contact details are required.");
        }
    }

    // -------------------------------------------------------------- queries

    public List<LostItem> getAllLostItems() {
        List<LostItem> list = new ArrayList<LostItem>(lostItems.values());
        Collections.sort(list);
        return list;
    }

    public List<FoundItem> getAllFoundItems() {
        List<FoundItem> list = new ArrayList<FoundItem>(foundItems.values());
        Collections.sort(list);
        return list;
    }

    /** Finds a lost OR found report by its ID (e.g. L001 or F002). */
    public Item getItemById(String id) throws ItemNotFoundException {
        String key = normalise(id);
        Item item = lostItems.get(key);
        if (item == null) {
            item = foundItems.get(key);
        }
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }

    public LostItem getLostItem(String id) throws ItemNotFoundException {
        LostItem item = lostItems.get(normalise(id));
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }

    public FoundItem getFoundItem(String id) throws ItemNotFoundException {
        FoundItem item = foundItems.get(normalise(id));
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }

    @Override
    public List<Item> search(String keyword) {
        List<Item> results = new ArrayList<Item>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        String key = keyword.trim().toLowerCase();
        for (Item item : allItems()) {
            String haystack = (item.getId() + " " + item.getName() + " " + item.getDescription() + " "
                    + item.getLocation() + " " + item.getCategory().getLabel() + " "
                    + item.getContact().getName()).toLowerCase();
            if (haystack.contains(key)) {
                results.add(item);
            }
        }
        Collections.sort(results);
        return results;
    }

    // ------------------------------------------------------------- matching

    /** Open found items that look like the given lost item, best match first. */
    public List<MatchResult> findMatchesForLost(String lostId) throws ItemNotFoundException {
        LostItem lost = getLostItem(lostId);
        List<MatchResult> results = new ArrayList<MatchResult>();
        for (FoundItem found : foundItems.values()) {
            if (!found.isOpen()) {
                continue;
            }
            int score = matchStrategy.score(lost, found);
            if (score >= MIN_MATCH_SCORE) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        Collections.sort(results);
        return results;
    }

    /** Open lost items that look like the given found item, best match first. */
    public List<MatchResult> findMatchesForFound(String foundId) throws ItemNotFoundException {
        FoundItem found = getFoundItem(foundId);
        List<MatchResult> results = new ArrayList<MatchResult>();
        for (LostItem lost : lostItems.values()) {
            if (!lost.isOpen()) {
                continue;
            }
            int score = matchStrategy.score(lost, found);
            if (score >= MIN_MATCH_SCORE) {
                results.add(new MatchResult(lost, found, score));
            }
        }
        Collections.sort(results);
        return results;
    }

    /** Links a lost report with a found report (both must still be open). */
    public void confirmMatch(String lostId, String foundId)
            throws ItemNotFoundException, MatchException, StorageException {
        LostItem lost = getLostItem(lostId);
        FoundItem found = getFoundItem(foundId);
        if (!lost.isOpen()) {
            throw new MatchException(lost.getId() + " is already " + lost.getStatus().getLabel().toLowerCase() + ".");
        }
        if (!found.isOpen()) {
            throw new MatchException(found.getId() + " is already " + found.getStatus().getLabel().toLowerCase() + ".");
        }
        lost.setStatus(ItemStatus.MATCHED);
        lost.setMatchedWithId(found.getId());
        found.setStatus(ItemStatus.MATCHED);
        found.setMatchedWithId(lost.getId());
        persist();
    }

    /**
     * Hands the found item back to its owner. The student ID given must be the
     * one recorded when the item was reported lost.
     */
    public void markClaimed(String lostId, String studentId)
            throws ItemNotFoundException, MatchException, StorageException {
        LostItem lost = getLostItem(lostId);
        if (lost.getStatus() == ItemStatus.CLAIMED) {
            throw new MatchException(lost.getId() + " has already been claimed.");
        }
        if (lost.getStatus() != ItemStatus.MATCHED) {
            throw new MatchException(lost.getId() + " has not been matched with a found item yet.");
        }
        if (studentId == null || !lost.getContact().getStudentId().equalsIgnoreCase(studentId.trim())) {
            throw new MatchException("Student ID does not match the owner recorded for " + lost.getId() + ".");
        }
        FoundItem found = getFoundItem(lost.getMatchedWithId());
        lost.setStatus(ItemStatus.CLAIMED);
        found.setStatus(ItemStatus.CLAIMED);
        persist();
    }

    // ----------------------------------------------------------- statistics

    public String getStatistics() {
        Map<ItemStatus, Integer> lost = countByStatus(lostItems.values());
        Map<ItemStatus, Integer> found = countByStatus(foundItems.values());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Lost reports  : %d  (open %d, matched %d, claimed %d)%n",
                lostItems.size(), lost.get(ItemStatus.OPEN), lost.get(ItemStatus.MATCHED), lost.get(ItemStatus.CLAIMED)));
        sb.append(String.format("  Found reports : %d  (open %d, matched %d, claimed %d)%n",
                foundItems.size(), found.get(ItemStatus.OPEN), found.get(ItemStatus.MATCHED), found.get(ItemStatus.CLAIMED)));
        return sb.toString();
    }

    private Map<ItemStatus, Integer> countByStatus(Collection<? extends Item> items) {
        Map<ItemStatus, Integer> counts = new EnumMap<ItemStatus, Integer>(ItemStatus.class);
        for (ItemStatus status : ItemStatus.values()) {
            counts.put(status, 0);
        }
        for (Item item : items) {
            counts.put(item.getStatus(), counts.get(item.getStatus()) + 1);
        }
        return counts;
    }

    // ----------------------------------------------------------- file export

    /** Writes a readable text report of all lost items, found items and matches. */
    public void exportReport(String filePath) throws StorageException {
        File file = new File(filePath);
        try {
            FileUtil.ensureParentDirectory(file);
            try (PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))) {
                out.println("CAMPUS LOST & FOUND - REPORT");
                out.println("Generated on: " + Validator.formatDate(LocalDate.now()));
                out.println();
                out.println("SUMMARY");
                out.print(getStatistics());
                out.println();

                out.println("LOST ITEMS");
                out.println(Item.SUMMARY_HEADER);
                for (LostItem item : getAllLostItems()) {
                    out.println(item.getSummaryLine());
                }
                out.println();

                out.println("FOUND ITEMS");
                out.println(Item.SUMMARY_HEADER);
                for (FoundItem item : getAllFoundItems()) {
                    out.println(item.getSummaryLine());
                }
                out.println();

                out.println("MATCHED / RETURNED ITEMS");
                boolean any = false;
                for (LostItem lost : getAllLostItems()) {
                    if (lost.getMatchedWithId() != null) {
                        any = true;
                        out.println(lost.getId() + " (" + lost.getName() + ", owner "
                                + lost.getContact().getName() + ")  <->  " + lost.getMatchedWithId()
                                + "  [" + lost.getStatus().getLabel() + "]");
                    }
                }
                if (!any) {
                    out.println("(none)");
                }
                if (out.checkError()) {
                    throw new IOException("write error");
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not write report to '" + filePath + "': " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------- helpers

    private List<Item> allItems() {
        List<Item> all = new ArrayList<Item>();
        all.addAll(lostItems.values());
        all.addAll(foundItems.values());
        return all;
    }

    private String normalise(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }

    private void persist() throws StorageException {
        repository.saveAll(allItems());
    }

    private void loadFromStorage() throws StorageException {
        for (Item item : repository.loadAll()) {
            int number = idNumber(item.getId());
            if (item instanceof LostItem) {
                lostItems.put(item.getId(), (LostItem) item);
                lostCounter = Math.max(lostCounter, number);
            } else if (item instanceof FoundItem) {
                foundItems.put(item.getId(), (FoundItem) item);
                foundCounter = Math.max(foundCounter, number);
            }
        }
    }

    private int idNumber(String id) throws StorageException {
        try {
            return Integer.parseInt(id.substring(1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new StorageException("Corrupt data: invalid item ID '" + id + "'.", e);
        }
    }
}
