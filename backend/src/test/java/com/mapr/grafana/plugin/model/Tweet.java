package com.mapr.grafana.plugin.model;

import com.fasterxml.jackson.annotation.*;
import org.ojai.types.OTimestamp;

/**
 * Represents tweet. Created only for testing purpose.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tweet {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("author")
    private String author;

    @JsonProperty("content")
    private String content;

    @JsonProperty("likes")
    private int likes;

    private OTimestamp time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @JsonGetter("time")
    public OTimestamp getTime() {
        return time;
    }

    public void setTime(OTimestamp time) {
        this.time = time;
    }

    @JsonSetter("time")
    public void setTime(String timeString) {

        if (timeString == null) {
            this.time = null;
            return;
        }

        this.time = OTimestamp.parse(timeString);
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "id='" + id + '\'' +
                ", author='" + author + '\'' +
                ", content='" + content + '\'' +
                ", likes=" + likes +
                ", time=" + time +
                '}';
    }
}
