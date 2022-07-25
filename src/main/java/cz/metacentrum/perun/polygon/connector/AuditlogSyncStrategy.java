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
import org.springframework.web.client.HttpClientErrorException;

import cz.metacentrum.perun.polygon.connector.GroupMemberSchemaAdapter.GroupMemberRelationBean;
import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditEvent;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditMessage;
import cz.metacentrum.perun.polygon.connector.rpc.model.AuditMessagesPageQuery;
import cz.metacentrum.perun.polygon.connector.rpc.model.GroupMemberRelation;
import cz.metacentrum.perun.polygon.connector.rpc.model.InputGetMessagesPage;
import cz.metacentrum.perun.polygon.connector.rpc.model.PaginatedAuditMessages;
import cz.metacentrum.perun.polygon.connector.rpc.model.PerunBean;
import cz.metacentrum.perun.polygon.connector.rpc.model.SortingOrder;

public class AuditlogSyncStrategy implements SyncStrategy {

	private PerunRPC perun;
	private SchemaManager schemaManager;
	private String consumerName;
	private Integer pageSize = 0;

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
	
	public AuditlogSyncStrategy(SchemaManager schemaManager, PerunRPC perun, String name, Integer size) {
		this.perun = perun;
		this.schemaManager = schemaManager;
		this.consumerName = name;
		this.pageSize = size;
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
		Integer lastMessageId = perun.getAuditlogManager().getLastMessageId();
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
	        AuditMessagesPageQuery pageQuery = new AuditMessagesPageQuery();
	        InputGetMessagesPage inputPage = new InputGetMessagesPage();
	        
	        /* check message limit */
	        if(pageSize > 0 && lastMessageId - lastProcessedId > pageSize) {
	        	LOG.warn("Synchronization backlog is too large, will read page by page.");
	        	lastProcessedId = lastMessageId - pageSize;
	        }
	        	
	        perun.getAuditlogManager().setLastProcessedId(consumerName, lastProcessedId);
	        
	        Boolean keepProcessing = true;
	        
	        pageQuery.setPageSize(pageSize);
	        pageQuery.setOrder(SortingOrder.ASCENDING);
	        
	        while(lastProcessedId < lastMessageId && keepProcessing) {
	        	pageQuery.setOffset(lastProcessedId);
	        	inputPage.setQuery(pageQuery);
		        LOG.info("Reading auditlog messages starting from {0} to {1} out of {2}", lastProcessedId, lastProcessedId + pageSize - -1, lastMessageId);
		        //PaginatedAuditMessages messagePage = perun.getAuditlogManager().getMessagesPage(inputPage);
	        	List<AuditMessage> messages = perun.getAuditlogManager().pollConsumerMessages(consumerName);
	        	// messages = messagePage.getData();
	        	LOG.info("Received {0} messages", messages != null ? messages.size() : 0);
		        if(messages == null || messages.size() == 0) {
	        		LOG.info("No auditlog messages available.");
	        		keepProcessing = false;
	        	} else {
	        		for(AuditMessage message : messages) {
	        			lastProcessedId = message.getId();
	        			try {

	        				AuditEvent event = message.getEvent();

	        				LOG.info("Auditlog message {0}: {1}", event.getName(), event.getMessage());

	        				SyncToken deltaToken = new SyncToken(message.getId());
	        				finalToken = deltaToken;

	        				SyncDeltaBuilder deltaBuilder = new SyncDeltaBuilder();
	        				deltaBuilder.setToken(deltaToken);				

	        				SyncDeltaType deltaType;

	        				// get delta type

	        				/*
	        				 * Delete operations
	        				 */

	        				if(event.getName().matches(".*Deleted") ||
	        						event.getName().matches(".*UserExtSourceRemovedFromUser")) {

	        					/*
	        					 * Deletion events:
	        					 *   - single Perun object has been deleted
	        					 *   
	        					 * NOTE: roughly equivalent to deletionEventProcessor in perun-ldapc
	        					 */
	        					deltaType = SyncDeltaType.DELETE;

	        					EventObjectInfo info = fetchBeanFromEvent(event);
	        					if(info == null) {
	        						continue;
	        					}
	        					if(!objectClass.is(ObjectClass.ALL_NAME) && 
	        							!objectClass.is(info.getObjectClass().getObjectClassValue())) {
	        						continue;
	        					}

	        					SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(info.getObjectClass());
	        					deltaBuilder.setUid(adapter.getUid(info.getObject()));
	        					deltaBuilder.setObjectClass(info.getObjectClass());

	        				} else if(event.getName().matches(".*MemberRemovedFromGroup") ||
	        						event.getName().matches(".*MemberRemovedFromGroupTotally")) {		

	        					/*
	        					 * Deletion of Perun relation interpreted as ICF object:
	        					 *   - GroupMember object representing group membership
	        					 */
	        					deltaType = SyncDeltaType.DELETE;
	        					ObjectClass deltaObjectClass = new ObjectClass(GroupMemberSchemaAdapter.OBJECTCLASS_NAME);
	        					if(!objectClass.is(ObjectClass.ALL_NAME) && 
	        							!objectClass.is(deltaObjectClass.getObjectClassValue())) {
	        						continue;
	        					}

	        					GroupMemberRelation group_member = new GroupMemberRelation();
	        					group_member.setG(event.getGroup().getId());
	        					group_member.setM(event.getMember().getId());
	        					SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(deltaObjectClass);
	        					deltaBuilder.setUid(adapter.getUid(group_member));
	        					deltaBuilder.setObjectClass(deltaObjectClass);

	        				} else if(event.getName().matches(".*AllUserExtSourcesDeletedForUser")) {

	        					/* 
	        					 * Deletion of group of objects
	        					 * 
	        					 * It was agreed that this event will be replaced by a set of more concrete 
	        					 * events UserExtSourceRemovedFromUser.
	        					 */
	        					LOG.info("Event does not contain info on specific objects, skipping.");
	        					continue;

	        					/*
	        					 * Create operations 
	        					 */

	        				} else if(event.getName().matches(".*Created") ||
	        						event.getName().matches(".*GroupCreatedInVo") ||
	        						event.getName().matches(".*GroupCreatedAsSubgroup") ||
	        						event.getName().matches(".*UserExtSourceAddedToUser")) {

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
	        					if(freshBean == null) {
	        						LOG.info("No {0} with id {1} was found, skipping", info.getObject().getBeanName(), info.getObject().getId());
	        						continue;
	        					}
	        					deltaBuilder.setObject(adapter.mapObject(info.getObjectClass(), freshBean).build());

	        				} else if(event.getName().matches(".*MemberAddedToGroup") ||
	        						event.getName().matches(".*MemberValidatedInGroup")) {

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
	        					if(freshBean == null) {
	        						LOG.info("No {0} with id {1}:{2} was found, skipping", "GroupMember", event.getGroup().getId(), event.getMember().getId().toString());
	        						continue;
	        					}
	        					GroupMemberRelation group_member = ((GroupMemberRelationBean)freshBean).getRelation();
	        					deltaBuilder.setObject(adapter.mapObject(deltaObjectClass, group_member).build());


	        					/*
	        					 * Update operations
	        					 */

	        				} else if(event.getName().matches(".*Updated") ||
	        						event.getName().matches(".*GroupMoved")) {

	        					/*
	        					 * Update events for the single Perun objects (this also includes renames)
	        					 *
	        					 * NOTE: roughly equivalent to updateEventProcessor in perun-ldapc 
	        					 */
	        					deltaType = SyncDeltaType.UPDATE;

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
	        					if(freshBean == null) {
	        						LOG.info("No {0} with id {1} was found, skipping", info.getObject().getBeanName(), info.getObject().getId());
	        						continue;
	        					}
	        					deltaBuilder.setObject(adapter.mapObject(info.getObjectClass(), freshBean).build());

	        				} else if(event.getName().matches(".*MemberExpiredInGroup") ||
	        						event.getName().matches(".*MemberValidatedInGroup")) {

	        					/*
	        					 * Changes to relations represented as ICF objects:
	        					 *   -  
	        					 *   
	        					 *   NOTE: relations can not be renamed, only deleted
	        					 */
	        					deltaType = SyncDeltaType.UPDATE;
	        					ObjectClass deltaObjectClass = new ObjectClass(GroupMemberSchemaAdapter.OBJECTCLASS_NAME);
	        					if(!objectClass.is(ObjectClass.ALL_NAME) && 
	        							!objectClass.is(deltaObjectClass.getObjectClassValue())) {
	        						continue;
	        					}
	        					SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(deltaObjectClass);
	        					ObjectSearch search = schemaManager.getObjectSearchForObjectClass(deltaObjectClass);
	        					PerunBean freshBean = search.readPerunBeanById(event.getGroup().getId(), event.getMember().getId());
	        					if(freshBean == null) {
	        						LOG.info("No {0} with id {1}:{2} was found, skipping", "GroupMember", event.getGroup().getId(), event.getMember().getId());
	        						continue;
	        					}
	        					GroupMemberRelation group_member = ((GroupMemberRelationBean)freshBean).getRelation();
	        					deltaBuilder.setObject(adapter.mapObject(deltaObjectClass, group_member).build());

	        				} else if(event.getName().matches(".*Attribute.*")) {

	        					/* 
	        					 * Change of attributes of some object/relation
	        					 *
	        					 */
	        					deltaType = SyncDeltaType.UPDATE;

	        					if(event.getName().matches(".*ForMemberAndGroup.*")) {

	        						ObjectClass deltaObjectClass = new ObjectClass(GroupMemberSchemaAdapter.OBJECTCLASS_NAME);
	        						if(!objectClass.is(ObjectClass.ALL_NAME) && 
	        								!objectClass.is(deltaObjectClass.getObjectClassValue())) {
	        							continue;
	        						}
	        						SchemaAdapter adapter = schemaManager.getSchemaAdapterForObjectClass(deltaObjectClass);
	        						ObjectSearch search = schemaManager.getObjectSearchForObjectClass(deltaObjectClass);
	        						PerunBean freshBean = search.readPerunBeanById(event.getGroup().getId(), event.getMember().getId());
		        					if(freshBean == null) {
		        						LOG.info("No {0} with id {1}:{2} was found, skipping", "GroupMember", event.getGroup().getId(), event.getMember().getId());
		        						continue;
		        					}
	        						deltaBuilder.setObject(adapter.mapObject(deltaObjectClass, freshBean).build());

	        					} else {

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
		        					if(freshBean == null) {
		        						LOG.info("No {0} with id {1} was found, skipping", info.getObject().getBeanName(), info.getObject().getId());
		        						continue;
		        					}
	        						deltaBuilder.setObject(adapter.mapObject(info.getObjectClass(), freshBean).build());

	        					}

	        				} else {
	        					LOG.info("Unknown auditlog message {0}, skipping.", event.getName());
	        					continue;
	        				}

	        				deltaBuilder.setDeltaType(deltaType);

	        				LOG.info("Handling event as {0} of {1}:{2}", deltaType.toString(),
	        						deltaBuilder.getObjectClass() != null ? deltaBuilder.getObjectClass().getObjectClassValue() : "null", 
	        								deltaBuilder.getUid() != null ? deltaBuilder.getUid().getUidValue() : "null");

	        				handler.handle(deltaBuilder.build());

	        			} catch(HttpClientErrorException e) {
	        				LOG.warn("Exception from Perun operation: {0}", e.getMessage());
	        				continue;
	        			}
	        		} /* end for */
	        	} /* end if */ 
	        } /* end while loop */
	        
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
