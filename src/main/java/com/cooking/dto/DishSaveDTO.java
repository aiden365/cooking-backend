package com.cooking.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class DishSaveDTO {

    private Long id;

    private String name;

    private String takeTimes;

    private Integer checkStatus;

    private String tips;

    private String imgPath;

    private List<FlavorItem> flavors;

    private List<MaterialItem> materials;

    private List<StepItem> steps;

    @Data
    public static class FlavorItem {
        private Long id;
        private String flavorName;
        private String dosage;
    }

    @Data
    public static class MaterialItem {
        private Long id;
        private String materialName;
        private String dosage;
        private String deal;
    }

    @Data
    public static class StepItem {
        private Long id;
        private Integer sort;
        private String stepDescribe;
        private String stepImage;
    }
}
