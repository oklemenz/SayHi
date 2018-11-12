with ranges as (
    select (range*5+1)::text || ' - ' || (range*5+5)::text AS range,
            range*5+1 AS r_min, range*5+5 AS r_max
    from generate_series(0,20) AS t(range)
)
select
    r.range,
    count(a.*) as count
from ranges r
    left outer join analytics_json a
        on cast(a.data->>'age' as integer) between r.r_min and r.r_max
where
    space = 'standard' and
    cast(data->>'year' as integer) = 2018
group by
    r.range
order by
    r.range;