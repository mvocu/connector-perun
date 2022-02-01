package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.ObjectClass;

public interface SchemaManager {

	SchemaAdapter getSchemaAdapterForObjectClass(ObjectClass objectClass);
	
	ObjectSearch getObjectSearchForObjectClass(ObjectClass objectClass);

}