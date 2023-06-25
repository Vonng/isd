
--============================================================--
--                    Record Tables                           --
--============================================================--

------------------------------------------------
-- isd.daily   : daily observation summary
-- isd.monthly : monthly observation summary
-- isd.yearly  : yearly observation summary
-- isd.refresh_full()
-- isd.refresh(begin date, end date)
------------------------------------------------


------------------------------------------------
-- isd_daily
--   daily observation summary data
------------------------------------------------
-- DROP TABLE IF EXISTS isd.daily;
CREATE TABLE IF NOT EXISTS isd.daily
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
    PRIMARY KEY (station, ts)
); -- PARTITION BY RANGE (ts);

COMMENT ON TABLE  isd.daily IS 'isd daily observation summary';
COMMENT ON COLUMN isd.daily.station IS 'station id 6USAF+5WBAN';
COMMENT ON COLUMN isd.daily.ts IS 'observation date';
COMMENT ON COLUMN isd.daily.temp_mean IS 'average (℃)';
COMMENT ON COLUMN isd.daily.temp_min IS 'min ℃';
COMMENT ON COLUMN isd.daily.temp_max IS 'max ℃';
COMMENT ON COLUMN isd.daily.dewp_mean IS 'mean dew point (℃)';
COMMENT ON COLUMN isd.daily.slp_mean IS 'sea level pressre (hPa)';
COMMENT ON COLUMN isd.daily.stp_mean IS 'station level pressure (hPa)';
COMMENT ON COLUMN isd.daily.vis_mean IS 'visible distance (m)';
COMMENT ON COLUMN isd.daily.wdsp_mean IS 'mean wind speed (m/s)';
COMMENT ON COLUMN isd.daily.wdsp_max IS 'max wind speed (m/s)';
COMMENT ON COLUMN isd.daily.gust IS 'gust wind speed (m/s)';
COMMENT ON COLUMN isd.daily.prcp_mean IS 'precipitation (mm)';
COMMENT ON COLUMN isd.daily.prcp IS 'rectified precipitation (mm)';
COMMENT ON COLUMN isd.daily.sndp IS 'snow depth (mm)';
COMMENT ON COLUMN isd.daily.is_foggy IS '(F)og';
COMMENT ON COLUMN isd.daily.is_rainy IS '(R)ain or Drizzle';
COMMENT ON COLUMN isd.daily.is_snowy IS '(S)now or pellets';
COMMENT ON COLUMN isd.daily.is_hail IS '(H)ail';
COMMENT ON COLUMN isd.daily.is_thunder IS '(T)hunder';
COMMENT ON COLUMN isd.daily.is_tornado IS '(T)ornado or Funnel Cloud';
COMMENT ON COLUMN isd.daily.temp_count IS '用于计算温度统计量的记录数量';
COMMENT ON COLUMN isd.daily.dewp_count IS '用于计算平均露点的记录数量';
COMMENT ON COLUMN isd.daily.slp_count IS '用于计算海平面气压统计量的记录数量';
COMMENT ON COLUMN isd.daily.stp_count IS '用于计算站点气压统计量的记录数量';
COMMENT ON COLUMN isd.daily.wdsp_count IS '用于计算风速统计量的记录数量';
COMMENT ON COLUMN isd.daily.visib_count IS '用于计算视距的记录数量';
COMMENT ON COLUMN isd.daily.temp_min_f IS '最低温度是统计得出（而非直接上报）';
COMMENT ON COLUMN isd.daily.temp_max_f IS '同上，最高温度';
COMMENT ON COLUMN isd.daily.prcp_flag IS '降水量标记: ABCDEFGHI';

CREATE INDEX IF NOT EXISTS daily_ts_idx ON isd.monthly (ts);




