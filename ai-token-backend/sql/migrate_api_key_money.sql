-- 为 t_api_key 添加金额额度字段
ALTER TABLE t_api_key
  ADD COLUMN money_quota DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '金额额度(元)，0=无额度，-1=不限制',
  ADD COLUMN used_money DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '已用金额(元)';

-- 为 money_quota 添加索引（用于余额查询）
CREATE INDEX idx_money_quota ON t_api_key(money_quota);
