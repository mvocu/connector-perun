package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;
import java.util.List;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.Facility;

public class FacilitySchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	public static final String NS_FACILITY_ATTR = "urn:perun:facility:attribute-def";
	public static final String NS_FACILITY_ATTR_DEF = "urn:perun:facility:attribute-def:def";
	public static final String NS_FACILITY_ATTR_OPT = "urn:perun:facility:attribute-def:opt";
	public static final String NS_FACILITY_ATTR_CORE = "urn:perun:facility:attribute-def:core";
	public static final String NS_FACILITY_ATTR_VIRT = "urn:perun:facility:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "Facility";
	
	private LinkedHashSet<String> attrNames = null;

	public FacilitySchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  Facility object class -----------------
		ObjectClassInfoBuilder facility = new ObjectClassInfoBuilder();
		facility.setType(OBJECTCLASS_NAME);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("facility_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		facility.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("facility_name");
		name.setRequired(true);
		facility.addAttributeInfo(name.build());
		
		// read User attribute definitions from Perun
		addAttributesFromNamespace(facility, NS_FACILITY_ATTR_CORE, attrNames);
		addAttributesFromNamespace(facility, NS_FACILITY_ATTR_DEF, attrNames);
		addAttributesFromNamespace(facility, NS_FACILITY_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(facility, NS_FACILITY_ATTR_OPT, attrNames);

		return facility;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		Facility facility = (Facility)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(mapName(facility));
		out.setUid(facility.getId().toString());
		List<Attribute> attributes = perun.getAttributesManager().getFacilityAttributes(facility.getId());
		if(!attributes.isEmpty()) {
			for(Attribute attr: attributes) {
				out.addAttribute(mapAttribute(attr));
			}
		}
		return out;
	}

	private String mapName(Facility facility) {
		return facility.getName();
	}
}
