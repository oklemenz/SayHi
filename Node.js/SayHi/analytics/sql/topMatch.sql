select
    tags_json.data->>'name' as name,
    match.gender,
    match.match_side,
    match.count
from (
    select
        data->>'matchTagKey' as tagKey,
        data->>'gender' as gender,
        data->>'matchSide' as match_side,
        count(analytics_json) as count
    from analytics_json
    where
        space = 'standard' and
        cast(data->>'year' as integer) = 2018 and
        data->>'event' = 'match_tag'
    group by
        data->>'matchTagKey',
        data->>'gender',
        data->>'matchSide'
    order by
        count desc
) as match
inner join tags_json
    on tags_json.key = match.tagKey;