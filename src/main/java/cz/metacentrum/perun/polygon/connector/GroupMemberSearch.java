package cz.metacentrum.perun.polygon.connector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
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
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberData;
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberRelation;
import cz.metacentrum.perun.polygon.connector.rpc.model.Member;
import cz.metacentrum.perun.polygon.connector.rpc.model.SimpleAttribute;

public class GroupMemberSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(GroupMemberSearch.class);
	
	public GroupMemberSearch(ObjectClass objectClass, PerunRPC perun) {
		super(objectClass, perun);
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
				LOG.info("Reading GroupMember with uid {0}", uid);
				String[] parts = uid.split(":");
				Integer groupId = Integer.valueOf(parts[0]);
				Member member = perun.getGroupsManager().getGroupMemberById(groupId, Integer.valueOf(parts[1]));
				LOG.info("Query returned {0} group member", member);
				
				if(member != null) {
					GroupMemberRelation memberRelation = new GroupMemberRelation();
					memberRelation.setG(groupId);
					memberRelation.setM(member.getId());
					memberRelation.setS(member.getStatus());
					memberRelation.setSg(member.getSourceGroupId());
					memberRelation.setT(member.getMembershipType());
					mapResult(memberRelation, 
						  perun.getAttributesManager().getMemberGroupAttributes(member.getId(), groupId), 
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
		if(pageSize > 0) {
			int size = members.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			members = members.subList(pageOffset, last);
			remaining = size - last;
		}
		LOG.info("Query returned {0} members", members.size());
		for(GroupMemberRelation member : members) {
			mapResult(member, 
				groupsMembers.getGroupMemberAttributes().get(member.getG().toString()).get(member.getM().toString()), 
				handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(GroupMemberRelation member, List<Attribute> attrs, ResultsHandler handler) {
		Integer groupId = member.getG();
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		String name = groupId.toString() + ":" + member.getM();
		out.setName(name);
		out.setUid(name);

		// -- manually mapped attributes:
		// member_group_group_id
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("member_group_group_id");
		ab.addValue(groupId);
		out.addAttribute(ab.build());
		// member_group_member_id
		ab = new AttributeBuilder();
		ab.setName("member_group_member_id");
		ab.addValue(member.getM());
		out.addAttribute(ab.build());
		// member_group_membership_type
		ab = new AttributeBuilder();
		ab.setName("member_group_membership_type");
		ab.addValue(member.getT());
		out.addAttribute(ab.build());
		// member_group_source_group_id
		ab = new AttributeBuilder();
		ab.setName("member_group_source_group_id");
		ab.addValue(member.getSg());
		out.addAttribute(ab.build());
		// member_group_source_group_status
		ab = new AttributeBuilder();
		ab.setName("member_group_source_group_status");
		ab.addValue(member.getS());
		out.addAttribute(ab.build());
		
		if(attrs != null) {
			for(Attribute attr: attrs) {
				out.addAttribute(createAttribute(attr));
			}
		}
		handler.handle(out.build());
	}

}
