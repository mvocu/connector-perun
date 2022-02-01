package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;
import java.util.List;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;
import cz.metacentrum.perun.polygon.connector.rpc.model.RichGroup;

public class GroupSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	private static final String NS_GROUP_ATTR = "urn:perun:group:attribute-def";
	private static final String NS_GROUP_ATTR_DEF = "urn:perun:group:attribute-def:def";
	private static final String NS_GROUP_ATTR_OPT = "urn:perun:group:attribute-def:opt";
	private static final String NS_GROUP_ATTR_CORE = "urn:perun:group:attribute-def:core";
	private static final String NS_GROUP_ATTR_VIRT = "urn:perun:group:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "Group";
	
	private LinkedHashSet<String> attrNames = null;

	public class GroupInfoObject extends PerunBean {

		public RichGroup 	group;
		public List<Integer> 	includedInGroups;
		public String		voName;
		
		public GroupInfoObject(String voName, RichGroup group, List<Integer> includedInGroups) {
			super();
			this.setId(group.getId());
			this.setBeanName(group.getBeanName());
			this.voName = voName;
			this.group = group;
			this.includedInGroups = includedInGroups;
		}
		
		public GroupInfoObject(String voName, RichGroup group) {
			super();
			this.setId(group.getId());
			this.setBeanName(group.getBeanName());
			this.voName = voName;
			this.group = group;
		}

		public String getVoName() {
			return voName;
		}

		public void setVoName(String voName) {
			this.voName = voName;
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

	public GroupSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  Group object class -----------------
		ObjectClassInfoBuilder group = new ObjectClassInfoBuilder();
		group.setType(OBJECTCLASS_NAME);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("group_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		group.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("group_name");
		name.setRequired(true);
		group.addAttributeInfo(name.build());
		
		// parent group id
		AttributeInfoBuilder parent_group_id = new AttributeInfoBuilder("group_parent_group_id", Integer.class);
		parent_group_id.setNativeName(NS_GROUP_ATTR + ":parent_group_id");
		parent_group_id.setMultiValued(false);
		parent_group_id.setCreateable(true);
		parent_group_id.setUpdateable(true);
		parent_group_id.setRequired(false);
		group.addAttributeInfo(parent_group_id.build());

		// included in group id
		AttributeInfoBuilder included_in_group_id = new AttributeInfoBuilder("group_included_in_group_id", Integer.class);
		included_in_group_id.setNativeName(NS_GROUP_ATTR + ":included_in_group_id");
		included_in_group_id.setMultiValued(true);
		included_in_group_id.setCreateable(true);
		included_in_group_id.setUpdateable(true);
		included_in_group_id.setRequired(false);
		group.addAttributeInfo(included_in_group_id.build());

		// read User attribute definitions from Perun
		addAttributesFromNamespace(group, NS_GROUP_ATTR_CORE, attrNames);
		addAttributesFromNamespace(group, NS_GROUP_ATTR_DEF, attrNames);
		addAttributesFromNamespace(group, NS_GROUP_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(group, NS_GROUP_ATTR_OPT, attrNames);

		return group;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		GroupInfoObject group_info = (GroupInfoObject)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(group_info.getVoName() + ":" + group_info.getGroup().getName());
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
				out.addAttribute(mapAttribute(attr));
			}
		}
		return out;
	}

}

