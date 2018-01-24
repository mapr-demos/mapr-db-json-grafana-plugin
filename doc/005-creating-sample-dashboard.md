# Creating Sample Dashboard

## Contents

* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Dashboard import](#overview)
* [Number of albums released by language](#number-of-albums-released-by-language)
* [Number of artists that started career](#number-of-artists-that-started-career)
* [Artists raw documents](#artists-raw-documents)
* [Albums raw documents](#albums-raw-documents)
* [Average artists rating by area](#average-artists-rating-by-area)
* [Average artists rating by gender](#average-artists-rating-by-gender)
* [Plugin documentation links](#plugin-documentation-links)

## Overview

[Dashboards](http://docs.grafana.org/guides/basic_concepts/#dashboard) can be thought of as of a set of one or more 
Panels organized and arranged into one or more Rows. This document explains how to create sample Dashboard which 
visualizes [MapR Music dataset.](https://github.com/mapr-demos/mapr-music/tree/master/dataset)

![](images/sample-dashboard.png?raw=true "Sample dashboard")

## Prerequisites

This document explains how to create sample Dashboard which visualizes 
[MapR Music dataset.](https://github.com/mapr-demos/mapr-music/tree/master/dataset) Please, follow 
[instructions](https://github.com/mapr-demos/mapr-music/blob/master/doc/tutorials/004-import-the-data-set.md) from MapR 
Music tutorial to import the Data Set.

## Dashboard import

Repository contains exported [MapR-DB JSON Dashboard](../dashboard/mapr-db-json-dashboard.json) which is ready to be 
imported to your Grafana installation. Please, follow 
[instructions from Grafana Docs](http://docs.grafana.org/reference/export_import/#importing-a-dashboard) of you want 
to import the sample dashboard. Otherwise, you can follow this document to create the dashboard by your own.

## Number of albums released by language

Lets start creating MapR-DB JSON Dashboard from Graph Panel, which visualizes number of Albums released by year 
depending on Album's language. We need to choose `Timeseries` metrics type and `Document count` as aggregation. 
[Albums](https://github.com/mapr-demos/mapr-music/blob/master/doc/tutorials/004-import-the-data-set.md#data-set-description) 
JSON documents contain `released_date` time field, which must be specified as `Time field` option.

We will consider 4 languages and will create 4 targets with corresponding queries to 
MapR-DB `/apps/albums` JSON Table. Albums documents contain `language` field, so we can query Albums against that field:

#### English:

```
{"$condition": {"$eq": {"language": "eng"}}}
```

#### French:

```
{"$condition": {"$eq": {"language": "fra"}}}
```

#### Spanish:

```
{"$condition": {"$eq": {"language": "spa"}}}
```

#### German:

```
{"$condition": {"$eq": {"language": "deu"}}}
```

Also, lets change default `Limit` oprion value in order to query more datapoints and get more accurate visualization.

After completing all these steps, we should be able to see something similar to the image below:
![](images/albums-released-by-language.png?raw=true "Number of albums released by language")

## Number of artists that started career

Now, lets visualize number of Artists, which started career at certain year on another Graph Panel. To do so, we even 
don't have to specify MapR-DB query. All we need is to fill `Time field` option with `begin_date` value, which 
corresponds to the 
[Artist's JSON document](https://github.com/mapr-demos/mapr-music/blob/master/doc/tutorials/004-import-the-data-set.md#data-set-description) 
`begin_date` field. You're already familiar with other options such as `Type`, `Metric`, `Limit` and `Table`, 
value of which points to Artists MapR-DB JSON Table.

Below you can see Graph Panel, which visualizes the number of artists that started career:
![](images/artists-started-career.png?raw=true "Number of artists that started career")

## Artists raw documents

MapR-DB Datasource Plugin also provides the ability to visualize raw JSON documents using Table panels. Lets create such
panel to visualize Artists documents. All you need is to choose `Raw Document` as metric `Type` and specify MapR-DB 
JSON Table using `Table` option. You can also use `Select` option to list fields of JSON documents, which will be queries.
This option allows you to reduce amount of data sent over network. At `Optipons` tab choose columns which will be 
displayed. `Columns Styles` tab allows you to format `begin_date` and `end_date` values in the desired way.

After that you should be able to see something like on the image below:
![](images/artists.png?raw=true "Artists")

## Albums raw documents

Visualizing Albums raw document is similar to visualizing [Artists raw documents](#artists-raw-documents). Only value of 
`Table` option and list of available columns will be changed.
![](images/albums.png?raw=true "Albums")

## Average artists rating by area

Now, lets visualize average Artists' rating depending on Artist's area. In this case we will use `Field avg` metric 
aggregation. Also we will specify `Metric field` option value to correspond to Artist's `rating` field. `Field avg` metric 
aggregation will compute average of the specified `Metric field` values inside certain time interval. So, if 
multiple Artists documents overlap inside time interval(for example 1 year), metric value will be determined as 
average of Artists' `rating` field values.

To divide metrics depending on Artists are we must create target for each area and specify MapR-DB Query Condition:


#### United States:

```
{"$condition": {"$eq": {"area": "United States"}}}
```

#### Germany:

```
{"$condition": {"$eq": {"area": "Germany"}}}
```

#### South Africa:

```
{"$condition": {"$eq": {"area": "South Africa"}}}
```


![](images/average-artists-rating-be-area.png?raw=true "Average artists rating by area")

## Average artists rating by gender

Creating another Graph Panel, which visualizes average artists rating by gender is very similar to creating the 
[previous one](#average-artists-rating-by-area). Here we create only two targets and query Artists documents against 
`gender` field:

#### Male:

```
{"$condition": {"$eq": {"gender": "Male"}}}
```

#### Female:

```
{"$condition": {"$eq": {"gender": "Female"}}}
```


![](images/average-rating-by-gender.png?raw=true "Average artists rating by gender")

## Plugin documentation links

* [MapR-DB JSON Grafana Plugin Overview](001-overview.md)
* [Installation](002-installation.md)
* [Raw Documents metrics support](003-raw-documents-support.md)
* [Time series metrics support](004-time-series-support.md)
* [Creating sample dashboard](005-creating-sample-dashboard.md)
