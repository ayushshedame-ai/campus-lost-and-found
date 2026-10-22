package com.campus.lostfound;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.campus.lostfound.exception.InvalidInputException;
import com.campus.lostfound.exception.ItemNotFoundException;
import com.campus.lostfound.exception.MatchException;
import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemCategory;
import com.campus.lostfound.model.LostItem;
import com.campus.lostfound.model.MatchResult;
import com.campus.lostfound.model.Person;
import com.campus.lostfound.service.DemoData;
import com.campus.lostfound.service.KeywordMatchStrategy;
import com.campus.lostfound.service.LostFoundService;
import com.campus.lostfound.storage.FileItemRepository;
import com.campus.lostfound.util.InputParser;
import com.campus.lostfound.util.Validator;

/**
 * Console user interface for the Campus Lost &amp; Found Management System.
 * Run this class (Run As -&gt; Java Application) to start the program.
 */
public class Main {

    private static final String DATA_FILE = "data" + File.separator + "lostfound.csv";
    private static final String REPORT_FILE = "data" + File.separator + "report.txt";

    private final Scanner scanner = new Scanner(System.in);
    private final LostFoundService service;

    public Main(LostFoundService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--web")) {
            com.campus.lostfound.web.ServerMain.main(args);
            return;
        }

        try {
            LostFoundService service = new LostFoundService(
                    new FileItemRepository(DATA_FILE), new KeywordMatchStrategy());

            if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
                new Main(service).run();
                return;
            }

            Scanner startupScanner = new Scanner(System.in);
            System.out.println("=====================================================");
            System.out.println("   CAMPUS LOST & FOUND MANAGEMENT SYSTEM");
            System.out.println("=====================================================");
            System.out.println("Select your mode:");
            System.out.println("  1. Launch Full-Stack Web App (Frontend + REST Backend) [Recommended]");
            System.out.println("  2. Launch Classic Console Terminal Menu");
            System.out.print("Enter choice (1 or 2, default 1): ");

            String input = "";
            if (startupScanner.hasNextLine()) {
                input = startupScanner.nextLine().trim();
            }

