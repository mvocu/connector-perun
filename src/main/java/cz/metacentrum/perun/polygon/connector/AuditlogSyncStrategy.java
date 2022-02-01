package cz.metacentrum.perun.polygon.connector;

import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.spi.SyncTokenResultsHandler;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditEvent;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditMessage;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;

public class AuditlogSyncStrategy implements SyncStrategy {

	private PerunRPC perun;
	private SchemaManager schemaManager;
	private String consumerName;

	private static final Log LOG = Log.getLog(GroupSearch.class);
	
	private class EventObjectInfo {
	
		private PerunBean object;
		private ObjectClass objectClass;
		
		public EventObjectInfo() {
			object = null;
			objectClass = null;
		}

		public EventObjectInfo(PerunBean object, ObjectClass objectClass) {
			this.object = object;
			this.objectClass = objectClass;
		}
		public PerunBean getObject() {
			return object;
		}
		public void setObject(PerunBean object) {
			this.object = object;
		}
		public ObjectClass getObjectClass() {
			return objectClass;
		}
		public void setObjectClass(ObjectClass objectClass) {
			this.objectClass = objectClass;
		}
		
		
	}
	
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
			LOG.info("No auditlog messages available.");
		} else {
			for(AuditMessage message : messages) {
				LOG.info("Auditlog message: {0}", message.getEvent().getMessage());
				
				AuditEvent event = message.getEvent();
				
				SyncToken deltaToken = new SyncToken(message.getId());
				finalToken = deltaToken;
				
				SyncDeltaBuilder deltaBuilder = new SyncDeltaBuilder();
				deltaBuilder.setToken(deltaToken);				

				SyncDeltaType deltaType;
				
				// get delta type
				if(event.getName().matches("Deleted$")) {
					deltaType = SyncDeltaType.DELETE;
					
				} else if(event.getName().matches("Created$") ||
					  event.getName().matches("GroupCreatedInVo$") ||
					  event.getName().matches("GroupCreatedAsSubgroup$") ||
					  event.getName().matches("UserExtSourceAddedToUser$")) {

					/*
					 * Creation events: 
					 *   - single Perun object was created
					 *   - possible relations to other Perun objects (VO, parent Group) are encoded 
					 *     within the primary object properties  
					 *     
					 * NOTE: roughly equivalent to creationEventProcessor in perun-ldapc
					 */
					deltaType = SyncDeltaType.CREATE;

					/* 
					 * NOTE: Make sure this call returns the right bean here, 
					 * especially when the event contains more than one bean.
					 */
					EventObjectInfo info = fetchBeanFromEvent(event);
					if(info == null) {
						continue;
					}
					if(!objectClass.is(ObjectClass.ALL_NAME) && 
					   !objectClass.is(info.getObjectClass().getObjectClassValue())) {
						continue;
					}
					SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(info.getObjectClass());
					ObjectSearch search = schemaManager.getObjectSearchForObjectClass(info.getObjectClass());
					PerunBean freshBean = search.readPerunBeanById(info.getObject().getId());
					deltaBuilder.setObject(adapter.mapObject(info.getObjectClass(), freshBean).build());
					
				} else if(event.getName().matches("MemberAddedToGroup$") ||
					  event.getName().matches("MemberValidatedInGroup$")) {
					
					/*
					 * Creation of Perun relation interpreted as ICF objects:
					 *   - GroupMember object representing user membership in group
					 *     
					 *  NOTE: roughly equivalent to groupEventProcessor in perun-ldapc
					 */
					deltaType = SyncDeltaType.CREATE_OR_UPDATE;
					ObjectClass deltaObjectClass = new ObjectClass(GroupMemberSchemaAdapter.OBJECTCLASS_NAME);
					if(!objectClass.is(ObjectClass.ALL_NAME) && 
				           !objectClass.is(deltaObjectClass.getObjectClassValue())) {
								continue;
					}
					SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(deltaObjectClass);
					ObjectSearch search = schemaManager.getObjectSearchForObjectClass(deltaObjectClass);
					PerunBean freshBean = search.readPerunBeanById(event.getGroup().getId(), event.getMember().getId());
					deltaBuilder.setObject(adapter.mapObject(deltaObjectClass, freshBean).build());
					
					
				} else if(event.getName().matches("Updated$")) {
					deltaType = SyncDeltaType.UPDATE;
					
				} else {
					deltaType = SyncDeltaType.CREATE_OR_UPDATE;
				}
				
				deltaBuilder.setDeltaType(deltaType);
				
				handler.handle(deltaBuilder.build());
			}
		}

		if (handler instanceof SyncTokenResultsHandler && finalToken != null) {
			((SyncTokenResultsHandler)handler).handleResult(finalToken);
		}
		
	}

	@Override
	public SyncToken getLatestSyncToken(ObjectClass objectClass) {
		Integer lastMessageId = perun.getAuditlogManager().getLastMessageId();
		return new SyncToken(lastMessageId);
	}

	/**
	 * Returns one selected bean from audit event.
	 * NOTE: do not call for events with more beans, unless you know what you are doing 
	 * (you may not get what you intend to and may miss some info)
	 * 
	 * @param event - audit event to get the bean from
	 * @return  bean from event
	 */
	private EventObjectInfo fetchBeanFromEvent(AuditEvent event) {
		EventObjectInfo info = new EventObjectInfo();
		
		/*
		 * The order here is important for the correct operation:
		 *   - Group must come before Vo (for GroupCreatedInVo event)
		 *   - UserExtSource must come before User (for UserExtSourceAddedToUser) 
		 */
		if(event.getUserExtSource() != null) {
			/* UserExt */
			info.setObject(event.getUserExtSource());
			info.setObjectClass(new ObjectClass(UserExtSchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getUser() != null) {
			/* User */
			info.setObject(event.getUser());
			info.setObjectClass(new ObjectClass(UserSchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getExtSource() != null) {
			/* ExtSource */
			info.setObject(event.getExtSource());
			info.setObjectClass(new ObjectClass(ExtSourceSchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getFacility() != null) {
			/* Facility */
			info.setObject(event.getFacility());
			info.setObjectClass(new ObjectClass(FacilitySchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getGroup() != null ) {
			/* Group */
			info.setObject(event.getGroup());
			info.setObjectClass(new ObjectClass(GroupSchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getVo() != null) {
			/* Vo */
			info.setObject(event.getVo());
			info.setObjectClass(new ObjectClass(VoSchemaAdapter.OBJECTCLASS_NAME));
		} else if(event.getMember() != null) {
			/* VoMember */
			info.setObject(event.getMember());
			info.setObjectClass(new ObjectClass(VoMemberSchemaAdapter.OBJECTCLASS_NAME));
		} else {
			return null;
		}
		
		return info;
	}

	public AuditlogSyncStrategy() {
		super();
	}
}
