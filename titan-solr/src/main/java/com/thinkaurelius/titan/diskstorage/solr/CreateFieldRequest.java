package com.thinkaurelius.titan.diskstorage.solr;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CreateFieldRequest extends SolrRequest {

    private static final String CREATE_FIELDS = "/schema/fields";

    private ModifiableSolrParams params;
    private List<SolrField> fields;

    public CreateFieldRequest() {
        super(METHOD.POST, CREATE_FIELDS);
        this.params = new ModifiableSolrParams();
    }

    @Override
    public SolrParams getParams() {
        return params;
    }

    public CreateFieldRequest add(final Collection<SolrField> fields) {
        if (this.fields == null) {
            this.fields = new ArrayList<SolrField>();
        }

        this.fields.addAll(fields);

        return this;
    }

    @Override
    public Collection<ContentStream> getContentStreams() throws IOException {
        try {
            return ClientUtils.toContentStreams(getJSON().toString(), "application/json");
        } catch (JSONException e) {
            throw new IOException(e.toString());
        }
    }

    public JSONArray getJSON() throws JSONException {
        JSONArray array = new JSONArray();

        for (SolrField field : fields) {
            array.put(field.getJSON());
        }

        return array;
    }

    @Override
    public SolrResponse process(SolrServer server) throws SolrServerException, IOException {
        long startTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        CreateFieldResponse res = new CreateFieldResponse();
        res.setResponse(server.request(this));
        long endTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        res.setElapsedTime(endTime - startTime);
        return res;
    }
}
