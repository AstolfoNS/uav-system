package com.tf.backend.core.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        // 自动填充创建时间和更新时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);

        // 自动填充创建者和更新者 (0L 表示系统操作)
        this.strictInsertFill(metaObject, "createdBy", Long.class, 0L);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, 0L);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 自动更新修改时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 自动更新修改者
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, 0L);
    }
}
