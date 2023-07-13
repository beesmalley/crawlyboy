package osproject;

import javax.swing.*;
/**
 * Hello world!
*/

public final class App {
    private App() {
    }

    /**
     * Main entry point of the web crawler.
     *
     * @param args Command-line arguments (unused).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new WebCrawlerGUI();
            }
        });
    }

}
