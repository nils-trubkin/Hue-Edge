package com.nilstrubkin.hueedge;

import android.content.Context;

import com.nilstrubkin.hueedge.resources.BridgeResource;

import java.io.Serializable;

public class ResourceReference implements Serializable {
    private static final long serialVersionUID = 6654859715107436411L;
    private final String category;
    private final String id;
    private int iconRes;
    private int iconColor;

    public ResourceReference(String category, String id) {
        this.category = category;
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public int getIconColor() {
        return iconColor;
    }

    public void setIconColor(int iconColor) {
        this.iconColor = iconColor;
    }

    public int compareTo(Context ctx, ResourceReference ref) {
        int categoryDiff = category.compareTo(ref.category);
        if (categoryDiff != 0)
            return categoryDiff;
        else {
            BridgeResource thisBr = HueBridge.getInstance(ctx).getResource(this);
            BridgeResource thatBr = HueBridge.getInstance(ctx).getResource(ref);
            String thisName = thisBr.getName();
            String thatName = thatBr.getName();
            int nameDiff = thisName.compareTo(thatName);
            if (nameDiff != 0)
                return nameDiff;
            else
                return thisBr.getUnderBtnText().compareTo(thatBr.getUnderBtnText());
        }
    }
}
