package org.cloudhack.openlichen.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.TextView;

import org.cloudhack.openlichen.R;
import org.cloudhack.openlichen.models.SpecimenInfo;

import java.security.SecurityPermission;
import java.util.List;

public class SpecimenInfoSpinnerAdapter extends ArrayAdapter<SpecimenInfo>{
    private List<SpecimenInfo> specimenInfo;

    public SpecimenInfoSpinnerAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public void setDataSet(List<SpecimenInfo> specimenInfo) {
        this.specimenInfo = specimenInfo;
    }

    @Nullable
    @Override
    public SpecimenInfo getItem(int position) {
        return (specimenInfo != null) ? specimenInfo.get(position) : null;
    }

    @Override
    public int getCount() {
        return (specimenInfo != null) ? specimenInfo.size() : 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position,convertView,parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position,convertView,parent);
    }


    private View createView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.species_item, parent, false);
        }
        SpecimenInfo specimen = specimenInfo.get(position);
        TextView nameText = convertView.findViewById(R.id.specimen_name);
        TextView countText = convertView.findViewById(R.id.specimen_count);
        nameText.setText(specimen.getName());
        int count = specimen.getAffectdViews().size();
        countText.setText(String.valueOf(count));
        countText.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        return convertView;
    }

}
