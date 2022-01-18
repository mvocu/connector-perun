package cz.metacentrum.perun.polygon.connector;

import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditMessage;

public class AuditlogSyncStrategy implements SyncStrategy {

	private PerunRPC perun;
	private SchemaManager schemaManager;
	private String consumerName;

	private static final Log LOG = Log.getLog(GroupSearch.class);
	
	public AuditlogSyncStrategy(SchemaManager schemaManager, PerunRPC perun, String name) {
		this.perun = perun;
		this.schemaManager = schemaManager;
		this.consumerName = name;
	}

	@Override
	public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {

		SchemaAdapter schemaAdapter = null;
		
		if(objectClass.is(ObjectClass.ALL_NAME)) {
		} else {
			schemaAdapter = schemaManager.getSchemaAdapterForObjectClass(objectClass);
			if(schemaAdapter == null)
		            throw new InvalidAttributeValueException("No definition for object class " + objectClass);
		}
		
	        Integer lastProcessedId = 0;
	        if (token == null) {
	        	token = getLatestSyncToken(objectClass);
	        }
	        Object fromTokenValue = token.getValue();
	        if (fromTokenValue instanceof Integer) {
	        	lastProcessedId = (Integer)fromTokenValue;
	        } else {
	        	LOG.warn("Synchronization token is not integer, ignoring");
	        }
	        SyncToken finalToken = token;

		perun.getAuditlogManager().setLastProcessedId(consumerName, lastProcessedId);

		List<AuditMessage> messages = perun.getAuditlogManager().pollConsumerMessages(consumerName);
		if(messages == null) {
			
		}
		for(AuditMessage event : messages) {
			LOG.info("Auditlog message: " + event.getEvent().getMessage());
			finalToken = new SyncToken(event.getId());
		}
	}

	@Override
	public SyncToken getLatestSyncToken(ObjectClass objectClass) {
		Integer lastMessageId = perun.getAuditlogManager().getLastMessageId();
		return new SyncToken(lastMessageId);
	}

}
