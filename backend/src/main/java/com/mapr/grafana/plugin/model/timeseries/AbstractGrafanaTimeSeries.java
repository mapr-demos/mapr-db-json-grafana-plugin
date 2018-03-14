package com.mapr.grafana.plugin.model.timeseries;

import org.ojai.Document;
import org.ojai.types.ODate;
import org.ojai.types.OTime;
import org.ojai.types.OTimestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents abstract time series metrics.
 */
public abstract class AbstractGrafanaTimeSeries implements GrafanaTimeSeries {

    public static class Datapoint {
        private double value;
        private long timestamp;

        public Datapoint(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public Datapoint() {
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    protected List<Datapoint> datapoints = new ArrayList<>();
    protected String target;
    protected String timeFieldPath;
    protected long intervalMs;

    public AbstractGrafanaTimeSeries() {
    }

    public AbstractGrafanaTimeSeries(String target, String timeFieldPath, long intervalMs) {
        this.target = target;
        this.timeFieldPath = timeFieldPath;
        this.intervalMs = intervalMs;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<Datapoint> getDatapoints() {
        return Collections.unmodifiableList(datapoints);
    }

    protected Long getDocumentTimestamp(Document document, String timeFieldPath) {

        if (timeFieldPath == null || timeFieldPath.isEmpty()) {
            throw new IllegalArgumentException("Time field path can not be null.");
        }

        Object dateObject = document.getValue(timeFieldPath).getObject();
        if (dateObject == null) {
            throw new IllegalArgumentException("Time field can not contain null values.");
        }

        if (dateObject instanceof ODate) {
            return ((ODate) dateObject).toDate().getTime();
        } else if (dateObject instanceof OTime) {
            return ((OTime) dateObject).toDate().getTime();
        } else if (dateObject instanceof OTimestamp) {
            return ((OTimestamp) dateObject).toDate().getTime();
        } else if (dateObject instanceof String) {
            return Long.valueOf((String) dateObject);
        } else if (dateObject instanceof Double) { // TODO add support of time patterns
            return ((Double) dateObject).longValue();
        } else {
            return (Long) dateObject;
        }
    }

    @Override
    public String toString() {
        return "AbstractGrafanaTimeSeries{" +
                "datapoints=" + datapoints +
                ", target='" + target + '\'' +
                ", timeFieldPath='" + timeFieldPath + '\'' +
                ", intervalMs=" + intervalMs +
                '}';
    }
}
