package com.cooking.base;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuthInfo {
    private String userId;
}
