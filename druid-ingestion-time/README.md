# druid-ingestion-time

Custom Apache Druid extension that adds a `currentTimestamp` ingestion transform.

## Installation

To install the extension:

1. Copy `druid-ingestion-time` JAR file into your Druid `extensions` directory.
2. Edit `conf/_common/common.runtime.properties` to add `"druid-ingestion-time"` to `druid.extensions.loadList`
   parameter. It should look like:

   ```
   druid.extensions.loadList=[..., "druid-ingestion-time"]
   ```
   There may be a few other extensions there too.

3. Restart your Druid cluster.

## How to Use

Once the extension is loaded onto Druid, the transform can be used in `transformSpec`:

```json
{
  "transformSpec": {
    "transforms": [
      {
        "type": "currentTimestamp",
        "name": "ingestedAt"
      }
    ]
  }
}
```

Parameters:

- `type` - must be `"currentTimestamp"`
- `name` - name of the output column
 
The resulting value is the current wall-clock timestamp in epoch milliseconds, stored as a `LONG`.

## Build

To build the extension, run `mvn package` and you'll get a file in `target` directory.
Unpack the `tar.gz`.

```bash
$ tar xzf target/druid-ingestion-time-31.0.2-bin.tar.gz
$ ls druid-ip-range-filter/
LICENSE                  README.md               druid-ingestion-time-31.0.2.jar
```

---