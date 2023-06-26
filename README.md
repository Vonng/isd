# ISD —— Integrated Surface Dataset

[中文说明](README_ZH.md)

Fetch, Parse, Analyze, and Visualize Integrated Surface Weather Station Dataset. [Demo](https://demo.pigsty.cc/d/isd-overview)

Including 30000 meteorology station, daily, sub-hourly observation records, from 1900-2023.

[![ISD Overview](https://github.com/Vonng/isd/assets/8587410/827c1961-6426-46c6-9fb1-25cd93507924)](https://demo.pigsty.cc/d/isd-overview)

It is recommended to use with [Pigsty](https://github.com/Vonng/pigsty), the battery-included PostgreSQL distribution with Grafana & echarts for visualization. It will setup everything for your with `make all`;

Otherwise, you'll have to provide your own PostgreSQL instance, and setup grafana dashboards manually.



## Quick Start

**Clone this repo**

```bash
git clone https://github.com/Vonng/isd.git; cd isd;
```

**Prepare a PostgreSQL Instance**

Provide `PGURL` in [`Makefile`](Makefile) or export as an environment variable.

```bash
make sql              # setup postgres schema on target database
```

**Get isd station metadata**

The basic station metadata can be downloaded and loaded with:

```bash
make reload-station   # equivalent to get-station + load-station
```

**Fetch and load `isd.daily`**

To load `isd.daily` dataset, which is organized by yearly tarball files.
You can download the raw data from noaa and parse with isd parser

```bash
make get-parser       # download parser binary from github, you can just build with: make build
make reload-daily     # download and reload latest daily data and re-calculates monthly/yearly data
```

**Load Parsed Stable CSV Data**

Or just load the pre-parsed stable part from GitHub.
Which is well-formatted CSV that does not require an isd parser.

```bash
make get-stable       # download stable isd.daily dataset from Github
make load-stable      # load downloaded stable isd.daily dataset into database
```




## More Data

There are two parts of isd datasets needs to be regularly updated: station metadata & `isd.daily` of the latest year, you can reload them with:

```bash
make reload           # reload-station + reload-daily
```

You can download and load `isd.daily` in a specific year with:

```bash
bin/get-daily  2022                   # get daily observation summary of a specific year (1900-2023)
bin/load-daily 'postgres:///' 2022    # load daily data of a specific year 
```

You can also download and load `isd.hourly` in a specific year with:

```bash
bin/get-hourly  2022                  # get hourly observation record of a specific year (1900-2023)
bin/load-hourly 'postgres:///' 2022   # load hourly data of a specific year 
```


## Data

### Dataset

There are four official datasets

| Dataset     | Sample                                             | Document                                               | Comments                            |
|-------------|----------------------------------------------------|--------------------------------------------------------|-------------------------------------|
| ISD Hourly  | [isd-hourly-sample.csv](doc/isd-hourly-sample.csv) | [isd-hourly-document.pdf](doc/isd-hourly-document.pdf) | (Sub)Hour observation records       |
| ISD Daily   | [isd-daily-sample.csv](doc/isd-daily-sample.csv)   | [isd-daily-format.txt](doc/isd-daily-format.txt)       | Daily summary                       |
| ISD Monthly | N/A                                                | [isd-gsom-document.pdf](doc/isd-gsom-document.pdf)     | Not used, Generate from `isd.daily` |
| ISD Yearly  | N/A                                                | [isd-gsoy-document.pdf](doc/isd-gsoy-document.pdf)     | Not used, Generate from `isd.daily` |


**Daily Dataset**

- Tarball size 2.8GB (until 2023-06-24)
- Table size 24GB, Index size 6GB, Total size in PostgreSQL = 30GB
- If timescaledb compression is used, it will be compressed to around 4.5GB

**Hourly dataset**

- Tarball size 117GB
- Table size 1TB+ , Index size 600GB+
 


### Schema

- [sql/1_schema.sql](sql/1_schema.sql) : isd schema
- [sql/2_record.sql](sql/2_record.sql) : daily, monthly, yearly table schema
- [sql/3_hourly.sql](sql/3_hourly.sql) : optional hourly data schema
- [sql/4_data.sql](sql/4_data.sql) : dict, map, country data


```sql
CREATE TABLE isd.station
(
    station    VARCHAR(12) PRIMARY KEY,
    usaf       VARCHAR(6) GENERATED ALWAYS AS (substring(station, 1, 6)) STORED,
    wban       VARCHAR(5) GENERATED ALWAYS AS (substring(station, 7, 5)) STORED,
    name       VARCHAR(32),
    country    VARCHAR(2),
    province   VARCHAR(2),
    icao       VARCHAR(4),
    location   GEOMETRY(POINT),
    longitude  NUMERIC GENERATED ALWAYS AS (Round(ST_X(location)::NUMERIC, 6)) STORED,
    latitude   NUMERIC GENERATED ALWAYS AS (Round(ST_Y(location)::NUMERIC, 6)) STORED,
    elevation  NUMERIC,
    period     daterange,
    begin_date DATE GENERATED ALWAYS AS (lower(period)) STORED,
    end_date   DATE GENERATED ALWAYS AS (upper(period)) STORED
);
```


```sql
CREATE TABLE IF NOT EXISTS isd.daily
(
    station     VARCHAR(12) NOT NULL, -- station number 6USAF+5WBAN
    ts          DATE        NOT NULL, -- observation date
    -- temperature & dew point
    temp_mean   NUMERIC(3, 1),        -- mean temperature ℃
    temp_min    NUMERIC(3, 1),        -- min temperature ℃
    temp_max    NUMERIC(3, 1),        -- max temperature ℃
    dewp_mean   NUMERIC(3, 1),        -- mean dew point ℃
    -- pressure
    slp_mean    NUMERIC(5, 1),        -- sea level pressure (hPa)
    stp_mean    NUMERIC(5, 1),        -- station pressure (hPa)
    -- visible distance
    vis_mean    NUMERIC(6),           -- visible distance (m)
    -- wind speed
    wdsp_mean   NUMERIC(4, 1),        -- average wind speed (m/s)
    wdsp_max    NUMERIC(4, 1),        -- max wind speed (m/s)
    gust        NUMERIC(4, 1),        -- max wind gust (m/s) 
    -- precipitation / snow depth
    prcp_mean   NUMERIC(5, 1),        -- precipitation (mm)
    prcp        NUMERIC(5, 1),        -- rectified precipitation (mm)
    sndp        NuMERIC(5, 1),        -- snow depth (mm)
    -- FRSHTT (Fog/Rain/Snow/Hail/Thunder/Tornado)
    is_foggy    BOOLEAN,              -- (F)og
    is_rainy    BOOLEAN,              -- (R)ain or Drizzle
    is_snowy    BOOLEAN,              -- (S)now or pellets
    is_hail     BOOLEAN,              -- (H)ail
    is_thunder  BOOLEAN,              -- (T)hunder
    is_tornado  BOOLEAN,              -- (T)ornado or Funnel Cloud
    -- record count
    temp_count  SMALLINT,             -- record count for temp
    dewp_count  SMALLINT,             -- record count for dew point
    slp_count   SMALLINT,             -- record count for sea level pressure
    stp_count   SMALLINT,             -- record count for station pressure
    wdsp_count  SMALLINT,             -- record count for wind speed
    visib_count SMALLINT,             -- record count for visible distance
    -- temp marks
    temp_min_f  BOOLEAN,              -- aggregate min temperature
    temp_max_f  BOOLEAN,              -- aggregate max temperature
    prcp_flag   CHAR,                 -- precipitation flag: ABCDEFGHI
    PRIMARY KEY (station, ts)
); -- PARTITION BY RANGE (ts);

```



<details><summary>ISD Hourly</summary>

```sql
CREATE TABLE IF NOT EXISTS isd.hourly
(
    station    VARCHAR(12) NOT NULL, -- station id
    ts         TIMESTAMP   NOT NULL, -- timestamp
    -- air
    temp       NUMERIC(3, 1),        -- [-93.2,+61.8]
    dewp       NUMERIC(3, 1),        -- [-98.2,+36.8]
    slp        NUMERIC(5, 1),        -- [8600,10900]
    stp        NUMERIC(5, 1),        -- [4500,10900]
    vis        NUMERIC(6),           -- [0,160000]
    -- wind
    wd_angle   NUMERIC(3),           -- [1,360]
    wd_speed   NUMERIC(4, 1),        -- [0,90]
    wd_gust    NUMERIC(4, 1),        -- [0,110]
    wd_code    VARCHAR(1),           -- code that denotes the character of the WIND-OBSERVATION.
    -- cloud
    cld_height NUMERIC(5),           -- [0,22000]
    cld_code   VARCHAR(2),           -- cloud code
    -- water
    sndp       NUMERIC(5, 1),        -- mm snow
    prcp       NUMERIC(5, 1),        -- mm precipitation
    prcp_hour  NUMERIC(2),           -- precipitation duration in hour
    prcp_code  VARCHAR(1),           -- precipitation type code
    -- sky
    mw_code    VARCHAR(2),           -- manual weather observation code
    aw_code    VARCHAR(2),           -- auto weather observation code
    pw_code    VARCHAR(1),           -- weather code of past period of time
    pw_hour    NUMERIC(2),           -- duration of pw_code period
    -- misc
    -- remark     TEXT,
    -- eqd        TEXT,
    data       JSONB                 -- extra data
) PARTITION BY RANGE (ts);
```

</details>



## Parser

There are two parsers: [`isdd`](parser/isdd/isdd.go) and [`isdh`](parser/isdh/isdh.go), which takes noaa original yearly tarball as input, generate CSV as output (which could be directly consumed by PostgreSQL `COPY` command). 

```bash
NAME
        isd -- Intergrated Surface Dataset Parser

SYNOPSIS
        isd daily   [-i <input|stdin>] [-o <output|stout>] [-v]
        isd hourly  [-i <input|stdin>] [-o <output|stout>] [-v] [-d raw|ts-first|hour-first]

DESCRIPTION
        The isd program takes noaa isd daily/hourly raw tarball data as input.
        and generate parsed data in csv format as output. Works in pipe mode

        cat data/daily/2023.tar.gz | bin/isd daily -v | psql ${PGURL} -AXtwqc "COPY isd.daily FROM STDIN CSV;" 

        isd daily  -v -i data/daily/2023.tar.gz  | psql ${PGURL} -AXtwqc "COPY isd.daily FROM STDIN CSV;"
        isd hourly -v -i data/hourly/2023.tar.gz | psql ${PGURL} -AXtwqc "COPY isd.hourly FROM STDIN CSV;"

OPTIONS
        -i  <input>     input file, stdin by default
        -o  <output>    output file, stdout by default
        -p  <profpath>  pprof file path, enable if specified
        -d              de-duplicate rows for hourly dataset (raw, ts-first, hour-first)
        -v              verbose mode
        -h              print help

```



## UI

**ISD Overview**

Show all stations on a world map.

[![ISD Overview](https://github.com/Vonng/isd/assets/8587410/827c1961-6426-46c6-9fb1-25cd93507924)](ui/isd/isd-overview.json)


**ISD Country**

Show all stations among a country.

[![ISD Country](https://github.com/Vonng/isd/assets/8587410/9a21ed5d-8540-4410-b582-a25fa88e9186)](ui/isd/isd-country.json)


**ISD Station**

Visualize station metadata and daily/monthly/yearly summary


<details><summary>ISD Station Dashboard</summary>

[![ISD Station](https://github.com/Vonng/isd/assets/8587410/4c72d529-d309-4629-97e0-ed8315c3a7a9)](ui/isd/isd-station.json)

</details>


**ISD Detail**

Visualize hourly observation raw metrics. 

<details><summary>ISD Station Dashboard</summary>

[![ISD Detail](https://github.com/Vonng/isd/assets/8587410/726fad65-32c9-4a4c-b233-9ede7b0ae20d)](ui/isd/isd-detail.json)

</details>