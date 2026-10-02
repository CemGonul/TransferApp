CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_unpublished_created_at
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
