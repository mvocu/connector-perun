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
import cz.metacentrum.perun.polygon.connector.rpc.model.RichMember;

public class VoMemberSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	private static final String NS_MEMBER_ATTR_CORE = "urn:perun:member:attribute-def:core";
	private static final String NS_MEMBER_ATTR = "urn:perun:member:attribute-def";
	private static final String NS_MEMBER_ATTR_DEF = "urn:perun:member:attribute-def:def";
	private static final String NS_MEMBER_ATTR_OPT = "urn:perun:member:attribute-def:opt";
	private static final String NS_MEMBER_ATTR_VIRT = "urn:perun:member:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "VoMember";
	
	private LinkedHashSet<String> attrNames = null;

	public VoMemberSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  VoMember object class -----------------
		ObjectClassInfoBuilder member = new ObjectClassInfoBuilder();
		member.setType(OBJECTCLASS_NAME);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("member_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		member.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("member_name");
		name.setRequired(true);
		member.addAttributeInfo(name.build());

		// userId
		AttributeInfoBuilder user_id = new AttributeInfoBuilder("member_user_id", String.class);
		user_id.setNativeName("userId");
		user_id.setRequired(true);
		user_id.setCreateable(true);
		user_id.setUpdateable(false);
		user_id.setReadable(true);
		user_id.setMultiValued(false);
		member.addAttributeInfo(user_id.build());
		
		// voId
		AttributeInfoBuilder vo_id = new AttributeInfoBuilder("member_vo_id", String.class);
		vo_id.setNativeName("voId");
		vo_id.setRequired(true);
		vo_id.setCreateable(true);
		vo_id.setUpdateable(false);
		vo_id.setReadable(true);
		vo_id.setMultiValued(false);
		member.addAttributeInfo(vo_id.build());
		
		// status
		AttributeInfoBuilder status = new AttributeInfoBuilder("member_status", String.class);
		status.setNativeName("status");
		status.setRequired(true);
		status.setCreateable(true);
		status.setUpdateable(true);
		status.setReadable(true);
		status.setMultiValued(false);
		member.addAttributeInfo(status.build());
		
		// membershipType
		AttributeInfoBuilder m_type = new AttributeInfoBuilder("member_membership_type", String.class);
		m_type.setNativeName("membershipType");
		m_type.setRequired(true);
		m_type.setCreateable(true);
		m_type.setUpdateable(true);
		m_type.setReadable(true);
		m_type.setMultiValued(false);
		member.addAttributeInfo(m_type.build());
		
		// sourceGroupId
		AttributeInfoBuilder sg_id = new AttributeInfoBuilder("member_source_group_id", Integer.class);
		sg_id.setNativeName("sourceGroupId");
		sg_id.setRequired(false);
		sg_id.setCreateable(true);
		sg_id.setUpdateable(true);
		sg_id.setReadable(true);
		sg_id.setMultiValued(false);
		member.addAttributeInfo(sg_id.build());
		
		// sponsored
		AttributeInfoBuilder sponsored = new AttributeInfoBuilder("member_sponsored", Boolean.class);
		sponsored.setNativeName("sponsored");
		sponsored.setRequired(true);
		sponsored.setCreateable(true);
		sponsored.setUpdateable(true);
		sponsored.setReadable(true);
		sponsored.setMultiValued(false);
		member.addAttributeInfo(sponsored.build());

		// groupStatus
		AttributeInfoBuilder gs = new AttributeInfoBuilder("member_group_status", String.class);
		gs.setNativeName("groupStatus");
		gs.setRequired(true);
		gs.setCreateable(true);
		gs.setUpdateable(true);
		gs.setReadable(true);
		gs.setMultiValued(false);
		member.addAttributeInfo(gs.build());
		
		// groupStatuses
		AttributeInfoBuilder gss = new AttributeInfoBuilder("member_group_statuses", String.class);
		gss.setNativeName("groupStatuses");
		gss.setRequired(false);
		gss.setCreateable(true);
		gss.setUpdateable(true);
		gss.setReadable(true);
		gss.setMultiValued(true);
		member.addAttributeInfo(gss.build());
		
		// read Member attribute definitions from Perun
		addAttributesFromNamespace(member, NS_MEMBER_ATTR_CORE, attrNames);
		addAttributesFromNamespace(member, NS_MEMBER_ATTR_DEF, attrNames);
		addAttributesFromNamespace(member, NS_MEMBER_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(member, NS_MEMBER_ATTR_OPT, attrNames);
		
		return member;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		RichMember member = (RichMember)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(mapName(member));
		out.setUid(member.getId().toString());
		// object attributes
		// userId
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("member_user_id");
		ab.addValue(member.getUserId().toString());
		out.addAttribute(ab.build());
		// voId
		ab = new AttributeBuilder();
		ab.setName("member_vo_id");
		ab.addValue(member.getVoId().toString());
		out.addAttribute(ab.build());
		// status
		ab = new AttributeBuilder();
		ab.setName("member_status");
		ab.addValue(member.getStatus());
		out.addAttribute(ab.build());
		// membershipType
		ab = new AttributeBuilder();
		ab.setName("member_membership_type");
		ab.addValue(member.getMembershipType());
		out.addAttribute(ab.build());
		// sourceGroupId
		if(member.getSourceGroupId_JsonNullable().isPresent()) {
			ab = new AttributeBuilder();
			ab.setName("member_source_group_id");
			ab.addValue(member.getSourceGroupId());
			out.addAttribute(ab.build());
		}
		// sponsored
		ab = new AttributeBuilder();
		ab.setName("member_sponsored");
		ab.addValue(member.getSponsored());
		out.addAttribute(ab.build());
		// groupStatus
		ab = new AttributeBuilder();
		ab.setName("member_group_status");
		ab.addValue(member.getGroupStatus());
		out.addAttribute(ab.build());
		// groupStatuses
		ab = new AttributeBuilder();
		ab.setName("member_group_statuses");
		ab.addValue(member.getGroupStatuses() != null ? member.getGroupStatuses().toString() : "");
		out.addAttribute(ab.build());
		
		// defined attributes
		if(member.getMemberAttributes() != null) {
			for(Attribute attr: member.getMemberAttributes()) {
				out.addAttribute(mapAttribute(attr));
			}
		}
		return out;
	}

	private String mapName(RichMember member) {
		return member.getVoId().toString() + ":" + member.getUserId().toString();
	}

}
