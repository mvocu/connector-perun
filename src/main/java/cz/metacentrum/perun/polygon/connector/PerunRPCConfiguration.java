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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * @author michal.vocu@gmail.com
 *
 */
public class PerunRPCConfiguration extends AbstractConfiguration {

	private String perunUrl = null;
	
	private String perunUsername = null;
	
	private GuardedString perunPassword = null;
	
	private String perunNamespace = null;
	
	@ConfigurationProperty(required = true, order = 1)
	public String getPerunUrl() {
		return perunUrl;
	}

	public void setPerunUrl(String perunUrl) {
		this.perunUrl = perunUrl;
	}

	@ConfigurationProperty(required = true, order = 2)
	public String getPerunUsername() {
		return perunUsername;
	}

	public void setPerunUsername(String perunUsername) {
		this.perunUsername = perunUsername;
	}

	@ConfigurationProperty(required = true, order = 3)
	public GuardedString getPerunPassword() {
		return perunPassword;
	}

	public void setPerunPassword(GuardedString perunPassword) {
		this.perunPassword = perunPassword;
	}

	@ConfigurationProperty(required = true, order = 4)
	public String getPerunNamespace() {
		return perunNamespace;
	}

	public void setPerunNamespace(String perunNamespace) {
		this.perunNamespace = perunNamespace;
	}

	@Override
	public void validate() {
		// TODO Auto-generated method stub
		
	}

}
