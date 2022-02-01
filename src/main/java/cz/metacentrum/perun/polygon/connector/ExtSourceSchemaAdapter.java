package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.ExtSource;

public class ExtSourceSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	//private LinkedHashSet<String> attrNames = null;

	public static final String OBJECTCLASS_NAME = "ExtSource";
	
	public ExtSourceSchemaAdapter(PerunRPC perun) {
		super(perun);
		//attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		// ----------------  extSourceExtSource object class -----------------
		ObjectClassInfoBuilder extSource = new ObjectClassInfoBuilder();
		extSource.setType(OBJECTCLASS_NAME);
		//extSource.setAuxiliary(true);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("es_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		extSource.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("es_name");
		name.setRequired(true);
		extSource.addAttributeInfo(name.build());

		// type attribute
		AttributeInfoBuilder login = new AttributeInfoBuilder("es_type", String.class);
		login.setNativeName("urn:perun:es:attribute-def:es_type");
		login.setRequired(true);
		login.setCreateable(false);
		login.setUpdateable(false);
		login.setReadable(true);
		extSource.addAttributeInfo(login.build());

		// read extSource attribute definitions from Perun
		//addAttributesFromNamespace(extSource, NS_ES_ATTR_CORE, attrNames);
		//addAttributesFromNamespace(extSource, NS_ES_ATTR_DEF, attrNames);
		//addAttributesFromNamespace(extSource, NS_ES_ATTR_OPT, attrNames);
		//addAttributesFromNamespace(extSource, NS_ES_ATTR_VIRT, attrNames);
		
		return extSource;
		
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}
	
	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		ExtSource es = (ExtSource)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(es.getName());
		out.setUid(es.getId().toString());
		// es_type
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("es_type");
		ab.addValue(es.getType());
		out.addAttribute(ab.build());
		
		return out;
	}

}
