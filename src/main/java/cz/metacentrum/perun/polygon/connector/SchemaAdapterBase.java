package cz.metacentrum.perun.polygon.connector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Subtypes;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.AttributeDefinition;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;

public abstract class SchemaAdapterBase implements SchemaAdapter {

	private static final Log LOG = Log.getLog(SchemaAdapterBase.class);

	protected PerunRPC perun;
	
	public SchemaAdapterBase(PerunRPC perun) {
		this.perun = perun; 
	}

	@Override
	public abstract ObjectClassInfoBuilder getObjectClass();
	
	@Override
	public abstract String getObjectClassName();
	
	@Override
	public Uid getUid(Object source) {
		// this is the default implementation, that just returns Id of the Perun bean
		// NOTE - this must be overriden for objects creating uid differently
		return new Uid(((PerunBean)source).getId().toString());
	}

	@Override
	public abstract ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source);

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
			object.addAttributeInfo(createAttributeInfo(attrName, attrDef).build());
		}
		
	}
	
	protected AttributeInfoBuilder createAttributeInfo(String name, AttributeDefinition attrDef) {
		AttributeInfoBuilder attr = new AttributeInfoBuilder(name);
		Class<?> type = null;
		attr.setType(type = mapClass(attrDef.getType()));
		if(type == Map.class) {
			attr.setSubtype("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/subtypes#PolyString");
		}
		attr.setNativeName(attrDef.getNamespace() + ":" + attrDef.getFriendlyName());
		attr.setMultiValued(isMultivalued(attrDef));
		attr.setCreateable(attrDef.getWritable());
		attr.setUpdateable(attrDef.getWritable());
		attr.setRequired(false);
		
		return attr;
	}
	
	@Override
	public org.identityconnectors.framework.common.objects.Attribute mapAttribute(cz.metacentrum.perun.polygon.connector.rpc.model.Attribute attr) {
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName(SchemaAdapterBase.getAttributeName(attr));
		switch(attr.getType()) {
		case "java.util.ArrayList":
			try {
				ab.addValue((Collection<?>)Class.forName(attr.getType()).cast(attr.getValue()));
			} catch (ClassNotFoundException e) {
				LOG.error("Type {0} of attribute {1} is unknown", attr.getType(), attr.getFriendlyName());
			}
			break;
		case "java.util.LinkedHashMap":
			// TODO implement conversion from map
			if(attr.getValue() != null) {
				LinkedHashMap<String, String> converted = new LinkedHashMap<>();
				((LinkedHashMap<?,?>)attr.getValue()).entrySet()
					.stream().forEach(entry -> { 
						converted.put(entry.getKey().toString(), entry.getValue().toString()); });
				//ab.addValue(((LinkedHashMap<?,?>)attr.getValue()).toString());
				if(!converted.isEmpty()) {
					ab.addValue(converted);
				}
			}
			break;
		default:
			ab.addValue(attr.getValue());
			break;
		}
		return ab.build();
	}

	protected Class<?> mapClass(String name) {
		switch(name) {
		case "java.util.ArrayList":
			return String.class;

		case "java.util.LinkedHashMap":
			return Map.class;
			
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
