package cz.metacentrum.perun.polygon.connector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsIgnoreCaseFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

import cz.metacentrum.perun.polygon.connector.rpc.PerunRPC;

public abstract class ObjectSearchBase implements ObjectSearch {

	private static final Log LOG = Log.getLog(ObjectSearchBase.class);

	protected PerunRPC perun;
	protected ObjectClass objectClass;
	protected SchemaAdapter schemaAdapter;
	
	public ObjectSearchBase(ObjectClass objectClass, SchemaAdapter schemaAdapter, PerunRPC perun) {
		this.objectClass = objectClass;
		this.schemaAdapter = schemaAdapter;
		this.perun = perun;
	}
	
	private class SQLFilterVisitor implements FilterVisitor<String, Map<String, Object>> {

		protected String addParameter(final Map<String, Object> params, final Attribute attribute) {
			String name = attribute.getName() + params.size();
			return (String) params.put(name, AttributeUtil.getSingleValue(attribute));
		}

		protected String visitCompositeFilter(final CompositeFilter filter, String operator) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Filter subFilter : filter.getFilters()) {
				if (first) {
					sb.append("( ").append(subFilter.accept(this, null)).append(" )");
					first = false;
				} else {
					sb
					.append(" ")
					.append(operator)
					.append(" ( ")
					.append(subFilter.accept(this, null))
					.append(" )");
				}
			}
			return sb.toString();
		}

		protected boolean isAlwaysTrue(AttributeFilter filter) {
			return 
				null == filter.getAttribute() || 
				null == filter.getAttribute().getValue() || 
				filter.getAttribute().getValue().isEmpty();
		}


		@Override
		public String visitNotFilter(Map<String, Object> params, NotFilter filter) {
			return "NOT ( " + filter.getFilter().accept(this, null) + " )";
		}

		@Override
		public String visitAndFilter(Map<String, Object> params, AndFilter filter) {
			return visitCompositeFilter(filter, "AND");
		}

		@Override
		public String visitOrFilter(Map<String, Object> params, OrFilter filter) {
			return visitCompositeFilter(filter, "OR");
		}

		@Override
		public String visitContainsFilter(Map<String, Object> params, ContainsFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} LIKE :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitStartsWithFilter(Map<String, Object> params, StartsWithFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} LIKE CONCAT(:" 
						+ addParameter(params, filter.getAttribute()) + ", '%')";
			}
		}

		@Override
		public String visitEndsWithFilter(Map<String, Object> params, EndsWithFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} LIKE CONCAT('%',:" 
						+ addParameter(params, filter.getAttribute()) + ")";
			}
		}

		@Override
		public String visitEqualsFilter(Map<String, Object> params, EqualsFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} = :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitEqualsIgnoreCaseFilter(Map<String, Object> params, EqualsIgnoreCaseFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "UPPER(${" + filter.getName() + "}) = UPPER(:" 
						+ addParameter(params, filter.getAttribute()) + ")";
			}
		}

		@Override
		public String visitGreaterThanFilter(Map<String, Object> params, GreaterThanFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} > :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitGreaterThanOrEqualFilter(Map<String, Object> params, GreaterThanOrEqualFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} >= :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitLessThanFilter(Map<String, Object> params, LessThanFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} < :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitLessThanOrEqualFilter(Map<String, Object> params, LessThanOrEqualFilter filter) {
			if (isAlwaysTrue(filter)) {
				return "TRUE";
			} else {
				return "${" + filter.getName() + "} <= :" + addParameter(params, filter.getAttribute());
			}
		}

		@Override
		public String visitContainsAllValuesFilter(Map<String, Object> stringObjectMap, ContainsAllValuesFilter filter) {
			throw new RuntimeException("ContainsAll Filter translation is not supported to SQL");
		}

		@Override
		public String visitExtendedFilter(Map<String, Object> stringObjectMap, Filter filter) {
			throw new RuntimeException("Extended Filter translation is not supported to SQL");
		}
		
	}
	
	@Override
	public abstract void executeQuery(Filter filter, OperationOptions options, ResultsHandler handler);
	
	protected String createSQLQuery(Filter filter, Map<String, String> attrMap) {
		String query;
		Map<String, Object> params = new LinkedHashMap<>();
		SQLFilterVisitor visitor = new SQLFilterVisitor();
		query = filter.accept(visitor, params);
		return query;
	}
}
