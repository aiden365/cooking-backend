package com.cooking.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public class PageInfo<T> extends Page<T> {

    public long getPageNo() {
        return super.getCurrent();
    }

    public void setPageNo(long pageNo) {
        super.setCurrent(pageNo);
    }

    // 建议同时隐藏掉原有的 current 属性，防止 JSON 中出现重复字段
    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public long getCurrent() {
        return super.getCurrent();
    }
}