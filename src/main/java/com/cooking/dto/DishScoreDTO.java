package com.cooking.dto;

import lombok.Data;

@Data
public class DishScoreDTO {

    private Long dishId;

    private String dishName;

    private Double manipulationScoreAvg;

    private Double equalScoreAvg;

    private Double satisfactionScoreAvg;
}
