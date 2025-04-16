# Druid Same Or Null aggregator

This extension for Apache Druid provides a custom aggregation function that checks whether all values in a column are the same.
If the values are **different**, the aggregator returns `NULL`. Otherwise, it returns the **common value**.

The extension includes:
- **Native Aggregation Function**: `sameOrNull`
- **SQL Function**: `SAME_OR_NULL`

This feature is particularly useful for scenarios where you want to detect homogeneity within a group or handle inconsistent data gracefully.

---
## **Native Aggregation Function**
The `sameOrNull` native aggregation function ensures all values are identical, returning the common value or `NULL` otherwise.

### **Function Name**
`sameOrNull`

### **Parameters**
| Parameter    | Type   | Default Value         | Description                                                                                                                                                |
|--------------|--------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`       | String | `required`            | Must be `sameOrNull`                                                                                                                                       |
| `fieldName`  | String | `required`            | The name of the input column to aggregate.                                                                                                                 |
| `name`       | String | *same as `fieldName`* | The name of the output metric (optional).                                                                                                                  |
| `maxSize`    | int    | `1024`                | Memory configuration to hold the value during intermediate computation, depend on type of column (optional).                                               |
| `outputType` | String | `COMPLEX`             | Result type (optional). See [ColumnType](https://javadoc.io/doc/org.apache.druid/druid-processing/30.0.0/org/apache/druid/segment/column/ColumnType.html). |

### **Example Configuration**
Below is an example JSON query using the native `sameOrNull` aggregation function:

```json
{
   "queryType": "timeseries",
   "dataSource": {
      "type": "table",
      "name": "<data-source>"
   },
   "intervals": {
      "type": "intervals",
      "intervals": [
         "<start>/<end>"
      ]
   },
   "granularity": {
      "type": "all"
   },
   "aggregations": [
      {
         "type": "sameOrNull",
         "name": "<output-name>",
         "fieldName": "<column-name>",
         "maxSize": "<max-memory-buffer>",
         "outputType": "LONG"
      },
      {
         "type": "sameOrNull",
         "fieldName": "<column-name-2>"
      }
   ]
}
```

## **SQL Function**
The SQL equivalent of `sameOrNull`.

### **Function Name**
`SAME_OR_NULL`

```sql
SAME_OR_NULL(input_column)
```

### Example Query

```sql
SELECT 
    SAME_OR_NULL(value_column_1) AS same_value_1, 
    SAME_OR_NULL(value_column_2) AS same_value_2
FROM 
    my_data_source
```
---

### Build

To build the extension, run `mvn package` and you'll get a file in `target` directory.
Unpack the `tar.gz`.

```
$ tar xzf target/druid-same-or-null-31.0.2-bin.tar.gz
$ ls druid-same-or-null/
LICENSE           druid-same-or-null-31.0.2-javadoc.jar        druid-same-or-null-31.0.2.jar
README.md         druid-same-or-null-31.0.2-sources.jar
```

---

### Install

To install the extension:

1. Copy `druid-same-or-null` into your Druid `extensions` directory.
2. Edit `conf/_common/common.runtime.properties` to add `druid-same-or-null` to `druid.extensions.loadList`. (
   Edit `conf-quickstart/_common/common.runtime.properties` too if you are using the quickstart config.)
   It should look like: `druid.extensions.loadList=["druid-same-or-null"]`. There may be a few other extensions
   there too.
3. Restart Druid.
