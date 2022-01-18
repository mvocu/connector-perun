package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberRelation;

public class GroupMemberSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	private static final String NS_MEMBER_GROUP_ATTR = "urn:perun:member_group:attribute-def";
	private static final String NS_MEMBER_GROUP_ATTR_DEF = "urn:perun:member_group:attribute-def:def";
	private static final String NS_MEMBER_GROUP_ATTR_OPT = "urn:perun:member_group:attribute-def:opt";
	private static final String NS_MEMBER_GROUP_ATTR_VIRT = "urn:perun:member_group:attribute-def:virt";

	private static final String OBJECTCLASS_NAME = "GroupMember";
	
	private LinkedHashSet<String> attrNames = null;

	public GroupMemberSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  GroupMember object class -----------------
		ObjectClassInfoBuilder groupMember = new ObjectClassInfoBuilder();
		groupMember.setType(OBJECTCLASS_NAME);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("member_group_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		groupMember.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("member_group_id");
		name.setRequired(true);
		groupMember.addAttributeInfo(name.build());
		
		// group id
		AttributeInfoBuilder group_id = new AttributeInfoBuilder("member_group_group_id", Integer.class);
		group_id.setNativeName(NS_MEMBER_GROUP_ATTR + ":group_id");
		group_id.setMultiValued(false);
		group_id.setCreateable(false);
		group_id.setUpdateable(false);
		group_id.setRequired(true);
		groupMember.addAttributeInfo(group_id.build());

		// member id
		AttributeInfoBuilder member_id = new AttributeInfoBuilder("member_group_member_id", Integer.class);
		member_id.setNativeName(NS_MEMBER_GROUP_ATTR + ":member_id");
		member_id.setMultiValued(false);
		member_id.setCreateable(false);
		member_id.setUpdateable(false);
		member_id.setRequired(true);
		groupMember.addAttributeInfo(member_id.build());

		// membership type
		AttributeInfoBuilder membership_type = new AttributeInfoBuilder("member_group_membership_type", String.class);
		membership_type.setNativeName(NS_MEMBER_GROUP_ATTR + ":membership_type");
		membership_type.setMultiValued(false);
		membership_type.setCreateable(true);
		membership_type.setUpdateable(true);
		membership_type.setRequired(false);
		groupMember.addAttributeInfo(membership_type.build());
		
		// source group id
		AttributeInfoBuilder source_group_id = new AttributeInfoBuilder("member_group_source_group_id", Integer.class);
		source_group_id.setNativeName(NS_MEMBER_GROUP_ATTR + ":source_group_id");
		source_group_id.setMultiValued(false);
		source_group_id.setCreateable(true);
		source_group_id.setUpdateable(true);
		source_group_id.setRequired(false);
		groupMember.addAttributeInfo(source_group_id.build());

		// source group status
		AttributeInfoBuilder source_group_status = new AttributeInfoBuilder("member_group_source_group_status", Integer.class);
		source_group_status.setNativeName(NS_MEMBER_GROUP_ATTR + ":source_group_status");
		source_group_status.setMultiValued(false);
		source_group_status.setCreateable(true);
		source_group_status.setUpdateable(true);
		source_group_status.setRequired(false);
		groupMember.addAttributeInfo(source_group_status.build());

		// read GroupMember attribute definitions from Perun
		addAttributesFromNamespace(groupMember, NS_MEMBER_GROUP_ATTR, attrNames);
		addAttributesFromNamespace(groupMember, NS_MEMBER_GROUP_ATTR_DEF, attrNames);
		addAttributesFromNamespace(groupMember, NS_MEMBER_GROUP_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(groupMember, NS_MEMBER_GROUP_ATTR_OPT, attrNames);

		return groupMember;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		GroupMemberRelation member = (GroupMemberRelation)source;
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
		
		return out;
	}

}
