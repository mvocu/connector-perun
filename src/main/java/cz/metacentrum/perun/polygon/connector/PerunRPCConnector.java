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
import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

/**
 * @author michal.vocu@gmail.com
 *
 */
@ConnectorClass(displayNameKey = "cz.metacentrum.perun.polygon.connector", configurationClass = PerunRPCConfiguration.class)
public class PerunRPCConnector 
implements PoolableConnector, TestOp, SchemaOp, SearchOp<Filter>
{
	private static final Log LOG = Log.getLog(PerunRPCConnector.class);
	
	private PerunRPCConfiguration configuration = null;
	
	private PerunRPC perun = null;
	
	@Override
	public void test() {
		perun.getAuthzResolver().getPerunPrincipal();
	}

	@Override
	public Schema schema() {
		
		SchemaBuilder schemaBuilder = new SchemaBuilder(PerunRPCConnector.class);

		schemaBuilder.defineObjectClass((new UserSchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new UserExtSchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new VoSchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new VoMemberSchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new GroupSchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new FacilitySchemaAdapter(perun)).getObjectClass().build());
		schemaBuilder.defineObjectClass((new GroupMemberSchemaAdapter(perun)).getObjectClass().build());
		
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
	}

	@Override
	public void dispose() {
		perun = null;
	}

	@Override
	public void checkAlive() {
		perun.getUtils().getPerunRPCVersion();
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

		ObjectSearch search = null;
		
		switch(objectClass.getObjectClassValue()) {

		case "User":
			search = new UserSearch(objectClass, perun, configuration.getPerunNamespace());
			break;

		case "VirtualOrganization":
			search = new VoSearch(objectClass, perun);
			break;
			
		case "Group":
			search = new GroupSearch(objectClass, perun);
			break;
			
		case "GroupMember":
			search = new GroupMemberSearch(objectClass, perun);
			break;
			
		case "VoMember":
			search = new VoMemberSearch(objectClass, perun);
			break;
			
		case "Facility":
			search = new FacilitySearch(objectClass, perun);
			break;
			
		default:
			break;
		}
		if(search != null) {
			search.executeQuery(query, options, handler);
		}
		
		return;
	}

}
