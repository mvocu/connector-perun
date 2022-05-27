package cz.metacentrum.perun.polygon.connector;

import java.util.List;
import java.util.Map;

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

import cz.metacentrum.perun.polygon.connector.GroupMemberSchemaAdapter.GroupMemberRelationBean;
import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.Group;
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberData;
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberRelation;
import cz.metacentrum.perun.polygon.connector.rpc.model.Member;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;
import cz.metacentrum.perun.polygon.connector.rpc.model.RichMember;
import cz.metacentrum.perun.polygon.connector.rpc.model.User;

public class GroupMemberSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(GroupMemberSearch.class);
	
	public GroupMemberSearch(ObjectClass objectClass, SchemaAdapter adapter, PerunRPC perun) {
		super(objectClass, adapter, perun);
	}

	@Override
	public PerunBean readPerunBeanById(Integer id, Integer... ids) {
		if(ids.length == 0) {
			return null;
		}
		LOG.info("Reading GroupMember with id {0}:{1}", id, ids[0]);
		Member member = null;
		Group group = null;
		try {
			member = perun.getGroupsManager().getGroupMemberById(id, ids[0]);
			group = perun.getGroupsManager().getGroupById(id);
		} catch (HttpClientErrorException exeception) {
			LOG.info("Query returned none group member");
			return null;
		}

		GroupMemberRelationBean result = null;
		if(member != null) {
			GroupMemberRelation memberRelation = new GroupMemberRelation();
			memberRelation.setG(id);
			memberRelation.setM(member.getId());
			memberRelation.setS(member.getStatus());
			memberRelation.setSg(member.getSourceGroupId());
			memberRelation.setT(member.getMembershipType());
			memberRelation.setGn(group.getShortName());
			memberRelation.setPg(group.getParentGroupId());
			memberRelation.setU(member.getUserId());
			
			result = ((GroupMemberSchemaAdapter)this.schemaAdapter).new GroupMemberRelationBean(memberRelation);
		}
		
		return result;
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllGroupMembers(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				String[] parts = uid.split(":");
				Integer groupId = Integer.valueOf(parts[0]);
				GroupMemberRelationBean member = (GroupMemberRelationBean)readPerunBeanById(groupId, Integer.valueOf(parts[1]));
				
				if(member != null) {
					mapResult(member.getRelation(), 
						  perun.getAttributesManager().getMemberGroupAttributes(member.getRelation().getM(), groupId), 
						  handler);
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

	protected void readAllGroupMembers(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		LOG.info("Reading list of groups members.");
		GroupMemberData groupsMembers = perun.getIntegrationManager().getGroupMemberData();
		LOG.info("Total groups members found: {0}", groupsMembers.getRelations().size());
		List<GroupMemberRelation> members = groupsMembers.getRelations();

		int remaining = -1;
		
		LOG.info("Reading {0} GroupMembers from page at offset {1}", pageSize, pageOffset);
		if(pageSize != null && pageSize > 0) {
			int size = members.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			members = members.subList(pageOffset, last);
			remaining = size - last;
		}
		LOG.info("Query returned {0} members", members.size());
		for(GroupMemberRelation member : members) {
			Map<String, Map<String, List<Attribute>>> attrsMap = groupsMembers.getGroupMemberAttributes();
			List<Attribute> attrs = null;
			if(attrsMap != null) {
				Map<String, List<Attribute>> groupAttrsMap = attrsMap.get(member.getG().toString());
				if(groupAttrsMap != null) {
					attrs = groupAttrsMap.get(member.getM().toString());
				}
			}
			mapResult(member, attrs, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(GroupMemberRelation member, List<Attribute> attrs, ResultsHandler handler) {
		ConnectorObjectBuilder out = schemaAdapter.mapObject(objectClass, member);

		if(attrs != null) {
			for(Attribute attr: attrs) {
				out.addAttribute(schemaAdapter.mapAttribute(attr));
			}
		}
		
		handler.handle(out.build());
	}

}
