package com.tf.backend.core.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tf.backend.core.model.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {

    List<RoleEntity> getByUserId(@Param("userId") Long userId);

}
