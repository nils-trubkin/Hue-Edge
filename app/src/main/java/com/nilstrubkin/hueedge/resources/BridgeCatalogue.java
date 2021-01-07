package com.nilstrubkin.hueedge.resources;

import com.nilstrubkin.hueedge.ResourceReference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeCatalogue implements Serializable {
    private static final long serialVersionUID = -7313934162135591982L;

    private Map<String, LightResource> lights = new ConcurrentHashMap<>();
    private Map<String, GroupResource> groups = new ConcurrentHashMap<>();
    private Map<String, SceneResource> scenes = new ConcurrentHashMap<>();

    public Map<String, LightResource> getLights() {
        return lights;
    }

    public Map<String, GroupResource> getGroups() {
        return groups;
    }

    public Map<String, SceneResource> getScenes() {
        return scenes;
    }

    public void setLights(Map<String, LightResource> lights) {
        this.lights = lights;
    }

    public void setGroups(Map<String, GroupResource> groups) {
        this.groups = groups;
    }

    public void setScenes(Map<String, SceneResource> scenes) {
        this.scenes = scenes;
    }

    public Map<String, GroupResource> getRooms() {
        return getGroupsByType("Room");
    }

    public Map<String, GroupResource> getZones() {
        return getGroupsByType("Zone");
    }

    private Map<String, GroupResource> getGroupsByType(String type) {
        Map<String, GroupResource> groups = new HashMap<>();
        // Iterate over all groups
        for (String key : getGroups().keySet()) {
            try {
                GroupResource group = Objects.requireNonNull(getGroups().get(key));
                // If group of the requested type or it's the "0" group, add it
                if (group.getType().equals(type) || key.equals("0"))
                    groups.put(key, group);
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        return groups;
    }

    public static ResourceReference getGroup0Ref() {
        return new ResourceReference("groups", "0");
    }
}
