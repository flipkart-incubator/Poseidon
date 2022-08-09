package com.flipkart.poseidon.serviceclients;

import java.util.ArrayList;
import java.util.List;

public class FanoutContext {
    private int count;
    private List<Long> starTime;
    private List<Long> endTime;
    private int totalCount;
    private long totalTime;

    public FanoutContext() {
        this.count = 0;
        this.totalCount = 0;
        this.totalTime = 0L;
        this.starTime = new ArrayList<>();
        this.endTime = new ArrayList<>();
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Long> getStarTime() {
        return this.starTime;
    }

    public void setStarTime(List<Long> time) {
        this.starTime = starTime;
    }

    public List<Long> getEndTime() {
        return endTime;
    }

    public void setEndTime(List<Long> endTime) {
        this.endTime = endTime;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
}
