package com.thinkaurelius.titan.diskstorage.solr;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class SolrField {

    private String fieldName;
    private String fieldType;

    public SolrField(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("name",fieldName);
        object.put("type",fieldType);

        return object;
    }
}