------------------------------------------------
-- isd_monthly
--   monthly observation summary data
------------------------------------------------
-- DROP TABLE IF EXISTS isd.monthly;
CREATE TABLE IF NOT EXISTS isd.monthly
(
    ts           DATE,          -- 月份时间戳,yyyy-mm-01
    station      VARCHAR(12),   -- 11位台站号
    count        SMALLINT,      -- 本月有观测记录的天数
    temp_mean    NUMERIC(3, 1), -- 月平均气温
    temp_min     NUMERIC(3, 1), -- 月最低气温
    temp_max     NUMERIC(3, 1), -- 月最高气温
    temp_min_avg NUMERIC(3, 1), -- 月内每日最低气温均值
    temp_max_avg NUMERIC(3, 1), -- 月内每日最高气温均值
    dewp_mean    NUMERIC(3, 1), -- 月平均露点
    dewp_min     NUMERIC(3, 1), -- 月最低露点
    dewp_max     NUMERIC(3, 1), -- 月最高露点
    slp_mean     NUMERIC(5, 1), -- 月平均气压
    slp_min      NUMERIC(5, 1), -- 月最低气压
    slp_max      NUMERIC(5, 1), -- 月最高气压
    prcp_sum     NUMERIC(5, 1), -- 月总降水
    prcp_max     NUMERIC(5, 1), -- 月最大降水
    prcp_mean    NUMERIC(5, 1), -- 月平均降水
    wdsp_mean    NUMERIC(4, 1), -- 月平均风速
    wdsp_max     NUMERIC(4, 1), -- 月最大风速
    gust_max     NUMERIC(4, 1), -- 月最大阵风
    sunny_days   SMALLINT,      -- 月晴天日数
    windy_days   SMALLINT,      -- 月大风日数
    foggy_days   SMALLINT,      -- 月雾天日数
    rainy_days   SMALLINT,      -- 月雨天日数
    snowy_days   SMALLINT,      -- 月雪天日数
    hail_days    SMALLINT,      -- 月冰雹日数
    thunder_days SMALLINT,      -- 月雷暴日数
    tornado_days SMALLINT,      -- 月龙卷日数
    hot_days     SMALLINT,      -- 月高温日数
    cold_days    SMALLINT,      -- 月低温日数
    vis_4_days   SMALLINT,      -- 月能见度4km内日数
    vis_10_days  SMALLINT,      -- 月能见度4-10km内日数
    vis_20_days  SMALLINT,      -- 月能见度10-20km内日数
    vis_20p_days SMALLINT,      -- 月能见度20km上日数
    PRIMARY KEY (station, ts)
); -- PARTITION BY RANGE (ts);

