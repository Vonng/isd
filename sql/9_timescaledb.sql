--============================================================--
--            TimescaleDB Alternative Schema                  --
--============================================================--
-- This is optional schema for timescaledb.                   --


------------------------------------------------
-- isd.daily   : daily observation summary
-- isd.hourly  : hourly observation record
------------------------------------------------


------------------------------------------------
-- isd_daily
--   daily observation summary data
------------------------------------------------
-- DROP TABLE IF EXISTS isd.daily2;
CREATE TABLE IF NOT EXISTS isd.daily2
(
    -- 主键
    station     VARCHAR(12) NOT NULL, -- 台站号 6USAF+5WBAN
    ts          DATE        NOT NULL, -- 观测日期
    -- 温湿度
    temp_mean   NUMERIC(3, 1),        -- 平均温度 (℃)
    temp_min    NUMERIC(3, 1),        -- 最低温度 ℃
    temp_max    NUMERIC(3, 1),        -- 最高温度 ℃
    dewp_mean   NUMERIC(3, 1),        -- 平均露点 (℃)
    -- 气压
    slp_mean    NUMERIC(5, 1),        -- 海平面气压 (hPa)
    stp_mean    NUMERIC(5, 1),        -- 站点气压 (hPa)
    -- 视距
    vis_mean    NUMERIC(6),           -- 可视距离 (m)
    -- 风速
    wdsp_mean   NUMERIC(4, 1),        -- 平均风速 (m/s)
    wdsp_max    NUMERIC(4, 1),        -- 最大风速 (m/s)
    gust        NUMERIC(4, 1),        -- 最大阵风 (m/s)
    -- 降水/雪
    prcp_mean   NUMERIC(5, 1),        -- 降水量 (mm)
    prcp        NUMERIC(5, 1),        -- 根据降水标记修正后的降水量 (mm)
    sndp        NuMERIC(5, 1),        -- 当日最新上报的雪深 (mm)
    -- 天气现象 FRSHTT (Fog/Rain/Snow/Hail/Thunder/Tornado)
    is_foggy    BOOLEAN,              -- (F)og
    is_rainy    BOOLEAN,              -- (R)ain or Drizzle
    is_snowy    BOOLEAN,              -- (S)now or pellets
    is_hail     BOOLEAN,              -- (H)ail
    is_thunder  BOOLEAN,              -- (T)hunder
    is_tornado  BOOLEAN,              -- (T)ornado or Funnel Cloud
    -- 计算各个统计量所使用的观测记录数量
    temp_count  SMALLINT,             -- 用于计算温度统计量的记录数量
    dewp_count  SMALLINT,             -- 用于计算平均露点的记录数量
    slp_count   SMALLINT,             -- 用于计算海平面气压统计量的记录数量
    stp_count   SMALLINT,             -- 用于计算站点气压统计量的记录数量
    wdsp_count  SMALLINT,             -- 用于计算风速统计量的记录数量
    visib_count SMALLINT,             -- 用于计算视距的记录数量
    -- 辅助标记
    temp_min_f  BOOLEAN,              -- 最低温度是统计得出（而非直接上报）
    temp_max_f  BOOLEAN,              -- 同上，最高温度
    prcp_flag   CHAR,                 -- 降水量标记: ABCDEFGHI
    PRIMARY KEY (ts, station)
); -- PARTITION BY RANGE (ts);

COMMENT ON TABLE isd.daily2 IS 'isd daily observation summary in timescaledb';
COMMENT ON COLUMN isd.daily2.station IS 'station id 6USAF+5WBAN';
COMMENT ON COLUMN isd.daily2.ts IS 'observation date';
COMMENT ON COLUMN isd.daily2.temp_mean IS 'average (℃)';
COMMENT ON COLUMN isd.daily2.temp_min IS 'min ℃';
COMMENT ON COLUMN isd.daily2.temp_max IS 'max ℃';
COMMENT ON COLUMN isd.daily2.dewp_mean IS 'mean dew point (℃)';
COMMENT ON COLUMN isd.daily2.slp_mean IS 'sea level pressre (hPa)';
COMMENT ON COLUMN isd.daily2.stp_mean IS 'station level pressure (hPa)';
COMMENT ON COLUMN isd.daily2.vis_mean IS 'visible distance (m)';
COMMENT ON COLUMN isd.daily2.wdsp_mean IS 'mean wind speed (m/s)';
COMMENT ON COLUMN isd.daily2.wdsp_max IS 'max wind speed (m/s)';
COMMENT ON COLUMN isd.daily2.gust IS 'gust wind speed (m/s)';
COMMENT ON COLUMN isd.daily2.prcp_mean IS 'precipitation (mm)';
COMMENT ON COLUMN isd.daily2.prcp IS 'rectified precipitation (mm)';
COMMENT ON COLUMN isd.daily2.sndp IS 'snow depth (mm)';
COMMENT ON COLUMN isd.daily2.is_foggy IS '(F)og';
COMMENT ON COLUMN isd.daily2.is_rainy IS '(R)ain or Drizzle';
COMMENT ON COLUMN isd.daily2.is_snowy IS '(S)now or pellets';
COMMENT ON COLUMN isd.daily2.is_hail IS '(H)ail';
COMMENT ON COLUMN isd.daily2.is_thunder IS '(T)hunder';
COMMENT ON COLUMN isd.daily2.is_tornado IS '(T)ornado or Funnel Cloud';
COMMENT ON COLUMN isd.daily2.temp_count IS '用于计算温度统计量的记录数量';
COMMENT ON COLUMN isd.daily2.dewp_count IS '用于计算平均露点的记录数量';
COMMENT ON COLUMN isd.daily2.slp_count IS '用于计算海平面气压统计量的记录数量';
COMMENT ON COLUMN isd.daily2.stp_count IS '用于计算站点气压统计量的记录数量';
COMMENT ON COLUMN isd.daily2.wdsp_count IS '用于计算风速统计量的记录数量';
COMMENT ON COLUMN isd.daily2.visib_count IS '用于计算视距的记录数量';
COMMENT ON COLUMN isd.daily2.temp_min_f IS '最低温度是统计得出（而非直接上报）';
COMMENT ON COLUMN isd.daily2.temp_max_f IS '同上，最高温度';
COMMENT ON COLUMN isd.daily2.prcp_flag IS '降水量标记: ABCDEFGHI';

