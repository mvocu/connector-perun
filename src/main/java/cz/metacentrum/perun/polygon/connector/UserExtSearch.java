package cz.metacentrum.perun.polygon.connector;

import java.util.ArrayList;
import java.util.Arrays;
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
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;
import cz.metacentrum.perun.polygon.connector.rpc.model.User;
import cz.metacentrum.perun.polygon.connector.rpc.model.UserExtSource;

public class UserExtSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(UserExtSearch.class);

	public UserExtSearch(ObjectClass objectClass, SchemaAdapter adapter, PerunRPC perun) {
		super(objectClass, adapter, perun);
	}

	@Override
	public PerunBean readPerunBeanById(Integer id, Integer... ids) {
		LOG.info("Reading user ext source with uid {0}", id);
		List<UserExtSource> userExtSources = perun.getUsersManager().getUserExtSourcesByIds(Arrays.asList(id));
		LOG.info("Query returned {0} user ext sources", userExtSources.size());
		if(!userExtSources.isEmpty()) {
			return userExtSources.get(0);
		}
		return null;
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllUserExtSources(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				UserExtSource ues = (UserExtSource)readPerunBeanById(Integer.valueOf(uid));
				if(ues != null) {
					mapResult(ues, handler);
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

	protected void readAllUserExtSources(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		LOG.info("Reading all users");
		List<User> users = perun.getUsersManager().getUsers();
		LOG.info("Query returned {0} users", users.size());

		List<UserExtSource> userExtSources = new ArrayList<UserExtSource>();
		int remaining = -1;
		
		for(User user : users) {
			LOG.info("Reading user ext sources for user {0}", user.getId());
			userExtSources.addAll(perun.getUsersManager().getUserExtSources(user.getId()));
			LOG.info("Total user ext sources so far: {0}", userExtSources.size());
		}
		if(pageSize > 0) {
			int size = userExtSources.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			userExtSources = userExtSources.subList(pageOffset, last);
			remaining = size - last;
		}
		LOG.info("Query returned {0} user ext sources", userExtSources.size());
		for(UserExtSource ues : userExtSources) {
			mapResult(ues, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(UserExtSource userExtSource, ResultsHandler handler) {
		ConnectorObjectBuilder out = schemaAdapter.mapObject(objectClass, userExtSource);
		handler.handle(out.build());
	}


}
