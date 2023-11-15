create table messages
(
    id        uuid default gen_random_uuid() not null primary key,
    payload   text                           not null,
    timestamp decimal                        not null
);

