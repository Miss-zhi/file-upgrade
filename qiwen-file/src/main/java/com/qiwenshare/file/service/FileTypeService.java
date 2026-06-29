package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiwenshare.file.domain.file.*;
import com.qiwenshare.file.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileTypeService {

    private final FileTypeMapper typeMapper;
    private final FileClassificationMapper classMapper;
    private final FileBeanMapper fileBeanMapper;

    private static final Map<Integer, String[]> DEFAULT_CLASSIFICATION = Map.of(
        1, new String[]{"jpg","jpeg","png","gif","bmp","webp","svg","ico"},
        2, new String[]{"pdf","doc","docx","xls","xlsx","ppt","pptx","txt","md"},
        3, new String[]{"mp4","avi","mkv","mov","wmv","flv","webm"},
        4, new String[]{"mp3","wav","flac","aac","ogg","wma"},
        5, new String[]{""}
    );

    @PostConstruct
    @Transactional
    public void initDefaults() {
        if (typeMapper.selectCount(null) == 0) {
            String[] names = {"图片", "文档", "视频", "音乐", "其他"};
            for (int i = 0; i < names.length; i++) {
                FileType ft = new FileType();
                ft.setName(names[i]);
                ft.setOrderNum(i + 1);
                typeMapper.insert(ft);
                // insert classification
                int typeId = ft.getId();
                for (String ext : DEFAULT_CLASSIFICATION.getOrDefault(typeId, new String[]{""})) {
                    FileClassification fc = new FileClassification();
                    fc.setFileTypeId(typeId);
                    fc.setFileExtendName(ext.isEmpty() ? "" : ext);
                    classMapper.insert(fc);
                }
            }
        }
    }

    public List<FileType> listTypes() {
        return typeMapper.selectList(new LambdaQueryWrapper<FileType>().orderByAsc(FileType::getOrderNum));
    }

    public IPage<FileBean> listByType(Integer typeId, int pageNo, int pageSize) {
        List<FileClassification> list = classMapper.selectList(
            new LambdaQueryWrapper<FileClassification>().eq(FileClassification::getFileTypeId, typeId));
        List<String> exts = list.stream().map(FileClassification::getFileExtendName).filter(e -> !e.isEmpty()).collect(Collectors.toList());

        Page<FileBean> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<FileBean> w = new LambdaQueryWrapper<>();
        w.eq(FileBean::getIsFolder, false).eq(FileBean::getDeleted, 0);
        if (!exts.isEmpty()) {
            w.and(q -> exts.forEach(ext ->
                q.or().like(FileBean::getFilePath, "%." + ext)
            ));
        }
        w.orderByDesc(FileBean::getCreateTime);
        return fileBeanMapper.selectPage(page, w);
    }
}
