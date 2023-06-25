-- This part is optional
-- for sub-hourly observation data

------------------------------------------------
-- isd_hourly
--   hourly observation data
------------------------------------------------
CREATE TABLE IF NOT EXISTS isd.hourly
(
    station    VARCHAR(12) NOT NULL, -- station id
    ts         TIMESTAMP   NOT NULL, -- timestamp
    -- 气
    temp       NUMERIC(3, 1),        -- [-93.2,+61.8]
    dewp       NUMERIC(3, 1),        -- [-98.2,+36.8]
    slp        NUMERIC(5, 1),        -- [8600,10900]
    stp        NUMERIC(5, 1),        -- [4500,10900]
    vis        NUMERIC(6),           -- [0,160000]
    -- 风
    wd_angle   NUMERIC(3),           -- [1,360]
    wd_speed   NUMERIC(4, 1),        -- [0,90]
    wd_gust    NUMERIC(4, 1),        -- [0,110]
    wd_code    VARCHAR(1),           -- code that denotes the character of the WIND-OBSERVATION.
    -- 云
    cld_height NUMERIC(5),           -- [0,22000]
    cld_code   VARCHAR(2),           -- cloud code
    -- 水
    sndp       NUMERIC(5, 1),        -- mm snow
    prcp       NUMERIC(5, 1),        -- mm precipitation
    prcp_hour  NUMERIC(2),           -- precipitation duration in hour
    prcp_code  VARCHAR(1),           -- precipitation type code
    -- 天
    mw_code    VARCHAR(2),           -- manual weather observation code
    aw_code    VARCHAR(2),           -- auto weather observation code
    pw_code    VARCHAR(1),           -- weather code of past period of time
    pw_hour    NUMERIC(2),           -- duration of pw_code period
    -- 杂
    -- remark     TEXT,
    -- eqd        TEXT,
    data       JSONB                 -- extra data
) PARTITION BY RANGE (ts);


ALTER TABLE isd.hourly ALTER COLUMN station SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN cld_code SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN prcp_code SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN mw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN aw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN pw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly ALTER COLUMN wd_code SET STORAGE MAIN;

COMMENT ON TABLE  isd.hourly IS 'Integrated Surface Data (ISD) station from global hourly dataset';
COMMENT ON COLUMN isd.hourly.station IS '11 char usaf wban station identifier';
COMMENT ON COLUMN isd.hourly.ts IS 'observe timestamp in UTC';
COMMENT ON COLUMN isd.hourly.temp IS '[-93.2,+61.8] temperature of the air';
COMMENT ON COLUMN isd.hourly.dewp IS '[-98.2,+36.8] dew point temperature';
COMMENT ON COLUMN isd.hourly.slp IS '[8600,10900] air pressure relative to Mean Sea Level (MSL).';
COMMENT ON COLUMN isd.hourly.stp IS '[4500,10900] air pressure of station';
COMMENT ON COLUMN isd.hourly.vis IS '[0-160000] horizontal distance at which an object can be seen and identified';
COMMENT ON COLUMN isd.hourly.wd_angle IS '[1-360] angle measured in a clockwise direction';
COMMENT ON COLUMN isd.hourly.wd_speed IS '[0-900] rate of horizontal travel of air past a fixed point';
COMMENT ON COLUMN isd.hourly.wd_gust IS '[0-110] wind gust';
COMMENT ON COLUMN isd.hourly.wd_code IS 'code that denotes the character of the WIND-OBSERVATION.';
COMMENT ON COLUMN isd.hourly.cld_height IS 'the height above ground level';
COMMENT ON COLUMN isd.hourly.cld_code IS 'GF1-1 An indicator that denotes the start of a SKY-CONDITION-OBSERVATION data group.';
COMMENT ON COLUMN isd.hourly.sndp IS 'snow depth in mm';
COMMENT ON COLUMN isd.hourly.prcp IS 'precipitation in mm ';
COMMENT ON COLUMN isd.hourly.prcp_hour IS 'precipitation hour';
COMMENT ON COLUMN isd.hourly.prcp_code IS 'precipitation code';
COMMENT ON COLUMN isd.hourly.mw_code IS 'MW1, manual weather code';
COMMENT ON COLUMN isd.hourly.aw_code IS 'AW1, PRESENT-WEATHER-OBSERVATION automated occurrence identifier';
COMMENT ON COLUMN isd.hourly.pw_code IS 'AY1-1, PAST-WEATHER-OBSERVATION manual atmospheric condition code';
COMMENT ON COLUMN isd.hourly.pw_hour IS 'AY1-3, PAST-WEATHER-OBSERVATION period quantity, 过去一段时间天气的时长';
-- COMMENT ON COLUMN isd_hourly.remark IS 'remark data section';
-- COMMENT ON COLUMN isd_hourly.eqd IS ' element quality data section.';
COMMENT ON COLUMN isd.hourly.data IS 'additional data fields in json format';

-- indexes
CREATE INDEX IF NOT EXISTS hourly_ts_station_idx ON isd.hourly USING btree (ts, station);
CREATE INDEX IF NOT EXISTS hourly_station_ts_idx ON isd.hourly USING btree (station, ts);



------------------------------------------------
-- isd.create_partition
--    create yearly partition of isd_hourly
------------------------------------------------
CREATE OR REPLACE FUNCTION isd.create_partition(_year INTEGER, _upper INTEGER DEFAULT NULL) RETURNS TEXT AS
$$
DECLARE
    -- _part_name TEXT := CASE _upper WHEN NULL THEN format('isd_hourly_%s', _year) ELSE format('isd.hourly_%s_%s', _year,_upper) END;
    _part_name TEXT := format('isd.hourly_%s', _year);
    _part_lo   DATE := make_date(_year, 1, 1);
    -- _part_hi   DATE := CASE _upper WHEN NULL THEN make_date(_year + 1, 1, 1) ELSE make_date(_upper, 1, 1) END;
    _part_hi   DATE := coalesce(make_date(_upper, 1, 1), make_date(_year + 1, 1, 1));
    _sql       TEXT := format(
            $sql$
            CREATE TABLE IF NOT EXISTS %s PARTITION OF isd.hourly FOR VALUES FROM ('%s') TO ('%s');
            COMMENT ON TABLE %s IS 'isd.hourly partition from %s to %s';
            $sql$
        , _part_name, _part_lo, _part_hi, _part_name, _part_lo, _part_hi);
BEGIN
    RAISE NOTICE '%', _sql;
    EXECUTE _SQL;
    RETURN _part_name;
END;
$$ LANGUAGE PlPGSQL VOLATILE;
COMMENT ON FUNCTION isd.create_partition(_year INTEGER, _upper INTEGER) IS 'create yearly partition of isd.hourly';

-- optional: create partition for 1900-2023
-- the rest are yearly partition: from 1970 (10GB) to 2020 (40GB+)
-- SELECT isd.create_partition(1900, 1950); -- 20 GB
-- SELECT isd.create_partition(1950, 1960); -- 47 GB
-- SELECT isd.create_partition(1960, 1970); -- 41 GB
-- SELECT isd.create_partition(year::INTEGER) FROM generate_series(1970, 2023) year;

