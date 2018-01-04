package com.mapr.grafana.plugin.util;

import com.mapr.grafana.plugin.model.Tweet;
import org.ojai.types.OTimestamp;

import java.util.UUID;

public class TweetBuilder {

    private Tweet tweet;

    public TweetBuilder() {
        this.tweet = new Tweet();
        this.tweet.setId(UUID.randomUUID().toString());
        this.tweet.setTime(new OTimestamp(System.currentTimeMillis()));
        this.tweet.setLikes(0);
    }

    public static TweetBuilder tweet() {
        return new TweetBuilder();
    }

    public TweetBuilder id(String id) {
        this.tweet.setId(id);
        return this;
    }

    public TweetBuilder author(String author) {
        this.tweet.setAuthor(author);
        return this;
    }

    public TweetBuilder likes(int likes) {
        this.tweet.setLikes(likes);
        return this;
    }

    public TweetBuilder content(String content) {
        this.tweet.setContent(content);
        return this;
    }

    public TweetBuilder time(long timestamp) {
        this.tweet.setTime(new OTimestamp(timestamp));
        return this;
    }

    public TweetBuilder time(String timeString) {
        this.tweet.setTime(OTimestamp.parse(timeString));
        return this;
    }

    public Tweet build() {
        return this.tweet;
    }

}
