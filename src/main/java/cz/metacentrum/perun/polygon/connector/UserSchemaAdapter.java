package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;
import java.util.Optional;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.RichUser;
import cz.metacentrum.perun.polygon.connector.rpc.model.UserExtSource;

public class UserSchemaAdapter extends SchemaAdapterBase {

	private static final Log LOG = Log.getLog(UserSchemaAdapter.class);

	private static final String NS_USER_ATTR_CORE = "urn:perun:user:attribute-def:core";
	private static final String NS_USER_ATTR_DEF = "urn:perun:user:attribute-def:def";
	private static final String NS_USER_ATTR_OPT = "urn:perun:user:attribute-def:opt";
	private static final String NS_USER_ATTR_VIRT = "urn:perun:user:attribute-def:virt";

	public static final String OBJECTCLASS_NAME = "User";
	
	private String namespace;
	private LinkedHashSet<String> attrNames = null;

	public UserSchemaAdapter(PerunRPC perun, String namespace) {
		super(perun);
		this.namespace = namespace;
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {
		
		attrNames.clear();
		
		// ----------------  User object class -----------------
		ObjectClassInfoBuilder user = new ObjectClassInfoBuilder();
		user.setType(OBJECTCLASS_NAME);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("user_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		user.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("user_login");
		name.setRequired(true);
		user.addAttributeInfo(name.build());
		
		// read User attribute definitions from Perun
		addAttributesFromNamespace(user, NS_USER_ATTR_CORE, attrNames);
		addAttributesFromNamespace(user, NS_USER_ATTR_DEF, attrNames);
		addAttributesFromNamespace(user, NS_USER_ATTR_VIRT, attrNames);
		addAttributesFromNamespace(user, NS_USER_ATTR_OPT, attrNames);

		return user;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		RichUser user = (RichUser)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(mapName(user));
		out.setUid(user.getId().toString());
		if(user.getUserAttributes() != null) {
			for(Attribute attr: user.getUserAttributes()) {
				out.addAttribute(mapAttribute(attr));
			}
		}
		return out;
	}

	private String mapName(RichUser user) {
		Optional<UserExtSource> ues = user.getUserExtSources().stream()
			.filter(ue -> { return ue.getExtSource().getName().equals(namespace); })
			.findFirst();
		String name = null;
		if(ues.isPresent()) {
			name = ues.get().getLogin();
		}
		if(name == null || name.isBlank()) {
			name = user.getUuid().toString();
		}
		return name; 
	}

}
