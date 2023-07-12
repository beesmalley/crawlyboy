package osproject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A simple web crawler that records information about the links it visits.
 */
public class WebCrawler {
    private static final int MAX_PAGES = 100;
    private Set<String> visitedUrls;
    /**
     * Crawls the given URL and extracts information about the page.
     *
     * @param url The URL to crawl.
     */

    public void crawl(String url) {
        if (visitedUrls == null) {
            visitedUrls = new HashSet<>();
        }

        if (visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);

        try {
            Document document = Jsoup.connect(url).get();
            String title = document.title();
            String description = document.select("meta[name=description]").attr("content");
            String keywords = document.select("meta[name=keywords]").attr("content");

            System.out.println("Title: " + title);
            System.out.println("Description: " + description);
            System.out.println("Keywords: " + keywords);
            System.out.println("URL: " + url);
            System.out.println();

            Elements links = document.select("a[href]");
            int count = 0;
            for (Element link : links) {
                if (count >= MAX_PAGES) {
                    break;
                }

                String nextUrl = link.absUrl("href");
                if (!nextUrl.isEmpty()) {
                    crawl(nextUrl);
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
