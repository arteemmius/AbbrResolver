package com.university.abbrresolver.beans;

import com.university.abbrresolver.beans.Descriptor;
import java.util.ArrayList;
import java.util.List;

public class Sentence {

    private int indexInText;
    private int startPos;
    private int length;
    private String content;

    private List<Descriptor> descriptors = new ArrayList<>();

    public int getIndexInText() {
        return indexInText;
    }

    public void setIndexInText(int indexInText) {
        this.indexInText = indexInText;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Descriptor> getDescriptors() {
        return descriptors;
    }

    public void addDescriptor(Descriptor descriptor) {
        this.descriptors.add(descriptor);
    }

    @Override
    public String toString() {
        return content;
    }

    public void print() {
        System.out.println(content);
        StringBuilder sb = new StringBuilder();
        for (Descriptor descriptor : descriptors) {
            sb.append(" ").append(descriptor.toString());
        }
        System.out.println("Descriptors: " + sb.toString());
    }
}
