package com.thinkaurelius.titan.diskstorage.solr;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.thinkaurelius.titan.core.attribute.Geo;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.thinkaurelius.titan.diskstorage.BackendException;
import com.thinkaurelius.titan.diskstorage.configuration.Configuration;
import com.thinkaurelius.titan.diskstorage.configuration.ModifiableConfiguration;
import com.thinkaurelius.titan.diskstorage.indexing.IndexProvider;
import com.thinkaurelius.titan.diskstorage.indexing.IndexProviderTest;
import com.thinkaurelius.titan.diskstorage.indexing.IndexQuery;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.thinkaurelius.titan.graphdb.query.condition.PredicateCondition;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.ext.servlet.ServerServlet;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Jared Holmberg (jholmberg@bericotechnologies.com)
 */
public class SolrIndexTest extends IndexProviderTest {

    protected static final int NUM_SERVERS = 1;
    protected static final String CONFIG_DIR = "conf1";

    private static MiniSolrCloudCluster miniSolrCloudCluster;

    @BeforeClass
    public static void setUpMiniCluster() throws Exception {
        String solrHome = Joiner.on(File.separator).join(System.getProperty("user.dir"), "titan-solr", "target", "test-classes", "solr");
        File solrXml = new File(solrHome, "solr.xml");

        // Enable the schema api.
        ServletHolder servletHolder = new ServletHolder("SolrRestApi", ServerServlet.class);
        servletHolder.setInitParameter("org.restlet.application", "org.apache.solr.rest.SolrSchemaRestApi");

        SortedMap<ServletHolder, String> extraServlets = new TreeMap<ServletHolder, String>();
        extraServlets.put(servletHolder, "/schema/*");

        miniSolrCloudCluster = new MiniSolrCloudCluster(NUM_SERVERS, null, solrXml, extraServlets, null);
        uploadConfigDirToZk(Joiner.on(File.separator).join(solrHome, "collection1", "conf"));
    }

    @AfterClass
    public static void tearDownMiniCluster() throws Exception {
        miniSolrCloudCluster.shutdown();
    }

    @Override
    public IndexProvider openIndex() throws BackendException {
        return new SolrIndex(getLocalSolrTestConfig());
    }

    @Override
    public boolean supportsLuceneStyleQueries() {
        return true;
    }

    private Configuration getLocalSolrTestConfig() {
        final String index = "solr";
        ModifiableConfiguration config = GraphDatabaseConfiguration.buildConfiguration();

        config.set(SolrIndex.ZOOKEEPER_URL, miniSolrCloudCluster.getZkServer().getZkAddress(), index);
        config.set(SolrIndex.KEY_FIELD_NAMES, new String[]{
                "collection1=id",
                "edge=id",
                "restorable1=id",
                "restorable2=id",
                "store1=id",
                "vertex=id"
        }, index);
        config.set(SolrIndex.CONFIG_NAME, CONFIG_DIR, index);
        config.set(SolrIndex.NUM_SHARDS, 1, index);
        config.set(SolrIndex.MAX_SHARDS_PER_NODE, 1, index);
        config.set(SolrIndex.REPLICATION_FACTOR, 1, index);

        return config.restrictTo(index);
    }

    @Test
    public void storeWithBoundingBoxGeospatialSearch() throws BackendException
    {
        String[] stores = new String[] { "vertex" };

        Map<String,Object> doc1 = getDocument("Hello world",1001,5.2, Geoshape.point(48.0, 0.0));
        Map<String,Object> doc2 = getDocument("Tomorrow is the world",1010,8.5,Geoshape.point(49.0,1.0));
        Map<String,Object> doc3 = getDocument("Hello Bob, are you there?", -500, 10.1, Geoshape.point(47.0, 10.0));

        for (String store : stores) {
            initialize(store);

            add(store,"doc1",doc1,true);
            add(store,"doc2",doc2,true);
            add(store,"doc3",doc3,false);
        }

        clopen();

        for (String store : stores) {

            List<String> result = tx.query(new IndexQuery(store, PredicateCondition.of("location", Geo.WITHIN, Geoshape.box(46.5, -0.5, 50.5, 10.5))));
            assertEquals(3,result.size());
            assertEquals(ImmutableSet.of("doc1", "doc2", "doc3"), ImmutableSet.copyOf(result));
        }
    }

    private static ZkController getZkController() {
        SolrDispatchFilter dispatchFilter =
                (SolrDispatchFilter) miniSolrCloudCluster.getJettySolrRunners().get(0).getDispatchFilter().getFilter();
        return dispatchFilter.getCores().getZkController();
    }

    protected static void uploadConfigDirToZk(String collectionConfigDir) throws Exception {
        ZkController zkController = getZkController();
        zkController.uploadConfigDir(new File(collectionConfigDir), CONFIG_DIR);
    }
}
