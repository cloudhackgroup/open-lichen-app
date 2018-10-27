package org.cloudhack.openlichen.models;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class LichenReport {

    private String reportId;
    private double lat;
    private double lng;
    private int datetime;
    private Map<String, SpecimenData> samples;

    public String toJson(){
        return new Gson()
                .toJson(this);
    }
}
