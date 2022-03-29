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
import org.springframework.web.client.HttpClientErrorException;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Facility;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;

public class FacilitySearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(FacilitySearch.class);

	public FacilitySearch(ObjectClass objectClass, SchemaAdapter adapter, PerunRPC perun) {
		super(objectClass, adapter, perun);
	}

	@Override
	public PerunBean readPerunBeanById(Integer id, Integer... ids) {
		LOG.info("Reading facility with uid {0}", id);
		Facility facility = null;
		try {
			facility = perun.getFacilitiesManager().getFacilityById(Integer.valueOf(id));
		} catch (HttpClientErrorException exception) {
			LOG.info("Query returned no facility");
			return null;
		}
		LOG.info("Query returned {0} facility", facility.toString());
		
		return facility;
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllFacilities(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				Facility facility = (Facility)readPerunBeanById(Integer.valueOf(uid));
				if(facility != null) {
					mapResult(facility, handler);
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

	protected void readAllFacilities(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		List<Facility> facilities = null;
		int remaining = -1;
		
		LOG.info("Reading {0} facilities from page at offset {1}", pageSize, pageOffset);
		facilities = perun.getFacilitiesManager().getAllFacilities();
		if(pageSize != null && pageSize > 0) {
			int size = facilities.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			facilities = facilities.subList(pageOffset, last);
			remaining = size - last;
		}
		LOG.info("Query returned {0} facilities", facilities.size());
		for(Facility facility : facilities) {
			mapResult(facility, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(Facility facility, ResultsHandler handler) {
		handler.handle(schemaAdapter.mapObject(objectClass, facility).build());
	}

}
