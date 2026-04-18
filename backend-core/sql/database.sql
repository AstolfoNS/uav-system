DROP DATABASE IF EXISTS uav_system;
CREATE DATABASE IF NOT EXISTS uav_system;

USE uav_system;

SHOW TABLES;



CREATE TABLE IF NOT EXISTS users
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    username                    VARCHAR(64)     NOT NULL                                                                COMMENT '用户姓名',
    nickname                    VARCHAR(64)     NOT NULL                                                                COMMENT '用户昵称',
    avatar_url                  VARCHAR(1024)       NULL                                                                COMMENT '用户头像',
    email                       VARCHAR(32)     NOT NULL                                                                COMMENT '用户邮箱',
    phone_number                VARCHAR(32)         NULL                                                                COMMENT '用户手机号',
    password                    VARCHAR(255)        NULL                                                                COMMENT '用户密码',
    gender                      TINYINT             NULL    DEFAULT 0                                                      COMMENT '用户性别：0=未知，1=男，2=女',
    introduction                TEXT                NULL                                                                COMMENT '用户简介',
    last_active_time            DATETIME            NULL    DEFAULT CURRENT_TIMESTAMP                                      COMMENT '最后在线时间',
    last_login_time             DATETIME            NULL    DEFAULT CURRENT_TIMESTAMP                                      COMMENT '最后登录时间',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',
    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常，2=锁定',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT         GENERATED   ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED              COMMENT '唯一性标记',

    UNIQUE KEY uk_username(username, unique_if_active),
    UNIQUE KEY uk_email(email, unique_if_active),
    UNIQUE KEY uk_phone_number(phone_number, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户表';



CREATE TABLE IF NOT EXISTS roles
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    code                        VARCHAR(64)     NOT NULL                                                                COMMENT '角色编码',
    name                        VARCHAR(64)     NOT NULL                                                                COMMENT '角色名称',
    description                 VARCHAR(512)        NULL                                                                COMMENT '角色描述',
    level                       INT             NOT NULL    DEFAULT 0                                                   COMMENT '角色等级',
    sort_order                  INT             NOT NULL    DEFAULT 0                                                   COMMENT '排序顺序',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',
    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常，2=锁定',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT         GENERATED   ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED              COMMENT '唯一性标记',

    UNIQUE KEY uk_code(code, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '角色表';



CREATE TABLE IF NOT EXISTS user_roles
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    user_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '用户ID',
    role_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '角色ID',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    -- 状态字段
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    UNIQUE KEY uk_user_id_role_id(user_id, role_id, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户角色关系表';



CREATE TABLE IF NOT EXISTS permissions
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    code                        VARCHAR(64)     NOT NULL                                                                COMMENT '权限编码',
    name                        VARCHAR(64)     NOT NULL                                                                COMMENT '权限名称',
    type                        TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '权限类型：1=API，2=WEB，3=BOT',
    description                 VARCHAR(512)        NULL                                                                COMMENT '权限描述',
    sort_order                  INT             NOT NULL    DEFAULT 0                                                   COMMENT '排序顺序',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',
    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常，2=锁定',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT         GENERATED   ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED              COMMENT '唯一性标记',

    UNIQUE KEY uk_code(code, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '权限表';



CREATE TABLE IF NOT EXISTS role_permissions
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    role_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '角色ID',
    permission_id               BIGINT UNSIGNED NOT NULL                                                                COMMENT '权限ID',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    -- 状态字段
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    UNIQUE KEY uk_role_id_permission_id(role_id, permission_id, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '角色权限关系表';



CREATE TABLE IF NOT EXISTS yolo_nodes
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    node_name                   VARCHAR(64)     NOT NULL                                                                COMMENT '节点名称',
    description                 VARCHAR(512)        NULL                                                                COMMENT '节点描述',
    active_weight_name          VARCHAR(64)         NULL                                                                COMMENT '当前正在使用的活跃模型名称 (冗余字段，方便查询)',
    host                        VARCHAR(32)     NOT NULL                                                                COMMENT '节点HOST (IP或域名)',
    port                        VARCHAR(8)      NOT NULL                                                                COMMENT '节点PORT',
    http_protocol               VARCHAR(8)      NOT NULL    DEFAULT 'http'                                              COMMENT 'HTTP协议',
    api_version                 VARCHAR(4)      NOT NULL    DEFAULT 'v1'                                                COMMENT 'API版本',

    -- 审计与控制字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',

    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '节点状态：0=离线，1=在线，2=异常',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    UNIQUE KEY uk_host_port(host, port, unique_if_active),
    UNIQUE KEY uk_node_name(node_name, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'YOLO服务节点表';



CREATE TABLE IF NOT EXISTS yolo_weights
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    node_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '所属节点ID (关联 yolo_nodes.id)',
    filename                    VARCHAR(64)     NOT NULL                                                                COMMENT '模型文件名 (如: yolo26n.pt)',
    description                 VARCHAR(512)        NULL                                                                COMMENT '模型用途描述',
    is_active                   TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '是否为节点当前活跃模型：0=否，1=是',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',

    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    -- 保证同一个节点下的文件名不能重复
    UNIQUE KEY uk_node_filename(node_id, filename, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'YOLO节点模型权重记录表';



CREATE TABLE IF NOT EXISTS yolo_node_params
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    node_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '关联的节点ID',
    template_name               VARCHAR(64)     NOT NULL    DEFAULT '__default__'                                       COMMENT '参数模板名称',
    description                 VARCHAR(512)        NULL                                                                COMMENT '参数用途描述',
    params                      JSON                NULL                                                                COMMENT '当前生效的推理参数 (如: {"conf": 0.5, "iou": 0.65})',
    is_active                   TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '是否为当前正在生效的参数：0=否，1=是',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',

    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    UNIQUE KEY uk_node_template(node_id, template_name, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'YOLO节点推理参数表';



CREATE TABLE IF NOT EXISTS yolo_detection_records
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    node_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '执行此次预测的节点ID',
    code                        VARCHAR(64)     NOT NULL                                                                COMMENT '执行编码',
    task_type                   TINYINT         NOT NULL                                                                COMMENT '任务类型：1=图像检测, 2=视频检测',
    task_status                 TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '任务状态：0=待处理,1=处理中,2=成功,3=失败',
    original_filename           VARCHAR(255)    NOT NULL                                                                COMMENT '上传的原始文件名',

    -- 结果数据
    result_url                  VARCHAR(1024)       NULL                                                                COMMENT 'FastAPI/MinIO返回的渲染后结果文件远程URL',
    detect_count                INT             NOT NULL    DEFAULT 0                                                   COMMENT '检测到的目标总数 (视频可填0或提取关键帧总数)',
    detection_details           JSON                NULL                                                                COMMENT '详细检测结果JSON (bbox, confidence, class_name等)',
    error_message               TEXT                NULL                                                                COMMENT '失败时的错误原因',
    duration_ms                 BIGINT              NULL                                                                COMMENT '推理耗时(毫秒)，用于性能监控',

    -- 审计字段
    created_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP                                   COMMENT '创建时间',
    updated_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP       COMMENT '更新时间',
    created_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '创建者ID（0表示系统）',
    updated_by                  BIGINT UNSIGNED NOT NULL    DEFAULT 0                                                   COMMENT '修改者ID（0表示系统）',
    remark                      TEXT                NULL                                                                COMMENT '备注',

    -- 状态字段
    status                      TINYINT         NOT NULL    DEFAULT 1                                                   COMMENT '数据状态：0=禁用，1=正常',
    is_deleted                  TINYINT         NOT NULL    DEFAULT 0                                                   COMMENT '逻辑删除：0=未删除，1=已删除',
    opt_lock_version            INT             NOT NULL    DEFAULT 0                                                   COMMENT '乐观锁版本号',
    -- 唯一性保证
    unique_if_active            TINYINT GENERATED ALWAYS AS (IF(is_deleted = 0, 1, NULL)) STORED                        COMMENT '唯一性标记',

    UNIQUE KEY uk_code(code, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'YOLO目标检测历史记录表';

-- 历史库增量迁移：若已存在 yolo_detection_records，请补充 task_status 字段。
ALTER TABLE yolo_detection_records
    ADD COLUMN IF NOT EXISTS task_status TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0=待处理,1=处理中,2=成功,3=失败' AFTER task_type;

-- 将历史数据回填到 task_status：status=1 视为成功，其余视为失败。
UPDATE yolo_detection_records
SET task_status = CASE WHEN status = 1 THEN 2 ELSE 3 END
WHERE task_status = 0;



INSERT INTO users (username, nickname, email, phone_number, password) VALUE
('TimeLeafing', 'A Leafing', '1780884916@qq.com', '19870875936', '$2a$10$ZagrihNTFfX5lC4puPBv7.Da3HdoaMNSgq6.6DjIn4Re3XDUbIN72');

INSERT INTO yolo_nodes (node_name, host, port, http_protocol, api_version) VALUE
    ('uav-yolo1', '127.0.0.1', '8000', 'http', 'v1');

INSERT INTO yolo_node_params (node_id, template_name, params)
VALUES (1, 'sample template', '{"iou": 0.7, "conf": 0.21}');


-- ================= RBAC 初始化（幂等） =================

-- 1) 角色：系统管理员
INSERT INTO roles (code, name, description, level, sort_order)
SELECT 'admin', '系统管理员', '拥有系统所有 API 权限', 100, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'admin' AND is_deleted = 0
);

-- 2) API 权限（controller 级）
INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'user:profile:view', '查看个人资料', 1, 'GET /users/profile', 10
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'user:profile:view' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'user:profile:update', '更新个人资料', 1, 'PUT /users/profile', 11
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'user:profile:update' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'file:upload:common', '上传通用文件', 1, 'POST /files/upload', 20
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'file:upload:common' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'file:upload:avatar', '上传头像', 1, 'POST /files/upload/avatar', 21
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'file:upload:avatar' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:page', '分页查询模型节点', 1, 'GET /yolo-nodes/page', 30
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:page' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:detail', '查看模型节点详情', 1, 'GET /yolo-nodes/{id}', 31
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:detail' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:create', '创建模型节点', 1, 'POST /yolo-nodes', 32
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:create' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:update', '更新模型节点', 1, 'PUT /yolo-nodes/{id}', 33
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:update' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:delete', '删除模型节点', 1, 'DELETE /yolo-nodes/{id}', 34
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:delete' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:node:sync', '同步模型节点', 1, 'POST /yolo-nodes/{id}/sync', 35
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:node:sync' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:apply', '应用参数模板', 1, 'PUT /yolo-nodes/{id}/params/template/{templateName}/apply', 40
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:apply' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:list', '查看参数模板列表', 1, 'GET /yolo-nodes/{id}/params/templates', 41
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:list' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:detail', '查看参数模板详情', 1, 'GET /yolo-nodes/{id}/params/templates/{templateName}', 42
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:detail' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:create', '创建参数模板', 1, 'POST /yolo-nodes/{id}/params/templates', 43
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:create' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:update', '更新参数模板', 1, 'PUT /yolo-nodes/{id}/params/templates/{templateName}', 44
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:update' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:param:delete', '删除参数模板', 1, 'DELETE /yolo-nodes/{id}/params/templates/{templateName}', 45
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:param:delete' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:weight:list', '查看模型权重', 1, 'GET /yolo-nodes/{id}/weights', 50
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:weight:list' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:weight:upload', '上传模型权重', 1, 'POST /yolo-nodes/{id}/weights', 51
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:weight:upload' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:weight:switch', '切换模型权重', 1, 'PUT /yolo-nodes/{id}/weights/{filename}/active', 52
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:weight:switch' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'model:weight:delete', '删除模型权重', 1, 'DELETE /yolo-nodes/{id}/weights/{filename}', 53
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'model:weight:delete' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'inference:image:run', '执行图片推理', 1, 'POST /inference/nodes/{nodeId}/image', 60
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inference:image:run' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'inference:video:run', '执行视频推理', 1, 'POST /inference/nodes/{nodeId}/video', 61
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inference:video:run' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'inference:record:page', '分页查看推理记录', 1, 'GET /inference/records/page', 62
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inference:record:page' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'inference:record:detail', '查看推理记录详情', 1, 'GET /inference/records/{id}', 63
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inference:record:detail' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'inference:record:delete', '删除推理记录', 1, 'DELETE /inference/records/{id}', 64
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'inference:record:delete' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:role:create', '新增角色', 1, 'POST /admin/rbac/roles', 70
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:role:create' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:user:page', '分页查询RBAC用户', 1, 'GET /admin/rbac/users/page', 71
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:user:page' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:role:list', '查询RBAC角色列表', 1, 'GET /admin/rbac/roles', 72
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:role:list' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:permission:list', '查询RBAC权限列表', 1, 'GET /admin/rbac/permissions', 73
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:permission:list' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:user:role:update', '更新用户角色集合', 1, 'PUT /admin/rbac/users/{userId}/roles', 74
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:user:role:update' AND is_deleted = 0
);

INSERT INTO permissions (code, name, type, description, sort_order)
SELECT 'rbac:role:permission:update', '更新角色权限集合', 1, 'PUT /admin/rbac/roles/{roleId}/permissions', 75
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'rbac:role:permission:update' AND is_deleted = 0
);

-- 3) 角色绑定权限：admin 绑定全部以上 API 权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'user:profile:view', 'user:profile:update',
    'file:upload:common', 'file:upload:avatar',
    'model:node:page', 'model:node:detail', 'model:node:create', 'model:node:update', 'model:node:delete', 'model:node:sync',
    'model:param:apply', 'model:param:list', 'model:param:detail', 'model:param:create', 'model:param:update', 'model:param:delete',
    'model:weight:list', 'model:weight:upload', 'model:weight:switch', 'model:weight:delete',
    'inference:image:run', 'inference:video:run', 'inference:record:page', 'inference:record:detail', 'inference:record:delete',
    'rbac:role:create', 'rbac:user:page', 'rbac:role:list', 'rbac:permission:list',
    'rbac:user:role:update', 'rbac:role:permission:update'
)
WHERE r.code = 'admin'
  AND r.is_deleted = 0
  AND p.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
        AND rp.is_deleted = 0
  );

-- 4) 用户绑定角色：将初始化用户 TimeLeafing 绑定为 admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'admin'
WHERE u.username = 'TimeLeafing'
  AND u.is_deleted = 0
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.is_deleted = 0
  );


SELECT * FROM users;
SELECT * FROM user_roles;
SELECT * FROM roles;
SELECT * FROM role_permissions;
SELECT * FROM permissions;
SELECT * FROM yolo_nodes;
SELECT * FROM yolo_weights;
SELECT * FROM yolo_node_params;
SELECT * FROM yolo_detection_records;