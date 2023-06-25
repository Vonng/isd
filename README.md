# ISD —— Integrated Surface Dataset


## SYNOPSIS

Download, Parse, Visualize Integrated Surface Weather Station Dataset. [Demo](https://demo.pigsty.cc/d/isd-overview)

Including 30000 meteorology station, daily, sub-hourly observation records, from 1900-2023.

It is recommended to use with [Pigsty](https://github.com/Vonng/pigsty), the battery-included PostgreSQL distribution with Grafana & echarts for visualization.

[![](doc/img/isd-overview.jpg)](https://demo.pigsty.cc/d/isd-overview)




## Quick Start

Prepare a PostgreSQL Instance, and provide `PGURL` in [`Makefile`](Makefile) or pass it as environment variable.

You will need isd **parser** to load daily/hourly data into database:

```bash
make get-parser   # just download the parser binary from github
make build        # or build with go if you have the go toolchain
```

To setup database schema (required) and grafana dashboards (optional):

```bash
make sql        # load isd database schema into postgres (via PGURL env)
make ui         # setup grafana dashboards
```

To load some data into database, you can use the following shortcuts

```bash
make data               # download and load station/isd.daily(2023) data 
make reload-station     # download and reload station metadata only
make reload-daily       # download and reload latest daily data and re-calculates monthly/yearly data
```

Or if you don't want to parse the raw data from NOAA, just download & load the parsed CSV (stable) with:

```bash
make reload-station     # station metadata still need to be downloaded

# load cleansed, already parsed csv isd.daily stable dataset (~2023.06.24)
make get-stable         # download stable isd.daily dataset from Github 
make load-stable        # load downloaded stable isd.daily dataset into database
```


### Get More

If you wish to get daily/hourly data of a specific year, use `get-daily`, and `get-hourly` scripts.

```bash
bin/get-daily  2022   # get daily observation summary of a specific year (1900-2023)
bin/get-hourly 2022   # get sub-hourly observation record of a specific year (1900-2023)
```

And you can load the downloaded data into database via `load-daily` and `load-hourly` scripts.

```bash
bin/load-daily  'postgres:///' 2021  # load isd.daily of year 2021 into database
bin/load-hourly 'postgres:///' 2022  # load isd.hourly of year 2022 into database
```



## Data

### Dataset

| Dataset      | Sample                                               | Document                                                | Comments                              |
| ----------- | -------------------------------------------------- | ------------------------------------------------------ | --------------------------------- |
| ISD Hourly  | [isd-hourly-sample.csv](doc/isd-hourly-sample.csv) | [isd-hourly-document.pdf](doc/isd-hourly-document.pdf) | (Sub-) Hour oberservation records |
| ISD Daily   | [isd-daily-sample.csv](doc/isd-daily-sample.csv)   | [isd-daily-format.txt](doc/isd-daily-format.txt)       | Daily summary                     |
| ISD Monthly | N/A                                                | [isd-gsom-document.pdf](doc/isd-gsom-document.pdf)     | Not used, gen from daily          |
| ISD Yearly  | N/A                                                | [isd-gsoy-document.pdf](doc/isd-gsoy-document.pdf)     | Not used, gen from monthly        |

Daily Data: Original tarball size 3GB, table size 24 GB

Hourly Data: Original tarball size 105GB, Table size 1TB (+600GB Indexes).

It is recommended have at least 40GB for daily dataset in target PostgreSQL, and 2TB for full hourly dataset. 



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
    dewp_mean   NUMERIC(3, 1),        -- mean dew point (℃)
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

Dashboard [definition](ui/isd/isd-overview.json)

**ISD Station**

Dashboard [definition](ui/isd/isd-station.json)

![](doc/img/isd-station.jpg)

**ISD Monthly**

Dashboard [definition](ui/isd/isd-monthly.json)

![](doc/img/isd-monthly.jpg)

