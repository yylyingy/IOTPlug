package com.silverlit.onenetedp.model;

import java.util.ArrayList;

/**
 * Created by Yangyl on 2016/10/20.
 */

public class CtrlBean {
    private ArrayList<Datastreams> datastreams;
    public CtrlBean(Datastreams datastream){
        datastreams = new ArrayList<>();
        datastreams.add(datastream);
    }

    public static class Datastreams{
        private String id ;
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ArrayList<Datapoints> getDatapoints() {
            return datapoints;
        }

        public void setDatapoints(ArrayList<Datapoints> datapoints) {
            this.datapoints = datapoints;
        }

        private ArrayList<Datapoints> datapoints;
        public Datastreams(Datapoints datapoint){
            datapoints = new ArrayList<>();
            datapoints.add(datapoint);
        }

        public static class Datapoints<T>{
            public T getValue() {
                return value;
            }

            public void setValue(T value) {
                this.value = value;
            }

            private T value;

        }
    }

}
