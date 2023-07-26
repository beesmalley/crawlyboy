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
    private JButton pauseButton;
    private JButton resumeButton;
    private Object pauseLock = new Object();
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
        // Initialize the visitedUrls set here
        visitedUrls = new HashSet<>();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        exportButton = new JButton("Export Data");
        urlTextField = new JTextField("http://google.com");
        outputTextArea = new JTextArea();
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        pauseButton.setEnabled(false);
        resumeButton.setEnabled(false);
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
        buttonPanel.add(resumeButton);
        buttonPanel.add(pauseButton);
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
     * Crawls the given URL and extracts information about the page.
     *
     * @param url The URL to crawl.
     */

    @SuppressWarnings("checkstyle:methodlength")
    private void crawl(String rootUrl, int maxDepth) {
        // Use a stack to perform iterative crawling
        ArrayList<CrawlingPage> stack = new ArrayList<>();
        stack.add(new CrawlingPage(rootUrl, 0));

        while (!stack.isEmpty() && !crawlWorker.isCancelled()) {
            CrawlingPage page = stack.remove(stack.size() - 1);
            if (page.depth <= maxDepth && !visitedUrls.contains(page.url)) {
                visitedUrls.add(page.url);
                processPage(page.url);

                if (visitedUrls.size() >= MAX_PAGES || crawlWorker.isCancelled()) {
                    stopButton.setEnabled(false);
                    return;
                }

                Elements links = getLinks(page.url);
                for (Element link : links) {
                    String nextUrl = getAbsoluteUrl(link);
                    if (!nextUrl.isEmpty() && !crawlWorker.isCancelled()) {
                        stack.add(new CrawlingPage(nextUrl, page.depth + 1));
                    }
                }

                // Check if the crawl is paused
                synchronized (pauseLock) {
                    while (crawlWorker.isPaused() && !crawlWorker.isCancelled()) {
                        statusLabel.setText("<html>Status: <font color='orange'><b>Paused</b></font></html>");
                        try {
                            pauseLock.wait(); // Wait until the worker is resumed
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static class CrawlingPage {
        private String url;
        private int depth;

        CrawlingPage(String url, int depth) {
            this.url = url;
            this.depth = depth;
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
        private boolean isPaused;
        private Object pauseLock = new Object(); // New pause lock object

        CrawlWorker(String rootUrl) {
            this.rootUrl = rootUrl;
            currentDepth = Integer.parseInt(depthTextField.getText()); // Set the initial depth value
            this.isPaused = false;
        }

        @Override
        protected Void doInBackground() {
            while (!isCancelled()) {
                if (!isPaused) {
                    crawl(rootUrl, currentDepth); // Pass the depth value to the crawling process
                } else {
                    synchronized (pauseLock) {
                        statusLabel.setText("<html>Status: <font color='orange'><b>Paused</b></font></html>");
                        try {
                            pauseLock.wait(); // Wait until the worker is resumed
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String htmlStatus = "<html>Status: <font color='green'><b>Crawling</b></font></html>";
                    statusLabel.setText(htmlStatus); // Update status after resuming
                }
            }
            return null;
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

        boolean isPaused() {
            return isPaused;
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            pauseButton.setEnabled(false); // Disable the "Pause" button when the crawling is done
            resumeButton.setEnabled(false); // Disable the "Resume" button when the crawling is done
            statusLabel.setText("<html>Status: <font color='red'><b>Not Crawling</b></font></html>");
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
