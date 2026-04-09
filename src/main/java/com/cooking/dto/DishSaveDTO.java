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
        @JSONField(name = "deal")
        private String deal;
    }

    @Data
    public static class StepItem {
        private Long id;
        @JSONField(name = "order")
        private Integer order;
        @JSONField(name = "stepDescribe", alternateNames = {"description"})
        private String stepDescribe;
        @JSONField(name = "stepImage")
        private String stepImage;
    }
}
