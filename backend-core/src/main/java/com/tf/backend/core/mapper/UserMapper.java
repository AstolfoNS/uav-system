package com.tf.backend.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tf.backend.core.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
