package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenameIsoFiles {
    public static void main(String[] args) {
        // Redirect console output to a ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        // Get the current directory of the executable or jar file
        Path currentPath = Paths.get("").toAbsolutePath();
        String isoDirectoryPath = currentPath.toString();

        // HashMap to store the extracted data
        Map<String, String> dataMap = new HashMap<>();

        // Lists for successful and unsuccessful mappings
        List<String> renamedFiles = new ArrayList<>();
        List<String> unmappedFiles = new ArrayList<>();

        try {
            // Load the HTML
            String htmlFilePath = "https://psxdatacenter.com/psx2/ulist2.html";
            Document doc = Jsoup.connect(htmlFilePath).get();

            // Loop through the desired tables
            for (int i = 302; i <= 354; i += 2) { // Start with table302 and process every second table
                // Retrieve the previous table (e.g., table301 for table302)
                String previousTableId = "table" + (i - 1);
                Element previousTable = doc.getElementById(previousTableId);

                // Process the current table (e.g., table302)
                String tableId = "table" + i;
                Element table = doc.getElementById(tableId);

                int gameCount = 0; // Counter for games in the current table

                if (table != null) {
                    // Retrieve all rows from the table
                    Elements rows = table.select("tr");

                    for (Element row : rows) {
                        Elements cols = row.select("td");

                        // Only extract the first and second columns
                        if (cols.size() >= 2) { // Ensure there are enough columns
                            String key = cols.get(1).text().replace("\u00A0", " ").trim(); // First column (SERIAL)
                            String value = cols.get(2).text().replace("\u00A0", " ").trim(); // Second column (TITLE)

                            if (!key.isEmpty() && !value.isEmpty()) { // Ignore empty values
                                dataMap.put(key, value);
                                gameCount++; // Increment game count for each valid row
                            }
                        }
                    }
                }

                if (previousTable != null) {
                    // Extract the text in the th.sectionheader2 element
                    Element headerElement = previousTable.selectFirst("th.sectionheader2");
                    if (headerElement != null) {
                        String headerText = headerElement.text().replace("\u00A0", " ").trim();
                        // Append game count to the header
                        System.out.println("Processing table with " + gameCount + " Games: " + headerText);
                    }
                }

                // Add a blank line only after the last previous table
                if (i == 354) {
                    System.out.println();
                }
            }

            // Process all .iso files in the current directory
            File isoDirectory = new File(isoDirectoryPath);
            if (isoDirectory.exists() && isoDirectory.isDirectory()) {
                File[] isoFiles = isoDirectory.listFiles((dir, name) -> name.endsWith(".iso"));

                if (isoFiles != null) {
                    for (File isoFile : isoFiles) {
                        String originalName = isoFile.getName().replace(".iso", "").trim(); // File name without extension

                        // Check if the file name exists in the HashMap
                        if (dataMap.containsKey(originalName)) {
                            String newName = dataMap.get(originalName).replace(":", "-").replace("/", "-").trim() + ".iso"; // New name from the second column
                            File renamedFile = new File(isoDirectoryPath + File.separator + newName);

                            // Rename the file
                            if (isoFile.renameTo(renamedFile)) {
                                renamedFiles.add("Renamed: " + isoFile.getName() + " -> " + renamedFile.getName());
                            } else {
                                System.err.println("Error renaming: " + isoFile.getName());
                            }
                        } else {
                            unmappedFiles.add("No mapping found for: " + isoFile.getName());
                        }
                    }

                    // Output renamed files
                    if (!renamedFiles.isEmpty()) {
                        System.out.println("### Renamed Files ###");
                        renamedFiles.forEach(System.out::println);
                        System.out.println(); // Blank line after renamed section
                    }

                    // Output files with no mapping
                    if (!unmappedFiles.isEmpty()) {
                        System.out.println("### Files with No Mapping ###");
                        unmappedFiles.forEach(System.out::println);
                        System.out.println(); // Blank line after unmapped section
                    }
                } else {
                    System.err.println("No .iso files found in the directory.");
                }
            } else {
                System.err.println("The current directory does not exist or is not a directory.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Show console output in a GUI window
        SwingUtilities.invokeLater(() -> {
            JTextArea textArea = new JTextArea(baos.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(null, scrollPane, "Program Output", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
