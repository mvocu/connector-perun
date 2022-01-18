package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

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

	private static final String OBJECTCLASS_NAME = "VoMember";
	
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
