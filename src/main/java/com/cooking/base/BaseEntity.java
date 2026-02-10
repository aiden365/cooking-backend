package com.cooking.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
import java.util.Date;


@Data
@FieldNameConstants
public class BaseEntity<T> implements Serializable {


    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    protected Date createTime;

    /**
     * 创建人id
     */
    @TableField(value = "create_user")
    protected Long createUser;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    protected Date updateTime;

    /**
     * 更新人id
     */
    @TableField(value = "update_user")
    protected Long updateUser;

    /**
     * 是否删除
     */
    @TableLogic
    @TableField(value = "deleted")
    protected Integer deleted;


}
