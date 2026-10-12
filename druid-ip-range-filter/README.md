# Druid IP Range Filter Extension

This repository provides an Apache Druid extension for filtering based on IP address ranges. This filter is useful in
network security and data analysis, where identifying records associated with specific IPs is essential.

It supports both string-based dimension functions and native dimension functions with `Complex<ipRange>` and
`Complex<ipRangeArray>` types.

The extension includes:

* Enhanced IP Range filtering using both IPv4 and IPv6 formats.
* Virtual Columns for dynamic filtering within queries.
* Support for CIDR, explicit ranges (e.g., 192.168.1.10-192.168.1.20), and fixed sets of IP addresses.

---

## String Dimension Functions

### **MultiRangeIPFilter (`type = ip_multi_range`)**

`MultiRangeIPFilter` allows filtering of records containing a column with IP addresses based on the set of IP
address ranges or CIDR blocks specified in the query. Specifically, a row is matched if the specified column 
contains an IP address that is within any of the ranges specified.

**Example:**

```json
{
  "type": "ip_multi_range",
  "dimension": "ipColumn",
  "ranges": [
    "192.168.1.1/192.168.1.50",
    "2001:0db8:85a3:0000:0000:8a2e:0370:7334/2001:0db8:85a3:0000:0000:8a2e:0370:7399",
    "192.168.1.0/24"
  ],
  "ignoreVersionMismatch": false
}
```

Parameter:
* `type`: should be `ip_multi_range` as type.
* `dimension`: Specifies the dimension (column) holding the IP address to be filtered.
* `ranges`: Defines the IP ranges with the format `lower/upper` or CIDR block. We can put IPv4 and IPv6 in the same set.
* `ignoreVersionMismatch`: When set to true, addresses that don’t match the defined IP type (IPv4 vs. IPv6) will be
  ignored if the ranges contain only one IP type, and the provided address is of a different type (default: false).

### **RangeMatchingIpFilter (`type = ip_range_match`)**

`RangeMatchingIpFilter` filters rows based on provided IP addresses by matching them against stored IPs, ranges, or
CIDR blocks in a string-typed column. It supports both IPv4 and IPv6 formats.

Supported data formats:

* Single IP (e.g. `192.168.1.10`).
* Explicit ranges (e.g. `192.168.1.10-192.168.1.20`). Accepts hyphen, en-dash, or slash-separated ranges.
* CIDR blocks (e.g. `192.168.1.0/24`)

**Example:**

```json
{
  "type": "ip_range_match",
  "dimension": "range",
  "values": [
    "192.168.1.50",
    "8.8.8.9"
  ],
  "ignoreVersionMismatch": false
}
```

Parameters:

* `type`: Must be `"ip_range_match"` as type.
* `dimension`: Name of the String-type dimension containing stored IP ranges.
* `values`: List of IP addresses to match against the stored ranges. Can include both IPv4 and IPv6.
* `ignoreVersionMismatch`: When set to true, addresses that don’t match the defined IP type (IPv4 vs. IPv6) will be
  ignored (default: false).

### **IPRangeFilteredVirtualColumn (`type = ip-range-filtered`)**

This virtual column filters IP addresses based on whether they fall within any of the IP ranges in a specified column.
Only the IPs explicitly listed in the values property that are also present in the delegate column's range will be
included in the output.

This feature is useful in scenarios where you need to filter or validate IP addresses against predefined ranges without
applying complex filtering logic at the application level.

**Example:**

```json
{
  "type": "ip-range-filtered",
  "name": "output-name",
  "delegate": "ips",
  "values": [
    "10.162.59.18",
    "10.161.12.13"
  ]
}
```

Parameters

* `type`: Must be `"ip-range-filtered"` as type.
* `name`:  Output name of the virtual column in the query results.
* `delegate`: Name of the column containing IP ranges to match against.
* `values`: A list of IP addresses to filter. Only IPs that exist in both this list and within the delegate column's
  range will be included in the output.

---

## Native Dimension Functions

### Native Dimension Types

This repository also includes native support for IP addresses, enabling faster filtering and virtual column
functionality. To use this feature, define your column types as follows:

