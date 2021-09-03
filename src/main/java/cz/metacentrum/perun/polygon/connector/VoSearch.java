package cz.metacentrum.perun.polygon.connector;

import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
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
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.Vo;

public class VoSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(VoSearch.class);

	public VoSearch(ObjectClass objectClass, PerunRPC perun) {
		super(objectClass, perun);
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllVos(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				LOG.info("Reading VO with uuid {0}", uid);
				Vo vo = perun.getVosManager().getVoById(Integer.parseInt(uid));
				LOG.info("Query returned {0} VO", vo.toString());
				
				if(vo != null) {
					mapResult(vo, handler);
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

	protected void readAllVos(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		List<Vo> vos = null;
		int remaining = -1;
		
		LOG.info("Reading {0} VOs from page at offset {1}", pageSize, pageOffset);
		vos = perun.getVosManager().getAllVos();
		LOG.info("Query returned {0} VOs", vos.size());
		if(pageSize > 0) {
			int size = vos.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			vos= vos.subList(pageOffset, last);
			remaining = size - last;
		}
		for(Vo vo : vos) {
			mapResult(vo, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(Vo vo, ResultsHandler handler) {
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(vo.getShortName());
		out.setUid(vo.getId().toString());
		List<Attribute> attrs = perun.getAttributesManager().getVoAttributes(vo.getId());
		for(Attribute attr: attrs) {
			out.addAttribute(createAttribute(attr));
		}
		handler.handle(out.build());
	}
	
}
