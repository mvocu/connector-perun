package cz.metacentrum.perun.polygon.connector;

import java.util.LinkedHashSet;
import java.util.List;

import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;
import cz.metacentrum.perun.polygon.connector.rpc.model.Attribute;
import cz.metacentrum.perun.polygon.connector.rpc.model.UserExtSource;

public class UserExtSchemaAdapter extends SchemaAdapterBase {

	private static final String NS_UES_ATTR_CORE = "urn:perun:ues:attribute-def:core";
	private static final String NS_UES_ATTR_DEF = "urn:perun:ues:attribute-def:def";
	private static final String NS_UES_ATTR_OPT = "urn:perun:ues:attribute-def:opt";
	private static final String NS_UES_ATTR_VIRT = "urn:perun:ues:attribute-def:virt";
	
	public static final String OBJECTCLASS_NAME = "UserExtSource";
	
	private LinkedHashSet<String> attrNames = null;

	public UserExtSchemaAdapter(PerunRPC perun) {
		super(perun);
		attrNames = new LinkedHashSet<>();
	}

	@Override
	public ObjectClassInfoBuilder getObjectClass() {

		attrNames.clear();
		
		// ----------------  UserExtSource object class -----------------
		ObjectClassInfoBuilder user = new ObjectClassInfoBuilder();
		user.setType(OBJECTCLASS_NAME);
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
		
		// login attribute
		AttributeInfoBuilder login = new AttributeInfoBuilder("ues_login", String.class);
		login.setNativeName("urn:perun:ues:attribute-def:ues_login");
		login.setRequired(true);
		login.setCreateable(false);
		login.setUpdateable(false);
		login.setReadable(true);
		user.addAttributeInfo(login.build());

		// loa attribute
		AttributeInfoBuilder loa = new AttributeInfoBuilder("ues_loa", Integer.class);
		loa.setNativeName("urn:perun:ues:attribute-def:ues_loa");
		loa.setRequired(true);
		loa.setCreateable(false);
		loa.setUpdateable(false);
		loa.setReadable(true);
		user.addAttributeInfo(loa.build());

		// read User attribute definitions from Perun
		addAttributesFromNamespace(user, NS_UES_ATTR_CORE, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_DEF, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_OPT, attrNames);
		addAttributesFromNamespace(user, NS_UES_ATTR_VIRT, attrNames);
		
		return user;
	}

	@Override
	public String getObjectClassName() {
		return OBJECTCLASS_NAME;
	}

	@Override
	public ConnectorObjectBuilder mapObject(ObjectClass objectClass, Object source) {
		UserExtSource userExtSource = (UserExtSource)source;
		ConnectorObjectBuilder out = new ConnectorObjectBuilder();
		out.setObjectClass(objectClass);
		out.setName(userExtSource.getUserId().toString() + ":" + userExtSource.getExtSource().getId().toString());
		out.setUid(userExtSource.getId().toString());
		// ues_login
		AttributeBuilder ab = new AttributeBuilder();
		ab.setName("ues_login");
		ab.addValue(userExtSource.getLogin());
		out.addAttribute(ab.build());
		// ues_loa
		ab = new AttributeBuilder();
		ab.setName("ues_loa");
		ab.addValue(userExtSource.getLoa());
		out.addAttribute(ab.build());
		// defined attributes
		List<Attribute> attrs = perun.getAttributesManager().getUserExtSourceAttributes(userExtSource.getId());
		if(attrs != null) {
			for(Attribute attr: attrs) {
				out.addAttribute(mapAttribute(attr));
			}
		}

		return out;
	}

}
