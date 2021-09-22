package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class FacilitySchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	public static final String NS_FACILITY_ATTR = "urn:perun:facility:attribute-def";
	public static final String NS_FACILITY_ATTR_DEF = "urn:perun:facility:attribute-def:def";
	public static final String NS_FACILITY_ATTR_OPT = "urn:perun:facility:attribute-def:opt";
	public static final String NS_FACILITY_ATTR_CORE = "urn:perun:facility:attribute-def:core";
	public static final String NS_FACILITY_ATTR_VIRT = "urn:perun:facility:attribute-def:virt";


	private LinkedHashSet<String> attrNames = null;

	public FacilitySchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		// ----------------  Facility object class -----------------
		ObjectClassInfoBuilder facility = new ObjectClassInfoBuilder();
		facility.setType("Facility");

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

}