COMMENT ON TABLE  isd.monthly IS 'isd monthly statistics';
COMMENT ON COLUMN isd.monthly.ts IS '月份时间戳,yyyy-mm-01';
COMMENT ON COLUMN isd.monthly.station IS '11位台站号';
COMMENT ON COLUMN isd.monthly.count IS '本月有观测记录的天数';
COMMENT ON COLUMN isd.monthly.temp_mean IS '月平均气温';
COMMENT ON COLUMN isd.monthly.temp_min IS '月最低气温';
COMMENT ON COLUMN isd.monthly.temp_max IS '月最高气温';
COMMENT ON COLUMN isd.monthly.temp_min_avg IS '月内每日最低气温均值';
COMMENT ON COLUMN isd.monthly.temp_max_avg IS '月内每日最高气温均值';
COMMENT ON COLUMN isd.monthly.dewp_mean IS '月平均露点';
COMMENT ON COLUMN isd.monthly.dewp_min IS '月最低露点';
COMMENT ON COLUMN isd.monthly.dewp_max IS '月最高露点';
COMMENT ON COLUMN isd.monthly.slp_mean IS '月平均气压';
COMMENT ON COLUMN isd.monthly.slp_min IS '月最低气压';
COMMENT ON COLUMN isd.monthly.slp_max IS '月最高气压';
COMMENT ON COLUMN isd.monthly.prcp_sum IS '月总降水';
COMMENT ON COLUMN isd.monthly.prcp_sum IS '月最大降水';
COMMENT ON COLUMN isd.monthly.prcp_mean IS '月平均降水';
COMMENT ON COLUMN isd.monthly.wdsp_mean IS '月平均风速';
COMMENT ON COLUMN isd.monthly.wdsp_max IS '月最大风速';
COMMENT ON COLUMN isd.monthly.gust_max IS '月最大阵风';
COMMENT ON COLUMN isd.monthly.sunny_days IS '月晴天日数';
COMMENT ON COLUMN isd.monthly.windy_days IS '月大风日数';
COMMENT ON COLUMN isd.monthly.foggy_days IS '月雾天日数';
COMMENT ON COLUMN isd.monthly.rainy_days IS '月雨天日数';
COMMENT ON COLUMN isd.monthly.snowy_days IS '月雪天日数';
COMMENT ON COLUMN isd.monthly.hail_days IS '月冰雹日数';
COMMENT ON COLUMN isd.monthly.thunder_days IS '月雷暴日数';
COMMENT ON COLUMN isd.monthly.tornado_days IS '月龙卷日数';
COMMENT ON COLUMN isd.monthly.hot_days IS '月高温日数';
COMMENT ON COLUMN isd.monthly.cold_days IS '月低温日数';
COMMENT ON COLUMN isd.monthly.vis_4_days IS '月能见度4km内日数';
COMMENT ON COLUMN isd.monthly.vis_10_days IS '月能见度4-10km内日数';
COMMENT ON COLUMN isd.monthly.vis_20_days IS '月能见度10-20km内日数';
COMMENT ON COLUMN isd.monthly.vis_20p_days IS '月能见度20km上日数';

CREATE INDEX IF NOT EXISTS monthly_ts_idx ON isd.monthly (ts);


------------------------------------------------
-- isd_yearly
--   yearly observation summary data
------------------------------------------------
-- DROP TABLE IF EXISTS isd.yearly;
CREATE TABLE IF NOT EXISTS isd.yearly
(
    ts           DATE,          -- 年份时间戳,yyyy-01-01
    station      VARCHAR(12),   -- 11位台站号
    count        SMALLINT,      -- 本年度有观测记录的天数
    temp_min     NUMERIC(3, 1), -- 年最低气温
    temp_max     NUMERIC(3, 1), -- 年最高气温
    dewp_min     NUMERIC(3, 1), -- 年最低露点
    dewp_max     NUMERIC(3, 1), -- 年最高露点
    prcp_sum     NUMERIC(8, 1), -- 年总降水
    prcp_max     NUMERIC(5, 1), -- 年最大降水
    wdsp_max     NUMERIC(4, 1), -- 年最大风速
    gust_max     NUMERIC(4, 1), -- 年最大阵风
    sunny_days   SMALLINT,      -- 年晴天日数
    windy_days   SMALLINT,      -- 年大风日数
    foggy_days   SMALLINT,      -- 年雾天日数
    rainy_days   SMALLINT,      -- 年雨天日数
    snowy_days   SMALLINT,      -- 年雪天日数
    hail_days    SMALLINT,      -- 年冰雹日数
    thunder_days SMALLINT,      -- 年雷暴日数
    tornado_days SMALLINT,      -- 年龙卷日数
    hot_days     SMALLINT,      -- 年高温日数
    cold_days    SMALLINT,      -- 年低温日数
    vis_4_days   SMALLINT,      -- 年能见度4km内日数
    vis_10_days  SMALLINT,      -- 年能见度4-10km内日数
    vis_20_days  SMALLINT,      -- 年能见度10-20km内日数
    vis_20p_days SMALLINT,      -- 年能见度20km上日数
    PRIMARY KEY (station, ts)
); -- PARTITION BY RANGE (ts);

