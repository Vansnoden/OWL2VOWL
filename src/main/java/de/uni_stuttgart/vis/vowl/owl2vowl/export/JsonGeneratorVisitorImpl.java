/*
 * JsonGeneratorVisitor.java
 *
 */

package de.uni_stuttgart.vis.vowl.owl2vowl.export;

import de.uni_stuttgart.vis.vowl.owl2vowl.constants.Standard_Iris;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.data.VowlData;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.annotation.Annotation;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.nodes.classes.VowlClass;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.nodes.classes.VowlThing;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.nodes.datatypes.VowlDatatype;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.nodes.datatypes.VowlLiteral;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.properties.VowlDatatypeProperty;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.entities.properties.VowlObjectProperty;
import de.uni_stuttgart.vis.vowl.owl2vowl.model.individuals.VowlIndividual;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO Gemeinsamkeiten auslagern
 */
public class JsonGeneratorVisitorImpl implements JsonGeneratorVisitor {
	private final VowlData vowlData;
	private final Map<String, Object> root;
	private Map<String, Object> header;
	private Map<String, Object> metrics;
	private List<Object> namespace;
	private List<Object> _class;
	private List<Object> classAttribute;
	private List<Object> datatype;
	private List<Object> datatypeAttribute;
	private List<Object> objectProperty;
	private List<Object> objectPropertyAttribute;
	private Logger logger = LogManager.getLogger(JsonGeneratorVisitorImpl.class);

	public JsonGeneratorVisitorImpl(VowlData vowlData, Map<String, Object> root) {
		this.vowlData = vowlData;
		this.root = root;
		populateJsonRoot();
	}

	protected void populateJsonRoot() {
		header = new LinkedHashMap<>();
		metrics = new LinkedHashMap<>();
		namespace = new ArrayList<>();
		_class = new ArrayList<>();
		classAttribute = new ArrayList<>();
		datatype = new ArrayList<>();
		datatypeAttribute = new ArrayList<>();
		objectProperty = new ArrayList<>();
		objectPropertyAttribute = new ArrayList<>();

		root.put("header", header);
		root.put("namespace", namespace);
		root.put("metrics", metrics);
		root.put("class", _class);
		root.put("classAttribute", classAttribute);
		root.put("datatype", datatype);
		root.put("datatypeAttribute", datatypeAttribute);
		root.put("property", objectProperty);
		root.put("propertyAttribute", objectPropertyAttribute);

		namespace.add(new HashMap<>());
	}

	@Override
	public void visit(VowlThing vowlThing) {
		Map<String, Object> thingObject = new HashMap<>();
		thingObject.put("id", vowlData.getIdForEntity(vowlThing));
		thingObject.put("type", vowlThing.getType());

		_class.add(thingObject);

		Map<String, Object> thingAttributeObject = new HashMap<>();

		// TODO
		thingAttributeObject.put("id", vowlData.getIdForEntity(vowlThing));
		thingAttributeObject.put("label", 0);
		thingAttributeObject.put("iri", Standard_Iris.OWL_THING_CLASS_URI);

		classAttribute.add(thingAttributeObject);
	}

	@Override
	public void visit(VowlClass vowlClass) {
		Map<String, Object> classObject = new HashMap<>();
		classObject.put("id", vowlData.getIdForEntity(vowlClass));
		classObject.put("type", vowlClass.getType());

		_class.add(classObject);

		Map<String, Object> classAttributeObject = new HashMap<>();

		// TODO
		classAttributeObject.put("id", vowlData.getIdForEntity(vowlClass));
		classAttributeObject.put("label", getLabelsFromAnnotations(vowlClass.getAnnotations().getLabels()));
		classAttributeObject.put("iri", vowlClass.getIri().toString());
		classAttributeObject.put("description", getLabelsFromAnnotations(vowlClass.getAnnotations().getDescription()));
		classAttributeObject.put("comment", getLabelsFromAnnotations(vowlClass.getAnnotations().getComments()));
		// TODO nötig?
		classAttributeObject.put("isDefinedBy", 0);
		classAttributeObject.put("owlVersion", 0);
		classAttributeObject.put("superClasses", getListWithIds(vowlClass.getSuperEntities()));
		classAttributeObject.put("subClasses", getListWithIds(vowlClass.getSubEntities()));
		classAttributeObject.put("annotations", vowlClass.getAnnotations().getIdentifierToAnnotation());
		classAttributeObject.put("union", getListWithIds(vowlClass.getElementsOfUnion()));
		classAttributeObject.put("intersection", getListWithIds(vowlClass.getElementOfIntersection()));
		classAttributeObject.put("attributes", vowlClass.getAttributes());
		classAttributeObject.put("equivalent", getListWithIds(vowlClass.getEquivalentElements()));
		// TODO can a complement not be a list?
		classAttributeObject.put("complement", getListWithIds(vowlClass.getComplements()));
		classAttributeObject.put("instances", vowlClass.getInstances().size());
		classAttributeObject.put("individuals", createIndividualsJson(vowlClass.getIndividuals()));

		classAttribute.add(classAttributeObject);
	}

