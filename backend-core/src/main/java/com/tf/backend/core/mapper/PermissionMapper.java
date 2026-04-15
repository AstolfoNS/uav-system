package com.tf.backend.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tf.backend.core.model.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {

    List<PermissionEntity> getByUserId(@Param("userId") Long userId);

}
