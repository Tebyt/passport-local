package com.rank.rss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.xml.ws.Response;

import org.apache.log4j.BasicConfigurator;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilders.*;

import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.Document;
import static org.elasticsearch.node.NodeBuilder.*;

public class ElasticSearch {

	private Client client;

	// A bulk request holds an ordered IndexRequests and DeleteRequests and
	// allows to executes it in a single batch
	private BulkRequestBuilder bulkRequest;

	public ElasticSearch() {
		Node node = nodeBuilder()
				.settings(Settings.settingsBuilder().put("http.enabled", false).put("path.home", "/elasticsearch"))
				.client(true).node();
		client = node.client();
		bulkRequest = client.prepareBulk();
	}

	public Client getClient() {
		return this.client;
	}

	public void bulkIndexDocument(String document, String index, String type, String id) {
		// adds an index request to the list of actions to execute.
		bulkRequest.add(client.prepareIndex(index, type, id).setSource(document));
	}

	public boolean outputJson(String content) {
		try {
			File file = new File("C:\\Users\\ACA4\\Documents\\Debug.txt");

			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content + "\r\n");
			bw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void excuteBulkIndex() {
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();

		///
		if (bulkResponse.hasFailures()) {
			// process failures by iterating through each bulk response item
			System.out.println("Iterate through each bulk item to find the error");
			outputJson(bulkResponse.buildFailureMessage());
		}
	}

	public void deleteIndex(String index) {
		try {
			System.out.printf("deleting search index {%s}", index);
			client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
		} catch (Exception ex) {
			System.out.printf("Cannot delete index {%s} because it doesn't exist.", index);
		}
	}

	public String searchByDate(int dateRange, String index) {
		Calendar currentDate = Calendar.getInstance(); // Get the current date
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd"); // format
																		// it as
																		// per
																		// your
																		// requirement
		String dateNow = formatter.format(currentDate.getTime());
		currentDate.add(Calendar.DATE, -1 * dateRange);
		String fromNow = formatter.format(currentDate.getTime());

		SearchResponse response = client.prepareSearch(index)
				.setQuery(QueryBuilders.rangeQuery("_type").to(dateNow).from(fromNow)).execute().actionGet();

		return response.toString();
	}

	public String searchByKeywordList(ArrayList<String> keySet, String index) {
		MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(keySet, "keywords");
		SearchResponse response = client.prepareSearch(index).setQuery(queryBuilder).execute().actionGet();
		return response.toString();
	}

	public void showAllData() {
		SearchResponse response = client.prepareSearch().execute().actionGet();
		System.out.println(response.toString());
	}

	public void refresh() {
		client.admin().indices().prepareRefresh().execute().actionGet();
	}

	public static void main(String[] args) {
		// BasicConfigurator.configure();
		ElasticSearch es = new ElasticSearch();
		// es.indexDocument("");
		// es.indexDocument("");
		// es.getResponse();
		// es.SearchResponse2();

	}

}
