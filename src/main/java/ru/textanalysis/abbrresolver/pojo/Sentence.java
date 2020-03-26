package ru.textanalysis.abbrresolver.pojo;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sentence {

    private int indexInText;
    private int startPos;
    private int length;
    private String content;
    
    private static final Logger log = LoggerFactory.getLogger(Sentence.class.getName());  
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
        log.info(content);
        StringBuilder sb = new StringBuilder();
        for (Descriptor descriptor : descriptors) {
            sb.append(" ").append(descriptor.toString());
        }
        log.info("Descriptors: " + sb.toString());
    }
}