COMMENT ON TABLE isd.yearly IS 'isd yearly statistics';
COMMENT ON COLUMN isd.yearly.ts IS 'year timestamp, yyyy-01-01';
COMMENT ON COLUMN isd.yearly.station IS '11 char station number';
COMMENT ON COLUMN isd.yearly.count IS '本年度有观测记录的天数';
COMMENT ON COLUMN isd.yearly.temp_min IS '年最低气温';
COMMENT ON COLUMN isd.yearly.temp_max IS '年最高气温';
COMMENT ON COLUMN isd.yearly.dewp_min IS '年最低露点';
COMMENT ON COLUMN isd.yearly.dewp_max IS '年最高露点';
COMMENT ON COLUMN isd.yearly.prcp_sum IS '年总降水';
COMMENT ON COLUMN isd.yearly.prcp_max IS '年最大降水';
COMMENT ON COLUMN isd.yearly.wdsp_max IS '年最大风速';
COMMENT ON COLUMN isd.yearly.gust_max IS '年最大阵风';
COMMENT ON COLUMN isd.yearly.sunny_days IS '年晴天日数';
COMMENT ON COLUMN isd.yearly.windy_days IS '年大风日数';
COMMENT ON COLUMN isd.yearly.foggy_days IS '年雾天日数';
COMMENT ON COLUMN isd.yearly.rainy_days IS '年雨天日数';
COMMENT ON COLUMN isd.yearly.snowy_days IS '年雪天日数';
COMMENT ON COLUMN isd.yearly.hail_days IS '年冰雹日数';
COMMENT ON COLUMN isd.yearly.thunder_days IS '年雷暴日数';
COMMENT ON COLUMN isd.yearly.tornado_days IS '年龙卷日数';
COMMENT ON COLUMN isd.yearly.hot_days IS '年高温日数';
COMMENT ON COLUMN isd.yearly.cold_days IS '年低温日数';
COMMENT ON COLUMN isd.yearly.vis_4_days IS '年能见度4km内日数';
COMMENT ON COLUMN isd.yearly.vis_10_days IS '年能见度4-10km内日数';
COMMENT ON COLUMN isd.yearly.vis_20_days IS '年能见度10-20km内日数';
COMMENT ON COLUMN isd.yearly.vis_20p_days IS '年能见度20km上日数';

CREATE INDEX IF NOT EXISTS yearly_ts_idx ON isd.yearly (ts);



--============================================================--
--                      Functions                             --
--============================================================--

