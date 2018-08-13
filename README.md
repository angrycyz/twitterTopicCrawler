
------------------------------------------
# CRAWLING AND LINK ANALYSIS
------------------------------------------

------------------------------------------
RUN THE PROGRAM WITH MAVEN
------------------------------------------
1) First make sure you have maven installed: use   *mvn -v*   to check the version.

2) Compile the progam use  *mvn compile*.

3) Run the crawler program in command line with:

mvn exec:java -Dexec.mainClass="Crawler"

The program will ask you to provide the topic, link limit, depth limit, for example:

*#robinhood 20000 0*

Not if you give a -1 as the link limit or depth limit, the program will take as there's no limit on link number or depth. 

press return after you entered the directory, the program will start indexing.

6) Check the statistics in **output.txt** file, the file will automatically be generated after running the crawler program. Four kind of statistics will be included:

    a. Number of unique links extracted
    
    b. Frequency distribution by domain
    
    c. Breakdown of links by type (e.g., text, image, video)
    
    d. For each crawled page, compute the number of incoming and outgoing links. Report the top-25 pages with the highest number of incoming and outgoing links.

------------------------------------------
EXPLANATION ON PROGRAM
------------------------------------------
### TwitterApiConnector

This program connects with Twitter API with ConsumerKey, ConsumerSecret, AccessToken and AccessTokenSecret. Remember to insert your own acess key in the program. I use an open source library Twitter4j to configure the Twitter Api settings. 
There are two methods in this class including configure method and search method.


### Crawler

The program extracts all links from the search result we get by using Twitter search API, and crawl all the links we extracted. 

I use a queue to store all the links waiting to get crawled. The crawling process will follow the BFS algorithm until the link limit or depth limit is reached.

After pages has been crawled, we put the statistic result into a file named **output.txt**

------------------------------------------
Statistics
------------------------------------------

I run the program with 20000 limit. which means the program will take 20000 links from the queue and trys to crawl them. With 20000 limit and no depth limit, the output statistics is in the output20000.txt file.

With the data from 3.d, we can't implement a PageRank. Since PageRank is query dependent but in this case we use a topic as the query. 

And we can implement a HITS. HITS is query dependent and in this case we do have a search term and the top 25 pages of highest number of incoming and outgoing links represents for the hubs and authorities. We can go from the largest hubs and go along the web to update and normalize the authority score and the hub score. After updating the score we can even repeat the previous step.


