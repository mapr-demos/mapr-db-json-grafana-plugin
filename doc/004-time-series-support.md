# Time Series Metrics Support

## Contents

* [Overview](#overview)
* [Aggregations](#aggregations)
* [Plugin documentation links](#plugin-documentation-links)

## Overview

Time series metrics can be visualized using [Gpaph Panel](http://docs.grafana.org/features/panels/graph/#graph-panel).
![Time Series at Graph Panel](images/time-series-overview.png?raw=true "Time Series at Graph Panel")

To use time series metrics you must set metric type to `Timeseries`(using `Type` option) and choose aggregation(using 
`Metric` option). Keep in mind, that some aggregations require `Metric field` option to be set. Also, as with 
[Raw Documents Metrics](003-raw-documents-support.md) you can query metrics using conditions, use time range and limit.

Note: you can not specify fields for projection in Time Series, since it already uses projection to query only required fields.

## Aggregations

Currently, the following aggregations are supported:
1. Document count

Metric value is number of documents which match specified condition(if specified) and fit within some time interval. 
Time interval is defined by Grafana and depends on panel zoom.

2. Field value

Metric value is value of specified metric field of the first document that matches specified condition(if specified) and 
fits within some time interval. Time interval is defined by Grafana and depends on panel zoom.

3. Field min

Metric value is minimal value of specified metric field of the documents that match specified condition(if specified) 
and fit within some time interval. Time interval is defined by Grafana and depends on panel zoom.

4. Field max

Metric value is maximal value of specified metric field of the documents that match specified condition(if specified) 
and fit within some time interval. Time interval is defined by Grafana and depends on panel zoom.

5. Field avg

Metric value is average value of specified metric field of the documents that match specified condition(if specified) 
and fit within some time interval. Time interval is defined by Grafana and depends on panel zoom.

## Plugin documentation links

* [MapR-DB JSON Grafana Plugin Overview](001-overview.md)
* [Installation](002-installation.md)
* [Raw Documents metrics support](003-raw-documents-support.md)
* [Time series metrics support](004-time-series-support.md)
* [Creating sample dashboard](005-creating-sample-dashboard.md)
