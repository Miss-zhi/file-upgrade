package com.qiwenshare.file.service;

import com.qiwenshare.file.domain.user.Permission;
import com.qiwenshare.file.mapper.PermissionMapper;
import com.qiwenshare.file.vo.user.PermissionVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper mapper;

    public List<Permission> listAll() {
        return mapper.selectList(null);
    }

    public List<PermissionVO> getTree() {
        List<Permission> all = listAll();
        return buildTree(null, all);
    }

    private List<PermissionVO> buildTree(Long parentId, List<Permission> all) {
        return all.stream()
            .filter(p -> Objects.equals(p.getParentId(), parentId))
            .sorted(Comparator.comparing(Permission::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(p -> {
                PermissionVO vo = new PermissionVO();
                vo.setPermissionId(p.getPermissionId());
                vo.setParentId(p.getParentId());
                vo.setPermissionName(p.getPermissionName());
                vo.setResourceType(p.getResourceType());
                vo.setPermissionCode(p.getPermissionCode());
                vo.setOrderNum(p.getOrderNum());
                vo.setChildren(buildTree(p.getPermissionId(), all));
                return vo;
            }).toList();
    }

    @PostConstruct
    @Transactional
    public void initDefaults() {
        if (mapper.selectCount(null) > 0) return;

        // 父级权限
        Object[][] tree = {
            {null, "文件管理", 0, "file", 1},
            {null, "用户管理", 0, "user", 2},
            {null, "系统管理", 0, "system", 3},
            {null, "管理面板", 0, "dashboard", 4},
        };
        Map<String, Long> idMap = new HashMap<>();
        int id = 1;
        for (Object[] n : tree) {
            Permission p = new Permission();
            p.setPermissionId((long) id);
            p.setParentId((Long) n[0]);
            p.setPermissionName((String) n[1]);
            p.setResourceType((int) n[2]);
            p.setPermissionCode((String) n[3]);
            p.setOrderNum((int) n[4]);
            mapper.insert(p);
            idMap.put((String) n[3], (long) id);
            id++;
        }

        // 子权限：文件管理下
        String[][] sub = {
            {"文件上传", "file:upload"},
            {"文件下载", "file:download"},
            {"文件删除", "file:delete"},
            {"文件分享", "file:share"},
        };
        Long fileId = idMap.get("file");
        for (int i = 0; i < sub.length; i++) {
            Permission p = new Permission();
            p.setPermissionId((long) id);
            p.setParentId(fileId);
            p.setPermissionName(sub[i][0]);
            p.setResourceType(1);
            p.setPermissionCode(sub[i][1]);
            p.setOrderNum(i + 1);
            mapper.insert(p);
            id++;
        }

        // 子权限：用户管理下
        String[][] sub2 = {{"用户列表", "user:list"}, {"角色分配", "user:role"}};
        Long userId = idMap.get("user");
        for (int i = 0; i < sub2.length; i++) {
            Permission p = new Permission();
            p.setPermissionId((long) id);
            p.setParentId(userId);
            p.setPermissionName(sub2[i][0]);
            p.setResourceType(1);
            p.setPermissionCode(sub2[i][1]);
            p.setOrderNum(i + 1);
            mapper.insert(p);
            id++;
        }

        // 系统管理下
        Permission np = new Permission();
        np.setPermissionId((long) id);
        np.setParentId(idMap.get("system"));
        np.setPermissionName("公告管理");
        np.setResourceType(1);
        np.setPermissionCode("system:notice");
        np.setOrderNum(1);
        mapper.insert(np);
    }
}
