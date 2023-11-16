drop table if exists messages;
create table messages
(
    id        uuid default gen_random_uuid() not null primary key,
    payload   json                           not null,
    timestamp decimal                        not null
);

INSERT INTO messages (payload, timestamp) VALUES ('{"a":"b"}', 1234);