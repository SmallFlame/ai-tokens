-- 为 t_call_log 添加客户端 IP 字段
ALTER TABLE t_call_log
  ADD COLUMN client_ip VARCHAR(64) NULL COMMENT '客户端IP' AFTER request_id;

-- 为 client_ip 添加索引（用于按 IP 排查调用记录）
CREATE INDEX idx_client_ip ON t_call_log(client_ip);
