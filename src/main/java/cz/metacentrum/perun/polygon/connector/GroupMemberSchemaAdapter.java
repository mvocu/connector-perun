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
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;

public class GroupMemberSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	private static final String NS_MEMBER_GROUP_ATTR = "urn:perun:member_group:attribute-def";
	private static final String NS_MEMBER_GROUP_ATTR_DEF = "urn:perun:member_group:attribute-def:def";
	private static final String NS_MEMBER_GROUP_ATTR_OPT = "urn:perun:member_group:attribute-def:opt";
	private static final String NS_MEMBER_GROUP_ATTR_VIRT = "urn:perun:member_group:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "GroupMember";
	
	private LinkedHashSet<String> attrNames = null;

	public class GroupMemberRelationBean extends PerunBean {
		
		private GroupMemberRelation relation;

		public GroupMemberRelationBean() {
			super();
			this.relation = null;
		}
		
		public GroupMemberRelationBean(GroupMemberRelation relation) {
			super();
			this.relation = relation;
		}

		public GroupMemberRelation getRelation() {
			return relation;
		}

		public void setRelation(GroupMemberRelation relation) {
			this.relation = relation;
		}
		
	}
	
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
		AttributeInfoBuilder group_id = new AttributeInfoBuilder("member_group_group_id", String.class);
		group_id.setNativeName("groupId");
		group_id.setMultiValued(false);
		group_id.setCreateable(false);
		group_id.setUpdateable(false);
		group_id.setRequired(true);
		groupMember.addAttributeInfo(group_id.build());

		// member id
		AttributeInfoBuilder member_id = new AttributeInfoBuilder("member_group_member_id", Integer.class);
		member_id.setNativeName("memberId");
		member_id.setMultiValued(false);
		member_id.setCreateable(false);
		member_id.setUpdateable(false);
		member_id.setRequired(true);
		groupMember.addAttributeInfo(member_id.build());

		// membership type
		AttributeInfoBuilder membership_type = new AttributeInfoBuilder("member_group_membership_type", String.class);
		membership_type.setNativeName("membershipType");
		membership_type.setMultiValued(false);
		membership_type.setCreateable(true);
		membership_type.setUpdateable(true);
		membership_type.setRequired(false);
		groupMember.addAttributeInfo(membership_type.build());
		
		// source group id
		AttributeInfoBuilder source_group_id = new AttributeInfoBuilder("member_group_source_group_id", Integer.class);
		source_group_id.setNativeName("sourceGroupId");
		source_group_id.setMultiValued(false);
		source_group_id.setCreateable(true);
		source_group_id.setUpdateable(true);
		source_group_id.setRequired(false);
		groupMember.addAttributeInfo(source_group_id.build());

		// source group status
		AttributeInfoBuilder source_group_status = new AttributeInfoBuilder("member_group_source_group_status", Integer.class);
		source_group_status.setNativeName("sourceGroupStatus");
		source_group_status.setMultiValued(false);
		source_group_status.setCreateable(true);
		source_group_status.setUpdateable(true);
		source_group_status.setRequired(false);
		groupMember.addAttributeInfo(source_group_status.build());

		// source group status
		AttributeInfoBuilder group_name = new AttributeInfoBuilder("member_group_group_name", String.class);
		group_name.setNativeName("groupName");
		group_name.setMultiValued(false);
		group_name.setCreateable(true);
		group_name.setUpdateable(true);
		group_name.setRequired(false);
		groupMember.addAttributeInfo(group_name.build());

		// source group status
		AttributeInfoBuilder parent_group = new AttributeInfoBuilder("member_group_parent_group_id", Integer.class);
		parent_group.setNativeName("parentGroupId");
		parent_group.setMultiValued(false);
		parent_group.setCreateable(true);
		parent_group.setUpdateable(true);
		parent_group.setRequired(false);
		groupMember.addAttributeInfo(parent_group.build());

		// source group status
		AttributeInfoBuilder user_id = new AttributeInfoBuilder("member_group_user_id", String.class);
		user_id.setNativeName("userId");
		user_id.setMultiValued(false);
		user_id.setCreateable(true);
		user_id.setUpdateable(true);
		user_id.setRequired(false);
		groupMember.addAttributeInfo(user_id.build());

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
	public Uid getUid(Object source) {
		GroupMemberRelation member = (GroupMemberRelation)source;
		Integer groupId = member.getG();
		return new Uid(groupId.toString() + ":" + member.getM());
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		GroupMemberRelation member = null;
		if(source instanceof GroupMemberRelation) {
			member = (GroupMemberRelation)source;
		} else if(source instanceof GroupMemberRelationBean) {
			member = ((GroupMemberRelationBean)source).getRelation();
		} else {
			throw new ClassCastException("Invalid object class " + objectClass.toString() + " passed as parameter.");
		}
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
		ab.addValue(groupId.toString());
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
		// member_group_group_name
		ab = new AttributeBuilder();
		ab.setName("member_group_group_name");
		ab.addValue(member.getGn());
		out.addAttribute(ab.build());
		// member_group_parent_group_id
		ab = new AttributeBuilder();
		ab.setName("member_group_parent_group_id");
		ab.addValue(member.getPg());
		out.addAttribute(ab.build());
		// member_group_user_id
		ab = new AttributeBuilder();
		ab.setName("member_group_user_id");
		ab.addValue(member.getU().toString());
		out.addAttribute(ab.build());
		
		return out;
	}

}
