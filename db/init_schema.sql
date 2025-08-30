CREATE SCHEMA IF NOT EXISTS gatieottae;
SET search_path TO gatieottae;

-- Member table
CREATE TABLE member (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    profile_image_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Auth Provider table
CREATE TABLE auth_provider (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    UNIQUE (provider, provider_user_id)
);

-- Travel Group table
CREATE TABLE travel_group (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    owner_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Group Member table
CREATE TABLE group_member (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (group_id, member_id)
);

-- Invitation table
CREATE TABLE invitation (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    invited_by INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    token VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Schedule table
CREATE TABLE schedule (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Schedule Participant table
CREATE TABLE schedule_participant (
    id SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL REFERENCES schedule(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'invited',
    UNIQUE (schedule_id, member_id)
);

-- Poll table
CREATE TABLE poll (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Poll Option table
CREATE TABLE poll_option (
    id SERIAL PRIMARY KEY,
    poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_text VARCHAR(255) NOT NULL
);

-- Poll Vote table
CREATE TABLE poll_vote (
    id SERIAL PRIMARY KEY,
    poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_id INTEGER NOT NULL REFERENCES poll_option(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    voted_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (poll_id, member_id)
);

-- Expense table
CREATE TABLE expense (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    paid_by INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    paid_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Expense Share table
CREATE TABLE expense_share (
    id SERIAL PRIMARY KEY,
    expense_id INTEGER NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    share NUMERIC(12,2) NOT NULL,
    UNIQUE (expense_id, member_id)
);

-- Settlement table
CREATE TABLE settlement (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    from_member INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    to_member INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL,
    settled_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Chat Message table
CREATE TABLE chat_message (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES member(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Chat Read table
CREATE TABLE chat_read (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL REFERENCES chat_message(id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    UNIQUE (message_id, member_id)
);

-- Notification table
CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    member_id INTEGER NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Guide table
CREATE TABLE guide (
    id SERIAL PRIMARY KEY,
    group_id INTEGER NOT NULL REFERENCES travel_group(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    created_by INTEGER REFERENCES member(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Guide Day table
CREATE TABLE guide_day (
    id SERIAL PRIMARY KEY,
    guide_id INTEGER NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
    day_index INTEGER NOT NULL,
    date DATE,
    UNIQUE (guide_id, day_index)
);

-- Guide Item table
CREATE TABLE guide_item (
    id SERIAL PRIMARY KEY,
    guide_day_id INTEGER NOT NULL REFERENCES guide_day(id) ON DELETE CASCADE,
    time TIME,
    title VARCHAR(128) NOT NULL,
    description TEXT
);

-- Indexes for performance (examples)
CREATE INDEX idx_group_member_group ON group_member(group_id);
CREATE INDEX idx_group_member_member ON group_member(member_id);
CREATE INDEX idx_schedule_group ON schedule(group_id);
CREATE INDEX idx_expense_group ON expense(group_id);
CREATE INDEX idx_chat_message_group ON chat_message(group_id);
CREATE INDEX idx_notification_member ON notification(member_id);