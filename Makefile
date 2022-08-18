#==============================================================#
# File      :   Makefile
# Ctime     :   2020-11-03
# Mtime     :   2021-07-21
# Desc      :   Makefile shortcuts
# Path      :   Makefile
# Copyright (C) 2019-2020 Ruohang Feng
#==============================================================#


###############################################################
# Public API
###############################################################
reload: get-daily load-daily refresh    # reload latest (this year) daily summary data
reload-hourly: get-hourly load-hourly   # reload latest (this year) hourly raw data
all: baseline reload                    # create minimal viable product with latest data
baseline: sql ui download load-meta     # setup postgres database & grafana dashboards


###############################################################
# Environment
###############################################################
# your own environment
PGURL?=postgres:///isd
GRAFANA_USERNAME=admin
GRAFANA_PASSWORD=admin
GRAFANA_ENDPOINT=http://localhost:3000

# pigsty environment
# PGURL?=postgres://dbuser_dba:DBUser.DBA@10.10.10.10/meta
# GRAFANA_USERNAME=admin
# GRAFANA_PASSWORD=pigsty
# GRAFANA_ENDPOINT=http://10.10.10.10:3000

show:
	@echo $(PGURL)
	psql $(PGURL) -c '\dt+ isd.*'
###############################################################



###############################################################
# Download
###############################################################

# get basic data set (no observation data)
download: get-meta get-parser get-station get-history

# download meta data to data/meta dir
get-meta:
	bin/get-meta.sh

# download isd daily & hourly data parser binaries from github
get-parser:
	bin/get-parser.sh

# download meta data: isd station list
get-station:
	bin/get-station.sh

# download meta data: isd station observation records
get-history:
	bin/get-history.sh

# download current year's daily data (add year to download specific year data)
get-daily:
	bin/get-daily.sh

# download current year's hourly data (add year to download specific year data)
get-hourly:
	bin/get-hourly.sh


###############################################################
# Init Database Schema
###############################################################
# create schema on local machine (DANGEROUS, will wipe isd schema)
sql:
	psql $(PGURL) -f sql/schema.sql


###############################################################
# Init Dashboards on Grafana
###############################################################
# add dashboard to grafana according to GRAFANA_X environment
ui:
	cd ui && ./grafana.py load

###############################################################
# Meta Data (dict/const tables)
###############################################################

# dump meta data to data/meta dir
dump-meta:
	bin/dump-meta.sh $(PGURL)

# dump meta data to data/meta dir
load-meta:
	bin/load-meta.sh $(PGURL)

###############################################################
# Load ISD Observation Data
###############################################################

# load current year hourly data (parser required)
load-hourly:
	bin/load-hourly.sh $(PGURL)

# load current year daily data  (parser required)
load-daily:
	bin/load-daily.sh $(PGURL)

# refresh latest partition of monthly & yearly data
refresh:
	bin/refresh.sh $(PGURL)


###############################################################
# PARSER (you can just use pre-compiled binaries)
###############################################################

# build hourly data parser
isdd:
	cd parser/isdd && go build
	mv -f parser/isdd/isdd bin/isdd

# build daily data parser
isdh:
	cd parser/isdh && go build
	mv -f parser/isdh/isdh bin/isdh

release-darwin:
	cd parser/isdh && CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isdh
	cd parser/isdd && CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isdd
	mv -f parser/isdh/isdh isdh
	mv -f parser/isdd/isdd isdd
	upx isdh
	upx isdd
	tar -cf bin/release/isd_darwin-amd64.tar.gz isdh isdd

release-darwin-arm:
	cd parser/isdh && CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build  -a -ldflags '-extldflags "-static"' -o isdh
	cd parser/isdd && CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build  -a -ldflags '-extldflags "-static"' -o isdd
	mv -f parser/isdh/isdh isdh
	mv -f parser/isdd/isdd isdd
	upx isdh
	upx isdd
	tar -cf bin/release/isd_darwin-arm64.tar.gz isdh isdd

release-linux:
	cd parser/isdh && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isdh
	cd parser/isdd && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build  -a -ldflags '-extldflags "-static"' -o isdd
	mv -f parser/isdh/isdh isdh
	mv -f parser/isdd/isdd isdd
	upx isdh
	upx isdd
	tar -cf bin/release/isd_linux-amd64.tar.gz isdh isdd

clean:
	rm -rf isdh isdd
	rm -rf bin/isdh bin/isdd

release: clean release-linux release-darwin

.PHONY: sql ui get-isd-station get-isd-history load-meta dump-meta load-daily load-hourly createdb
