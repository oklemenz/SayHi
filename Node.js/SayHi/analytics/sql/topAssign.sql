select
    tags_json.data->>'name' as name,
    match.gender,
    match.count,
    match.value
from (
    select
        data->>'tagEffectiveKey' as tagKey,
        data->>'gender' as gender,
        data->>'value' as value,
        count(analytics_json) as count
    from analytics_json
    where
        space = 'standard' and
        cast(data->>'year' as integer) = 2018 and
        data->>'event' = 'tag_assign'
    group by
        data->>'tagEffectiveKey',
        data->>'gender',
        data->>'value'
    order by
        count desc
) as match
inner join tags_json
    on tags_json.key = match.tagKey;