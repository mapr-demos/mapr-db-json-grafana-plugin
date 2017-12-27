package com.mapr.grafana.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents time series metrics.
 */
public class GrafanaTimeSeries implements GrafanaMetrics {

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

    private List<Datapoint> datapoints = new ArrayList<>();
    private String target;

    public GrafanaTimeSeries() {
    }

    public GrafanaTimeSeries(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<Datapoint> getDatapoints() {
        return datapoints;
    }

    public void setDatapoints(List<Datapoint> datapoints) {
        this.datapoints = datapoints;
    }

    public void addDatapoint(Datapoint datapoint) {
        this.datapoints.add(datapoint);
    }

    public void addDatapoint(long value, long timestamp) {
        this.addDatapoint(new Datapoint(value, timestamp));
    }

    @Override
    public String toString() {
        return "GrafanaTimeSeries{" +
                "datapoints=" + datapoints +
                ", target='" + target + '\'' +
                '}';
    }
}
