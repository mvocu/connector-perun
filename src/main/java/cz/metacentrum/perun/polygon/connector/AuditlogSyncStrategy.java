package cz.metacentrum.perun.polygon.connector;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class AuditlogSyncStrategy implements SyncStrategy {

	private PerunRPC perun;
	private String consumerName;
	
	public AuditlogSyncStrategy(PerunRPC perun) {
		this.perun = perun;
	}

	@Override
	public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
		Integer lastProcessedId = 0;
		perun.getAuditlogManager().setLastProcessedId(consumerName, lastProcessedId);
		perun.getAuditlogManager().pollConsumerMessages(consumerName);
	}

	@Override
	public SyncToken getLatestSyncToken(ObjectClass objectClass) {
		Integer lastMessageId = perun.getAuditlogManager().getLastMessageId();
		return new SyncToken(lastMessageId);
	}

}
