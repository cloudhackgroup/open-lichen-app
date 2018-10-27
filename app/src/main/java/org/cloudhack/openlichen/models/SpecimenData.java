package org.cloudhack.openlichen.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
@AllArgsConstructor
public class SpecimenData {
    private int milimetersCovered;
    private int tilesCovered;
}
