CREATE TABLE notifications (
                               id UUID PRIMARY KEY,

                               event_id UUID NOT NULL,
                               user_id UUID NOT NULL,
                               transfer_id UUID NOT NULL,

                               type VARCHAR(50) NOT NULL,
                               message VARCHAR(500) NOT NULL,

                               created_at TIMESTAMP WITH TIME ZONE NOT NULL
);