#==============================================================#
# File      :   Makefile
# Ctime     :   2020-11-03
# Mtime     :   2020-11-03
# Desc      :   Makefile shortcuts
# Path      :   Makefile
# Copyright (C) 2019-2020 Ruohang Feng
#==============================================================#

###############################################################
# Public objective
###############################################################


###############################################################
# Download
###############################################################
# dump meta data to data/meta dir
dump-meta:
	bin/dump-meta.sh

# download meta data: isd station list
get-isd-station:
	bin/get-isd-station.sh

# download meta data: isd station observation records
get-isd-history:
	bin/get-isd-history.sh

# download current year's daily data
get-daily:
	bin/get-isd-daily.sh

# download current year's hourly data
get-hourly:
	bin/get-isd-hourly.sh


###############################################################
# Load
###############################################################

# dump meta data to data/meta dir
load-meta:
	bin/load-meta.sh

# load current year daily data
load-daily:
	bin/load-isd-daily.sh

# load current year hourly data
load-hourly:
	bin/load-isd-hourly.sh

# refresh latest partition
refresh:
	bin/refresh-latest.sh

###############################################################
# Database
###############################################################

# assume a local connectable postgres database
createdb:
	psql postgres -c 'CREATE DATABASE isd;'
	psql isd -c 'CREATE EXTENSION postgis;'

# create schema on local machine
schema:
	psql isd -f sql/schema.sql


###############################################################
# PARSER
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

.PHONY: get-daily-2020 get-hourly-2020 get-isd-station get-isd-history load-meta dump-meta load-daily load-hourly createdb
