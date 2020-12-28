package com.nilstrubkin.hueedge;

import java.io.Serializable;

public class ResourceReference implements Serializable {
    public final String category;
    public final String id;
    public int iconRes;

    public ResourceReference(String category, String id) {
        this.category = category;
        this.id = id;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }
}
