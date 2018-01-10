package com.mapr.grafana.plugin.model.timeseries;

import com.mapr.grafana.plugin.model.GrafanaMetrics;
import org.ojai.Document;

public interface GrafanaTimeSeries extends GrafanaMetrics {

    /**
     * Note, that datapoints should be ordered by timestamp. Thus, order of method invocation may be significant.
     *
     * @param document document which will be converted to datapoint.
     */
    void addDocument(Document document);
}
