# ISD —— Intergrated Surface Data



## SYNOPSIS

Download, Parse, Visualize Intergrated Suface Dataset.

Including 30000 meteorology station, sub-hourly observation records, from 1900-2020.

![](doc/img/isd-overview.jpg)





## Get Started

1. **Clone repo**

    ```bash
   git clone https://github.com/Vonng/isd && cd isd 
   ```

2. **Prepare a postgres database** 

    Connect via something like `isd` or `postgres://user:pass@host/dbname`)
   
   ```bash
   # skip this if you already have a viable database
   PGURL=postgres
   psql ${PGURL} -c 'CREATE DATABASE isd;'
   
   # database connection string, something like `isd` or `postgres://user:pass@host/dbname`
   PGURL='isd'
   psql ${PGURL} -AXtwc 'CREATE EXTENSION postgis;'
   
   # create tables, partitions, functions
   psql ${PGURL} -AXtwf 'sql/schema.sql'
   ```
   
3. **Download data**

 * [ISD Station](https://www1.ncdc.noaa.gov/pub/data/noaa/isd-history.csv): Station metadata, id, name, location, country, etc...  
 * [ISD History](https://www1.ncdc.noaa.gov/pub/data/noaa/isd-inventory.csv.z): Station observation records: observation count per month 
 * [ISD Hourly](https://www.ncei.noaa.gov/data/global-hourly/archive/csv/): Yearly archived station (sub-)hourly observation records  
 * [ISD Daily](https://www.ncei.noaa.gov/data/global-summary-of-the-day/archive/): Yearly archvied station daily aggregated summary

```bash             
git clone https://github.com/Vonng/isd && cd isd
bin/get-isd-station.sh         # download isd station from noaa (proxy makes it faster)
bin/get-isd-history.sh         # download isd history observation from noaa
bin/get-isd-hourly.sh <year>   # download isd hourly data (yearly tarball 1901-2020)
bin/get-isd-daily.sh <year>    # download isd daily data  (yearly tarball 1929-2020) 
```

4. **Build Parser**

   There are two ISD dataset parsers written in Golang : `isdh` for isd hourly dataset and `isdd` for isd daily dataset.
   
   `make isdh` and `make isdd` will build it and copy to bin. These parsers are required for loading data into database. 
   
   But you can [download](https://github.com/Vonng/isd/releases/tag/untagged-bc5c87c65e1245aa4f29) pre-compiled binary to skip this phase.
   
5. **Load data**

   Metadata includes `world_fences`, `china_fences`, `isd_elements`, `isd_mwcode`, `isd_station`, `isd_history`.
   They are gzipped csv file lies in [`data/meta/`](data/meta/). You have to download `isd_station` and `isd_history` before loading it.   

```bash
# load contant metadata table `world_fences`, `china_fences`, `isd_elements`, `isd_mwcode`, 
# load variable metadata table `isd_station`, `isd_history` (downloaded to data/meta)
bin/load-meta.sh 
```

```bash
# load a year's daily data to database 
bin/load-isd-daily <year> 

# load a year's hourly data to database
bin/laod-isd-hourly <year>
```

   > Note that the original `isd_daily` dataset has some un-cleansed data, refer [caveat](doc/isd-daily-caveat.md) for detail.  



## Data

### Dataset

| 数据集      | 样本                                               | 文档                                                   | 备注                              |
| ----------- | -------------------------------------------------- | ------------------------------------------------------ | --------------------------------- |
| ISD Hourly  | [isd-hourly-sample.csv](doc/isd-hourly-sample.csv) | [isd-hourly-document.pdf](doc/isd-hourly-document.pdf) | (Sub-) Hour oberservation records |
| ISD Daily   | [isd-daily-sample.csv](doc/isd-daily-sample.csv)   | [isd-daily-format.txt](doc/isd-daily-format.txt)       | Daily summary                     |
| ISD Monthly | N/A                                                | [isd-gsom-document.pdf](doc/isd-gsom-document.pdf)     | Not used, gen from daily          |
| ISD Yearly  | N/A                                                | [isd-gsoy-document.pdf](doc/isd-gsoy-document.pdf)     | Not used, gen from monthly        |

### Schema

Data schema [definition](sql/schema.sql)







## Data Update

ISD Daily and ISD hourly dataset will rolling update each day. Run following scripts to load latest data into database.

```bash
# download, clean, reload latest hourly dataset
bin/get-isd-daily.sh
bin/load-isd-daily.sh

# download, clean, reload latest daily dataset
bin/get-isd-daily.sh
bin/load-isd-daily.sh

# recalculate latest partition of monthly and yearly
bin/refresh-latest.sh
```



## UI

### **ISD Overview**

Dashboard [definition](ui/isd-overview.json)

### ISD Station

Dashboard [definition](ui/isd-station.json)

![](doc/img/isd-station.jpg)

### ISD Monthly

Dashboard [definition](ui/isd-monthly.json)

![](doc/img/isd-monthly.jpg)

