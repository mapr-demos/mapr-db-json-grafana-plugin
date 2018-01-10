package com.mapr.grafana.plugin.dao;

import com.mapr.grafana.plugin.model.Tweet;
import org.ojai.Document;
import org.ojai.store.Connection;
import org.ojai.store.DocumentStore;
import org.ojai.store.DriverManager;

/**
 * Created only for testing purposes.
 */
public class TweetDao {

    private static final String CONNECTION_URL = "ojai:mapr:";
    private Connection connection;
    private DocumentStore store;

    public TweetDao(String tablePath) {
        this.connection = DriverManager.getConnection(CONNECTION_URL);
        this.store = connection.getStore(tablePath);
    }

    public void save(Tweet tweet) {

        final Document createdOjaiDoc = connection.newDocument(tweet);

        // Insert the document into the OJAI store
        store.insertOrReplace(createdOjaiDoc);
    }

}