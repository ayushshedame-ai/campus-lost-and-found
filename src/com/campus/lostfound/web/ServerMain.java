package com.campus.lostfound.web;

import java.io.File;

import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.service.KeywordMatchStrategy;
import com.campus.lostfound.service.LostFoundService;
import com.campus.lostfound.storage.FileItemRepository;

/**
 * Main entry point for the Web Server.
 *
 * <p>Run this class to launch the full-stack web application (backend + frontend).
 * Students can then open their web browser at http://localhost:8080.</p>
 */
public class ServerMain {

    public static final int DEFAULT_PORT = 8080;
    private static final String DATA_FILE = "data" + File.separator + "lostfound.csv";
    private static final String WEB_DIR = "web";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            File webFolder = new File(WEB_DIR);
            if (!webFolder.exists()) {
                // If run from an IDE subfolder, check parent
                File parentWeb = new File(".." + File.separator + WEB_DIR);
                if (parentWeb.exists()) {
                    webFolder = parentWeb;
                }
            }

            LostFoundService service = new LostFoundService(
                    new FileItemRepository(DATA_FILE), new KeywordMatchStrategy());

            WebServer server = new WebServer(port, service, webFolder);
            server.start();

            System.out.println(">> Press Ctrl+C in terminal to stop the server.");
        } catch (StorageException e) {
            System.err.println("Database storage error: " + e.getMessage());
            System.err.println("Check or remove the data file: " + DATA_FILE);
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
