package com.nilstrubkin.hueedge;

import java.io.Serializable;

public class ResourceReference implements Serializable {
    public final String category;
    public final String id;

    public ResourceReference(String category, String id) {
        this.category = category;
        this.id = id;
    }
}
