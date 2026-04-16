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

    UNIQUE KEY uk_node(node_id, unique_if_active),
    UNIQUE KEY uk_template_name(template_name, unique_if_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'YOLO节点推理参数表';



CREATE TABLE IF NOT EXISTS yolo_detection_records
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    node_id                     BIGINT UNSIGNED NOT NULL                                                                COMMENT '执行此次预测的节点ID',
    code                        VARCHAR(64)     NOT NULL                                                                COMMENT '执行编码',
    task_type                   TINYINT         NOT NULL                                                                COMMENT '任务类型：1=图像检测, 2=视频检测',
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



INSERT INTO users (username, nickname, email, phone_number, password) VALUE
('TimeLeafing', 'A Leafing', '1780884916@qq.com', '19870875936', '$2a$10$ZagrihNTFfX5lC4puPBv7.Da3HdoaMNSgq6.6DjIn4Re3XDUbIN72');

INSERT INTO yolo_nodes (node_name, host, port, http_protocol, api_version) VALUE
    ('uav-yolo1', '127.0.0.1', '8000', 'http', 'v1');

SELECT * FROM users;
SELECT * FROM yolo_nodes;
SELECT * FROM yolo_weights;
SELECT * FROM yolo_node_params;
