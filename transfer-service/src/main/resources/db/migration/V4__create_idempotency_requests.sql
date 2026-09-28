CREATE TABLE idempotency_requests (
                                      id UUID PRIMARY KEY,

                                      user_id UUID NOT NULL,

                                      idempotency_key VARCHAR(100) NOT NULL,

                                      request_hash VARCHAR(64) NOT NULL,

                                      transfer_id UUID,

                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT uk_idempotency_user_key
                                          UNIQUE (user_id, idempotency_key),

                                      CONSTRAINT fk_idempotency_transfer
                                          FOREIGN KEY (transfer_id)
                                              REFERENCES transfers(id)
);