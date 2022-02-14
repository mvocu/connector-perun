package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

public interface SchemaAdapter {

	 public ObjectClassInfoBuilder getObjectClass();

	 public String getObjectClassName();
	 
	 public Uid getUid(Object source);
	 
	 public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source);

	 public Attribute mapAttribute(cz.metacentrum.perun.polygon.connector.rpc.model.Attribute attr);
}
