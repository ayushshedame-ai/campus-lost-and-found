package com.campus.lostfound.util;

import java.io.File;
import java.io.IOException;

/**
 * Small helpers for file handling.
 */
public final class FileUtil {

    private FileUtil() {
        // utility class - no instances
    }

    /** Creates the parent folder of the file (e.g. "data/") if it does not exist yet. */
    public static void ensureParentDirectory(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create folder: " + parent);
        }
    }
}
