/**
 * Copyright (c) 2021 CESNET
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.metacentrum.perun.polygon.connector;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

/**
 * @author michal.vocu@gmail.com
 *
 */
@ConnectorClass(displayNameKey = "cz.metacentrum.perun.polygon.connector", configurationClass = PerunRPCConfiguration.class)
public class PerunRPCConnector 
implements PoolableConnector, TestOp, SchemaOp, SearchOp<Filter>, SyncOp, SchemaManager
{
	private static final Log LOG = Log.getLog(PerunRPCConnector.class);
	
	private PerunRPCConfiguration configuration = null;
	
	private PerunRPC perun = null;
	
	private Map<String, SchemaAdapter> mapObjectClassToSchemaAdapter = null;
	private Map<String, ObjectSearch> mapObjectClassToObjectSearch = null;
	
	@Override
	public void test() {
		perun.getAuthzResolver().getPerunPrincipal();
	}

	@Override
	public Schema schema() {
		
		SchemaBuilder schemaBuilder = new SchemaBuilder(PerunRPCConnector.class);

		mapObjectClassToSchemaAdapter.values().forEach(adapter -> {
			schemaBuilder.defineObjectClass(adapter.getObjectClass().build());
		});
		
		schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(), SearchOp.class);
		schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp.class);

		return schemaBuilder.build();
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public void init(Configuration cfg) {
	        LOG.info("Initializing {0} connector instance {1}", this.getClass().getSimpleName(), this);
	        this.configuration = (PerunRPCConfiguration) cfg;
	        final String[] passwordArray = { null };
		configuration.getPerunPassword().access(new GuardedString.Accessor() {
			
			@Override
			public void access(char[] clearChars) {
				passwordArray[0] = new String(clearChars);
			}
		});
	        this.perun = new PerunRPC(
	        		configuration.getPerunUrl(),
	        		configuration.getPerunUsername(),
	        		passwordArray[0]
	        		);

	        initSchemaAdapters();
	        initObjectSearch();
	}

	@Override
	public void dispose() {
		perun = null;
	}

	@Override
	public void checkAlive() {
		perun.getUtils().getPerunSystemTimeInMillis();
	}

	@Override
	public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
		// just pass the filter as is to the executeQuery method
		return new FilterTranslator<Filter>() {

			@Override
			public List<Filter> translate(Filter filter) {
				return Arrays.asList(filter);
			}
			
		};
	}

	@Override
	public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {

		ObjectSearch search = getObjectSearchForObjectClass(objectClass);
		
		if(search != null) {
			search.executeQuery(query, options, handler);
		} else {
			LOG.error("No method found to search for objectclass {0}", objectClass.getObjectClassValue());
		}
		
		return;
	}

	@Override
	public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
		SyncStrategy strategy = new AuditlogSyncStrategy(this, perun, configuration.getPerunConsumerName());
		strategy.sync(objectClass, token, handler, options);
	}

	@Override
	public SyncToken getLatestSyncToken(ObjectClass objectClass) {
		SyncStrategy strategy = new AuditlogSyncStrategy(this, perun, configuration.getPerunConsumerName());
		return strategy.getLatestSyncToken(objectClass);
	}

	private void initSchemaAdapters() {
		List<SchemaAdapter> adapters = Arrays.asList(
				new ExtSourceSchemaAdapter(perun),
				new UserSchemaAdapter(perun, configuration.getPerunNamespace()),
				new UserExtSchemaAdapter(perun),
				new VoSchemaAdapter(perun),
				new VoMemberSchemaAdapter(perun),
				new GroupSchemaAdapter(perun),
				new FacilitySchemaAdapter(perun),
				new GroupMemberSchemaAdapter(perun)
				);
		mapObjectClassToSchemaAdapter = new LinkedHashMap<>(); 
		adapters.stream().forEach(adapter -> { 
			mapObjectClassToSchemaAdapter.put(adapter.getObjectClassName(), adapter);
		});
		LOG.info("Initialized {0} schema adapters", mapObjectClassToSchemaAdapter.size());
	}
	
	private void initObjectSearch() {
		mapObjectClassToObjectSearch = new LinkedHashMap<>();
		for(Map.Entry<String, SchemaAdapter> entry : mapObjectClassToSchemaAdapter.entrySet()) {
			ObjectClass objectClass = new ObjectClass(entry.getKey());
			mapObjectClassToObjectSearch.put(
					entry.getKey(),
					createSearchForObjectClass(objectClass, entry.getValue()));
		}
		LOG.info("Initialized {0} object searchers", mapObjectClassToObjectSearch.size());
	}
	
	private ObjectSearch createSearchForObjectClass(ObjectClass objectClass, SchemaAdapter adapter) {
		ObjectSearch search = null;
		
		switch(objectClass.getObjectClassValue()) {

		case "ExtSource":
			search = new ExtSourceSearch(objectClass, adapter, perun);
			break;
			
		case "User":
			search = new UserSearch(objectClass, adapter, perun);
			break;

		case "UserExtSource":
			search = new UserExtSearch(objectClass, adapter, perun);
			break;
			
		case "VirtualOrganization":
			search = new VoSearch(objectClass, adapter, perun);
			break;
			
		case "Group":
			search = new GroupSearch(objectClass, adapter, perun);
			break;
			
		case "GroupMember":
			search = new GroupMemberSearch(objectClass, adapter, perun);
			break;
			
		case "VoMember":
			search = new VoMemberSearch(objectClass, adapter, perun);
			break;
			
		case "Facility":
			search = new FacilitySearch(objectClass, adapter, perun);
			break;
			
		default:
			break;
		}

		return search;
	}

	@Override
	public SchemaAdapter getSchemaAdapterForObjectClass(ObjectClass objectClass) {
		return mapObjectClassToSchemaAdapter.get(objectClass.getObjectClassValue());
	}
	
	@Override
	public ObjectSearch getObjectSearchForObjectClass(ObjectClass objectClass) {
		return mapObjectClassToObjectSearch.get(objectClass.getObjectClassValue());
	}
}
