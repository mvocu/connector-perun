package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;

public interface SyncStrategy  {
	
	public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options);
	
	public SyncToken getLatestSyncToken(ObjectClass objectClass);
}
