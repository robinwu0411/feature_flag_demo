package com.ffs.server.model.dto;

import com.ffs.server.model.entity.Rollout;

public class RolloutDto {
    private Integer percentage;
    private String serveValue;

    public static RolloutDto from(Rollout r) {
        RolloutDto d = new RolloutDto();
        d.percentage = r.getPercentage();
        d.serveValue = r.getServeValue();
        return d;
    }

    public Integer getPercentage() { return percentage; }
    public String getServeValue() { return serveValue; }
}
