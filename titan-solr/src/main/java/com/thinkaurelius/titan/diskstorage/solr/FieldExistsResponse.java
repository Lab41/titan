package com.thinkaurelius.titan.diskstorage.solr;

import com.thinkaurelius.titan.diskstorage.EntryMetaData;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.util.NamedList;

import java.util.ArrayList;
import java.util.List;

public class FieldExistsResponse extends SolrResponseBase {

    private List<SolrField> fields = new ArrayList<SolrField>();

    @Override
    public void setResponse(NamedList<Object> res) {
        super.setResponse(res);

        List fields = (List)res.get("fields");
        extractFields(fields);
    }

    private void extractFields(List fields) {

        for (Object value : fields) {
            NamedList field = (NamedList) value;
            String fieldName = (String) field.get("name");
            String fieldType = (String) field.get("type");
            this.fields.add(new SolrField(fieldName, fieldType));
        }
    }

    public List<SolrField> getFields() {
        return fields;
    }
}
