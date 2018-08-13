import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import java.util.*;
import java.util.stream.Collectors;

public class TwitterApiConnector {
    public Twitter configTwitter() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey()
                .setOAuthConsumerSecret()
                .setOAuthAccessToken()
                .setOAuthAccessTokenSecret();
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public List<String> searchTweets(Twitter twitter, String queryStr) {
        try {
            Query query = new Query(queryStr);
            query.setCount(100);
            QueryResult result = twitter.search(query);

            return result.getTweets().stream()
                    .map(item -> item.getText())
                    .collect(Collectors.toList());
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String query = "#robinhood";

        TwitterApiConnector con = new TwitterApiConnector();
        Twitter twitter = con.configTwitter();
        List<String> searchResult = con.searchTweets(twitter, query);
        System.out.println(searchResult.toString());
        System.out.println(searchResult.size());

    }
}
