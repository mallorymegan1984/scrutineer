package com.aconex.scrutineer.elasticsearch;

import com.aconex.scrutineer.IdAndVersion;
import com.fasterxml.sort.DataReaderFactory;
import com.fasterxml.sort.DataWriterFactory;
import com.fasterxml.sort.SortConfig;
import com.fasterxml.sort.Sorter;
import com.fasterxml.sort.util.NaturalComparator;
import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ElasticSearchIdAndVersionStreamIntegrationTest {

    private static final String INDEX_NAME = "engr";
    private Client client;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetStreamFromElasticSearch() {
        SortConfig sortConfig = new SortConfig().withMaxMemoryUsage(256*1024*1024);
        DataReaderFactory<IdAndVersion> dataReaderFactory = new IdAndVersionDataReaderFactory();
        DataWriterFactory<IdAndVersion> dataWriterFactory = new IdAndVersionDataWriterFactory();
        Sorter sorter = new Sorter(sortConfig, dataReaderFactory, dataWriterFactory, new NaturalComparator<IdAndVersion>());
        ElasticSearchIdAndVersionStream elasticSearchIdAndVersionStream = new ElasticSearchIdAndVersionStream(new ElasticSearchDownloader(client, INDEX_NAME), new ElasticSearchSorter(sorter), new IteratorFactory(), SystemUtils.getJavaIoTmpDir().getAbsolutePath());

        elasticSearchIdAndVersionStream.open();
        Iterator<IdAndVersion> iterator = elasticSearchIdAndVersionStream.iterator();

        assertIdAndVerison(iterator.next(), 1,1);
        assertIdAndVerison(iterator.next(), 2,2);
        assertIdAndVerison(iterator.next(), 3,3);
    }

    public void assertIdAndVerison(IdAndVersion idAndVersion, long expectedId, long expectedVersion) {
        assertThat(idAndVersion.getId(), is(expectedId));
        assertThat(idAndVersion.getVersion(), is(expectedVersion));
    }



    @Before
    public void setup() {
        Node node = nodeBuilder().local(true).node();
        client = node.client();
        deleteIndexIfExists();

        indexIdAndVersion("1", 1);
        indexIdAndVersion("3", 3);
        indexIdAndVersion("2", 2);

        client.admin().indices().prepareFlush(INDEX_NAME).execute().actionGet();
    }

    @After
    public void teardown() {
        client.close();
    }

    private void deleteIndexIfExists() {
        if (client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet().exists()) {
            client.admin().indices().prepareDelete(INDEX_NAME).execute().actionGet();
        }
    }

    private void indexIdAndVersion(String id, long version) {
        client.prepareIndex(INDEX_NAME,"idandversion").setId(id).setOperationThreaded(false).setVersion(version).setVersionType(VersionType.EXTERNAL).setSource("{value:1}").execute().actionGet();
    }
}
