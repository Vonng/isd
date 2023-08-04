# ISD —— 集成地表气象站数据

下载、解析、分析、绘制 ISD 数据集，包括3万+气象站过去120年间的亚小时级原始观测数据。[Demo](https://demo.pigsty.cc/d/isd-overview)

[![ISD Overview](https://github.com/Vonng/isd/assets/8587410/827c1961-6426-46c6-9fb1-25cd93507924)](https://demo.pigsty.cc/d/isd-overview)

推荐与  [Pigsty](https://github.com/Vonng/pigsty) 共同使用，它提供了一个开箱即用的 PostgreSQL 实例，Grafana 与 Echarts 可视化环境。

您可以使用 `make all` 在 Pigsty 上一键完成安装，否则您需要自行准备 PostgreSQL 实例，并手动导入 Grafana 面板。



## 快速上手

**克隆本仓库**

```bash
git clone https://github.com/Vonng/isd.git; cd isd;
```

**准备一个 PostgreSQL 实例**

你应当通过 [`Makefile`](Makefile) 里的 `PGURL` 变量，或导出的环境变量来传递数据库实例的连接信息。

```bash
make sql              # setup postgres schema on target database
```

**获取并导入ISD气象站元数据**

这是一份每日更新的气象站元数据，包含了气象站的经纬度、海拔、名称、国家、省份等信息，使用以下命令下载并导入。

```bash
make reload-station   # equivalent to get-station + load-station
```

**获取并导入最新的 `isd.daily` 数据**

`isd.daily` 是一个每日更新的数据集，包含了全球各气象站的日观测数据摘要，使用以下命令下载并导入。
请注意，直接从 NOAA 网站下载的原始数据需要经过**解析**方可入库，所以你需要下载或构建一个 ISD 数据 Parser。

```bash
make get-parser       # 从 Github 下载 Parser 二进制，当然你也可以用 make build 直接用 go 构建。
make reload-daily     # 下载本年度最新的 isd.daily 数据并导入数据库中
```

**加载解析好的 CSV 数据集**

ISD Daily 数据集有一些脏数据与[重复数据](doc/isd-daily-caveat.md)，如果你不想手工解析处理清洗，这里也提供了一份解析好的稳定CSV数据集。

该数据集包含了截止到 2023-06-24 的 `isd.daily` 数据，你可以直接下载并导入 PostgreSQL 中，不需要 Parser，

```bash
make get-stable       # download stable isd.daily dataset from Github
make load-stable      # load downloaded stable isd.daily dataset into database
```





## 更多数据

ISD数据集有两个部分是每日更新的，气象站元数据，以及最新年份的 `isd.daily` （如 2023 年的 Tarball）。

你可以使用以下命令下载并刷新这两个部分。如果数据集没有更新，那么这些命令不会重新下载同样的数据包

```bash
make reload           # 实际上是：reload-station + reload-daily
```

你也可以使用以下命令下载并加载特定年份的 `isd.daily` 数据：

```bash
bin/get-daily  2022                   # 获取 2022 年的每日气象观测摘要 (1900-2023)
bin/load-daily 'postgres:///' 2022    # 加载 2022 年的每日气象观测摘要 (1900-2023) 
```

除了每日摘要 `isd.daily`， ISD 还提供了一份更详细的亚小时级原始观测记录 `isd.hourly`，下载与加载的方式与前者类似：

```bash
bin/get-hourly  2022                  # get hourly observation record of a specific year (1900-2023)
bin/load-hourly 'postgres:///' 2022   # load hourly data of a specific year 
```


## 数据

### 数据集概要

ISD提供了四个数据集：亚小时级原始观测数据，每日统计摘要数据，月度统计摘要，年度统计摘要

| 数据集         | 样本                                                 | 文档                                                     | 备注                          |
|-------------|----------------------------------------------------|--------------------------------------------------------|-----------------------------|
| ISD Hourly  | [isd-hourly-sample.csv](doc/isd-hourly-sample.csv) | [isd-hourly-document.pdf](doc/isd-hourly-document.pdf) | 亚小时级观测记录                    |
| ISD Daily   | [isd-daily-sample.csv](doc/isd-daily-sample.csv)   | [isd-daily-format.txt](doc/isd-daily-format.txt)       | 每日统计摘要                      |
| ISD Monthly | N/A                                                | [isd-gsom-document.pdf](doc/isd-gsom-document.pdf)     | 没有用到，因为可以从 `isd.daily` 计算生成 |
| ISD Yearly  | N/A                                                | [isd-gsoy-document.pdf](doc/isd-gsoy-document.pdf)     | 没有用到，因为可以从 `isd.daily` 计算生成 |


**每日摘要数据集**

- 压缩包大小 2.8GB (截止至 2023-06-24)
- 表大小 24GB，索引大小 6GB，PostgreSQL 中总大小约为 30GB
- 如果启用了 timescaledb 压缩，总大小可以压缩到 4.5 GB。

**亚小时级观测数据级**

- 压缩包总大小 117GB
- 灌入数据库后表大小 1TB+ ，索引大小 600GB+，总大小 1.6TB



### 数据库模式

- [sql/1_schema.sql](sql/1_schema.sql) : 基本表结构
- [sql/2_record.sql](sql/2_record.sql) : daily, monthly, yearly 表机构
- [sql/3_hourly.sql](sql/3_hourly.sql) : 可选的 hourly 表结构
- [sql/4_data.sql](sql/4_data.sql) : 国家/行政区划数据，气象要素字典表

**气象站元数据表** 

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

**每日摘要表**

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

**亚小时级原始观测数据表**

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



## 解析器

NOAA ISD 提供的原始数据是高度压缩的专有格式，需要通过解析器加工，才能转换为数据库表的格式。

针对 Daily 与 Hourly 两份数据集，这里提供了两个 Parser： [`isdd`](parser/isdd/isdd.go) and [`isdh`](parser/isdh/isdh.go)。
这两个解析器都以年度数据压缩包作为输入，产生 CSV 结果作为输出，以管道的方式工作，如下所示：

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



## 用户界面

这里提供了几个使用 Grafana 制作的 Dashboard，可以用于探索 ISD 数据集，查询气象站与历史气象数据。


**ISD Overview**

全局概览，总体指标与气象站导航。

[![ISD Overview](https://github.com/Vonng/isd/assets/8587410/827c1961-6426-46c6-9fb1-25cd93507924)](ui/isd/isd-overview.json)


**ISD Country**

展示单个国家/地区内所有的气象站。

[![ISD Country](https://github.com/Vonng/isd/assets/8587410/9a21ed5d-8540-4410-b582-a25fa88e9186)](ui/isd/isd-country.json)


**ISD Station**

展示单个气象站的详细信息，元数据，天/月/年度汇总指标。

<details><summary>ISD Station Dashboard</summary>

[![ISD Station](https://github.com/Vonng/isd/assets/8587410/4c72d529-d309-4629-97e0-ed8315c3a7a9)](ui/isd/isd-station.json)

</details>


**ISD Detail**

展示一个气象站原始亚小时级观测指标数据，需要 `isd.hourly` 数据集。

<details><summary>ISD Station Dashboard</summary>

[![ISD Detail](https://github.com/Vonng/isd/assets/8587410/726fad65-32c9-4a4c-b233-9ede7b0ae20d)](ui/isd/isd-detail.json)

</details>


## 协议

[MIT License](LICENSE)