SELECT create_hypertable('isd.daily2', 'ts', chunk_time_interval => interval '1000day');
ALTER TABLE isd.daily2
    SET (
        timescaledb.compress,
        timescaledb.compress_orderby = 'ts ASC',
        timescaledb.compress_segmentby = 'station',
        timescaledb.compress_chunk_time_interval='1000day'
    );





------------------------------------------------
-- isd.hourly2
--   hourly observation data
------------------------------------------------
CREATE TABLE IF NOT EXISTS isd.hourly2
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
);


ALTER TABLE isd.hourly2 ALTER COLUMN station SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN cld_code SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN prcp_code SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN mw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN aw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN pw_code SET STORAGE MAIN;
ALTER TABLE isd.hourly2 ALTER COLUMN wd_code SET STORAGE MAIN;
-- indexes
CREATE INDEX IF NOT EXISTS hourly2_ts_station_idx ON isd.hourly2 USING btree (ts, station);
CREATE INDEX IF NOT EXISTS hourly2_station_ts_idx ON isd.hourly2 USING btree (station, ts);


COMMENT ON TABLE  isd.hourly2 IS 'Integrated Surface Data (ISD) station from global hourly dataset';
COMMENT ON COLUMN isd.hourly2.station IS '11 char usaf wban station identifier';
COMMENT ON COLUMN isd.hourly2.ts IS 'observe timestamp in UTC';
COMMENT ON COLUMN isd.hourly2.temp IS '[-93.2,+61.8] temperature of the air';
COMMENT ON COLUMN isd.hourly2.dewp IS '[-98.2,+36.8] dew point temperature';
COMMENT ON COLUMN isd.hourly2.slp IS '[8600,10900] air pressure relative to Mean Sea Level (MSL).';
COMMENT ON COLUMN isd.hourly2.stp IS '[4500,10900] air pressure of station';
COMMENT ON COLUMN isd.hourly2.vis IS '[0-160000] horizontal distance at which an object can be seen and identified';
COMMENT ON COLUMN isd.hourly2.wd_angle IS '[1-360] angle measured in a clockwise direction';
COMMENT ON COLUMN isd.hourly2.wd_speed IS '[0-900] rate of horizontal travel of air past a fixed point';
COMMENT ON COLUMN isd.hourly2.wd_gust IS '[0-110] wind gust';
COMMENT ON COLUMN isd.hourly2.wd_code IS 'code that denotes the character of the WIND-OBSERVATION.';
COMMENT ON COLUMN isd.hourly2.cld_height IS 'the height above ground level';
COMMENT ON COLUMN isd.hourly2.cld_code IS 'GF1-1 An indicator that denotes the start of a SKY-CONDITION-OBSERVATION data group.';
COMMENT ON COLUMN isd.hourly2.sndp IS 'snow depth in mm';
COMMENT ON COLUMN isd.hourly2.prcp IS 'precipitation in mm ';
COMMENT ON COLUMN isd.hourly2.prcp_hour IS 'precipitation hour';
COMMENT ON COLUMN isd.hourly2.prcp_code IS 'precipitation code';
COMMENT ON COLUMN isd.hourly2.mw_code IS 'MW1, manual weather code';
COMMENT ON COLUMN isd.hourly2.aw_code IS 'AW1, PRESENT-WEATHER-OBSERVATION automated occurrence identifier';
COMMENT ON COLUMN isd.hourly2.pw_code IS 'AY1-1, PAST-WEATHER-OBSERVATION manual atmospheric condition code';
COMMENT ON COLUMN isd.hourly2.pw_hour IS 'AY1-3, PAST-WEATHER-OBSERVATION period quantity, 过去一段时间天气的时长';
-- COMMENT ON COLUMN isd.hourly2.remark IS 'remark data section';
-- COMMENT ON COLUMN isd.hourly2.eqd IS ' element quality data section.';
COMMENT ON COLUMN isd.hourly2.data IS 'additional data fields in json format';



SELECT create_hypertable('isd.hourly2', 'ts', chunk_time_interval => interval '365day');
ALTER TABLE isd.daily2
    SET (
        timescaledb.compress,
        timescaledb.compress_orderby = 'ts ASC',
        timescaledb.compress_segmentby = 'station',
        timescaledb.compress_chunk_time_interval='365day'
        );

