/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.textanalysis.abbrresolver.classifiermodel;

import java.util.Comparator;

/**
 *
 * @author artee
 */
public class PredictionData {
    private String topic;
    private double value;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
    
    public static final Comparator<PredictionData> COMPARE_BY_VALUE = new Comparator<PredictionData>() {
        @Override
        public int compare(PredictionData lhs, PredictionData rhs) {
            return Double.compare(lhs.getValue(), rhs.getValue());
        }
    };    
    
}