	private Object createIndividualsJson(Set<IRI> individuals) {
		List<Object> individualList = new ArrayList<>();

		for (IRI individualIri : individuals) {
			VowlIndividual individual = vowlData.getIndividualMap().get(individualIri);

			Map<String, Object> fields = new HashMap<>();
			fields.put("iri", individual.getIri().toString());
			fields.put("labels", getLabelsFromAnnotations(individual.getAnnotations().getLabels()));
			fields.put("description", getLabelsFromAnnotations(individual.getAnnotations().getDescription()));
			fields.put("comment", getLabelsFromAnnotations(individual.getAnnotations().getComments()));
			fields.put("annotations", individual.getAnnotations().getIdentifierToAnnotation());

			individualList.add(fields);
		}

		return individualList;
	}

	@Override
	public void visit(VowlLiteral vowlLiteral) {
		Map<String, Object> object = new HashMap<>();
		//object.put("id", vowlData.getIdForEntity(vowlLiteral));
		//object.put("type", vowlLiteral.getType());

		datatype.add(object);
	}

	@Override
	public void visit(VowlDatatype vowlDatatype) {
		Map<String, Object> object = new HashMap<>();
		//object.put("id", vowlData.getIdForEntity(vowlDatatype));
		//object.put("type", vowlDatatype.getType());

		datatype.add(object);
	}

	@Override
	public void visit(VowlObjectProperty vowlObjectProperty) {
		if (vowlObjectProperty.getDomains().isEmpty() || vowlObjectProperty.getRanges().isEmpty()) {
			logger.info("Domain or range is empty in object property: " + vowlObjectProperty);
			return;
		}

		Map<String, Object> object = new HashMap<>();
		object.put("id", vowlData.getIdForEntity(vowlObjectProperty));
		object.put("type", vowlObjectProperty.getType());

		objectProperty.add(object);

		Map<String, Object> propertyAttributes = new HashMap<>();
		propertyAttributes.put("domain", vowlData.getIdForIri(vowlObjectProperty.getJsonDomain()));
		propertyAttributes.put("range", vowlData.getIdForIri(vowlObjectProperty.getJsonRange()));
		propertyAttributes.put("id", vowlData.getIdForEntity(vowlObjectProperty));
		propertyAttributes.put("label", getLabelsFromAnnotations(vowlObjectProperty.getAnnotations().getLabels()));
		propertyAttributes.put("iri", vowlObjectProperty.getIri().toString());
		propertyAttributes.put("description", getLabelsFromAnnotations(vowlObjectProperty.getAnnotations().getDescription()));
		propertyAttributes.put("comment", getLabelsFromAnnotations(vowlObjectProperty.getAnnotations().getComments()));
		propertyAttributes.put("attributes", vowlObjectProperty.getAttributes());
		propertyAttributes.put("annotations", vowlObjectProperty.getAnnotations().getIdentifierToAnnotation());
		propertyAttributes.put("inverse", getIdForIri(vowlObjectProperty.getInverse()));
		propertyAttributes.put("superproperty", getListWithIds(vowlObjectProperty.getSuperEntities()));
		propertyAttributes.put("subproperty", getListWithIds(vowlObjectProperty.getSubEntities()));
		propertyAttributes.put("equivalent", getListWithIds(vowlObjectProperty.getEquivalentElements()));
		propertyAttributes.put("minCardinality", getCardinality(vowlObjectProperty.getMinCardinality()));
		propertyAttributes.put("maxCardinality", getCardinality(vowlObjectProperty.getMaxCardinality()));
		propertyAttributes.put("cardinality", getCardinality(vowlObjectProperty.getExactCardinality()));

		objectPropertyAttribute.add(propertyAttributes);
	}

	@Override
	public void visit(VowlDatatypeProperty vowlDatatypeProperty) {
		Map<String, Object> object = new HashMap<>();
		//object.put("id", vowlData.getIdForEntity(vowlDatatypeProperty));
		//object.put("type", vowlDatatypeProperty.getType());

		objectProperty.add(object);
	}

	@Override
	public void visit(VowlIndividual vowlIndividual) {

	}

	protected List<String> getListWithIds(Collection<IRI> iriList) {
		List<String> idList = iriList.stream().map(iri -> String.valueOf(vowlData.getIdForIri(iri))).collect(Collectors.toList());

		return idList;
	}

	protected String getIdForIri(IRI iri) {
		if (iri == null) {
			return null;
		}

		return vowlData.getIdForIri(iri);
	}

	protected Map<String, String> getLabelsFromAnnotations(Collection<Annotation> annotations) {
		Map<String, String> languageToValue = new HashMap<>();

		for (Annotation annotation : annotations) {
			languageToValue.put(annotation.getLanguage(), annotation.getValue());
		}

		return languageToValue;
	}

	/**
	 *
	 * @return Empty string if value is <= 0 else the value.
	 */
	protected String getCardinality(Integer value) {
		if (value <= 0) {
			return StringUtils.EMPTY;
		}

		return value.toString();
	}
}
