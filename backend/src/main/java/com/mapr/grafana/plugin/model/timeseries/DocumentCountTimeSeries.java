package com.mapr.grafana.plugin.model.timeseries;

import org.ojai.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO document
 */
public class DocumentCountTimeSeries extends AbstractGrafanaTimeSeries {

    private static final Logger log = LoggerFactory.getLogger(DocumentCountTimeSeries.class);

    public DocumentCountTimeSeries(String target, String timeFieldPath, long intervalMs) {
        super(target, timeFieldPath, intervalMs);
    }

    /**
     * Note, that since this implementation uses {@link java.util.List} as underlying collection for storing datapoints,
     * the order of method invocation is significant. Thus, documents should be added in order of timestamp increasing.
     *
     * @param document document which will be converted to datapoint.
     */
    @Override
    public void addDocument(Document document) {
        try {

            if (document == null) {
                throw new IllegalArgumentException("Document can not be null");
            }

            long timestamp = getDocumentTimestamp(document, timeFieldPath);

            if (this.datapoints.isEmpty()) {
                this.datapoints.add(new Datapoint(1, timestamp));
            } else {
                Datapoint last = this.datapoints.get(this.datapoints.size() - 1);
                if (timestamp - last.getTimestamp() >= intervalMs) {
                    this.datapoints.add(new Datapoint(1, timestamp));
                } else {
                    last.setValue(last.getValue() + 1);
                }
            }

        } catch (Exception e) {
            log.debug("Exception occurred while adding OJAI document '{}' as datapoint with time field: '{}'. " +
                    "Exception: '{}'", document, timeFieldPath, e);
        }
    }

    @Override
    public String toString() {
        return "DocumentCountTimeSeries{" +
                "intervalMs=" + intervalMs +
                ", datapoints=" + datapoints +
                ", target='" + target + '\'' +
                ", timeFieldPath='" + timeFieldPath + '\'' +
                '}';
    }
}
