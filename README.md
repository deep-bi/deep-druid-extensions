# Custom extensions pack for [Apache Druid](https://druid.apache.org/)

| Extension name                     | Description                                                                                                                        | Docs                                                      |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| Druid-Azure-extensions             | Extensions for Azure with fixed connection to Azure Government                                                                     | [README.md](druid-azure-extensions/README.md)             |
| Druid-encrypting-password-provider | Password provider extension that encrypts runtime configuration passwords using AES algorithm with a hardcoded encryption key.     | [README.md](druid-encrypting-password-provider/README.md) |
| Druid-filtering-emitter            | Extension allowing to filter the metrics emitted by other emitters.                                                                | [README.md](druid-filtering-emitter/README.md)            |
| Exact-distinct-count-aggregator    | Provides a more reliable and efficient way to count the number of unique values in a column than existent approximate aggregators. | [README.md](exact-distinct-count-aggregator/README.md)    |

Extensions from this package can be automatically downloaded using
the [pull-deps tool](https://druid.apache.org/docs/latest/operations/pull-deps/).

For example, to download the Deep Druid Filtering Emitter (v28.0.0) and place it into the `/extensions` directory, execute the
following command in the `$DRUID_HOME` directory:

```bash
java -classpath "./lib/*" org.apache.druid.cli.Main tools pull-deps -c bi.deep:druid-filtering-emitter:28.0.0
```
