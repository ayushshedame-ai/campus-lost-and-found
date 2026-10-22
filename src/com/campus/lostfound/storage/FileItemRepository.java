package com.campus.lostfound.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemCategory;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.LostItem;
import com.campus.lostfound.model.Person;
import com.campus.lostfound.util.FileUtil;

/**
 * Stores reports in a plain text file, one report per line, fields separated
 * by '|'. Special characters inside fields are escaped so that any text the
 * user types can be stored safely.
 *
 * Line layout (14 fields):
 * type|id|name|description|category|location|date|status|matchedWith|
 * personName|studentId|phone|email|storedAt
 */
public class FileItemRepository implements ItemRepository {

    private static final int FIELD_COUNT = 14;
    private static final String HEADER =
            "# type|id|name|description|category|location|date|status|matchedWith|"
                    + "personName|studentId|phone|email|storedAt";

    private final String path;

    public FileItemRepository(String path) {
        this.path = path;
    }

    // ------------------------------------------------------------------ save

    @Override
    public void saveAll(Collection<Item> items) throws StorageException {
        File target = new File(path);
        File temp = new File(path + ".tmp");
        try {
            FileUtil.ensureParentDirectory(target);
            // write to a temporary file first so a crash cannot corrupt the real data file
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
                writer.write(HEADER);
                writer.newLine();
                for (Item item : items) {
                    writer.write(toLine(item));
                    writer.newLine();
                }
            }
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Could not save data to '" + path + "': " + e.getMessage(), e);
        }
    }

    private String toLine(Item item) {
        Person p = item.getContact();
        String storedAt = (item instanceof FoundItem) ? ((FoundItem) item).getStoredAt() : "";
        String matched = item.getMatchedWithId() == null ? "" : item.getMatchedWithId();
        String[] fields = {
                item.getType(), item.getId(), item.getName(), item.getDescription(),
                item.getCategory().name(), item.getLocation(), item.getDate().toString(),
                item.getStatus().name(), matched,
                p.getName(), p.getStudentId(), p.getPhone(), p.getEmail(), storedAt
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ load

    @Override
    public List<Item> loadAll() throws StorageException {
        List<Item> items = new ArrayList<Item>();
        File file = new File(path);
        if (!file.exists()) {
            return items; // first run - nothing saved yet
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue; // skip blank lines and the header comment
                }
                items.add(parseLine(line, lineNumber));
            }
        } catch (IOException e) {
            throw new StorageException("Could not read data from '" + path + "': " + e.getMessage(), e);
        }
        return items;
    }

    private Item parseLine(String line, int lineNumber) throws StorageException {
        String[] f = line.split("\\|", -1);
        if (f.length != FIELD_COUNT) {
            throw new StorageException("Corrupt data at line " + lineNumber + ": expected "
                    + FIELD_COUNT + " fields but found " + f.length + ".");
        }
        try {
            String type = f[0];
            String id = unescape(f[1]);
            String name = unescape(f[2]);
            String description = unescape(f[3]);
            ItemCategory category = ItemCategory.valueOf(f[4]);
            String location = unescape(f[5]);
            LocalDate date = LocalDate.parse(f[6]);
            ItemStatus status = ItemStatus.valueOf(f[7]);
            String matchedWith = unescape(f[8]);
            Person person = new Person(unescape(f[9]), unescape(f[10]), unescape(f[11]), unescape(f[12]));

            Item item;
            if ("LOST".equals(type)) {
                item = new LostItem(id, name, description, category, location, date, person);
            } else if ("FOUND".equals(type)) {
                item = new FoundItem(id, name, description, category, location, date, person, unescape(f[13]));
            } else {
                throw new StorageException("Corrupt data at line " + lineNumber + ": unknown type '" + type + "'.");
            }
            item.setStatus(status);
            if (!matchedWith.isEmpty()) {
                item.setMatchedWithId(matchedWith);
            }
            return item;
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new StorageException("Corrupt data at line " + lineNumber + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------- escape helpers

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    private static String unescape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                if (next == 'p') {
                    sb.append('|');
                } else if (next == 'n') {
                    sb.append('\n');
                } else {
                    sb.append(next); // "\\\\" becomes a single backslash
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
