package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 用户表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@TableName("tbl_user")
@Accessors(chain = true)
public class UserEntity extends BaseEntity {

    /**
     * 用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户账户
     */
    @TableField("user_code")
    private String userCode;

    /**
     * 用户密码
     */
    @TableField("user_pass")
    private String userPass;

    /**
     * 用户类型：1=普通用户，2=管理员用户
     */
    @TableField("type")
    private Integer type;

    /**
     * 用户年龄
     */
    @TableField("age")
    private Integer age;

    /**
     * 用户性别
     */
    @TableField("gender")
    private Integer gender;

    /**
     * 用户身高
     */
    @TableField("stature")
    private Integer stature;

    /**
     * 用户体重，单位KG
     */
    @TableField("weight")
    private Integer weight;



}
