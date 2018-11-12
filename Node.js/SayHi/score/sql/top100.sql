select
    row_number() OVER () as pos,
    data->>'alias' as alias,
    data->>'value' as score,
    key
from scores
where
    space = 'standard'
order by data->>'value' desc
limit 100;