------------------------------------------------
-- isd.refresh
-- recalculate latest partition of isd.monthly and isd_yearly according to isd.daily_latest
------------------------------------------------
CREATE OR REPLACE FUNCTION isd.refresh_full() RETURNS VOID AS
$$
TRUNCATE isd.monthly;
INSERT INTO isd.monthly
SELECT date_trunc('month', ts)                                                    AS ts,           -- 月份
       station,                                                                                    -- 站号
       count(*)                                                                   AS count,        -- 站号
       round(avg(temp_mean) ::NUMERIC, 1)::NUMERIC(3, 1)                          AS temp_mean,    -- 月平均气温
       round(min(temp_min) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_min,     -- 月最低气温均值
       round(max(temp_max) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_max,     -- 月最高气温均值
       round(avg(temp_min) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_min_avg, -- 月最低气温均值
       round(avg(temp_max) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_max_avg, -- 月最高气温均值
       round(avg(dewp_mean) ::NUMERIC, 1)::NUMERIC(3, 1)                          AS dewp_mean,    -- 月平均露点
       round(min(dewp_mean) ::NUMERIC, 1) ::NUMERIC(3, 1)                         AS dewp_min,     -- 月最低露点
       round(max(dewp_mean) ::NUMERIC, 1) ::NUMERIC(3, 1)                         AS dewp_max,     -- 月最高露点
       round(avg(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_mean,     -- 月平均气压
       round(min(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_min,      -- 月最低气压
       round(max(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_max,      -- 月最高气压
       round(sum(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_sum,     -- 月总降水
       round(max(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_max,     -- 月最大降水
       round(avg(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_mean,    -- 月平均降水
       round(avg(wdsp_mean) ::NUMERIC, 1) ::NUMERIC(4, 1)                         AS wdsp_mean,    -- 月平均风速
       round(max(wdsp_max) ::NUMERIC, 1) ::NUMERIC(4, 1)                          AS wdsp_max,     -- 月最大风速
       round(max(gust) ::NUMERIC, 1) ::NUMERIC(4, 1)                              AS gust_max,     -- 月最大阵风
       count(*) FILTER ( WHERE NOT is_foggy AND NOT is_rainy AND NOT is_snowy
           AND NOT is_hail AND NOT is_thunder AND NOT is_tornado) ::SMALLINT      AS sunny_days,   -- 月晴天日数
       count(*) FILTER ( WHERE wdsp_max >= 17.2) ::SMALLINT                       AS windy_days,   -- 月大风日数
       count(*) FILTER (WHERE is_foggy) ::SMALLINT                                AS foggy_days,   -- 月雾天日数
       count(*) FILTER (WHERE is_rainy) ::SMALLINT                                AS rainy_days,   -- 月雨天日数
       count(*) FILTER (WHERE is_snowy) ::SMALLINT                                AS snowy_days,   -- 月雪天日数
       count(*) FILTER (WHERE is_hail) ::SMALLINT                                 AS hail_days,    -- 月冰雹日数
       count(*) FILTER (WHERE is_thunder) ::SMALLINT                              AS thunder_days, -- 月雷暴日数
       count(*) FILTER (WHERE is_tornado) ::SMALLINT                              AS tornado_days, -- 月龙卷日数
       count(*) FILTER ( WHERE temp_max >= 30 ) ::SMALLINT                        AS hot_days,     -- 月高温日数
       count(*) FILTER ( WHERE temp_min < 0 ) ::SMALLINT                          AS cold_days,    -- 月低温日数
       count(*) FILTER ( WHERE vis_mean >= 0 AND vis_mean < 4000 ) ::SMALLINT     AS vis_4_days,   -- 月能见度4km内日数
       count(*) FILTER ( WHERE vis_mean >= 4000 AND vis_mean < 10000)::SMALLINT   AS vis_10_days,  -- 能见度4-10km时间占比百分数
       count(*) FILTER ( WHERE vis_mean >= 10000 AND vis_mean < 20000 )::SMALLINT AS vis_20_days,  -- 能见度10-20km时间占比百分数
       count(*) FILTER ( WHERE vis_mean >= 20000 )::SMALLINT                      AS vis_20p_days  -- 能见度20km+时间占比百分数
FROM isd.daily
GROUP by date_trunc('month', ts), station
ORDER BY 2, 1;

TRUNCATE isd.yearly;
INSERT INTO isd.yearly
SELECT date_trunc('year', ts)::DATE AS ts,               -- 年份
       station,                                          -- 站号
       sum(count)                       AS count,        -- 年观测记录总数
       min(temp_min)                    AS temp_min,     -- 年最低气温
       max(temp_max)                    AS temp_max,     -- 年最高气温
       min(dewp_min)                    AS dewp_min,     -- 年最低露点
       max(dewp_max)                    AS dewp_max,     -- 年最高露点
       sum(prcp_sum)                    AS prcp_sum,     -- 年总降水
       max(prcp_max)                    AS prcp_max,     -- 年最大降水
       max(wdsp_max)                    AS wdsp_max,     -- 年最大风速
       max(gust_max)                    AS gust_max,     -- 年最大阵风
       sum(sunny_days)                  AS sunny_days,   -- 年晴天日数
       sum(windy_days)                  AS windy_days,   -- 年大风日数
       sum(foggy_days)                  AS foggy_days,   -- 年雾天日数
       sum(rainy_days)                  AS rainy_days,   -- 年雨天日数
       sum(snowy_days)                  AS snowy_days,   -- 年雪天日数
       sum(hail_days)                   AS hail_days,    -- 年冰雹日数
       sum(thunder_days)                AS thunder_days, -- 年雷暴日数
       sum(tornado_days)                AS tornado_days, -- 年龙卷日数
       sum(hot_days)                    AS hot_days,     -- 年高温日数
       sum(cold_days)                   AS cold_days,    -- 年低温日数
       sum(vis_4_days)                  AS vis_4_days,   -- 年能见度4km内日数
       sum(vis_10_days)                 AS vis_10_days,  -- 年能见度4-10km内日数
       sum(vis_20_days)                 AS vis_20_days,  -- 年能见度10-20km内日数
       sum(vis_20p_days)                AS vis_20p_days  -- 年能见度20km上日数
FROM isd.monthly
GROUP by date_trunc('year', ts), station
ORDER BY 2, 1;
$$ LANGUAGE SQL;

COMMENT ON FUNCTION isd.refresh_full() IS 'recalculate entire isd.monthly and isd.yearly';


------------------------------------------------
-- isd.refresh
--    refresh monthly & yearly data by interval
------------------------------------------------
CREATE OR REPLACE FUNCTION isd.refresh(_from DATE DEFAULT NULL, _to DATE DEFAULT NULL) RETURNS VOID AS
$$
DECLARE
    _from DATE := coalesce(_from, date_trunc('year', now())::DATE);
    _to   DATE := coalesce(_to, now()::DATE);
BEGIN
    RAISE NOTICE 'refresh isd.monthly [%,%] from isd.daily [%,%]', date_trunc('month', _from)::DATE, date_trunc('month', _to)::DATE, _from, _to;
    INSERT INTO isd.monthly
    SELECT date_trunc('month', ts)                                                    AS ts,           -- 月份
           station,                                                                                    -- 站号
           count(*)                                                                   AS count,        -- 月内观测记录数
           round(avg(temp_mean) ::NUMERIC, 1)::NUMERIC(3, 1)                          AS temp_mean,    -- 月平均气温
           round(min(temp_min) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_min,     -- 月最低气温均值
           round(max(temp_max) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_max,     -- 月最高气温均值
           round(avg(temp_min) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_min_avg, -- 月最低气温均值
           round(avg(temp_max) ::NUMERIC, 1) ::NUMERIC(3, 1)                          AS temp_max_avg, -- 月最高气温均值
           round(avg(dewp_mean) ::NUMERIC, 1)::NUMERIC(3, 1)                          AS dewp_mean,    -- 月平均露点
           round(min(dewp_mean) ::NUMERIC, 1) ::NUMERIC(3, 1)                         AS dewp_min,     -- 月最低露点
           round(max(dewp_mean) ::NUMERIC, 1) ::NUMERIC(3, 1)                         AS dewp_max,     -- 月最高露点
           round(avg(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_mean,     -- 月平均气压
           round(min(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_min,      -- 月最低气压
           round(max(slp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                          AS slp_max,      -- 月最高气压
           round(sum(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_sum,     -- 月总降水
           round(max(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_max,     -- 月最大降水
           round(avg(prcp_mean) ::NUMERIC, 1) ::NUMERIC(5, 1)                         AS prcp_mean,    -- 月平均降水
           round(avg(wdsp_mean) ::NUMERIC, 1) ::NUMERIC(4, 1)                         AS wdsp_mean,    -- 月平均风速
           round(max(wdsp_max) ::NUMERIC, 1) ::NUMERIC(4, 1)                          AS wdsp_max,     -- 月最大风速
           round(max(gust) ::NUMERIC, 1) ::NUMERIC(4, 1)                              AS gust_max,     -- 月最大阵风
           count(*) FILTER ( WHERE NOT is_foggy AND NOT is_rainy AND NOT is_snowy
               AND NOT is_hail AND NOT is_thunder AND NOT is_tornado) ::SMALLINT      AS sunny_days,   -- 月晴天日数
           count(*) FILTER ( WHERE wdsp_max >= 17.2) ::SMALLINT                       AS windy_days,   -- 月大风日数
           count(*) FILTER (WHERE is_foggy) ::SMALLINT                                AS foggy_days,   -- 月雾天日数
           count(*) FILTER (WHERE is_rainy) ::SMALLINT                                AS rainy_days,   -- 月雨天日数
           count(*) FILTER (WHERE is_snowy) ::SMALLINT                                AS snowy_days,   -- 月雪天日数
           count(*) FILTER (WHERE is_hail) ::SMALLINT                                 AS hail_days,    -- 月冰雹日数
           count(*) FILTER (WHERE is_thunder) ::SMALLINT                              AS thunder_days, -- 月雷暴日数
           count(*) FILTER (WHERE is_tornado) ::SMALLINT                              AS tornado_days, -- 月龙卷日数
           count(*) FILTER ( WHERE temp_max >= 30 ) ::SMALLINT                        AS hot_days,     -- 月高温日数
           count(*) FILTER ( WHERE temp_min < 0 ) ::SMALLINT                          AS cold_days,    -- 月低温日数
           count(*) FILTER ( WHERE vis_mean >= 0 AND vis_mean < 4000 ) ::SMALLINT     AS vis_4_days,   -- 月能见度4km内日数
           count(*) FILTER ( WHERE vis_mean >= 4000 AND vis_mean < 10000)::SMALLINT   AS vis_10_days,  -- 能见度4-10km时间占比百分数
           count(*) FILTER ( WHERE vis_mean >= 10000 AND vis_mean < 20000 )::SMALLINT AS vis_20_days,  -- 能见度10-20km时间占比百分数
           count(*) FILTER ( WHERE vis_mean >= 20000 )::SMALLINT                      AS vis_20p_days  -- 能见度20km+时间占比百分数
    FROM isd.daily
    WHERE ts BETWEEN _from AND _to
    GROUP by date_trunc('month', ts), station
    ORDER BY 2, 1
    ON CONFLICT (ts, station) DO UPDATE SET
        count = EXCLUDED.count,
        temp_mean = EXCLUDED.temp_mean,temp_min = EXCLUDED.temp_min,temp_max = EXCLUDED.temp_max,temp_min_avg = EXCLUDED.temp_min_avg,temp_max_avg = EXCLUDED.temp_max_avg,
        dewp_mean = EXCLUDED.dewp_mean,dewp_min = EXCLUDED.dewp_min,dewp_max = EXCLUDED.dewp_max,slp_mean = EXCLUDED.slp_mean,slp_min = EXCLUDED.slp_min,slp_max = EXCLUDED.slp_max,
        prcp_sum = EXCLUDED.prcp_sum,prcp_max = EXCLUDED.prcp_max,prcp_mean = EXCLUDED.prcp_mean,
        wdsp_mean = EXCLUDED.wdsp_mean,wdsp_max = EXCLUDED.wdsp_max,gust_max = EXCLUDED.gust_max,
        sunny_days = EXCLUDED.sunny_days,windy_days = EXCLUDED.windy_days,foggy_days = EXCLUDED.foggy_days,rainy_days = EXCLUDED.rainy_days,snowy_days = EXCLUDED.snowy_days,
        hail_days = EXCLUDED.hail_days,thunder_days = EXCLUDED.thunder_days,tornado_days = EXCLUDED.tornado_days,hot_days = EXCLUDED.hot_days,cold_days = EXCLUDED.cold_days,
        vis_4_days = EXCLUDED.vis_4_days,vis_10_days = EXCLUDED.vis_10_days,vis_20_days = EXCLUDED.vis_20_days,vis_20p_days = EXCLUDED.vis_20p_days;

    RAISE NOTICE 'refresh isd.yearly [%,%] from isd.daily [%,%]',
        extract(year FROM _from), extract(year FROM _to), date_trunc('month', _from)::DATE, date_trunc('month', _to)::DATE;
    INSERT INTO isd.yearly
    SELECT date_trunc('year', ts)::DATE AS ts,               -- 年份
           station,                                          -- 站号
           sum(count)                       AS count,        -- 年观测记录总数
           min(temp_min)                    AS temp_min,     -- 年最低气温
           max(temp_max)                    AS temp_max,     -- 年最高气温
           min(dewp_min)                    AS dewp_min,     -- 年最低露点
           max(dewp_max)                    AS dewp_max,     -- 年最高露点
           sum(prcp_sum)                    AS prcp_sum,     -- 年总降水
           max(prcp_max)                    AS prcp_max,     -- 年最大降水
           max(wdsp_max)                    AS wdsp_max,     -- 年最大风速
           max(gust_max)                    AS gust_max,     -- 年最大阵风
           sum(sunny_days)                  AS sunny_days,   -- 年晴天日数
           sum(windy_days)                  AS windy_days,   -- 年大风日数
           sum(foggy_days)                  AS foggy_days,   -- 年雾天日数
           sum(rainy_days)                  AS rainy_days,   -- 年雨天日数
           sum(snowy_days)                  AS snowy_days,   -- 年雪天日数
           sum(hail_days)                   AS hail_days,    -- 年冰雹日数
           sum(thunder_days)                AS thunder_days, -- 年雷暴日数
           sum(tornado_days)                AS tornado_days, -- 年龙卷日数
           sum(hot_days)                    AS hot_days,     -- 年高温日数
           sum(cold_days)                   AS cold_days,    -- 年低温日数
           sum(vis_4_days)                  AS vis_4_days,   -- 年能见度4km内日数
           sum(vis_10_days)                 AS vis_10_days,  -- 年能见度4-10km内日数
           sum(vis_20_days)                 AS vis_20_days,  -- 年能见度10-20km内日数
           sum(vis_20p_days)                AS vis_20p_days  -- 年能见度20km上日数
    FROM isd.monthly
    WHERE ts BETWEEN date_trunc('year', _from) AND date_trunc('year', _to)
    GROUP by date_trunc('year', ts), station
    ORDER BY 2, 1
    ON CONFLICT (ts, station) DO UPDATE SET
        count = EXCLUDED.count,
        temp_min = EXCLUDED.temp_min, temp_max = EXCLUDED.temp_max, dewp_min = EXCLUDED.dewp_min, dewp_max = EXCLUDED.dewp_max,
        prcp_sum = EXCLUDED.prcp_sum, prcp_max = EXCLUDED.prcp_max, wdsp_max = EXCLUDED.wdsp_max, gust_max = EXCLUDED.gust_max,
        sunny_days = EXCLUDED.sunny_days, windy_days = EXCLUDED.windy_days, foggy_days = EXCLUDED.foggy_days, rainy_days = EXCLUDED.rainy_days,
        snowy_days = EXCLUDED.snowy_days, hail_days = EXCLUDED.hail_days, thunder_days = EXCLUDED.thunder_days, tornado_days = EXCLUDED.tornado_days,
        hot_days = EXCLUDED.hot_days, cold_days = EXCLUDED.cold_days,
        vis_4_days = EXCLUDED.vis_4_days, vis_10_days = EXCLUDED.vis_10_days, vis_20_days = EXCLUDED.vis_20_days, vis_20p_days = EXCLUDED.vis_20p_days;

END
$$
LANGUAGE PlPGSQL
VOLATILE PARALLEL UNSAFE;

COMMENT ON FUNCTION isd.refresh(DATE,DATE) IS 'recalculate the latest year of isd.monthly and isd.yearly';

-- SELECT isd.refresh_full();
-- SELECT isd.refresh();