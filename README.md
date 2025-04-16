# Deep.BI extensions pack for Apache Druid

This repository is for [Apache Druid](https://druid.apache.org/) extensions that we have created
at [Deep.BI](https://www.deep.bi/).

| Extension name                     | Description                                                                                                                        | Docs                                                                          |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| druid-encrypting-password-provider | Password provider extension that encrypts runtime configuration passwords using AES algorithm with a hardcoded encryption key.     | [README.md](druid-encrypting-password-provider/README.md)                     |
| druid-filtering-emitter            | Extension allowing to filter the metrics emitted by other emitters.                                                                | [README.md](druid-filtering-emitter/README.md)                                |
| exact-distinct-count-aggregator    | Provides a more reliable and efficient way to count the number of unique values in a column than existent approximate aggregators. | [README.md](exact-distinct-count-aggregator/README.md)                        |
| druid-same-or-null-aggregator      | Provides an aggregation function that returns NULL if the values are different                                                     | [README.md](druid-same-or-null-aggregator/README.md)                          |
| druid-Azure-extensions             | Extensions for Azure with fixed connection to Azure Government. Available in this pack up to and including version 28.0.0.         | [Released in Apache Druid 29.0.0](https://github.com/apache/druid/pull/15523) |

Extensions from this package can be automatically downloaded using
the [pull-deps tool](https://druid.apache.org/docs/latest/operations/pull-deps/).

For example, to download the Deep Druid Filtering Emitter (v28.0.0) and place it into the `/extensions` directory,
execute the
following command in the `$DRUID_HOME` directory:

```bash
java -classpath "./lib/*" org.apache.druid.cli.Main tools pull-deps -c bi.deep:druid-filtering-emitter:31.0.2
```
