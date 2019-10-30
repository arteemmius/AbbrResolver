package com.university.abbrresolver.beans;

public class Descriptor {

    private int type;
    private int startPos;
    private int length;
    private String value;
    private boolean mayStayInEnd;
    private String desc;

    public Descriptor(int type, int startPos, int length, String value) {
        this.type = type;
        this.startPos = startPos;
        this.length = length;
        this.value = value;
        this.mayStayInEnd = false;
    }

    public Integer getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

    public Integer getStartPos() {
        return startPos;
    }

    public Integer getLength() {
        return length;
    }

    public String getValue() {
        return value;
    }

    public boolean isMayStayInEnd() {
        return mayStayInEnd;
    }

    public void setMayStayInEnd(boolean mayStayInEnd) {
        this.mayStayInEnd = mayStayInEnd;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return value;
    }
}
