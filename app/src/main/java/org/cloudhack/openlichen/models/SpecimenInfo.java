package org.cloudhack.openlichen.models;

import android.view.View;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class SpecimenInfo {
    private String name;
    private List<View> affectdViews;
}
