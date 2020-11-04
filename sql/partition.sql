-------------------------------------------------
-- create isd_hourly partitions
-------------------------------------------------

-----------------------------------
-- cleanup all isd_hourly partitions
-----------------------------------
DO
$$
    DECLARE
        _relname TEXT;
    BEGIN
        FOR _relname IN SELECT relname FROM pg_class WHERE relname ~ '^isd_hourly_\d{4}$'
            LOOP
                RAISE NOTICE 'DROP TABLE %s;', _relname;
                EXECUTE 'DROP TABLE IF EXISTS ' || _relname || ';';
            END LOOP;
    END
$$;

-----------------------------------
-- create all isd_hourly partitions
-----------------------------------
-- three merged partition: 50year, 10year, 10year
SELECT create_isd_hourly_partition(1900, 1950); -- 20 GB
SELECT create_isd_hourly_partition(1950, 1960); -- 47 GB
SELECT create_isd_hourly_partition(1960, 1970); -- 41 GB

-- the rest are yearly partition: from 1970 (10GB) to 2020 (41GB)
SELECT create_isd_hourly_partition(year::INTEGER)
FROM generate_series(1970, 2020) year;



-------------------------------------------------
-- create isd_daily / monthly / yearly partitions
-------------------------------------------------
CREATE TABLE IF NOT EXISTS isd_daily_stable PARTITION OF isd_daily FOR VALUES FROM ('1900-01-01') TO ('2020-01-01');
CREATE TABLE IF NOT EXISTS isd_daily_latest PARTITION OF isd_daily FOR VALUES FROM ('2020-01-01') TO (MAXVALUE);
COMMENT ON TABLE isd_daily_stable IS 'ISD年度摘要汇总表(稳定历史数据，2020前)';
COMMENT ON TABLE isd_daily_latest IS 'ISD年度摘要汇总表(最近一年数据，2020后)';

CREATE TABLE IF NOT EXISTS isd_monthly_stable PARTITION OF isd_monthly FOR VALUES FROM ('1900-01-01') TO ('2020-01-01');
CREATE TABLE IF NOT EXISTS isd_monthly_latest PARTITION OF isd_monthly FOR VALUES FROM ('2020-01-01') TO (MAXVALUE);
COMMENT ON TABLE isd_monthly_stable IS 'ISD年度摘要汇总表(稳定历史数据，2020前)';
COMMENT ON TABLE isd_monthly_latest IS 'ISD年度摘要汇总表(最近一年数据，2020后)';

CREATE TABLE IF NOT EXISTS isd_yearly_stable PARTITION OF isd_yearly FOR VALUES FROM ('1900-01-01') TO ('2020-01-01');
CREATE TABLE IF NOT EXISTS isd_yearly_latest PARTITION OF isd_yearly FOR VALUES FROM ('2020-01-01') TO (MAXVALUE);
COMMENT ON TABLE isd_yearly_stable IS 'ISD年度摘要汇总表(稳定历史数据，2020前)';
COMMENT ON TABLE isd_yearly_latest IS 'ISD年度摘要汇总表(最近一年数据，2020后)';

