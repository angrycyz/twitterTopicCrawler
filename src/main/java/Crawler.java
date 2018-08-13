import javafx.util.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import twitter4j.Twitter;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.Logger;

public class Crawler {
    public static Logger logger = Logger.getLogger("Indexer");
    public static final String HREF= "a[href]";
    public static final String HREF_KEY = "abs:href";
    public static final String SLASH = "/";
    public static final int TIMEOUT = 500;
    public static final String OUTFILE = "output";

    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public Queue<Pair<String, Integer>> getUrlsFromTweets(List<String> searchResults) {
        Queue<Pair<String, Integer>> urls = new LinkedList<>();

        for (String tweet : searchResults) {
            Matcher matcher = urlPattern.matcher(tweet);

            while (matcher.find())
            {
                urls.offer(new Pair<>(tweet.substring(matcher.start(0),
                        matcher.end(0)), 0));
            }

        }
        return urls;
    }

    public Set<String> getUrlsFromPage(String curUrl) {

        Set<String> urlSet = new HashSet<>();
        try {
            Connection con = Jsoup.connect(curUrl);
            con.timeout(TIMEOUT);
            Document document = con.get();
            Elements links = document.select(HREF);

            for (Element link : links) {
                urlSet.add(link.attr(HREF_KEY));
            }

        } catch (Exception e) {
//            logger.info("Content type is not " +
//                    "text/*, application/xml, or application/xhtml+xml," +
//                    "cannot be parsed");
        }

        return urlSet;
    }

