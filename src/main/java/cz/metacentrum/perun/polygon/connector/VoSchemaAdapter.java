package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class VoSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	public static final String NS_VO_ATTR = "urn:perun:vo:attribute-def";
	public static final String NS_VO_ATTR_DEF = "urn:perun:vo:attribute-def:def";
	public static final String NS_VO_ATTR_OPT = "urn:perun:vo:attribute-def:opt";
	public static final String NS_VO_ATTR_CORE = "urn:perun:vo:attribute-def:core";
	public static final String NS_VO_ATTR_VIRT = "urn:perun:vo:attribute-def:virt";

	private LinkedHashSet<String> attrNames = null;

	public VoSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		// ----------------  VoMember object class -----------------
		ObjectClassInfoBuilder vo = new ObjectClassInfoBuilder();
		vo.setType("VirtualOrganization");

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("vo_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		vo.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("vo_name");
		name.setRequired(true);
		vo.addAttributeInfo(name.build());

		// read Member attribute definitions from Perun
		addAttributesFromNamespace(vo, NS_VO_ATTR_CORE, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_DEF, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_OPT, attrNames);
		
		return vo;
	}

}
