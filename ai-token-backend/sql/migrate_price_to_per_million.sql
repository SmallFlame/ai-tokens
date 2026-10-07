-- 将模型单价从「元/千Token」迁移到「元/百万Token」
-- 原价 × 1000 = 新价（例如 0.015 元/千T → 15 元/百万T）
-- 执行前请先备份 t_model 表！

UPDATE t_model
SET
  input_price          = input_price          * 1000,
  output_price         = output_price         * 1000,
  cache_creation_price = CASE WHEN cache_creation_price IS NOT NULL THEN cache_creation_price * 1000 ELSE NULL END,
  cache_read_price     = CASE WHEN cache_read_price     IS NOT NULL THEN cache_read_price     * 1000 ELSE NULL END;
