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

# build hourly data parser
isdd: bin/isdh
	cd parser/isdd && go build
	mv -f parser/isdd/isdd bin/isdd

# build daily data parser
isdh: bin/isdd
	cd parser/isdh && go build
	mv -f parser/isdh/isdh bin/isdh

# download meta data: isd station list
get-isd-station:
	bin/get-isd-station.sh

# download meta data: isd station observation records
get-isd-history:
	bin/get-isd-history.sh

# dump meta data to data/meta dir
dump-meta:
	bin/dump-meta.sh

# download current year's daily data
get-daily:
	bin/get-isd-daily.sh

# download current year's hourly data
get-hourly:
	bin/get-isd-hourly.sh

# load current year daily data
load-daily:
	bin/load-isd-daily.sh

# load current year hourly data
load-hourly:
	bin/load-isd-hourly.sh

# assume a local connectable postgres database
createdb:
	psql postgres -c 'CREATE DATABASE isd;'
	psql isd -c 'CREATE EXTENSION postgis;'
	psql isd -f sql/schema.sql

.PHONY: get-daily-2020 get-hourly-2020 get-isd-station get-isd-history load-meta dump-meta load-daily load-hourly createdb
