package osproject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
    private JTextField urlTextField;
    private JTextArea outputTextArea;
    private CrawlWorker crawlWorker;

    /**
     * Constructs a WebCrawlerGUI object and initializes the GUI components.
    */
    public WebCrawlerGUI() {
        super("Web Crawler");
        setLayout(new BorderLayout());

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        urlTextField = new JTextField("http://example.com");
        outputTextArea = new JTextArea();

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(new JLabel("Root URL: "), BorderLayout.WEST);
        inputPanel.add(urlTextField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                outputTextArea.setText("");
                String rootUrl = urlTextField.getText().trim();
                crawlWorker = new CrawlWorker(rootUrl);
                crawlWorker.execute();
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    /**
     * Crawls the given URL and extracts information about the page.
     *
     * @param url The URL to crawl.
     */

    @SuppressWarnings("checkstyle:methodlength")
    private void crawl(String url) {
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

        Elements links = getLinks(url);
        for (Element link : links) {
            String nextUrl = getAbsoluteUrl(link);
            if (!nextUrl.isEmpty() && !crawlWorker.isCancelled()) {
                crawl(nextUrl);
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

            SwingUtilities.invokeLater(() -> {
                outputTextArea.append("Title: " + title + "\n");
                outputTextArea.append("Description: " + description + "\n");
                outputTextArea.append("Keywords: " + keywords + "\n");
                outputTextArea.append("URL: " + url + "\n\n");
                outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
            });
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
        }

        @Override
        protected Void doInBackground() {
            crawl(rootUrl);
            return null;
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
}