    public String getDomain(String curUrl) {
        try {
            URI uri = new URI(curUrl);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getType(String curUrl) {
        try {
            URL url = new URL(curUrl);
            URLConnection con = url.openConnection();
            con.setConnectTimeout(TIMEOUT);
            String contentType = con.getContentType();
            if (contentType == null) {
                return null;
            }
            int slashIndex = contentType.indexOf(SLASH);
            if (slashIndex != -1) {
                return contentType.substring(0, slashIndex).toLowerCase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isValid(String curUrl) {
        UrlValidator urlValidator = new UrlValidator();
        return urlValidator.isValid(curUrl);
    }

    public String getRedirectUrl(String curUrl) {
        try {
            URLConnection con = new URL(curUrl).openConnection();
            con.setConnectTimeout(TIMEOUT);
            con.connect();
            InputStream is = con.getInputStream();
            URL reUrl = con.getURL();
            is.close();
            return reUrl.toString();
        } catch (Exception e) {
//            logger.info(curUrl + "cannot be connected, need certification");
        }
        return null;
    }

    public void crawl(Queue<Pair<String, Integer>> urls, Set<String> visited,
                      Map<String, Integer> domainFreq,
                      Map<String, List<String>> linkType,
                      Map<String, Integer> linkTypeFreq,
                      Map<String, Integer> inLinks,
                      Map<String, Integer> outLinks,
                      int limit, int depth) {

        int crawlCount = 0;

        while (!urls.isEmpty()) {
            /* count how many pages we took from queue */
            crawlCount++;

            Pair<String, Integer> cur = urls.poll();
            String curUrl = cur.getKey();
            int curDepth = cur.getValue();

            /* get redirect url */
            curUrl = getRedirectUrl(curUrl);

            if (curUrl == null) {
                continue;
            }

            /* check valid and whether the page is visited or not */
            if (!isValid(curUrl) || visited.contains(curUrl)) {
                continue;
            }

            /* get domain and type */
            String domain = getDomain(curUrl);
            String type = getType(curUrl);

            /* update map */
            visited.add(curUrl);
            domainFreq.put(domain, domainFreq.getOrDefault(domain, 0) + 1);
            if (type != null) {
                linkTypeFreq.put(type, linkTypeFreq.getOrDefault(type, 0) + 1);
                List<String> typeList = linkType.getOrDefault(type, new ArrayList<>());
                typeList.add(curUrl);
                linkType.put(type, typeList);
            }

            /* get pages from url */
            Set<String> nextUrls = getUrlsFromPage(curUrl);
            outLinks.put(curUrl, nextUrls.size());

            for (String nextUrl: nextUrls) {
                inLinks.put(nextUrl, inLinks.getOrDefault(nextUrl, 0) + 1);
                urls.offer(new Pair<>(nextUrl, curDepth + 1));
            }

            if (crawlCount%200 == 0) {
                logger.info("Crawled" + Integer.toString(crawlCount) + "links");
                logger.info("Current depth " + curDepth + " links");
                logger.info("Crawler has " + urls.size() + " links");
            }

            if (limit != -1 && crawlCount >= limit) {
                logger.info("Crawled" + Integer.toString(crawlCount) + "links");
                logger.info("Current depth " + curDepth + " links");
                break;
            }

            if (depth != -1 && curDepth >= depth) {
                logger.info("Crawled" + Integer.toString(crawlCount) + "links");
                logger.info("Current depth " + curDepth + " links");
                break;
            }
        }
    }

    public void outputStatistics(String query, Set<String> visited,
                      Map<String, Integer> domainFreq,
                      Map<String, List<String>> linkType,
                      Map<String, Integer> linkTypeFreq,
                      Map<String, Integer> inLinks,
                      Map<String, Integer> outLinks) {

        Comparator<Map.Entry<String, Integer>> numDescending = new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        };

        List<Map.Entry<String, Integer>> inList = new ArrayList<>(inLinks.entrySet());
        List<Map.Entry<String, Integer>> outList = new ArrayList<>(outLinks.entrySet());

        Collections.sort(inList, numDescending);
        Collections.sort(outList, numDescending);

        List<String> top25inLinks = new ArrayList<>();
        List<String> top25outLinks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            top25inLinks.add(inList.get(i).getKey());
            top25outLinks.add(outList.get(i).getKey());
        }

        logger.info("Number of unique links extracted: " + visited.size());
        logger.info("Frequency distribution by domain: " + domainFreq.toString());
//        logger.info("Breakdown of links by type: " + linkType.toString());
        logger.info("Breakdown of links frequency by type: " + linkTypeFreq.toString());
        logger.info("Top 25 incoming Links: " + top25inLinks.toString());
        logger.info("Top 25 outgoing Links: " + top25outLinks.toString());

        PrintWriter outputWriter;
        try {
            outputWriter = new PrintWriter(
                    OUTFILE + ".txt");
            outputWriter.println("Query: " + query);
            outputWriter.println("Number of unique links extracted: " + visited.size());
            outputWriter.println("Frequency distribution by domain: " + domainFreq.toString());
//            outputWriter.println("Breakdown of links by type: " + linkType.toString());
            outputWriter.println("Breakdown of links frequency by type: " + linkTypeFreq.toString());
            outputWriter.println("Top 25 incoming Links: " + top25inLinks.toString());
            outputWriter.println("Top 25 outgoing Links: " + top25outLinks.toString());
            outputWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getTopicFromArgs(String args[]) {
        if (args.length == 0) {
            System.out.println("Please give a topic, limit and depth, seperate with space, -1 is no limit...");
            Scanner scanner = new Scanner(System.in);
            try {
                while (true) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] lineArgs = line.trim().split("\\s+");
                        if (lineArgs.length == 3) {
                            return lineArgs;
                        } else {
                            System.err.println("Invalid arguments number");
                        }
                        break;
                    }
                }
            } catch(IllegalStateException | NoSuchElementException e) {
                e.printStackTrace();
            }
        } else if (args.length == 3) {
            return args;
        } else {
            System.err.println("Invalid arguments number");
        }
        return null;
    }


    public static void main(String[] args){

        String[] topicArgs = getTopicFromArgs(args);
        if (topicArgs == null) {
            System.exit(1);
        }

        /* when given limit is -1, we take it as no limit */
        // "#robinhood" 20000 -1
        String query = topicArgs[0];
        int limit = Integer.parseInt(topicArgs[1]);
        int depth = Integer.parseInt(topicArgs[2]);

        Queue<Pair<String, Integer>> urls = new LinkedList<>();
        TwitterApiConnector con = new TwitterApiConnector();
        Crawler crawler = new Crawler();
        Twitter twitter = con.configTwitter();

        int count = 0;

        while (urls.size() == 0) {
            List<String> searchResult = con.searchTweets(twitter, query);

            urls = crawler.getUrlsFromTweets(searchResult);
            System.out.println(urls.toString());

            if (count++ == 10) {
                logger.info("Cannot find seed page with current query. " +
                        "Please try a new one");
                System.exit(1);
            }
        }

        Set<String> visited = new HashSet<>();
        Map<String, Integer> domainFreq = new HashMap<>();
        Map<String, List<String>> linkType = new HashMap<>();
        Map<String, Integer> linkTypeFreq = new HashMap<>();
        Map<String, Integer> inLinks = new HashMap<>();
        Map<String, Integer> outLinks = new HashMap<>();

        crawler.crawl(urls, visited, domainFreq, linkType, linkTypeFreq, inLinks, outLinks, limit, depth);

        crawler.outputStatistics(query, visited, domainFreq, linkType, linkTypeFreq, inLinks, outLinks);
    }
}
