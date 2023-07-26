package osproject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.LineBorder;
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
    private JButton pauseButton;
    private JButton resumeButton;
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
    @SuppressWarnings("checkstyle:MagicNumber")
    public WebCrawlerGUI() {
        super("Web Crawler");
        setLayout(new BorderLayout());

        depthTextField = new JTextField("1"); // Default depth value of 1
        depthTextField.setBackground(Color.decode("#CCCCCC")); // Set the background color to gray
        depthLabel = new JLabel("Depth:");
        // Initialize the visitedUrls set here
        visitedUrls = new HashSet<>();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        exportButton = new JButton("Export Data");
        urlTextField = new JTextField("http://google.com");
        urlTextField.setBackground(Color.decode("#CCCCCC")); // Set the background color to gray
        outputTextArea = new JTextArea();
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        pauseButton.setEnabled(false);
        resumeButton.setEnabled(false);
        statusLabel = new JLabel("<html>Status: <font color='red'><b>Not Crawling</b></font></html>");
        statusLabel.setBackground(Color.decode("#CCCCCC")); // Set the background color to gray
        statusLabel.setOpaque(true); // Set the label to be opaque to show the background color

        JPanel inputPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final int dimensions = 5;
        gbc.insets = new Insets(dimensions, dimensions, dimensions, dimensions);
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel rootUrl = new JLabel("Root URL:");
        inputPanel.add(rootUrl, gbc);
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
        buttonPanel.add(resumeButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(exportButton);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        //Customization center (keeping everything right here so its easy to change):
        depthLabel.setForeground(Color.WHITE);
        rootUrl.setForeground(Color.WHITE);
        String mainColor = "#017058";
        buttonPanel.setBackground(Color.decode(mainColor)); //bottom panel with the buttons
        inputPanel.setBackground(Color.decode(mainColor));  //top panel with the text fields
        outputTextArea.setBackground(Color.decode("#69A297"));  //window where all the entries are printed
        LineBorder colorfulBorder = new LineBorder(Color.decode("#69A297"), 6);  //border around window
        getRootPane().setBorder(colorfulBorder);

        depthTextField.addActionListener(new DepthInputListener());

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                pauseButton.setEnabled(true);
                resumeButton.setEnabled(false);
                outputTextArea.setText("");
                // Clear previous crawl data
                visitedUrls.clear();
                websiteInfoList.clear();

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

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (crawlWorker != null) {
                    crawlWorker.pause();
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    statusLabel.setText("<html>Status: <font color='orange'><b>Paused</b></font></html>");
                }
            }
        });

        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (crawlWorker != null) {
                    crawlWorker.resume();
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    statusLabel.setText("<html>Status: <font color='green'><b>Crawling</b></font></html>");
                }
            }
        });

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
     * Processes the page at the given URL and extracts information.
     *
     * @param url The URL of the page to process.
     */

    private void processPage(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            String originalTitle = document.title().trim();
            String description = document.select("meta[name=description]").attr("content");
            String keywords = document.select("meta[name=keywords]").attr("content");

            // Check if keywords and description are blank
            if (keywords.isEmpty() && description.isEmpty()) {
                // Extract information from the title tag
                String[] titleParts = originalTitle.split(" - ");
                if (titleParts.length > 1) {
                    final String extractedKeywords = titleParts[0].trim();
                    keywords = extractedKeywords;
                    final String extractedDescription = titleParts[1].trim();
                    description = extractedDescription;
                } else {
                    final String extractedDescription = originalTitle.trim();
                    description = extractedDescription;
                }
            }

            // Replace empty values with "N/A"
            final String title = originalTitle.isEmpty() ? "N/A" : originalTitle;
            description = description.isEmpty() ? "N/A" : description;
            keywords = keywords.isEmpty() ? "N/A" : keywords;

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
    @SuppressWarnings("checkstyle:NestedIfDepth")
    private class CrawlWorker extends SwingWorker<Void, Void> {
        private String rootUrl;
        private boolean isPaused;
        private Object pauseLock = new Object(); // New pause lock object

        CrawlWorker(String rootUrl) {
            this.rootUrl = rootUrl;
            currentDepth = Integer.parseInt(depthTextField.getText()); // Set the initial depth value
            this.isPaused = false;
        }

        @Override
        protected Void doInBackground() {
            crawl(rootUrl, currentDepth); // Start the crawling process
            return null;
        }

        private void crawl(String url, int maxDepth) {
            if (isCancelled()) {
                return;
            }

            if (isJavaScriptPage(url) || isLoginPage(url)) {
                return; // Skip JavaScript pages and login pages
            }

            // Fetch and parse robots.txt file
            if (!isAllowedByRobotsTxt(url)) {
                return;
            }

            processPage(url);

            if (visitedUrls.size() >= MAX_PAGES || isCancelled()) {
                stopButton.setEnabled(false);
                return;
            }

            if (maxDepth > 0) {
                Elements links = getLinks(url);
                for (Element link : links) {
                    String nextUrl = getAbsoluteUrl(link);
                    if (!nextUrl.isEmpty()) {
                        if (!visitedUrls.contains(nextUrl) && !isCancelled()) {
                            visitedUrls.add(nextUrl);
                            crawl(nextUrl, maxDepth - 1); // Recursive call to crawl the next URL
                        }
                    }
                }
            }

            // Check if the crawl is paused
            synchronized (pauseLock) {
                while (isPaused && !isCancelled()) {
                    statusLabel.setText("<html>Status: <font color='orange'><b>Paused</b></font></html>");
                    try {
                        pauseLock.wait(); // Wait until the worker is resumed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Update status after resuming
            if (!isCancelled()) {
                String htmlStatus = "<html>Status: <font color='green'><b>Crawling</b></font></html>";
                statusLabel.setText(htmlStatus);
            }
        }

        private boolean isJavaScriptPage(String url) {
            // Add checks to detect JavaScript pages
            return url.contains(".js");
        }

        private boolean isLoginPage(String url) {
            // Add checks to detect login pages
            // If the URL points to a login page, return true, else return false.
            return url.contains("login") || url.contains("signin");
        }

        private boolean isAllowedByRobotsTxt(String url) {
            try {
                URL robotsUrl = new URL(url + "/robots.txt");
                HttpURLConnection connection = (HttpURLConnection) robotsUrl.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Check if the robots.txt file contains rules for the user-agent
                        if (line.startsWith("User-agent: *")) {
                            while ((line = reader.readLine()) != null) {
                                // Check if the user-agent is allowed to access the URL
                                if (line.startsWith("Disallow: ")) {
                                    String disallowedPath = line.substring("Disallow: ".length());
                                    if (url.contains(disallowedPath)) {
                                        return false; // URL is disallowed by robots.txt
                                    }
                                }
                            }
                        }
                    }
                    reader.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                // Handle MalformedURLException
            } catch (IOException e) {
                e.printStackTrace();
                // Handle IOException
            }

            return true; // If no robots.txt is found or no disallow rules match, assume URL is allowed
        }

        void pause() {
            isPaused = true;
        }

        void resume() {
            isPaused = false;
            synchronized (pauseLock) {
                pauseLock.notifyAll(); // Notify the worker thread to resume
            }
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false); // Disable the "Pause" button when the crawling is done
            resumeButton.setEnabled(false); // Disable the "Resume" button when the crawling is done
            String htmlStatus = "<html>Status: <font color='red'><b>Not Crawling</b></font></html>";
            statusLabel.setText(htmlStatus);
            isPaused = false; // Reset the pause status when the crawling is done
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
