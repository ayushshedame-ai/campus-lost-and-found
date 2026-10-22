package com.campus.lostfound.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.LostItem;
import com.campus.lostfound.model.MatchResult;
import com.campus.lostfound.model.Person;

/**
 * Beginner-friendly JSON utility written in pure standard Java.
 *
 * <p>Why pure Java? Students don't need to install external libraries like Jackson or Gson.
 * Everything runs directly out-of-the-box with any standard Java JDK.</p>
 */
public final class SimpleJson {

    private SimpleJson() {
        // Utility class
    }

    // =========================================================================
    // JSON Serialization (Java Objects -> JSON Strings)
    // =========================================================================

    /** Escapes special characters so strings are valid JSON literals. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Serializes a Person object into JSON. */
    public static String toJson(Person p) {
        if (p == null) {
            return "null";
        }
        return "{"
                + "\"name\":\"" + escape(p.getName()) + "\","
                + "\"studentId\":\"" + escape(p.getStudentId()) + "\","
                + "\"phone\":\"" + escape(p.getPhone()) + "\","
                + "\"email\":\"" + escape(p.getEmail()) + "\""
                + "}";
    }

    /** Serializes an Item (LostItem or FoundItem) into JSON. */
    public static String toJson(Item item) {
        if (item == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(item.getId())).append("\",");
        sb.append("\"type\":\"").append(escape(item.getType())).append("\",");
        sb.append("\"name\":\"").append(escape(item.getName())).append("\",");
        sb.append("\"description\":\"").append(escape(item.getDescription())).append("\",");
        sb.append("\"category\":\"").append(escape(item.getCategory().name())).append("\",");
        sb.append("\"categoryLabel\":\"").append(escape(item.getCategory().getLabel())).append("\",");
        sb.append("\"location\":\"").append(escape(item.getLocation())).append("\",");
        sb.append("\"date\":\"").append(item.getDate().toString()).append("\",");
        sb.append("\"status\":\"").append(item.getStatus().name()).append("\",");
        sb.append("\"statusLabel\":\"").append(escape(item.getStatus().getLabel())).append("\",");
        sb.append("\"contactRole\":\"").append(escape(item.getContactRole())).append("\",");

        if (item.getMatchedWithId() != null) {
            sb.append("\"matchedWithId\":\"").append(escape(item.getMatchedWithId())).append("\",");
        } else {
            sb.append("\"matchedWithId\":null,");
        }

        if (item instanceof FoundItem) {
            sb.append("\"storedAt\":\"").append(escape(((FoundItem) item).getStoredAt())).append("\",");
        } else {
            sb.append("\"storedAt\":null,");
        }

        sb.append("\"contact\":").append(toJson(item.getContact()));
        sb.append("}");
        return sb.toString();
    }

    /** Serializes a list of items to a JSON array string. */
    public static String itemsToJson(List<? extends Item> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(items.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /** Serializes a MatchResult object to JSON. */
    public static String toJson(MatchResult match) {
        if (match == null) {
            return "null";
        }
        return "{"
                + "\"score\":" + match.getScore() + ","
                + "\"lostItem\":" + toJson(match.getLostItem()) + ","
                + "\"foundItem\":" + toJson(match.getFoundItem())
                + "}";
    }

    /** Serializes a list of MatchResult objects to JSON. */
    public static String matchesToJson(List<MatchResult> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(matches.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /** Returns a standard JSON error response: {"success":false,"error":"..."} */
    public static String errorJson(String message) {
        return "{\"success\":false,\"error\":\"" + escape(message) + "\"}";
    }

    /** Returns a standard JSON success response: {"success":true,"message":"..."} */
    public static String successJson(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    // =========================================================================
    // JSON Deserialization (Ultra-forgiving parser for students & API requests)
    // =========================================================================

    /**
     * Parses a JSON object string into a key-value Map.
     * Supports standard quoted JSON (`{"key":"val"}`), unquoted keys (`{key:"val"}`),
     * and single-quoted values (`{'key':'val'}`).
     */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<String, String>();
        if (json == null) {
            return map;
        }
        String text = json.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            return map;
        }

        int i = 1;
        int len = text.length() - 1;

        while (i < len) {
            // Skip whitespace & commas
            while (i < len && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == ',')) {
                i++;
            }
            if (i >= len || text.charAt(i) == '}') {
                break;
            }

            // Read Key (quoted or unquoted)
            String key;
            char quote = text.charAt(i);
            if (quote == '"' || quote == '\'') {
                i++; // skip quote
                int start = i;
                while (i < len && text.charAt(i) != quote) {
                    if (text.charAt(i) == '\\') i++;
                    i++;
                }
                key = text.substring(start, i);
                if (i < len && text.charAt(i) == quote) i++;
            } else {
                int start = i;
                while (i < len && text.charAt(i) != ':' && !Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                key = text.substring(start, i).trim();
            }

            // Skip whitespace & colon ':'
            while (i < len && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == ':')) {
                i++;
            }

            // Read Value
            if (i >= len || text.charAt(i) == '}') {
                break;
            }

            String val = null;
            quote = text.charAt(i);
            if (quote == '"' || quote == '\'') {
                i++; // skip quote
                StringBuilder valBuilder = new StringBuilder();
                while (i < len && text.charAt(i) != quote) {
                    if (text.charAt(i) == '\\' && i + 1 < len) {
                        i++;
                        char escaped = text.charAt(i);
                        if (escaped == 'n') valBuilder.append('\n');
                        else if (escaped == 'r') valBuilder.append('\r');
                        else if (escaped == 't') valBuilder.append('\t');
                        else valBuilder.append(escaped);
                    } else {
                        valBuilder.append(text.charAt(i));
                    }
                    i++;
                }
                val = valBuilder.toString();
                if (i < len && text.charAt(i) == quote) i++;
            } else {
                // Non-quoted value (number, boolean, null, or raw word)
                int start = i;
                while (i < len && text.charAt(i) != ',' && text.charAt(i) != '}') {
                    i++;
                }
                val = text.substring(start, i).trim();
                if ("null".equalsIgnoreCase(val)) {
                    val = null;
                }
            }

            if (key != null && !key.isEmpty()) {
                map.put(key, val);
            }
        }
        return map;
    }
}
