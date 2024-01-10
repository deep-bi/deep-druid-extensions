# Druid exact distinct count aggregator

Provides a more reliable and efficient way to count the number of unique values in a column than existent approximate
aggregators.

This aggregator uses a HashSet to store the unique values, which provides constant-time lookup and insertion.

Nulls and empty strings are ignored by the aggregator. This means that they will not be counted as unique values.

To use this Apache Druid extension, include `exact-distinct-count-aggregator` in the extensions load list.

You can add it to your Druid native query as follows:

```
{
  "queryType": "timeseries",    
  "dataSource": {
    "type": "table",
    "name": "wikipedia"
  },
  "intervals": {
    "type": "intervals",
    "intervals": [
      "-146136543-09-08T08:23:32.096Z/146140482-04-24T15:36:27.903Z"
    ]
  },
  "filter": {
    "type": "not",
    "field": {
      "type": "expression",
      "expression": "isNew"
    }
  },
  "granularity": {
    "type": "all"
  },
  "aggregations": [
    {      
      "type": "exactDistinctCount", 
      "name": "test",                 // name to be displayed
      "fieldNames": ["comment", "cityName"],         // fields to be counted
      "maxNumberOfValues": 5000     
      "failOnLimitExceeded": true    
    }
  ]
}
```

# Configuration options

| Name                  | Description                                                                                                                            | Possible Values  | Default |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|------------------|---------|
| `maxNumberOfValues`   | Max number of values to be aggregated                                                                                                  | Positive Integer | 10000   |
| `failOnLimitExceeded` | Defines behavior on reaching the limit.<br/> `true`: throwing an exception<br/> `false`: logging warning and returning the limit value | Boolean          | `False` |

# Limitations

- Aggregator is not supported in GroupBy queries.
