package com.example.exam;

// Класс для хранения данных о типе неисправности
public class FaultTypeStat {
    private String faultType;
    private int count;

    public FaultTypeStat(String faultType, int count) {
        this.faultType = faultType;
        this.count = count;
    }

    public String getFaultType() {
        return faultType;
    }

    public int getCount() {
        return count;
    }
}
