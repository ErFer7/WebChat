CREATE TABLE "user" (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE chat (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    is_group_chat BOOLEAN NOT NULL
);

CREATE TABLE chat_member (
    id SERIAL PRIMARY KEY,
    user_id INT,
    chat_id INT,
    FOREIGN KEY (user_id) REFERENCES "user" (id),
    FOREIGN KEY (chat_id) REFERENCES chat (id)
);

CREATE TABLE message (
    id SERIAL PRIMARY KEY,
    text VARCHAR(1000) NOT NULL,
    sender_id INT,
    chat_id INT,
    sent_at TIMESTAMP NOT NULL,
    FOREIGN KEY (sender_id) REFERENCES "user" (id),
    FOREIGN KEY (chat_id) REFERENCES chat (id)
);