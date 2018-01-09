package com.mapr.grafana.plugin.util;

import com.mapr.grafana.plugin.model.GrafanaQueryRequest;
import com.mapr.grafana.plugin.model.GrafanaQueryTarget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;


public class GrafanaTestQueryRequestBuilder {


    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    public static final String DEFAULT_DATE_FROM = "1970-01-01";
    public static final String DEFAULT_DATE_TO = "2999-01-01";
    public static final long DEFAULT_PANEL_ID = 0;
    public static final long DEFAULT_INTERVAL_MS = 60 * 60 * 1000;
    public static final long DEFAULT_DATAPOINTS_NUM = 1234;

    private GrafanaQueryRequest request;

    public class TargetBuilder {

        public static final long DEFAULT_LIMIT = 500;

        private GrafanaQueryTarget target;

        TargetBuilder(String type) {
            target = new GrafanaQueryTarget();
            target.setType(type);
            target.setRefId(UUID.randomUUID().toString());
            target.setLimit(DEFAULT_LIMIT);
        }

        public TargetBuilder withTable(String table) {
            target.setTable(table);
            return this;
        }

        public TargetBuilder withLimit(long limit) {
            target.setLimit(limit);
            return this;
        }

        public TargetBuilder withCondition(String condition) {
            target.setCondition(condition);
            return this;
        }

        public TargetBuilder withMetric(String metric) {
            target.setMetric(metric);
            return this;
        }

        public TargetBuilder withMetricField(String metricField) {
            target.setMetricField(metricField);
            return this;
        }

        public TargetBuilder withTimeField(String timeField) {
            target.setTimeField(timeField);
            return this;
        }

        public TargetBuilder withTarget(String targetName) {
            target.setTarget(targetName);
            return this;
        }

        public TargetBuilder select(String... fields) {
            target.setSelectFields(new HashSet<>(Arrays.asList(fields)));
            return this;
        }

        public GrafanaTestQueryRequestBuilder addTarget() {

            if (GrafanaTestQueryRequestBuilder.this.request.getTargets() == null ||
                    GrafanaTestQueryRequestBuilder.this.request.getTargets().isEmpty()) {

                GrafanaTestQueryRequestBuilder.this.request.setTargets(new HashSet<>());
            }
            GrafanaTestQueryRequestBuilder.this.request.getTargets().add(target);

            return GrafanaTestQueryRequestBuilder.this;
        }

    }

    public GrafanaTestQueryRequestBuilder() {

        request = new GrafanaQueryRequest();
        request.setPanelId(DEFAULT_PANEL_ID);
        request.setIntervalMs(DEFAULT_INTERVAL_MS);
        request.setMaxDataPoints(DEFAULT_DATAPOINTS_NUM);

        GrafanaQueryRequest.Range range = new GrafanaQueryRequest.Range();
        try {
            range.setFrom(formatter.parse(DEFAULT_DATE_FROM));
            range.setTo(formatter.parse(DEFAULT_DATE_TO));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        request.setRange(range);
    }

    public GrafanaTestQueryRequestBuilder withPanelId(long panelId) {
        request.setPanelId(panelId);
        return this;
    }

    public GrafanaTestQueryRequestBuilder withIntervalMs(long intervalMs) {
        request.setIntervalMs(intervalMs);
        return this;
    }

    public GrafanaTestQueryRequestBuilder withMaxDatapoints(long maxDatapoints) {
        request.setMaxDataPoints(maxDatapoints);
        return this;
    }

    public GrafanaTestQueryRequestBuilder rangeFrom(Date date) {
        request.getRange().setFrom(date);
        return this;
    }

    public GrafanaTestQueryRequestBuilder rangeTo(Date date) {
        request.getRange().setTo(date);
        return this;
    }

    public GrafanaTestQueryRequestBuilder rangeFrom(String dateString) {

        try {
            request.getRange().setFrom(formatter.parse(dateString));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return this;
    }

    public GrafanaTestQueryRequestBuilder rangeTo(String dateString) {

        try {
            request.getRange().setTo(formatter.parse(dateString));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return this;
    }

    public TargetBuilder withRawDocumentTarget() {
        return new TargetBuilder(GrafanaQueryTarget.RAW_DOCUMENT_TYPE);
    }

    public TargetBuilder withTimeSeriesTarget() {
        return new TargetBuilder(GrafanaQueryTarget.TIME_SERIES_TYPE);
    }

    public GrafanaQueryRequest build() {
        return request;
    }

}
