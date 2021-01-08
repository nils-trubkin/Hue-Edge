package com.nilstrubkin.hueedge.resources;

import com.squareup.moshi.FromJson;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeCatalogueAdapter {
    @FromJson
    BridgeCatalogue fromJson(BridgeCatalogue state) {
        Map<String, LightResource> lights = state.getLights();
        Map<String, GroupResource> groups = state.getGroups();
        Map<String, SceneResource> scenes = state.getScenes();

        Map<String, LightResource> filteredLights = new ConcurrentHashMap<>();
        Map<String, GroupResource> filteredGroups = new ConcurrentHashMap<>();
        Map<String, SceneResource> filteredScenes = new ConcurrentHashMap<>();

        // Add all the lights
        for(String key : lights.keySet()){
            LightResource light = lights.get(key);
            try {
                Objects.requireNonNull(light).setId(key);
                filteredLights.put(key, light);
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        // Add Rooms, Zones and 0 group
        for(String key : groups.keySet()){
            GroupResource group = groups.get(key);
            try {
                String type = Objects.requireNonNull(group).getType();
                if (type.equals("Room") || type.equals("Zone") || key.equals("0")) {
                    group.setId(key);
                    filteredGroups.put(key, group);
                }
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        // Add all GroupScenes
        for(String key : scenes.keySet()){
            SceneResource scene = scenes.get(key);
            try {
                String type = Objects.requireNonNull(scene).getType();
                if (type.equals("GroupScene")) {
                    String groupId = scene.getGroup();
                    String groupName;
                    GroupResource group = Objects.requireNonNull(filteredGroups.get(groupId));
                    groupName = group.getName();
                    scene.setId(key);
                    scene.setGroupName(groupName);
                    filteredScenes.put(key, scene);
                }
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        state.setLights(filteredLights);
        state.setScenes(filteredScenes);
        state.setGroups(filteredGroups);
        return state;
    }
}
