package com.cooking.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cooking.base.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;


/**
 * <p>
 * 用户分享评论表
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Data
@FieldNameConstants
@Accessors(chain = true)
@TableName("tbl_user_share_comment")
public class UserShareCommentEntity extends BaseEntity {

    /**
     * 用户分享ID
     */
    @TableField("user_share_id")
    private Long userShareId;

    /**
     * 评论用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 上级评论ID，默认0表示最顶级评论
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 评论内容
     */
    @TableField("content")
    private String content;

    /**
     * 用户名
     */
    @TableField(exist = false)
    private String userName;
}
