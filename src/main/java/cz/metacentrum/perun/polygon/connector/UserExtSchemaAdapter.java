package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;

import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public class UserExtSchemaAdapter extends SchemaAdapterBase {

	public static final String NS_UES_ATTR_CORE = "urn:perun:ues:attribute-def:core";
	public static final String NS_UES_ATTR_DEF = "urn:perun:ues:attribute-def:def";
	public static final String NS_UES_ATTR_OPT = "urn:perun:ues:attribute-def:opt";
	public static final String NS_UES_ATTR_VIRT = "urn:perun:ues:attribute-def:virt";
	
	private LinkedHashSet<String> attrNames = null;

	public UserExtSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {
		// ----------------  UserExtSource object class -----------------
		ObjectClassInfoBuilder user = new ObjectClassInfoBuilder();
		user.setType("UserExtSource");
		//user.setAuxiliary(true);

		// remap __UID__ attribute
		AttributeInfoBuilder uid = new AttributeInfoBuilder(Uid.NAME, String.class);
		uid.setNativeName("ues_id");
		uid.setRequired(false);
		uid.setCreateable(false);
		uid.setUpdateable(false);
		uid.setReadable(true);
		user.addAttributeInfo(uid.build());

		// remap __NAME__ attribute
		AttributeInfoBuilder name = new AttributeInfoBuilder(Name.NAME, String.class);
		name.setNativeName("ues_uid");
		name.setRequired(true);
		user.addAttributeInfo(name.build());
		
		// read User attribute definitions from Perun
		addAttributesFromNamespace(user, NS_UES_ATTR_CORE, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_DEF, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_OPT, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_VIRT, attrNames);
		
		return user;
	}

}
