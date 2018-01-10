# Raw Documents Metrics Support

## Contents

* [Overview](#overview)
* [Conditions](#conditions)
* [Projection](#projection)
* [Time range](#time-range)
* [Plugin documentation links](#plugin-documentation-links)

## Overview

MapR-DB JSON Grafana Plugin allows to visualize raw JSON documents as rows of 
[Grafana Table Panel](http://docs.grafana.org/features/panels/table_panel/). 

![Raw JSON documents](images/raw-json-overview.png?raw=true "Raw JSON documents")

As you can see at image above JSON documents are displayed as rows of table. You need to specify MapR-DB JSON Table 
which will be used for data visualisation, `table` option allows you to do so.

Use `Options` tab to choose columns:
![Table Panel Options Tab](images/table-panel-options-tab.png?raw=true "Table Panel Options Tab")

At `Metrics` tab you can find other options, which will be described in details below.

## Conditions

Using `condition` option you can query documents using 
[MapR-DB Shell](https://maprdocs.mapr.com/60/ReferenceGuide/mapr_dbshell.html) condition syntax.

Assuming that you query `/apps/albums` table using 
[MapR Music Dataset](https://github.com/mapr-demos/mapr-music/blob/master/doc/tutorials/004-import-the-data-set.md) 
you can find the examples below helpful:

1. Albums with rating greater than `3.7`
```
{"$condition": {"$gt": {"rating": 3.7}}}
```

2. Albums with first track duration less that `5 sec`:
```
{"$condition": {"$lt": {"tracks[0].length": 5000}}}
```

3. `Promotion` albums with non-empty packaging string or with rating in range of `(3,5)`:
```
{ "$where":{"$and":[{"$eq":{"status":"Promotion"}}, {"$or":[ {"$ne":{"packaging":""}}, {"$gt":{"rating":3}}, {"$lt":{"rating":5}} ] } ] } }
```

## Projection
In order to reduce amount of data, processed by datasource backend and sent by network specify only desired fields using 
`Select` option. It accepts comma-separated list of fields, which will be returned by MapR-DB. For example:

```
_id,name,released_date,status,packaging,rating
```

## Time range

Grafana allows you to query metrics within some 
[Time range](http://docs.grafana.org/reference/timerange/#time-range-controls). In order to use time range with 
Raw Documents Metrics you need to specify time field using `Time field` option. It must point to any document field, 
which contains valid time value. Currently the following time field values are supported:
* Time type values

```json

{  
   "id":"1",
   "time_field":{  
      "$time":"14:35:28.981"
   }
}

```

* Timestamp type values

```json

{  
   "id":"2",
   "time_field":{  
      "$date":"2017-04-24T22:35:28.981Z"
   }
}

```

* Date type values

```json

{  
   "id":"3",
   "time_field":{  
      "$dateDay":"2017-04-23"
   }
}

```

* Numeric Unix Epoch value:

```json

{  
   "id":"4",
   "time_field":1514813625
}

```

## Plugin documentation links

* [MapR-DB JSON Grafana Plugin Overview](001-overview.md)
* [Installation](002-installation.md)
* [Raw Documents metrics support](003-raw-documents-support.md)
* [Time series metrics support](004-time-series-support.md)
