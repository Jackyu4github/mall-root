-- ===========================================
-- A. 安全创建枚举类型（带存在性校验）
-- ===========================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'dict_status_enum'
    ) THEN
        CREATE TYPE dict_status_enum AS ENUM ('ENABLED', 'DISABLED');
    END IF;
END$$;

-- ===========================================
-- B. 创建表（带存在性校验）
-- ===========================================
CREATE TABLE IF NOT EXISTS sys_dict (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(64),
    dict_type    VARCHAR(64),           -- 类型/分组，如: gender、order_status
    dict_key     VARCHAR(128),           -- 业务键，如: M/F、PAID/CREATED
    dict_value   VARCHAR(255),           -- 业务值，如: 1/0 或可读值
    label        VARCHAR(255),                    -- 展示名（可选）
    sort         INTEGER      NOT NULL DEFAULT 0, -- 排序
    status       VARCHAR(64) NOT NULL DEFAULT 'ENABLED',
    remark       TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 唯一约束：同一 dict_type 下，dict_key 唯一
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_sys_dict_type_key'
    ) THEN
        ALTER TABLE sys_dict
        ADD CONSTRAINT uk_sys_dict_type_key UNIQUE (dict_type, dict_key);
    END IF;
END$$;

-- ===========================================
-- C. updated_at 自动更新时间触发器
-- ===========================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_proc WHERE proname = 'sys_dict_set_updated_at'
    ) THEN
        CREATE OR REPLACE FUNCTION sys_dict_set_updated_at()
        RETURNS TRIGGER AS $BODY$
        BEGIN
            NEW.updated_at := NOW();
            RETURN NEW;
        END
        $BODY$ LANGUAGE plpgsql;
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'tr_sys_dict_set_updated_at'
    ) THEN
        CREATE TRIGGER tr_sys_dict_set_updated_at
        BEFORE UPDATE ON sys_dict
        FOR EACH ROW
        EXECUTE FUNCTION sys_dict_set_updated_at();
    END IF;
END$$;

-- ===========================================
-- D. 常用索引（类型过滤、状态过滤）
-- ===========================================
CREATE INDEX IF NOT EXISTS idx_sys_dict_type   ON sys_dict (dict_type);
CREATE INDEX IF NOT EXISTS idx_sys_dict_status ON sys_dict (status);

-- ===========================================
-- E. 示例数据（UPSERT 幂等）
--    例子：性别、订单状态、开关
-- ===========================================
-- 性别
INSERT INTO sys_dict (dict_type, dict_key, dict_value, label, sort, status, remark)
VALUES
 ('gender','M','1','男',10,'ENABLED','male'),
 ('gender','F','0','女',20,'ENABLED','female')
ON CONFLICT (dict_type, dict_key) DO UPDATE
SET dict_value = EXCLUDED.dict_value,
    label      = EXCLUDED.label,
    sort       = EXCLUDED.sort,
    status     = EXCLUDED.status,
    remark     = EXCLUDED.remark;

-- 订单状态（示例，与你现有 order_status_enum 可独立并存，用于前端展示或跨系统映射）
INSERT INTO sys_dict (dict_type, dict_key, dict_value, label, sort, status, remark)
VALUES
 ('order_status','CREATED','10','下单未支付',10,'ENABLED',NULL),
 ('order_status','PAID','20','已支付',20,'ENABLED',NULL),
 ('order_status','RECEIVED','30','已接单',30,'ENABLED',NULL),
 ('order_status','SHIPPED','40','已发货/出库',40,'ENABLED',NULL),
 ('order_status','COMPLETED','50','已完成',50,'ENABLED',NULL),
 ('order_status','CANCELED','90','已取消',90,'ENABLED',NULL)
ON CONFLICT (dict_type, dict_key) DO UPDATE
SET dict_value = EXCLUDED.dict_value,
    label      = EXCLUDED.label,
    sort       = EXCLUDED.sort,
    status     = EXCLUDED.status,
    remark     = EXCLUDED.remark;

-- 开关
INSERT INTO sys_dict (dict_type, dict_key, dict_value, label, sort, status, remark)
VALUES
 ('switch','ON','1','开启',10,'ENABLED',NULL),
 ('switch','OFF','0','关闭',20,'ENABLED',NULL)
ON CONFLICT (dict_type, dict_key) DO UPDATE
SET dict_value = EXCLUDED.dict_value,
    label      = EXCLUDED.label,
    sort       = EXCLUDED.sort,
    status     = EXCLUDED.status,
    remark     = EXCLUDED.remark;

-- ===========================================
-- F. 可选：注释（便于数据字典工具识别）
-- ===========================================
COMMENT ON TABLE  sys_dict IS '通用字典表：dict_type+dict_key 唯一';
COMMENT ON COLUMN sys_dict.dict_type  IS '字典类型/分组，如 gender/order_status';
COMMENT ON COLUMN sys_dict.dict_key   IS '键：类型内唯一';
COMMENT ON COLUMN sys_dict.dict_value IS '值：字符串（可放编码/实际值）';
COMMENT ON COLUMN sys_dict.label      IS '显示名';
COMMENT ON COLUMN sys_dict.sort       IS '排序：小的在前';
COMMENT ON COLUMN sys_dict.status     IS 'ENABLED/DISABLED';
COMMENT ON COLUMN sys_dict.remark     IS '备注';




