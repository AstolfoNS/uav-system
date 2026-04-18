package com.tf.backend.core.application.domain.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tf.backend.core.model.vo.AdminRbacPermissionVO;
import com.tf.backend.core.model.vo.AdminRbacRoleVO;
import com.tf.backend.core.model.vo.AdminRbacUserVO;

import java.util.List;

public interface AdminRbacQueryService {

    IPage<AdminRbacUserVO> getUserPage(Integer current, Integer size, String keyword);

    List<AdminRbacRoleVO> getRoles();

    List<AdminRbacPermissionVO> getPermissions();
}
