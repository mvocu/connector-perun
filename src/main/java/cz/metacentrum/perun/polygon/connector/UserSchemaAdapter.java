package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class UserSchemaAdapter extends SchemaAdapterBase {

	public static final String NS_USER_ATTR_CORE = "urn:perun:user:attribute-def:core";
	public static final String NS_USER_ATTR_DEF = "urn:perun:user:attribute-def:def";
	public static final String NS_USER_ATTR_OPT = "urn:perun:user:attribute-def:opt";
	public static final String NS_USER_ATTR_VIRT = "urn:perun:user:attribute-def:virt";

	private LinkedHashSet<String> attrNames = null;

	public UserSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {
		
		// ----------------  User object class -----------------
		ObjectClassInfoBuilder user = new ObjectClassInfoBuilder();
		user.setType("User");

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

}
