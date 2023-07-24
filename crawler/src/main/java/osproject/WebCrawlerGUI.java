package osproject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A simple web crawler that records information about the links it visits.
*/
public class WebCrawlerGUI extends JFrame {
    private static final int MAX_PAGES = 100;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private Set<String> visitedUrls;
    private JButton startButton;
    private JButton stopButton;
    private JButton exportButton;
    private JTextField urlTextField;
    private JTextArea outputTextArea;
    private CrawlWorker crawlWorker;
    private Gson gson;
    private JTextField depthTextField;
    private JLabel depthLabel;
    private int currentDepth;
    private JLabel statusLabel;
    private ArrayList<WebsiteInfo> websiteInfoList = new ArrayList<>();

    /**
    * Represents the information about a website.
    */
    private static class WebsiteInfo {
        private String title;
        private String description;
        private String keywords;
        private String url;

        WebsiteInfo(String title, String description, String keywords, String url) {
            this.title = title;
            this.description = description;
            this.keywords = keywords;
            this.url = url;
        }
    }

    /**
     * Constructs a WebCrawlerGUI object and initializes the GUI components.
    */
    public WebCrawlerGUI() {
        super("Web Crawler");
        setLayout(new BorderLayout());

        depthTextField = new JTextField("1"); // Default depth value of 1
        depthLabel = new JLabel("Depth:");

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        exportButton = new JButton("Export Data");
        urlTextField = new JTextField("http://google.com");
        outputTextArea = new JTextArea();
        statusLabel = new JLabel("<html>Status: <font color='red'><b>Not Crawling</b></font></html>");


        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final int dimensions = 5;
        gbc.insets = new Insets(dimensions, dimensions, dimensions, dimensions);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Root URL:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        inputPanel.add(urlTextField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        inputPanel.add(depthLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        inputPanel.add(depthTextField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        inputPanel.add(statusLabel, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(exportButton);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        depthTextField.addActionListener(new DepthInputListener());

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                outputTextArea.setText("");
                String rootUrl = urlTextField.getText().trim();
                crawlWorker = new CrawlWorker(rootUrl);
                crawlWorker.execute();
                statusLabel.setText("<html>Status: <font color='green'><b>Crawling</b></font></html>");
            }

        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (crawlWorker != null) {
                    crawlWorker.cancel(true);
                }
                stopButton.setEnabled(false);
            }
        });
        stopButton.setEnabled(false);

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportData();
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setVisible(true);

        gson = new GsonBuilder().setPrettyPrinting().create();
        websiteInfoList = new ArrayList<>();
    }
    /**
     * Crawls the given URL and extracts information about the page.
     *
     * @param url The URL to crawl.
     */

    @SuppressWarnings("checkstyle:methodlength")
    private void crawl(String url, int depth) {
        if (visitedUrls == null) {
            visitedUrls = new HashSet<>();
        }

        if (visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);
        processPage(url);

        if (visitedUrls.size() >= MAX_PAGES || crawlWorker.isCancelled()) {
            stopButton.setEnabled(false);
            return;
        }

        if (depth <= 0 || visitedUrls.size() >= MAX_PAGES || crawlWorker.isCancelled()) {
            stopButton.setEnabled(false);
            return;
        }

        Elements links = getLinks(url);
        for (Element link : links) {
            String nextUrl = getAbsoluteUrl(link);
            if (!nextUrl.isEmpty() && !crawlWorker.isCancelled()) {
                crawl(nextUrl, depth - 1);
            }
        }
    }
    /**
     * Processes the page at the given URL and extracts information.
     *
     * @param url The URL of the page to process.
     */

    private void processPage(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            String title = document.title();
            String description = document.select("meta[name=description]").attr("content");
            String keywords = document.select("meta[name=keywords]").attr("content");

            // Check if keywords and description are blank
            if (keywords.isEmpty() && description.isEmpty()) {
                // Extract information from the title tag
                String[] titleParts = title.split(" - ");
                if (titleParts.length > 1) {
                    final String extractedKeywords = titleParts[0].trim();
                    keywords = extractedKeywords;
                    final String extractedDescription = titleParts[1].trim();
                    description = extractedDescription;
                } else {
                    final String extractedDescription = title.trim();
                    description = extractedDescription;
                }
            }

            final String finalKeywords = keywords; // Declare a final variable for the lambda expression
            final String finalDescription = description; // Declare a final variable for the lambda expression

            SwingUtilities.invokeLater(() -> {
                outputTextArea.append("Title: " + title + "\n");
                outputTextArea.append("Description: " + finalDescription + "\n");
                outputTextArea.append("Keywords: " + finalKeywords + "\n");
                outputTextArea.append("URL: " + url + "\n\n");
                outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
            });

            WebsiteInfo websiteInfo = new WebsiteInfo(title, description, keywords, url);
            websiteInfoList.add(websiteInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the links from the page at the given URL.
     *
     * @param url The URL of the page.
     * @return The links found on the page.
     */
    private Elements getLinks(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            return document.select("a[href]");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Elements();
    }

    /**
     * Retrieves the absolute URL from the given link element.
     *
     * @param link The link element.
     * @return The absolute URL.
     */
    private String getAbsoluteUrl(Element link) {
        return link.absUrl("href");
    }

    /**
     * SwingWorker class to perform the web crawling process in the background.
     */
    private class CrawlWorker extends SwingWorker<Void, Void> {
        private String rootUrl;

        CrawlWorker(String rootUrl) {
            this.rootUrl = rootUrl;
            currentDepth = Integer.parseInt(depthTextField.getText()); // Set the initial depth value
        }

        @Override
        protected Void doInBackground() {
            crawl(rootUrl, currentDepth); // Pass the depth value to the crawling process
            return null;
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("<html>Status: <font color='red'><b>Not Crawling</b></font></html>");
        }
    }

    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Data");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            String message = "";
            try (FileWriter writer = new FileWriter(filePath)) {
                String json = gson.toJson(websiteInfoList);
                writer.write(json);
                writer.flush();
                message = "Data exported successfully!";
                JOptionPane.showMessageDialog(this, message, "Export Data", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                message = "Error exporting data.";
                JOptionPane.showMessageDialog(this, "Error exporting data!", "Export Data", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class DepthInputListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                currentDepth = Integer.parseInt(depthTextField.getText());
            } catch (NumberFormatException ex) {
                // Handle the case where the user entered a non-integer value
                currentDepth = 1; // Set a default depth value
                depthTextField.setText("1");
            }
        }
    }
}
