package cz.metacentrum.perun.polygon.connector;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.web.client.HttpClientErrorException;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;
import cz.metacentrum.perun.polygon.connector.rpc.model.RichUser;
import cz.metacentrum.perun.polygon.connector.rpc.model.User;
import cz.metacentrum.perun.polygon.connector.rpc.model.UserExtSource;

public class UserSearch extends ObjectSearchBase {

	private static final Log LOG = Log.getLog(UserSearch.class);
	
	
	public UserSearch(ObjectClass objectClass, SchemaAdapter adapter, PerunRPC perun) {
		super(objectClass, adapter, perun);
	}

	@Override
	public PerunBean readPerunBeanById(Integer id, Integer... ids) {
		LOG.info("Reading user with uid {0}", id);
		List<RichUser> users;
		try {
			users = perun.getUsersManager().getRichUsersWithAttributesByIds(Arrays.asList(id));
		} catch (HttpClientErrorException exception) {
			LOG.info("Query returned no user");
			return null;
		}
		LOG.info("Query returned {0} users", users.size());
		
		if(!users.isEmpty()) {
			return users.get(0);
		}
		
		return null;
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllUsers(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				RichUser user = (RichUser)readPerunBeanById(Integer.valueOf(uid));
				if(user != null) {
					mapResult(user, handler);
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

	protected void readAllUsers(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		List<RichUser> users = null;
		int remaining = -1;
		
		LOG.info("Reading {0} users from page at offset {1}", pageSize, pageOffset);
		if(pageSize != null && pageSize > 0) {
			List<User> partUsers = perun.getUsersManager().getUsers();
			List<Integer> userIds = partUsers.stream()
				.map(user -> { return user.getId(); })
				.sorted()
				.collect(Collectors.toList());
			int size = userIds.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			userIds = userIds.subList(pageOffset, last);
			remaining = size - last;
			if(userIds.isEmpty()) {
				LOG.warn("User sublist created from original size {0} at offsets {1} and {2} is empty", size, pageOffset, last);
			}
			LOG.info("Asking for details for {0} users", userIds.size());
			users = perun.getUsersManager().getRichUsersWithAttributesByIds(userIds);
		} else {
			users = perun.getUsersManager().getAllRichUsersWithAttributes(true);
		}
		LOG.info("Query returned {0} users", users.size());
		for(RichUser user : users) {
			mapResult(user, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(RichUser user, ResultsHandler handler) {
		ConnectorObjectBuilder out = schemaAdapter.mapObject(objectClass, user);
		handler.handle(out.build());
	}

}
