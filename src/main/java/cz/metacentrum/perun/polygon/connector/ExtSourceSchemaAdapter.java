package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class ExtSourceSchemaAdapter extends SchemaAdapterBase implements SchemaAdapter {

	//private LinkedHashSet<String> attrNames = null;

	public ExtSourceSchemaAdapter(PerunRPC perun) {
		super(perun);
		//attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {
		// ----------------  extSourceExtSource object class -----------------
		ObjectClassInfoBuilder extSource = new ObjectClassInfoBuilder();
		extSource.setType("ExtSource");
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

}
