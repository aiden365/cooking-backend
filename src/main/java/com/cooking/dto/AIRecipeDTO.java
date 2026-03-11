package com.cooking.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
public class AIRecipeDTO {

    @JsonPropertyDescription("状态标识：成功返回 'success'，若食材非法或逻辑错误则返回 'error'")
    private String status;
    @JsonPropertyDescription("菜品名称，需与用户输入的 course 一致，如西红柿炒鸡蛋")
    private String dishName;
    @JSONField(name = "take_times")
    @JsonPropertyDescription("烹饪时间，如30分钟")
    private String takeTimes;
    @JsonPropertyDescription("食材列表，包含食材名称、用量、以及预处理说明")
    private List<Materials> materials;
    @JsonPropertyDescription("调料列表，包含调料名称、用量")
    private List<Flavors> flavors;
    @JsonPropertyDescription("烹饪步骤，包含步骤编号、步骤说明")
    private List<Steps> steps;
    @JsonPropertyDescription("给出菜谱制作相关的友好的建议")
    private String tips;

    @Data
    public static class Materials {
        @JsonPropertyDescription("食材名称，如：鸡蛋")
        private String name;
        @JsonPropertyDescription("食材用量，如：2-3个")
        private String dosage;
        @JsonPropertyDescription("预处理说明，如：打入碗中，加少许盐搅拌均匀")
        private String deal;
    }

    @Data
    public static class Flavors {
        @JsonPropertyDescription("调料名称，如：食用油")
        private String name;
        @JsonPropertyDescription("调料用量，如：适量")
        private String dosage;
    }

    @Data
    public static class Steps {
        @JSONField(name = "step_number")
        @JsonPropertyDescription("制作步骤顺序，如：1")
        private Integer stepNumber;
        @JsonPropertyDescription("步骤说明，如：热锅凉油，倒入打好的鸡蛋液，用中火炒至凝固后盛出备用。")
        private String instruction;
    }
}
