package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class GroupSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	public static final String NS_GROUP_ATTR = "urn:perun:group:attribute-def";
	public static final String NS_GROUP_ATTR_DEF = "urn:perun:group:attribute-def:def";
	public static final String NS_GROUP_ATTR_OPT = "urn:perun:group:attribute-def:opt";
	public static final String NS_GROUP_ATTR_CORE = "urn:perun:group:attribute-def:core";
	public static final String NS_GROUP_ATTR_VIRT = "urn:perun:group:attribute-def:virt";

	private LinkedHashSet<String> attrNames = null;

	public GroupSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		// ----------------  Group object class -----------------
		ObjectClassInfoBuilder group = new ObjectClassInfoBuilder();
		group.setType("Group");

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

}
