package com.tf.backend.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tf.backend.core.model.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
