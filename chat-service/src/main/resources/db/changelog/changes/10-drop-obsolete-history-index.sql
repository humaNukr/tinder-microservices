-- changeset humaNukr:10-drop-obsolete-history-index
DROP INDEX idx_messages_history_lookup;
-- rollback CREATE INDEX idx_messages_history_lookup ON messages (chat_id, id DESC) WHERE status = 'SENT';