package com.mapr.grafana.plugin.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * TODO doc
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrafanaQueryRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Range {
        Date from;
        Date to;

        public Date getFrom() {
            return from;
        }

        public void setFrom(Date from) {
            this.from = from;
        }

        public Date getTo() {
            return to;
        }

        public void setTo(Date to) {
            this.to = to;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }

    @NotNull
    private Long panelId;

    @NotNull
    private Range range;

    @NotNull
    private Long intervalMs;

    @NotNull
    private Set<GrafanaQueryTarget> targets;

    @NotNull
    private Long maxDataPoints;

    public Long getPanelId() {
        return panelId;
    }

    public void setPanelId(Long panelId) {
        this.panelId = panelId;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(Long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public Set<GrafanaQueryTarget> getTargets() {
        return (targets != null) ? targets : Collections.emptySet();
    }

    public void setTargets(Set<GrafanaQueryTarget> targets) {
        this.targets = targets;
    }

    public Long getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(Long maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    @Override
    public String toString() {
        return "GrafanaQueryRequest{" +
                "panelId=" + panelId +
                ", range=" + range +
                ", intervalMs=" + intervalMs +
                ", targets=" + targets +
                ", maxDataPoints=" + maxDataPoints +
                '}';
    }
}
