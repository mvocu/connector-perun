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
import cz.metacentrum.perun.polygon.connector.rpc.model.Group;
import cz.metacentrum.perun.polygon.connector.rpc.model.Member;
import cz.metacentrum.perun.polygon.connector.rpc.model.Vo;

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
				Member member = perun.getGroupsManager().getGroupMemberById(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
				LOG.info("Query returned {0} group member", member);
				
				if(member != null) {
					mapResult(Integer.valueOf(parts[0]), member, handler);
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
		
		LOG.info("Reading list of VOs");
		List<Vo> vos = perun.getVosManager().getAllVos();
		LOG.info("Query returned {0} VOs", vos.size());
		
		List<Group> groups = new ArrayList<Group>();
		for(Vo vo : vos) {
			LOG.info("Reading list of groups for VO {0}", vo.getId());
			groups.addAll(perun.getGroupsManager().getAllGroups(vo.getId()));
			LOG.info("Total groups so far: {0}", groups.size());
		}

		List<Pair<Integer,Member>> members = new ArrayList<Pair<Integer,Member>>();
		for(Group group: groups) {
			LOG.info("Reading list of members for group {0}", group.getId());
			members.addAll(perun.getGroupsManager().getGroupMembers(group.getId())
					.stream()
					.map(member -> { return new Pair<Integer,Member>(group.getId(), member); } ) 
					.collect(Collectors.toList())
					);
			LOG.info("Total members so far: {0}", members.size());
		}
		
		int remaining = -1;
		
		LOG.info("Reading {0} GroupMembers from page at offset {1}", pageSize, pageOffset);
		if(pageSize > 0) {
			int size = members.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			members = members.subList(pageOffset, last);
			remaining = size - last;
		}
		LOG.info("Query returned {0} members", members.size());
		for(Pair<Integer,Member> member : members) {
			mapResult(member.getKey(), member.getValue(), handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(Integer groupId, Member member, ResultsHandler handler) {
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		String name = groupId.toString() + ":" + member.getId();
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
		ab.addValue(member.getId());
		out.addAttribute(ab.build());
		// member_group_membership_type
		ab = new AttributeBuilder();
		ab.setName("member_group_membership_type");
		ab.addValue(member.getMembershipType());
		out.addAttribute(ab.build());
		// member_group_source_group_id
		ab = new AttributeBuilder();
		ab.setName("member_group_source_group_id");
		ab.addValue(member.getSourceGroupId());
		out.addAttribute(ab.build());
		// member_group_source_group_status
		ab = new AttributeBuilder();
		ab.setName("member_group_source_group_status");
		ab.addValue(member.getGroupStatuses().get(member.getSourceGroupId().toString()));
		out.addAttribute(ab.build());
		
		List<Attribute> attrs = perun.getAttributesManager().getMemberGroupAttributes(member.getId(), groupId);
		if(attrs != null) {
			for(Attribute attr: attrs) {
				out.addAttribute(createAttribute(attr));
			}
		}
		handler.handle(out.build());
	}


}
