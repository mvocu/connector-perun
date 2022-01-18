package cz.metacentrum.perun.polygon.connector;

import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsIgnoreCaseFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.ExtSource;

public class ExtSourceSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(ExtSourceSearch.class);

	public ExtSourceSearch(ObjectClass objectClass, SchemaAdapter schemaAdapter, PerunRPC perun) {
		super(objectClass, schemaAdapter, perun);
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllExtSources(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				LOG.info("Reading ext source with uid {0}", uid);
				ExtSource es = perun.getExtSourcesManager().getExtSourceById(Integer.valueOf(uid));
				LOG.info("Query returned {0} ext source", es);
				
				if(es != null) {
					mapResult(es, handler);
				}
				SearchResult result = new SearchResult(
						 null, 	/* cookie */ 
						 -1,	/* remainingResults */
						 true	/* completeResultSet */
						 );
				((SearchResultsHandler)handler).handleResult(result);
				return;
			}
			
		} else if(filter instanceof EqualsIgnoreCaseFilter) {
			
		} else {
			LOG.warn("Filter of type {0} is not supported", filter.getClass().getName());
			throw new RuntimeException("Unsupported query");
		}

	}

	private void readAllExtSources(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		LOG.info("Reading all ext sources");
		List<ExtSource> esList = perun.getExtSourcesManager().getExtSources();
		LOG.info("Query returned {0} ext sources", esList.size());

		int remaining = -1;
		
		if(pageSize > 0) {
			int size = esList.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			esList = esList.subList(pageOffset, last);
			remaining = size - last;
		}
		
		for(ExtSource es : esList) {
			mapResult(es, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
		
	}

	private void mapResult(ExtSource es, ResultsHandler handler) {
		handler.handle(schemaAdapter.mapObject(objectClass, es).build());
	}

}
