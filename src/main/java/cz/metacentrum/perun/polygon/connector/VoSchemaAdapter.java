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
import cz.metacentrum.perun.polygon.connector.rpc.model.Vo;

public class VoSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	private static final String NS_VO_ATTR = "urn:perun:vo:attribute-def";
	private static final String NS_VO_ATTR_DEF = "urn:perun:vo:attribute-def:def";
	private static final String NS_VO_ATTR_OPT = "urn:perun:vo:attribute-def:opt";
	private static final String NS_VO_ATTR_CORE = "urn:perun:vo:attribute-def:core";
	private static final String NS_VO_ATTR_VIRT = "urn:perun:vo:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "VirtualOrganization";
	
	private LinkedHashSet<String> attrNames = null;

	public VoSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  VoMember object class -----------------
		ObjectClassInfoBuilder vo = new ObjectClassInfoBuilder();
		vo.setType(OBJECTCLASS_NAME);

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

		// short name
		AttributeInfoBuilder short_name = new AttributeInfoBuilder("vo_short_name", String.class);
		short_name.setNativeName("shortName");
		short_name.setRequired(true);
		short_name.setCreateable(true);
		short_name.setUpdateable(true);
		short_name.setReadable(true);
		short_name.setMultiValued(false);
		vo.addAttributeInfo(short_name.build());
		

		// read Vo attribute definitions from Perun
		addAttributesFromNamespace(vo, NS_VO_ATTR_CORE, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_DEF, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(vo, NS_VO_ATTR_OPT, attrNames);
		
		return vo;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		Vo vo = (Vo)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(vo.getShortName());
		out.setUid(vo.getId().toString());
		// voId
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("vo_short_name");
		ab.addValue(vo.getShortName());
		out.addAttribute(ab.build());
		List<Attribute> attrs = perun.getAttributesManager().getVoAttributes(vo.getId());
		for(Attribute attr: attrs) {
			out.addAttribute(mapAttribute(attr));
		}
		return out;
	}

}
