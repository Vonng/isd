#==============================================================#
# File      :   Makefile
# Ctime     :   2020-11-03
# Mtime     :   2023-06-25
# Desc      :   Makefile shortcuts
# Path      :   Makefile
# Author    :   Ruohang Feng (rh@vonng.com)
# License   :   Apache-2.0
#==============================================================#

PGURL?=postgres:///
DATA_DIR=$(PWD)/data
GRAFANA_USERNAME?=admin
GRAFANA_PASSWORD?=admin
GRAFANA_ENDPOINT?=http://localhost:3000


#=============================================================#
# Summary
#=============================================================#
default: summary
summary:
	@echo "========== Database Info =========="
	@echo "PGURL=$(PGURL)"
	psql $(PGURL) -Xwqc '\dt+ isd.*'
	@echo "========== Latest Record =========="
	psql $(PGURL) -AXwtqc 'SELECT max(ts) AS "daily updated to" FROM isd.daily'
	@echo "\n========== Local Datasets ========="
	ls data/ data/daily data/hourly

	@echo "\n========= Help Information ========"
	@echo "make reload          # refresh latest station/daily data"
	@echo "make get-daily       # download latest daily data"
	@echo "make get-station     # download latest station data"


#=============================================================#
# Public API
#=============================================================#
all: sql ui reload
reload: reload-station reload-daily
reload-daily:   get-daily   load-daily   refresh
reload-hourly:  get-hourly  load-hourly
reload-station: get-station load-station


#=============================================================#
# Download
#=============================================================#
# get latest station/history meta data
get-station:
	bin/get-station

# get latest year's daily summary data
get-daily:
	bin/get-daily

# get latest year's hourly observation raw data
get-hourly:
	bin/get-hourly

# usually station/this year's daily data needs to be refreshed regularly
get-latest: get-station get-daily


#=============================================================#
# Load Data
#=============================================================#
# load latest station data
load-station:
	bin/load-station $(PGURL)

# load daily dataset of the latest year
load-daily:
	bin/load-daily $(PGURL)

# load hourly dataset of the latest year
load-hourly:
	bin/load-hourly $(PGURL)



#=============================================================#
# Stable Daily Data (Cleansed Dataset from 1900 - 2023.06)
#=============================================================#
# download, load, and refresh stable daily data set
stable: get-stable load-stable refresh_full

# get daily stable data set (3.4GB)
get-stable:
	bin/get-stable

# dump stable dataset from database
dump-stable:
	bin/dump-stable

# load entire stable daily dataset and refresh monthly/yearly partitions
load-stable:
	bin/load-stable $(PGURL)


#=============================================================#
# Monthly & Yearly Data Refresh
#=============================================================#
# refresh latest partition of monthly & yearly data
refresh:
	psql $(PGURL) -c 'SELECT isd.refresh();'

# refresh entire monthly & yearly data based on daily data
refresh-full:
	psql $(PGURL) -c 'SELECT isd.refresh_full();'


#=============================================================#
# Graphic UI
#=============================================================#
# add dashboard to grafana according to GRAFANA_X environment
ui:
	cd ui && ./grafana.py load
dd:
	cd ui && ./grafana.py dump



#=============================================================#
# Database Schema
#=============================================================#
# setup database schema (dangerous)
sql: drop create

# drop schema isd cascade
drop:
	psql $(PGURL) -f sql/0_cleanup.sql

# create schema
create:
	psql $(PGURL) -f sql/1_schema.sql
	psql $(PGURL) -f sql/2_record.sql
	psql $(PGURL) -f sql/3_hourly.sql
    psql $(PGURL) -f sql/4_data.sql


#=============================================================#
# PARSER (you can just use pre-compiled binaries)
#=============================================================#
# build parser with go, put into bin/isd
build:
	cd parser && CGO_ENABLED=0 go build -a -ldflags '-extldflags "-static"' -o isd
	mv -f parser/isd bin/isd
clean:
	rm -rf isd bin/isdd

release: clean release-linux release-darwin checksums clean
release-darwin: release-darwin-amd release-darwin-arm
release-darwin-amd:
	cd parser && CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isd
	mv parser/isd isd
	tar -cf dist/isd-darwin-amd64.tar.gz isd
release-darwin-arm:
	cd parser && CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build  -a -ldflags '-extldflags "-static"' -o isd
	mv parser/isd isd
	tar -cf dist/isd-darwin-arm64.tar.gz isd

release-linux: release-linux-amd release-linux-arm
release-linux-amd:
	cd parser && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isd
	mv parser/isd isd
	tar -cf dist/isd-linux-amd64.tar.gz isd
release-linux-arm:
	cd parser && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build  -a -ldflags '-extldflags "-static"' -o isd
	mv parser/isd isd
	tar -cf dist/isd-linux-arm64.tar.gz isd
checksums:
	cd dist; md5sum *.tar.gz > checksums


.PHONY: sql ui get-isd-station get-isd-history load-meta dump-meta load-daily load-hourly createdb
