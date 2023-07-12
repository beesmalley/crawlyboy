package osproject;

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
        WebCrawler crawler = new WebCrawler();
        crawler.crawl("https://google.com");
    }

}
