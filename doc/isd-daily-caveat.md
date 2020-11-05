# ISD Daily Caveat

Since some partition contains DIRTY records, you can't just pour daily data into `isd_daily`.

Most of them are duplicate primary key (station, ts), which means more than one record occurs on same day for same station.

To cleanse the data, you'll have to create a table like `isd_daily` without primary key and constraints. Do the cleanse procedure there and then pouring it to `isd_daily` table.


## How to find duplicated data

```sql
SELECT count(distinct station||ts::TEXT) from isd_daily; -- 154480349
SELECT count(*) FROM isd_daily; -- 154482464
SELECT station,ts, count(*) AS cnt FROM isd_daily  group by station,ts having count(*) > 1 order by 1,2;
```

**Currently , year 1964, 1965, 1973, 1985, 2020 contains duplicated records.**


### 1964

**Case**

Year 1964 contains one duplicate record: `station = '99999988703' and ts = '1964-12-31'`

```sql
SELECT * FROM isd_daily WHERE station = '99999988703' and ts = '1964-12-31'
```

**Cause**

`daily/1964/99999988702.csv` contains a record of `station 99999988703` which conflict with file `daily/1964/99999988703.csv`

**Solution**

Delete the record from `daily/1964/99999988702.csv`

```sql
SELECT * FROM isd_daily WHERE station = '99999988703' and ts = '1964-12-31' and slp_mean = '994.6';
DELETE FROM isd_daily WHERE station = '99999988703' and ts = '1964-12-31' and slp_mean = '994.6';
```



### 1965

**Case**

There are two duplicate records for station `72252012920` in `1965-03-10` and `1965-03-11`

```sql
SELECT * FROM isd_daily where ts between '1965-03-10' and '1965-03-11' and station = '72252012920'
```

```
 72252012920 | 1965-03-10 |   2
 72252012920 | 1965-03-11 |   2
```

**Cause**

Two supplements append to `daily/1965/72252012920.csv` which contains duplicated but more specific data

**Solution**

Use extra records and overwrite existing records

```sql
DELETE
FROM isd_daily_summary
WHERE ctid IN (
    SELECT min(ctid) as ctid
    FROM isd_daily_summary
    WHERE station = '72252012920'
      AND ts between '1965-03-10' and '1965-03-11'
    group by station, ts
    having count(*) > 1
);
```


### 1973

**Case**

Station `72253512909` have duplicate records in entire Dec. 1973

```sql
SELECT *
FROM isd_daily
WHERE station = '72253512909'
  AND ts BETWEEN '1973-12-01' AND '1973-12-31'
ORDER BY station, ts, ctid
```

**Cause**

File `daily/1973/72253599999.csv` contains records of station `72253512909`.

**Solution**

It is reasonable to keep data in `daily/1973/72253512909.csv` and remove duplicated records in `daily/1973/72253599999.csv`

```sql
DELETE
FROM isd_daily
WHERE ctid IN (
    SELECT max(ctid) as ctid
    FROM isd_daily
    WHERE station = '72253512909'
      AND ts BETWEEN '1973-12-01' AND '1973-12-31'
    group by station, ts
    having count(*) > 1
);

-- 25 rows affected in 3 ms
```


### 1985

**Case*

Station `72511114751` contains duplicated observation from `1985-06-01` to `1985-12-13`


**Cause**

File `daily/1985/72511114751.csv` contains duplicated records.

**Solution**

Since appended records have more elements. It is reasonable to overwrite former with appended rows.

72511114751 号站点，出现了1985下半年的数据重复，删除前面出现的部分，后一条数据量比前一条丰富，所以删除前一条。

> you can only do this once

```sql
DELETE
FROM isd_daily
WHERE ctid IN (
    SELECT min(ctid) as ctid
    FROM isd_daily
    WHERE station = '72511114751'
      AND ts BETWEEN '1985-06-01' AND '1985-12-31'
    group by station, ts
    having count(*) > 1

-- DELETE 214
);
```


### 2020

**Case*

Duplicated records in `2020-01-01`

```sql
SELECT *
FROM isd_daily
WHERE ctid IN (
    SELECT min(ctid) as ctid
    FROM isd_daily
    WHERE ts = '2020-01-01'
    group by station
    having count(*) > 1
) ORDER BY station;
-- 2113
```

**Cause**

year 2019 dataset contains year 2020 rows.

**Solution**

You could just run `bin/load-isd-daily.sh 2020`, it will truncate 2020 partition and eliminate this case 

```sql
DELETE
FROM isd_daily
WHERE ctid IN (
    SELECT min(ctid) as ctid
    FROM isd_daily
    WHERE ts = '2020-01-01'
    group by station
    having count(*) > 1
);
```

