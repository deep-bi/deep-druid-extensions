## Druid Filtering Emitter

Extension for [Apache Druid](https://druid.apache.org/) allowing to filter the metrics emmited by other emitters.

### Build

To build the extension, run `mvn package` and you'll get a file in `target` directory.
Unpack the `tar.gz`.

```
$ tar xzf target/druid-filtering-emitter-25.0.0-bin.tar.gz
$ ls druid-filtering-emitter/
LICENSE                  README.md                druid-filtering-emitter-25.0.0.jar
```

### Install

To install the extension:

1. Copy `druid-filtering-emitter` into your Druid `extensions` directory.
2. Edit `conf/_common/common.runtime.properties` to add `"druid-filtering-emitter"` to `druid.extensions.loadList`. (
   Edit `conf-quickstart/_common/common.runtime.properties` too if you are using the quickstart config.)
   It should look like: `druid.extensions.loadList=["druid-filtering-emitter"]`. There may be a few other extensions
   there
   too.
3. Restart Druid.

### Configure

To enable the filter you need use:

```hocon
druid.emitter = filtering
druid.emitter.filtering.emitter = <inner-emiter>
```

Replace `<inner-emiter>` with one of the emitter provided by the Druid. For example: `logging` (you would also need to
configure chosen inner emitter). The filtering logic is setup with these properties:

| Property                             | Description                                                                                                      |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `druid.emitter.filtering.key`        | A key to be extracted from the emitted event. Optional. Default value is `"metric"`                              |
| `druid.emitter.filtering.allowList`  | A list of strings. Names on this list passes to the inner emitter.                                               |
| `druid.emitter.filtering.blockList`  | A list of strings. Names on this list are ignored.                                                               |
| `druid.emitter.filtering.startsWith` | A list of strings. Event passes to the inner emitter when the value starts with one of the strings from this list.|
| `druid.emitter.filtering.regexMatch` | A list of strings. Event passes to the inner emitter when the value matches one of the regex patterns from this list. |

The filters are passed from the top to bottom.

1. If event matches `allowList` (and `allowList` is defined) then it is emitted, otherwise it is pass to lower filter. If no other filter is defined then event is blocked.
2. If event matches `blockList` (and `blockList` is defined) then it is blocked, otherwise it is pass to lower filter. If no other filter is defined then event is emitted.
3. If event matches `startsWith` (and `startsWith` is defined) then it is emitted, otherwise it is pass to lower filter. If no other filter is defined then event is blocked.
4. If event matches `regexMatch` (and `regexMatch` is defined) then it is emitted, otherwise the event is blocked.

You cannot define `allowList` and `blockList` together without defining at least one other filter. So:
```hocon
druid.emitter.filtering.allowList = ["a/1"]
druid.emitter.filtering.blockList = ["b/2"]
```
is not allowed. All other combinations of filters are permitted.

```hocon
druid.extensions.loadList = ["druid-filtering-emitter"]
druid.monitoring.monitors = ["org.apache.druid.java.util.metrics.JvmMonitor"]

# Configure the filtering emitter
druid.emitter = filtering
druid.emitter.filtering.emitter = logging
druid.emitter.filtering.startsWith = ["jvm/"]

# Configure properties of the wrapped emiter
druid.emitter.logging.logLevel = info
```
