# Deep.BI extensions pack for Apache Druid

This repository is for [Apache Druid](https://druid.apache.org/) extensions that we have created
at [Deep.BI](https://www.deep.bi/).

| Extension name                  | Description                                                                                                                                                            | Docs                                                                                                       |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| druid-filtering-emitter         | Extension allowing to filter the metrics emitted by other emitters.                                                                                                    | [README.md](druid-filtering-emitter/README.md)                                                             |
| exact-distinct-count-aggregator | Provides a more reliable and efficient way to count the number of unique values in a column than existent approximate aggregators.                                     | [README.md](exact-distinct-count-aggregator/README.md)                                                     |
| druid-same-or-null-aggregator   | Provides an aggregation function that returns NULL if the values are different                                                                                         | [README.md](druid-same-or-null-aggregator/README.md)                                                       |
| druid-ingestion-time            | Extension allowing to define a current timestamp transformation. Available in this pack up to version 37.0.0. Since Druid 38+ use `now()` expression function instead. | [Using now() in ingestion](https://druid.apache.org/docs/latest/querying/math-expr#using-now-in-ingestion) |
| druid-ip-range-filter           | Extension for filtering based on IP address ranges                                                                                                                     | [README.md](druid-ip-range-filter/README.md)                                                               |

Extensions from this package can be automatically downloaded using
the [pull-deps tool](https://druid.apache.org/docs/latest/operations/pull-deps/).

For example, to download the Deep Druid Filtering Emitter (v33.0.0) and place it into the `/extensions` directory,
execute the
following command in the `$DRUID_HOME` directory:

```bash
java -classpath "./lib/*" org.apache.druid.cli.Main tools pull-deps -c bi.deep:druid-filtering-emitter:33.0.0
```