1. **COMPLEX\<ipRange\> (`type: ipRange`)**: For a single IP address, use the `ipRange` type. It supports:
    - `IPv4` or `IPv6` addresses
    - `CIDR` notation
    - `lower-upper`, `lower/high`, or `lower -> upper` range formats

      **Example**:
    ```json
        {
            "dimensionsSpec": {
                "dimensions": [
                  {
                    "type": "ipRange",
                    "name": "ip_addresses"
                  }
                ]
             }
      }
    ```

2. **COMPLEX\<ipRangeArray\> (`type: ipRangeArray`)**: For multiple IP addresses, use the
   `ipRangeArray` type, which accepts a list of ipRange values.

   **Example**:
    ```json
      {
        "dimensionsSpec": {
            "dimensions": [
              {
                "type": "ipRangeArray",
                "name": "ipset_contents"
              }
            ]
          }
    }
    ```

The following filters are available for use with these native types:

1. **IPNativeRangeMatchingFilter (`type = ip_native_match`)** to match IP ranges directly
2. **IPNativeRangeArrayFilteredVirtualColumn (`type = ip-native-filtered`)** for use with virtual columns


### **IPNativeRangeMatchingFilter (`type = ip_native_match`)**

Same as `RangeMatchingIpFilter`, it enables filtering based on a provided list of IP addresses.
This filter is useful when retrieving rows where the stored IP ranges contain the provided IPs.

**Example:**

```json
{
  "type": "ip_native_match",
  "dimension": "range",
  "values": [
    "192.168.1.50",
    "8.8.8.9"
  ]
}
```

Parameters

* `type`: Must be `"ip_native_match"` as type.
* `dimension`: Name of the dimension (column) of type `Complex<ipRangeArray>`.
* `values`: List of IP addresses to match against the stored ranges. Can include both IPv4 and IPv6.

### IPNativeRangeArrayFilteredVirtualColumn (`type = ip-native-filtered`)

A virtual column that filters IPs based on whether they fall within any IP range in a `Complex<ipRangeArray>` dimension.
Equivalent to `ip-range-filtered` but dedicated for native `ipRangeArray` types.

**Example:**

```json
{
  "type": "ip-native-filtered",
  "name": "output-name",
  "delegate": "ips",
  "values": [
    "10.162.59.18",
    "10.161.12.13"
  ]
}
```

Parameters

* `type`: Must be `ip-native-filtered` as type.
* `name`:  Output name of the virtual column in the query results.
* `delegate`: Name of the column of type `Complex<ipRangeArray>` to match against.
* `values`: A list of IP addresses to filter. Only IPs that exist in both this list and within the delegate column's 
  range will be included in the output.

---

### Transform native type to a multi-value string

To convert the complex native IP format into a string use the `ip_native_stringify` expression function.

**Example:**

```json
{
  "type": "expression",
  "name": "ip-address-str",
  "expression": "ip_native_stringify(\"<column-name\")"
}
```

## How to Use

Using Filters

1. Add the desired filter (`ip_range_match` or `ip_native_match`) in the filter section of your query JSON.
2. Provide necessary parameters as shown in examples.
3. Execute the query against your Druid cluster. The filter will limit the results based on the specified IP conditions.

Using Virtual Columns

1. Define a virtual column (`ip-range-filtered` or `ip-native-filtered`) in the `virtualColumns` section of your query 
   JSON.
2. Provide the delegate column and values.
3. Execute the query against your Druid cluster. The filter will limit the results based on the specified IP conditions.

---

## Build

To build the extension, run `mvn package` and you'll get a file in `target` directory.
Unpack the `tar.gz`.

```bash
$ tar xzf target/druid-ip-range-filter-31.0.1-bin.tar.gz
$ ls druid-ip-range-filter/
LICENSE                  README.md               druid-ip-range-filter-31.0.1.jar
```

---

## Installation

To install the extension:

1. Copy `druid-ip-range-filter` into your Druid `extensions` directory.
2. Edit `conf/_common/common.runtime.properties` to add `"druid-ip-range-filter"` to `druid.extensions.loadList` 
   parameter. It should look like: 

   ```
   druid.extensions.loadList=["druid-ip-range-filter"]
   ```
   There may be a few other extensions there too.

3. Restart your Druid cluster.
