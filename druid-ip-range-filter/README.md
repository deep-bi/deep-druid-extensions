## Druid IP Range Filter Extension
*This repository contains IP Range filter extensions*

## Introduction

Apache Druid is a high-performance, column-oriented, distributed data store designed for fast analytics on large datasets.
One of the powerful features of Druid is its filtering capabilities, which allow users to limit the data returned by queries based on specific criteria.

This repository includes three custom filters designed to enhance IP address filtering capabilities within Druid:

1. **SingleTypeIPRangeFilter (`type = ip_single_range`)**
2. **MultiRangeIPFilter (`type = ip_multi_range`)**
3. **FixedSetIPFilter (`type = ip_fixed_range`)**

These filters enable users to filter records based on IP address ranges and fixed sets of IPs, supporting both IPv4 and IPv6 formats.

---

## Filters Overview

### 1. SingleTypeIPRangeFilter

**Description**:
`SingleTypeIPRangeFilter` allows filtering of records based on a single range of IP addresses of the same type (either IPv4 or IPv6).
It simplifies the process of retrieving data relevant to a specific IP range.

**Usage**:
This filter is particularly useful in scenarios where data needs to be analyzed or aggregated based on specific IP ranges, 
such as monitoring traffic from a particular geographical location or service.

**Example**:
```json
{
  "type": "ip_single_range",
  "dimension": "ipColumn",
  "range": {
    "lower": "192.168.1.1",
    "upper": "192.168.1.100",
    "lowerOpen": false,
    "upperOpen": false
  },
  "ignoreVersionMismatch": true
}
```

#### Parameter Descriptions
* `type`: should be `ip_single_range` as type.
* `dimension`: Specifies the dimension (column) holding the IP address to be filtered.
* `range`: Defines the IP range with optional boundary settings.
  * `lower`: The inclusive/exclusive lower bound of the range.
  * `upper`: The inclusive/exclusive upper bound of the range.
  * `lowerOpen`: Boolean indicating if lower bound is open in the range of values defined by the range (">" instead of ">="), (default: false )
  * `upperOpen`: Boolean indicating if upper bound is open on the range of values defined by range ("<" instead of "<="), (default: false )
* `ignoreVersionMismatch`: When true, addresses that don’t match the range type (IPv4 vs IPv6) are ignored (default: true).

### 2. MultiRangeIPFilter

**Description:**
`MultiRangeIPFilter` extends the functionality by allowing multiple ranges of IP addresses to be specified, supporting both IPv4 and IPv6 addresses.
This filter is ideal for scenarios where multiple network ranges need to be included in the filtering process.

**Usage:**
Use this filter when you need to analyze data from several non-contiguous IP address ranges simultaneously. 
It can be beneficial for security analytics, traffic analysis, and compliance reporting.

**Example:**

```json
{
  "type": "ip_multi_range",
  "ranges": [
    "192.168.1.1/192.168.1.50",
    "2001:0db8:85a3:0000:0000:8a2e:0370:7334/2001:0db8:85a3:0000:0000:8a2e:0370:7399"
  ],
  "ignoreVersionMismatch": true
}
```

#### Parameter Descriptions
* `type`: should be `ip_multi_range` as type.
* `dimension`: Specifies the dimension (column) holding the IP address to be filtered.
* `ranges`: Defines the IP ranges with the format `lower/upper`. We can put IPv4 and IPv6 in the same set.
* `ignoreVersionMismatch`: When set to true, addresses that don’t match the defined IP type (IPv4 vs. IPv6) will be 
ignored if the ranges contain only one IP type, and the provided address is of a different type (default: true). (default: true).


### 3. FixedIPSetFilter

**Description:**
`FixedIPSetFilter` enables filtering based on a fixed set of specific IP addresses. This filter is useful when you want 
to focus on known individual addresses rather than ranges.

**Usage:**
This filter can be employed in security contexts, such as whitelisting or blacklisting specific IPs, making it ideal for
access control or compliance checks.

**Example:**

```json
{
  "type": "ip_fixed_range",
  "ranges": [
    "192.168.1.10",
    "192.168.1.20",
    "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
  ]
}
```

#### Parameter Descriptions
* `type`: should be `ip_fixed_range` as type.
* `dimension`: Specifies the dimension (column) holding the IP address to be filtered.
* `ranges`: Defines the IP ranges set which can contain both IPv4 and IPv6.
* `ignoreInvalidAddress`: When set true, invalid addresses will be ignored (default: true).

---

## How to Use These Filters

1. Include the Filter in Your Query:
When crafting a Druid query, include the desired filter in the filter section of your query JSON.
2.	Define Filter Parameters:
Ensure that you specify the required parameters for the chosen filter type, such as IP ranges or sets.
3.	Execute the Query:
Run the query against your Druid cluster. The filter will limit the results based on the specified IP conditions.

---

### Build

To build the extension, run `mvn package` and you'll get a file in `target` directory.
Unpack the `tar.gz`.

```
$ tar xzf target/druid-ip-range-filter-31.0.0-bin.tar.gz
$ ls druid-ip-range-filter/
LICENSE                  README.md               druid-ip-range-filter-31.0.0.jar
```

---

### Install

To install the extension:

1. Copy `druid-ip-range-filter` into your Druid `extensions` directory.
2. Edit `conf/_common/common.runtime.properties` to add `druid-ip-range-filter` to `druid.extensions.loadList`. (
   Edit `conf-quickstart/_common/common.runtime.properties` too if you are using the quickstart config.)
   It should look like: `druid.extensions.loadList=["druid-ip-range-filter"]`. There may be a few other extensions
   there too.
3. Restart Druid.