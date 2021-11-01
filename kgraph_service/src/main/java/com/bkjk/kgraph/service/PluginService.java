package com.bkjk.kgraph.service;

import net.sf.json.JSONObject;

import java.util.List;

public interface PluginService {

    PluginProperties props = new PluginProperties();

    List run(JSONObject param);

}