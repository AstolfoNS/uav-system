DROP DATABASE IF EXISTS uav_system;
CREATE DATABASE IF NOT EXISTS uav_system;

USE uav_system;

SHOW TABLES;



CREATE TABLE IF NOT EXISTS users
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    username                    VARCHAR(64)     NOT NULL                                                                COMMENT '用户姓名',
    nickname                    VARCHAR(64)     NOT NULL                                                                COMMENT '用户昵称',
    avatar_url                  VARCHAR(512)        NULL                                                                COMMENT '用户头像',
    email                       VARCHAR(32)     NOT NULL                                                                COMMENT '用户邮箱',
    phone_number                VARCHAR(32)         NULL                                                                COMMENT '用户手机号',
    password                    VARCHAR(32)         NULL                                                                COMMENT '用户密码',
    gender                      TINYINT             NULL                                                                COMMENT '用户性别：0=未知，1=男，2=女',
    introduction                TEXT                NULL                                                                COMMENT '用户简介',
    last_active_time            DATETIME            NULL                                                                COMMENT '最后在线时间',
    last_login_time             DATETIME            NULL                                                                COMMENT '最后登录时间',

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

    UNIQUE KEY uk_username(username),
    UNIQUE KEY uk_email(email),
    UNIQUE KEY uk_phone_number(phone_number)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户表';



CREATE TABLE IF NOT EXISTS roles
(
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY                                     COMMENT '主键ID',

    code                        VARCHAR(32)     NOT NULL                                                                COMMENT '角色编码',
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

    code                        VARCHAR(32)     NOT NULL                                                                COMMENT '权限编码',
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