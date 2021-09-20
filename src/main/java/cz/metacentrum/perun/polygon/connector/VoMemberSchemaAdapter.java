package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class VoMemberSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	public static final String NS_MEMBER_ATTR_CORE = "urn:perun:member:attribute-def:core";
	public static final String NS_MEMBER_ATTR = "urn:perun:member:attribute-def";
	public static final String NS_MEMBER_ATTR_DEF = "urn:perun:member:attribute-def:def";
	public static final String NS_MEMBER_ATTR_OPT = "urn:perun:member:attribute-def:opt";
	public static final String NS_MEMBER_ATTR_VIRT = "urn:perun:member:attribute-def:virt";

	private LinkedHashSet<String> attrNames = null;

	public VoMemberSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		// ----------------  VoMember object class -----------------
		ObjectClassInfoBuilder member = new ObjectClassInfoBuilder();
		member.setType("VoMember");

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

}
