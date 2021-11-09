package cz.metacentrum.perun.polygon.connector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import cz.metacentrum.perun.polygon.connector.rpc.model.RichGroup;
import cz.metacentrum.perun.polygon.connector.rpc.model.Vo;

public class GroupSearch extends ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(GroupSearch.class);

	public class GroupInfoObject {
		public RichGroup 	group;
		public List<Integer> 	includedInGroups;

		
		public GroupInfoObject(RichGroup group, List<Integer> includedInGroups) {
			super();
			this.group = group;
			this.includedInGroups = includedInGroups;
		}
		
		public GroupInfoObject(RichGroup group) {
			super();
			this.group = group;
		}

		public RichGroup getGroup() {
			return group;
		}

		public void setGroup(RichGroup group) {
			this.group = group;
		}

		public Integer getParentGroupId() {
			return this.group.getParentGroupId();
		}

		public List<Integer> getIncludedInGroups() {
			return includedInGroups;
		}

		public void setIncludedInGroups(List<Integer> includedInGroups) {
			this.includedInGroups = includedInGroups;
		}
		
	}
	
	
	public GroupSearch(ObjectClass objectClass, PerunRPC perun) {
		super(objectClass, perun);
	}

	@Override
	public void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler) {
		if(filter == null) {
			// read all
			readAllGroups(options, handler);
			return;
		}

		// perform search
		if(filter instanceof EqualsFilter) {
			if(((EqualsFilter)filter).getAttribute().is(Uid.NAME)) {
				// read single object
				String uid = (String)AttributeUtil.getSingleValue(((EqualsFilter)filter).getAttribute());
				LOG.info("Reading group with id {0}", uid);
				RichGroup group = perun.getGroupsManager().getRichGroupByIdWithAttributesByNames(Integer.valueOf(uid), null);
				Vo vo = perun.getVosManager().getVoById(group.getVoId());
				LOG.info("Query returned {0} group", group);
				if(group != null) {
					GroupInfoObject group_info = new GroupInfoObject(group);
					List<Group> includedIn = perun.getGroupsManager().getGroupUnions(group.getId(), true);
					group_info.setIncludedInGroups(includedIn.stream()
							.map(g -> g.getId())
							.collect(Collectors.toList()));
					mapResult(vo.getName(), group_info, handler);
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

	protected void readAllGroups(OperationOptions options, ResultsHandler handler) {
		Integer pageSize = options.getPageSize();
		Integer pageOffset = options.getPagedResultsOffset();
		String pageResultsCookie = options.getPagedResultsCookie();
		
		List<RichGroup> groups = new ArrayList<RichGroup>();
		int remaining = -1;

		LOG.info("Reading list of VOs");
		List<Vo> vos = perun.getVosManager().getAllVos();
		LOG.info("Query returned {0} VOs", vos.size());
		
		Map<Integer, String> vo_names = vos.stream().collect(Collectors.toMap((vo -> vo.getId()), (vo -> vo.getName()) ));
		
		LOG.info("Reading {0} groups from page at offset {1}", pageSize, pageOffset);
		if(pageSize > 0) {
			List<Group> partGroups = new ArrayList<Group>();
			for(Vo vo : vos) {
				LOG.info("Reading list of groups for VO {0}", vo.getId());
				partGroups.addAll(perun.getGroupsManager().getAllGroups(vo.getId()));
				LOG.info("Total groups so far: {0}", partGroups.size());
			}
			List<Integer> groupIds = partGroups.stream()
				.map(group -> { return group.getId(); })
				.sorted()
				.collect(Collectors.toList());
			int size = groupIds.size();
			int last = (pageOffset + pageSize > size) ? size : pageOffset + pageSize; 
			groupIds = groupIds.subList(pageOffset, last);
			remaining = size - last;
			for(Integer groupId : groupIds) {
				groups.add(perun.getGroupsManager().getRichGroupByIdWithAttributesByNames(groupId, null));
			}
		} else {
			for(Vo vo : vos) {
				LOG.info("Reading list of groups for VO {0}", vo.getId());
				groups.addAll(perun.getGroupsManager().getAllRichGroupsWithAttributesByNames(vo.getId(), null));
				LOG.info("Total groups so far: {0}", groups.size());
			}
		}
		LOG.info("Query returned {0} groups", groups.size());
		for(RichGroup group : groups) {
			GroupInfoObject group_info = new GroupInfoObject(group);
			List<Group> includedIn = perun.getGroupsManager().getGroupUnions(group.getId(), true);
			group_info.setIncludedInGroups(includedIn.stream()
					.map(g -> g.getId())
					.collect(Collectors.toList()));
			mapResult(vo_names.get(group.getVoId()), group_info, handler);
		}
		SearchResult result = new SearchResult(
				 pageResultsCookie, 	/* cookie */ 
				 remaining,	/* remainingResults */
				 true	/* completeResultSet */
				 );
		((SearchResultsHandler)handler).handleResult(result);
	}

	private void mapResult(String prefix, GroupInfoObject group_info, ResultsHandler handler) {
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(prefix + ":" + group_info.getGroup().getName());
		out.setUid(group_info.getGroup().getId().toString());
		// -- manually mapped attributes:
		// group_parent_group_id
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("group_parent_group_id");
		ab.addValue(group_info.getParentGroupId());
		out.addAttribute(ab.build());
		// group_included_in_group_id
		ab = new AttributeBuilder();
		ab.setName("group_included_in_group_id");
		ab.addValue(group_info.getIncludedInGroups());
		out.addAttribute(ab.build());
		// defined group attributes
		if(group_info.getGroup().getAttributes() != null) {
			for(Attribute attr: group_info.getGroup().getAttributes()) {
				out.addAttribute(createAttribute(attr));
			}
		}
		handler.handle(out.build());
	}

}
