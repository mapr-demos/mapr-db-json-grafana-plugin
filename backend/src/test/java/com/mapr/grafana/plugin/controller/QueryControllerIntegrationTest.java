package com.mapr.grafana.plugin.controller;

import com.mapr.db.MapRDB;
import com.mapr.db.exceptions.DBException;
import com.mapr.grafana.plugin.Application;
import com.mapr.grafana.plugin.dao.TweetDao;
import com.mapr.grafana.plugin.model.GrafanaQueryRequest;
import com.mapr.grafana.plugin.model.GrafanaQueryTarget;
import com.mapr.grafana.plugin.model.Tweet;
import com.mapr.grafana.plugin.util.GrafanaTestQueryRequestBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mapr.grafana.plugin.util.TweetBuilder.tweet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class QueryControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(QueryControllerIntegrationTest.class);

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long INACCURATE_YEAR = 365 * DAY;

    private static final Tweet[] TIME_ORDERED_TWEETS = new Tweet[]{
            tweet().author("user1").content("tag1").time("2012-04-24T22:35:28.981Z").likes(1).build(),
            tweet().author("user2").content("tag2").time("2013-05-24T22:35:28.981Z").likes(2).build(),
            tweet().author("user3").content("tag1").time("2014-01-01T22:35:28.981Z").likes(3).build(),
            tweet().author("usertobequeried").content("tag1").time("2017-01-03T22:35:28.981Z").likes(4).build(),

            // Tweets published in a single day
            tweet().author("user5").content("tag3").time("2018-04-05T22:35:28.981Z").likes(5).build(),
            tweet().author("user6").content("tag3").time("2018-04-05T22:36:28.981Z").likes(6).build(),
            tweet().author("user7").content("tag3").time("2018-04-05T23:02:28.981Z").likes(7).build(),
            tweet().author("user8").content("tag3").time("2018-04-06T00:35:28.981Z").likes(8).build()
    };

    private MediaType contentType = new MediaType(APPLICATION_JSON.getType(), APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private String testTablePath;
    private MockMvc mockMvc;
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    public void initTestTablePath(@Value("${mfs.test.dir:/grafana-plugin}") String mfsTestDir) {
        this.testTablePath = String.format("%s/test-table-%s", mfsTestDir, UUID.randomUUID());
    }

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null", this.mappingJackson2HttpMessageConverter);
    }

    /**
     * Prepares test MapR-DB JSON Table. Assumes that MapR-FS contains test directory. Test MapR-DB JSON Table will be
     * created inside this directory. Note, that directory must have correct permission settings, so in case when tests
     * are run from dev machine(which is not the node of the cluster) with installed MapR Client you must add new user
     * on all of the cluster nodes. This user must have the same name, id and group id as user that runs
     * tests on the dev machine:
     * <p>
     * 1. Determine username, uid, gid at dev machine:
     * <br/><code>$ whoami</code> - prints username, for example: 'jdoe'
     * <br/><code>$ id `whoami`</code> - prints id and gid, for example: 'uid=1000(jdoe) gid=1000(jdoe)'
     * <p>
     * 2. Add new user on each of the cluster nodes:
     * <br/><code>$ useradd -u 1000 jdoe</code>
     * <p>
     * 3. Create test directory for MapR-DB JSON Table using test directory path('/grafana-plugin' by default):
     * <br/><code>$ hadoop fs -mkdir /grafana-plugin</code>
     * <p>
     * 4. Change ownership of newly created test directory:
     * <br/><code>$ hadoop fs -chown jdoe:jdoe /grafana-plugin</code>
     */
    @Before
    public void setup() throws Exception {

        try {
            log.info("Creating test MapR-DB Table: '{}'", testTablePath);
            MapRDB.createTable(testTablePath);

        } catch (DBException e) {

            log.warn("Error while creating test MapR-DB Table: '" + testTablePath + "'. All tests will be skipped.", e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("no such file or directory")) {
                log.error("Can not create test MapR-DB Table '{}' in non-existing directory. Please, check " +
                        "'mfs.test.dir' property", testTablePath);
            }

            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("permission denied")) {
                log.error("Table creation is denied. Refer MapR documentation and plugin documentation.");
            }

            throw new IllegalStateException(e);
        }

        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        // Fill the table with test data
        TweetDao tweetDao = new TweetDao(testTablePath);
        Stream.of(TIME_ORDERED_TWEETS).forEach(tweetDao::save);
    }

    @After
    public void tearDown() throws Exception {
        log.info("Deleting test MapR-DB Table: '{}'", testTablePath);
        MapRDB.deleteTable(testTablePath);
    }

    @Test
    public void shouldQueryRawDocuments() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withRawDocumentTarget()
                .withTable(testTablePath)
                .addTarget()
                .build();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("docs")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(TIME_ORDERED_TWEETS.length)))
                .andExpect(jsonPath("$[0].datapoints[0].author", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].content", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].likes", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].time", notNullValue()));
    }

    @Test
    public void shouldQueryRawDocumentsWithLimit() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withRawDocumentTarget()
                .withTable(testTablePath)
                .withLimit(2)
                .addTarget()
                .build();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("docs")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(2)))
                .andExpect(jsonPath("$[0].datapoints[0].author", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].content", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].likes", notNullValue()))
                .andExpect(jsonPath("$[0].datapoints[0].time", notNullValue()));
    }

    @Test
    public void shouldQueryRawDocumentsWithRange() throws Exception {

        Date afterFirstTweet = addDays(TIME_ORDERED_TWEETS[0].getTime().toDate(), 1);
        Date beforeThirdTweet = addDays(TIME_ORDERED_TWEETS[2].getTime().toDate(), -1);

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .rangeFrom(afterFirstTweet)
                .rangeTo(beforeThirdTweet)
                .withRawDocumentTarget()
                .withTable(testTablePath)
                .withTimeField("time") // corresponds to Tweet's 'time' field
                .addTarget()
                .build();

        // only second Tweet must be returned
        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("docs")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(1)))
                .andExpect(jsonPath("$[0].datapoints[0].author", is(TIME_ORDERED_TWEETS[1].getAuthor())))
                .andExpect(jsonPath("$[0].datapoints[0].content", is(TIME_ORDERED_TWEETS[1].getContent())))
                .andExpect(jsonPath("$[0].datapoints[0].likes", is(TIME_ORDERED_TWEETS[1].getLikes())))
                .andExpect(jsonPath("$[0].datapoints[0].time", notNullValue()));
    }

    @Test
    public void shouldQueryRawDocumentsWithCondition() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withRawDocumentTarget()
                .withTable(testTablePath)
                .withCondition("{\"$condition\": {\"$eq\": {\"author\": \"usertobequeried\"}}}")
                .addTarget()
                .build();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("docs")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(1)))
                .andExpect(jsonPath("$[0].datapoints[0].author", is("usertobequeried")));
    }

    @Test
    public void shouldQueryRawDocumentsWithProjection() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withRawDocumentTarget()
                .withTable(testTablePath)
                .select("author") // select only 'author' field
                .addTarget()
                .build();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("docs")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(TIME_ORDERED_TWEETS.length)))
                .andExpect(jsonPath("$[0].datapoints[0].author", notNullValue())) // must present
                .andExpect(jsonPath("$[0].datapoints[0].content").doesNotExist()) // must not present
                .andExpect(jsonPath("$[0].datapoints[0].likes").doesNotExist()) // must not present
                .andExpect(jsonPath("$[0].datapoints[0].time").doesNotExist()); // must not present
    }

    @Test
    public void shouldGetNumberOfTweetsByDay() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(DAY)
                .withTimeSeriesTarget()
                .withTarget("TweetsByTime")
                .withTable(testTablePath)
                .withTimeField("time")
                .withMetric(GrafanaQueryTarget.DOCUMENT_COUNT_METRIC)
                .addTarget()
                .build();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].target", is("TweetsByTime")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(5)));
    }

    @Test
    public void shouldGetNumberOfTweetsByDayForTag() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(DAY)
                .withTimeSeriesTarget()
                .withTarget("TweetsByTime: tag 1")
                    .withTable(testTablePath)
                    .withCondition("{\"$condition\": {\"$eq\": {\"content\": \"tag1\"}}}")
                    .withTimeField("time")
                    .withMetric(GrafanaQueryTarget.DOCUMENT_COUNT_METRIC)
                .addTarget()
                .withTimeSeriesTarget()
                .withTarget("TweetsByTime: tag 2")
                    .withTable(testTablePath)
                    .withCondition("{\"$condition\": {\"$eq\": {\"content\": \"tag2\"}}}")
                    .withTimeField("time")
                    .withMetric(GrafanaQueryTarget.DOCUMENT_COUNT_METRIC)
                .addTarget()
                .withTimeSeriesTarget()
                .withTarget("TweetsByTime: tag 3")
                    .withTable(testTablePath)
                    .withCondition("{\"$condition\": {\"$eq\": {\"content\": \"tag3\"}}}")
                    .withTimeField("time")
                    .withMetric(GrafanaQueryTarget.DOCUMENT_COUNT_METRIC)
                .addTarget()
                .build();

        Set<String> targets = new HashSet<>();
        targets.add("TweetsByTime: tag 1");
        targets.add("TweetsByTime: tag 2");
        targets.add("TweetsByTime: tag 3");

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].target", isIn(targets)))
                .andExpect(jsonPath("$[1].target", isIn(targets)))
                .andExpect(jsonPath("$[2].target", isIn(targets)))
                .andExpect(jsonPath("$[0].datapoints", hasSize(isIn(Arrays.asList(1, 3)))))
                .andExpect(jsonPath("$[1].datapoints", hasSize(isIn(Arrays.asList(1, 3)))))
                .andExpect(jsonPath("$[2].datapoints", hasSize(isIn(Arrays.asList(1, 3)))));
    }

    @Test
    public void shouldGetNumberOfTweetsLikesByDay() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(DAY)
                .withTimeSeriesTarget()
                .withTarget("TweetsLikesByTime")
                .withTable(testTablePath)
                .withTimeField("time")
                .withMetricField("likes")
                .withMetric(GrafanaQueryTarget.FIELD_VALUE_METRIC)
                .addTarget()
                .build();

        Set<Double> validLikeNums = Stream.of(TIME_ORDERED_TWEETS)
                .map(Tweet::getLikes)
                .map(Double::valueOf)
                .collect(Collectors.toSet());

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].target", is("TweetsLikesByTime")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(5)))
                .andExpect(jsonPath("$[0].datapoints[0][0]", isIn(validLikeNums)))
                .andExpect(jsonPath("$[0].datapoints[1][0]", isIn(validLikeNums)))
                .andExpect(jsonPath("$[0].datapoints[2][0]", isIn(validLikeNums)))
                .andExpect(jsonPath("$[0].datapoints[3][0]", isIn(validLikeNums)))
                .andExpect(jsonPath("$[0].datapoints[4][0]", isIn(validLikeNums)));
    }

    @Test
    public void shouldReturnMaxFieldValue() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(1000 * INACCURATE_YEAR) // make sure all tweets will be in range
                .withTimeSeriesTarget()
                .withTarget("MaxLikesValueOfAllTweets")
                .withTable(testTablePath)
                .withTimeField("time")
                .withMetricField("likes")
                .withMetric(GrafanaQueryTarget.FIELD_MAX_METRIC)
                .addTarget()
                .build();

        double max = Stream.of(TIME_ORDERED_TWEETS)
                .mapToDouble(Tweet::getLikes)
                .max()
                .getAsDouble();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].target", is("MaxLikesValueOfAllTweets")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(1)))
                .andExpect(jsonPath("$[0].datapoints[0][0]", is(max)));
    }

    @Test
    public void shouldReturnMinFieldValue() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(1000 * INACCURATE_YEAR) // make sure all tweets will be in range
                .withTimeSeriesTarget()
                .withTarget("MinLikesValueOfAllTweets")
                .withTable(testTablePath)
                .withTimeField("time")
                .withMetricField("likes")
                .withMetric(GrafanaQueryTarget.FIELD_MIN_METRIC)
                .addTarget()
                .build();

        double min = Stream.of(TIME_ORDERED_TWEETS)
                .mapToDouble(Tweet::getLikes)
                .min()
                .getAsDouble();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].target", is("MinLikesValueOfAllTweets")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(1)))
                .andExpect(jsonPath("$[0].datapoints[0][0]", is(min)));
    }

    @Test
    public void shouldReturnAverageFieldValue() throws Exception {

        GrafanaQueryRequest request = new GrafanaTestQueryRequestBuilder()
                .withIntervalMs(1000 * INACCURATE_YEAR) // make sure all tweets will be in range
                .withTimeSeriesTarget()
                .withTarget("AvgLikesValueOfAllTweets")
                .withTable(testTablePath)
                .withTimeField("time")
                .withMetricField("likes")
                .withMetric(GrafanaQueryTarget.FIELD_AVG_METRIC)
                .addTarget()
                .build();

        double average = Stream.of(TIME_ORDERED_TWEETS)
                .mapToDouble(Tweet::getLikes)
                .average()
                .getAsDouble();

        mockMvc.perform(post("/query")
                .content(json(request))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].target", is("AvgLikesValueOfAllTweets")))
                .andExpect(jsonPath("$[0].datapoints", hasSize(1)))
                .andExpect(jsonPath("$[0].datapoints[0][0]", is(average)));
    }

    private boolean withinSingleDay(Date first, Date second) {
        return Math.abs(first.getTime() - second.getTime()) <= DAY;
    }

    private Date addDays(Date date, int daysToAdd) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, daysToAdd);

        return cal.getTime();
    }


    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(o, APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

}