            if ("2".equals(input)) {
                new Main(service).run();
            } else {
                com.campus.lostfound.web.ServerMain.main(args);
            }
        } catch (StorageException e) {
            System.err.println("Could not start: " + e.getMessage());
            System.err.println("Fix or delete the file '" + DATA_FILE + "' and try again.");
        }
    }

    // ---------------------------------------------------------------- menu

    public void run() {
        System.out.println("=====================================================");
        System.out.println("   CAMPUS LOST & FOUND MANAGEMENT SYSTEM");
        System.out.println("=====================================================");
        boolean running = true;
        while (running) {
            printMenu();
            try {
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        reportLost();
                        break;
                    case "2":
                        reportFound();
                        break;
                    case "3":
                        System.out.println("\n--- All Lost Items ---");
                        printItems(service.getAllLostItems());
                        break;
                    case "4":
                        System.out.println("\n--- All Found Items ---");
                        printItems(service.getAllFoundItems());
                        break;
                    case "5":
                        viewDetails();
                        break;
                    case "6":
                        searchItems();
                        break;
                    case "7":
                        findMatches();
                        break;
                    case "8":
                        confirmMatch();
                        break;
                    case "9":
                        claimItem();
                        break;
                    case "10":
                        System.out.println("\n--- Statistics ---");
                        System.out.print(service.getStatistics());
                        break;
                    case "11":
                        service.exportReport(REPORT_FILE);
                        System.out.println("Report saved to: " + new File(REPORT_FILE).getAbsolutePath());
                        break;
                    case "12":
                        DemoData.load(service);
                        System.out.println("Sample data added (3 lost + 3 found items).");
                        break;
                    case "13":
                        System.out.println("Starting web server at http://localhost:8080 ...");
                        com.campus.lostfound.web.ServerMain.main(new String[0]);
                        return;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number from the menu.");
                }
            } catch (InvalidInputException | ItemNotFoundException | MatchException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (StorageException e) {
                System.out.println("File error: " + e.getMessage());
            } catch (NoSuchElementException e) {
                running = false; // input stream closed
            }
        }
        System.out.println("Goodbye!");
        scanner.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1.  Report a LOST item");
        System.out.println("2.  Report a FOUND item");
        System.out.println("3.  View all lost items");
        System.out.println("4.  View all found items");
        System.out.println("5.  View item details (by ID)");
        System.out.println("6.  Search items");
        System.out.println("7.  Find matches for a lost item");
        System.out.println("8.  Confirm a match");
        System.out.println("9.  Claim / return an item to its owner");
        System.out.println("10. Statistics");
        System.out.println("11. Export report to file");
        System.out.println("12. Load sample data");
        System.out.println("13. Switch to Web Application (Browser UI)");
        System.out.println("0.  Exit");
        System.out.print("Enter your choice: ");
    }

    // ------------------------------------------------------------- actions

    private void reportLost() throws InvalidInputException, StorageException, ItemNotFoundException {
        System.out.println("\n--- Report Lost Item ---");
        String name = ask("Item name: ", s -> Validator.requireNonBlank(s, "Item name"));
        String description = ask("Description (colour, brand, marks): ",
                s -> Validator.requireNonBlank(s, "Description"));
        ItemCategory category = askCategory();
        String location = ask("Where was it lost? ", s -> Validator.requireNonBlank(s, "Location"));
        LocalDate date = ask("Date lost (" + Validator.DATE_PATTERN + "): ", Validator::parseDate);
        Person owner = askPerson("Owner");

        LostItem item = service.reportLost(name, description, category, location, date, owner);
        System.out.println("\nLost item reported successfully. Your report ID is " + item.getId());

        List<MatchResult> matches = service.findMatchesForLost(item.getId());
        if (matches.isEmpty()) {
            System.out.println("No matching found items yet. Check again later (option 7).");
        } else {
            System.out.println("Possible matches already reported as found:");
            printMatches(matches);
        }
    }

    private void reportFound() throws InvalidInputException, StorageException, ItemNotFoundException {
        System.out.println("\n--- Report Found Item ---");
        String name = ask("Item name: ", s -> Validator.requireNonBlank(s, "Item name"));
        String description = ask("Description (colour, brand, marks): ",
                s -> Validator.requireNonBlank(s, "Description"));
        ItemCategory category = askCategory();
        String location = ask("Where was it found? ", s -> Validator.requireNonBlank(s, "Location"));
        LocalDate date = ask("Date found (" + Validator.DATE_PATTERN + "): ", Validator::parseDate);
        String storedAt = ask("Where is it kept now (e.g. Security Office)? ",
                s -> Validator.requireNonBlank(s, "Storage location"));
        Person finder = askPerson("Finder");

        FoundItem item = service.reportFound(name, description, category, location, date, finder, storedAt);
        System.out.println("\nFound item reported successfully. Report ID is " + item.getId());

        List<MatchResult> matches = service.findMatchesForFound(item.getId());
        if (matches.isEmpty()) {
            System.out.println("No matching lost reports yet.");
        } else {
            System.out.println("Possible owners (lost reports that look similar):");
            printMatches(matches);
        }
    }

    private void viewDetails() throws ItemNotFoundException {
        String id = prompt("Enter item ID (e.g. L001 or F001): ");
        Item item = service.getItemById(id);
        System.out.println();
        System.out.print(item);
    }

    private void searchItems() {
        String keyword = prompt("Enter a keyword (name, location, category, person...): ");
        List<Item> results = service.search(keyword);
        System.out.println("\n--- Search results for '" + keyword.trim() + "' ---");
        printItems(results);
    }

    private void findMatches() throws ItemNotFoundException {
        String lostId = prompt("Enter the LOST item ID (e.g. L001): ");
        LostItem lost = service.getLostItem(lostId);
        System.out.println("\nSearching matches for " + lost.getId() + " - " + lost.getName() + " ...");
        List<MatchResult> matches = service.findMatchesForLost(lostId);
        if (matches.isEmpty()) {
            System.out.println("No matching found items at the moment.");
        } else {
            printMatches(matches);
        }
    }

    private void confirmMatch() throws ItemNotFoundException, MatchException, StorageException {
        String lostId = prompt("Enter the LOST item ID: ");
        LostItem lost = service.getLostItem(lostId);

        List<MatchResult> suggestions = service.findMatchesForLost(lostId);
        if (!suggestions.isEmpty()) {
            System.out.println("Suggested matches:");
            printMatches(suggestions);
        }
        String foundId = prompt("Enter the FOUND item ID to link with " + lost.getId() + ": ");
        service.confirmMatch(lostId, foundId);

        FoundItem found = service.getFoundItem(foundId);
        System.out.println("\nMatch confirmed: " + lost.getId() + " <-> " + found.getId());
        System.out.println("Contact the owner : " + lost.getContact());
        System.out.println("Item is kept at   : " + found.getStoredAt());
    }

    private void claimItem() throws ItemNotFoundException, MatchException, StorageException {
        String lostId = prompt("Enter the LOST item ID being claimed: ");
        String studentId = prompt("Enter the owner's student ID for verification: ");
        service.markClaimed(lostId, studentId);
        System.out.println("Item returned to its owner. Both reports are now marked as Claimed.");
    }

    // --------------------------------------------------------- input helpers

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }

    /** Keeps asking until the parser accepts the input. */
    private <T> T ask(String label, InputParser<T> parser) {
        while (true) {
            String line = prompt(label);
            try {
                return parser.parse(line);
            } catch (InvalidInputException e) {
                System.out.println("  ! " + e.getMessage() + " Please try again.");
            }
        }
    }

    private ItemCategory askCategory() {
        ItemCategory[] all = ItemCategory.values();
        System.out.println("Categories:");
        for (int i = 0; i < all.length; i++) {
            System.out.println("  " + (i + 1) + ". " + all[i].getLabel());
        }
        return ask("Choose category (number or name): ", ItemCategory::fromInput);
    }

    private Person askPerson(String role) {
        System.out.println(role + " details:");
        String name = ask("  Name: ", s -> Validator.requireNonBlank(s, "Name"));
        String studentId = ask("  Student ID: ", Validator::validateStudentId);
        String phone = ask("  Phone (10 digits): ", Validator::validatePhone);
        String email = ask("  E-mail: ", Validator::validateEmail);
        return new Person(name, studentId, phone, email);
    }

    // -------------------------------------------------------- output helpers

    private void printItems(List<? extends Item> items) {
        if (items.isEmpty()) {
            System.out.println("  (no records)");
            return;
        }
        System.out.println(Item.SUMMARY_HEADER);
        System.out.println(line(Item.SUMMARY_HEADER.length()));
        for (Item item : items) {
            System.out.println(item.getSummaryLine());
        }
    }

    private void printMatches(List<MatchResult> matches) {
        int rank = 1;
        for (MatchResult match : matches) {
            System.out.println("  " + (rank++) + ". " + match);
        }
    }

    private String line(int length) {
        return new String(new char[length]).replace('\0', '-');
    }
}
