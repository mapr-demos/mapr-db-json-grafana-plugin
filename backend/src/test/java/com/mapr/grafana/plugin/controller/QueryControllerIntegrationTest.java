package com.mapr.grafana.plugin.controller;

import com.mapr.db.MapRDB;
import com.mapr.db.exceptions.DBException;
import com.mapr.grafana.plugin.Application;
import com.mapr.grafana.plugin.dao.TweetDao;
import com.mapr.grafana.plugin.model.GrafanaQueryRequest;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
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

    private static final Tweet[] TIME_ORDERED_TWEETS = new Tweet[]{
            tweet().author("user1").content("some content1").time("2012-04-24T22:35:28.981Z").likes(1).build(),
            tweet().author("user2").content("some content2").time("2013-05-24T22:35:28.981Z").likes(3).build(),
            tweet().author("user3").content("some content3").time("2014-01-01T22:35:28.981Z").likes(12).build(),
            tweet().author("usertobequeried").content("some content4").time("2017-01-03T22:35:28.981Z").likes(2).build(),
            tweet().author("user5").content("some content5").time("2018-01-03T22:35:28.981Z").likes(2).build()
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