package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

public interface SchemaAdapter {

	 public ObjectClassInfoBuilder getObjectClass();

	 public String getObjectClassName();
	 
	 public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source);

	 public Attribute mapAttribute(cz.metacentrum.perun.polygon.connector.rpc.model.Attribute attr);
}
