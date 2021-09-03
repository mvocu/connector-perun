package cz.metacentrum.perun.polygon.connector;

import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.AttributeDefinition;

public abstract class SchemaAdapterBase implements SchemaAdapter {

	private static final Log LOG = Log.getLog(SchemaAdapterBase.class);

	protected PerunRPC perun;
	
	public SchemaAdapterBase(PerunRPC perun) {
		this.perun = perun; 
	}

	@Override
	public abstract ObjectClassInfoBuilder getObjectClass();

	public static String mapPerunNamespace(String name) {
		if(name == null || name.isEmpty()) {
			return "";
		}
		
		String[] parts = name.split(":");
		if(parts.length < 5) {
			return name.replace(':', '_');
		}
		
		StringBuilder out = new StringBuilder();
		out.append(parts[2]);
		out.append("_");
		out.append(parts[4]);
		out.append("_");
		return out.toString();
	}
	
	public static String getAttributeName(Attribute attr) {
		if(attr == null) {
			return "";
		}
		
		StringBuilder out = new StringBuilder();
		out.append(mapPerunNamespace(attr.getNamespace()));
		out.append(attr.getFriendlyName().replace(':', '_'));
		return out.toString();
	}
	
	protected void addAttributesFromNamespace(ObjectClassInfoBuilder object, String namespace, Set<String> presentNames) {
		String prefix = mapPerunNamespace(namespace);
		for(AttributeDefinition attrDef : perun.getAttributesManager().getAttributeDefinitionsByNamespace(namespace))
		{
			String attrName = attrDef.getFriendlyName();
			attrName = prefix + attrName.replace(':', '_');
			if(presentNames.contains(attrName)) {
				LOG.warn("Found duplicate attribute name {0}, skipping", attrName);
				continue;
			}
			presentNames.add(attrName);
			object.addAttributeInfo(createAttribute(attrName, attrDef).build());
		}
		
	}
	
	protected AttributeInfoBuilder createAttribute(String name, AttributeDefinition attrDef) {
		AttributeInfoBuilder attr = new AttributeInfoBuilder(name);
		attr.setType(mapClass(attrDef.getType()));
		attr.setNativeName(attrDef.getNamespace() + ":" + attrDef.getBaseFriendlyName());
		attr.setMultiValued(isMultivalued(attrDef));
		attr.setCreateable(attrDef.getWritable());
		attr.setUpdateable(attrDef.getWritable());
		attr.setRequired(false);
		
		return attr;
	}
	
	protected Class<?> mapClass(String name) {
		switch(name) {
		case "java.util.ArrayList":
			return String.class;

		case "java.util.LinkedHashMap":
			return String.class;
			
		default:
			break;
		}
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return String.class;
		}
	}
	
	public static Boolean isMultivalued(AttributeDefinition attrDef) {
		return attrDef.getType().contains("ArrayList") || attrDef.getType().contains("HashMap");
	}
	
}
