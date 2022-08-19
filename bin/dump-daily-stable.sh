#!/usr/bin/env bash
set -uo pipefail

#==============================================================#
# File      :   dump-daily-stable.sh
# Ctime     :   2020-11-03
# Mtime     :   2022-08-19
# Desc      :   dump isd daily data, stable part (before 2022)
# Path      :   bin/dump-daily-table.sh
# Author    :   Vonng(fengruohang@outlook.com)
# Depend    :   curl,pv,psql
# Usage     :   bin/load-daily-stable.sh [pgurl=isd]
#==============================================================#

PROG_DIR="$(cd $(dirname $0) && pwd)"
PROG_NAME="$(basename $0)"
PROJ_DIR=$(dirname $PROG_DIR)

# PGURL specify target database connection string
PGURL=${1-'postgres:///isd'}
PARSER="${PROJ_DIR}/bin/isdd"
DATA_DIR="${PROJ_DIR}/data/daily"
DATA_FILE="${DATA_DIR}/daily_stable.csv.gz"

function log_info (){
    [ -t 2 ] && printf "\033[0;32m[$(date "+%Y-%m-%d %H:%M:%S")][INFO] $*\033[0m\n" 1>&2 || printf "[$(date "+%Y-%m-%d %H:%M:%S")][INFO] $*\n" 1>&2
}

# warn your about overwrite
log_info "copy isd.daily_stable to csv"
psql ${PGURL} -c 'COPY isd.daily_stable TO stdout CSV HEADER' | gzip -9 - > ${DATA_FILE}2

# ask for confirmation to remove old data
if [[ -f "${DATA_FILE}" ]]; then
    log_input "Remove ${DATA_FILE} (y/n):"
    read -r
    local reply=$(echo "$REPLY" | tr '[:upper:]' '[:lower:]')
    case "${reply}" in
        y|yes|ok|true|aye|on)
          rm -rf ${DATA_FILE}
        ;;
    esac
fi
