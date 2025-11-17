import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class TagExtractorGUI extends JFrame {

    private JTextArea outputArea;
    private JLabel fileLabel;
    private File textFile;
    private File stopWordsFile;

    private Map<String, Integer> tagMap = new TreeMap<>();

    public TagExtractorGUI() {
        super("Tag / Keyword Extractor");

        // ----- Top panel: file info -----
        JPanel topPanel = new JPanel(new BorderLayout());
        fileLabel = new JLabel("No text file selected");
        topPanel.add(fileLabel, BorderLayout.CENTER);

        // ----- Middle panel: buttons -----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadTextButton = new JButton("Load Text File");
        JButton loadStopButton = new JButton("Load Stop Words");
        JButton extractButton = new JButton("Extract Tags");
        JButton saveButton = new JButton("Save Output");

        buttonPanel.add(loadTextButton);
        buttonPanel.add(loadStopButton);
        buttonPanel.add(extractButton);
        buttonPanel.add(saveButton);

        // ----- Text Area with Scroll -----
        outputArea = new JTextArea(20, 60);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // ----- Layout for frame -----
        this.setLayout(new BorderLayout());
        this.add(topPanel, BorderLayout.NORTH);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(scrollPane, BorderLayout.CENTER);

        // ----- Button Listeners -----

        // Choose main text file
        loadTextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(".");
                int result = chooser.showOpenDialog(TagExtractorGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    textFile = chooser.getSelectedFile();
                    fileLabel.setText("Text file: " + textFile.getName());
                }
            }
        });

        // Choose stop words file
        loadStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(".");
                int result = chooser.showOpenDialog(TagExtractorGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    stopWordsFile = chooser.getSelectedFile();
                    // Show just to confirm; optional
                    JOptionPane.showMessageDialog(
                            TagExtractorGUI.this,
                            "Stop words file: " + stopWordsFile.getName(),
                            "Stop Words Loaded",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        // Run extraction
        extractButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textFile == null) {
                    JOptionPane.showMessageDialog(
                            TagExtractorGUI.this,
                            "Please select a text file first.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                if (stopWordsFile == null) {
                    JOptionPane.showMessageDialog(
                            TagExtractorGUI.this,
                            "Please select a stop words file first.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                try {
                    Set<String> stopWords = loadStopWords(stopWordsFile);
                    tagMap = extractTags(textFile, stopWords);
                    showTags(tagMap);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                            TagExtractorGUI.this,
                            "Error reading files: " + ex.getMessage(),
                            "I/O Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        // Save output to file
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tagMap == null || tagMap.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            TagExtractorGUI.this,
                            "No tags to save. Please run extraction first.",
                            "Nothing to Save",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                JFileChooser chooser = new JFileChooser(".");
                int result = chooser.showSaveDialog(TagExtractorGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File saveFile = chooser.getSelectedFile();
                    try (PrintWriter out = new PrintWriter(new FileWriter(saveFile))) {
                        out.print(outputArea.getText());
                        JOptionPane.showMessageDialog(
                                TagExtractorGUI.this,
                                "Tags saved to " + saveFile.getName(),
                                "Saved",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(
                                TagExtractorGUI.this,
                                "Error saving file: " + ex.getMessage(),
                                "Save Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });

        // ----- Frame settings -----
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(null);  // center on screen
        this.setVisible(true);
    }

    /**
     * Load stop words into a Set (all lowercase, one per line).
     */
    private Set<String> loadStopWords(File stopFile) throws IOException {
        Set<String> stopWords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(stopFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (!word.isEmpty()) {
                    stopWords.add(word);
                }
            }
        }
        return stopWords;
    }

    /**
     * Extract tags from the text file, ignoring stop words.
     * Returns a Map<String, Integer> of word -> frequency.
     */
    private Map<String, Integer> extractTags(File textFile, Set<String> stopWords) throws IOException {
        Map<String, Integer> map = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split on whitespace
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    // Remove non-letters
                    String cleaned = token.replaceAll("[^a-zA-Z]", "").toLowerCase();
                    if (cleaned.isEmpty()) {
                        continue; // skip empty
                    }
                    if (stopWords.contains(cleaned)) {
                        continue; // skip stop words
                    }

                    // Update frequency
                    Integer count = map.get(cleaned);
                    if (count == null) {
                        map.put(cleaned, 1);
                    } else {
                        map.put(cleaned, count + 1);
                    }
                }
            }
        }

        return map;
    }

    /**
     * Display tags and their frequencies in the JTextArea,
     * sorted by frequency (highest first).
     */
    private void showTags(Map<String, Integer> tagMap) {
        // Convert to list for sorting by frequency
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<>(tagMap.entrySet());

        // Sort by frequency descending, then alphabetical
        entries.sort((e1, e2) -> {
            int freqCompare = Integer.compare(e2.getValue(), e1.getValue());
            if (freqCompare != 0) {
                return freqCompare;
            }
            return e1.getKey().compareTo(e2.getKey());
        });

        StringBuilder sb = new StringBuilder();
        sb.append("Tag\tFrequency\n");
        sb.append("-----------------\n");
        for (Map.Entry<String, Integer> entry : entries) {
            sb.append(entry.getKey())
                    .append("\t")
                    .append(entry.getValue())
                    .append("\n");
        }

        outputArea.setText(sb.toString());
        outputArea.setCaretPosition(0); // scroll to top
    }

    public static void main(String[] args) {
        // Use system look-and-feel if possible
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TagExtractorGUI();
            }
        });
    }
}
