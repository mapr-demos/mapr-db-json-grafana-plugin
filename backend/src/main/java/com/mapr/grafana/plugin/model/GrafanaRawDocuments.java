package com.mapr.grafana.plugin.model;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the set of raw JSON documents.
 */
public class GrafanaRawDocuments<T> implements GrafanaMetrics {

    private Set<T> datapoints = new HashSet<>();

    @JsonGetter("type")
    public String getType() {
        return "docs";
    }

    public Set<T> getDatapoints() {
        return datapoints;
    }

    public void setDatapoints(Set<T> datapoints) {
        this.datapoints = datapoints;
    }

    public void addDatapoint(T datapoint) {
        this.datapoints.add(datapoint);
    }

    @Override
    public String toString() {
        return "GrafanaRawDocuments{" +
                "type=" + getType() +
                "datapoints=" + datapoints +
                '}';
    }
}
