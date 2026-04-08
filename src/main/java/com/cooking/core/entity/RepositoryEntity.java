package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 知识库表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_repository")
public class RepositoryEntity extends BaseEntity {

    /**
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 文件地址
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 知识类型，1：菜谱知识，2：营养知识
     */
    @TableField("type")
    private Integer type;
}
