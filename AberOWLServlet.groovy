import java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import uk.ac.aber.lus11.sparqowlapi.util.*;


int k = 500 ; // for SuggestTree
int maxLength = 10000 ; // for SuggestTree
OWLOntologyManager oManager;
SuggestTree allLabels = new SuggestTree(k, new HashMap<String, Integer>());
Map<String, Set<String>> allLabels2id = new HashMap<>() ;
Map<String, SuggestTree> labels = new LinkedHashMap<>(); 
Map<String, Map<String, Set<String>>> labels2id = new LinkedHashMap<>(); // ontUri -> label -> OWLClassIRI
Map<String, OWLOntology> ontologies = new LinkedHashMap<>();
List<OWLAnnotationProperty> aProperties = new ArrayList<>();
Map<String, QueryEngine> queryEngines = new LinkedHashMap<>();
Map<String, OWLOntologyManager> ontologyManagers = new LinkedHashMap<>();
OWLDataFactory df = OWLManager.getOWLDataFactory() ;
    
List<String> oList = new ArrayList<>();
        
Path filePath = new File(ontologyDescription).toPath();
Charset charset = Charset.defaultCharset();

oList = Files.readAllLines(filePath, charset);
        
loadOntologies(oList);
loadAnnotations();
loadLabels();
createReasoner();

    
Set<String> listOntologies() {
  return ontologies.keySet() ;
}
    
Set<String> queryNames(String query, String ontUri) {
  query = query.toLowerCase() ;
  Set<String> results = new LinkedHashSet<>() ;
  SuggestTree tree = null ;
  if (ontUri == null || ontUri.length()==0) { // query allLabels
    tree = allLabels ;
  } else { // query ontUri
    tree = labels.get(ontUri) ;
  }
  if (tree !=null) {
    SuggestTree.Node n = tree.autocompleteSuggestionsFor(query) ;
    if (n != null) {
      for (int i = 0 ; i < n.listLength() ; i++) {
	String elem = n.listElement(i) ;
	String elemForOWL ;
	if (elem.indexOf(" ")>-1) {
	  elemForOWL = "'"+elem+"'";
	} else {
	  elemForOWL = elem ;
	}
	Map<String, Set<String>> s2id = null ;
	if  (ontUri == null || ontUri.length()==0) {
	  s2id = allLabels2id ;
	} else {
	  s2id = labels2id.get(ontUri) ;
	}
	for (String id : s2id.get(elem)) {
	  results.add(elemForOWL) ;
	}
      }
    }
  }
  return results ;
}

void loadLabels() {
  for (String uri : ontologies.keySet()) {
    labels.put(uri, new SuggestTree(k, new HashMap<String, Integer>())) ;
    labels2id.put(uri, new LinkedHashMap<String, Set<String>>()) ;
    OWLOntology ont = ontologies.get(uri) ;
    for (OWLOntology o : ont.getImportsClosure()) {
      for (OWLClass c : o.getClassesInSignature(true)) {
	String classIRI = c.getIRI().toString() ;
	for (OWLAnnotation annotation : c.getAnnotations(o, df.getRDFSLabel())) {
	  if (annotation.getValue() instanceof OWLLiteral) {
	    OWLLiteral val = (OWLLiteral) annotation.getValue();
	    String label = val.getLiteral() ;
	    label = label.toLowerCase() ;
	    try {
	      allLabels.insert(label, maxLength - label.length()) ;
	    } catch (Exception E) {}
	    if (allLabels2id.get(label) == null) {
	      allLabels2id.put(label, new LinkedHashSet<String>()) ;
	    }
	    allLabels2id.get(label).add(c.getIRI().toString()) ;
	    if (labels2id.get(uri).get(label) == null) {
	      labels2id.get(uri).put(label, new LinkedHashSet<String>()) ;
	    }
	    labels2id.get(uri).get(label).add(c.getIRI().toString()) ;
	    try {
	      labels.get(uri).insert(label, maxLength - label.length()) ;
	    } catch (Exception E) {}
	  }
	}
      }                                                                                                                                          
      for (OWLObjectProperty c : o.getObjectPropertiesInSignature(true)) {
	String classIRI = c.getIRI().toString() ;
	for (OWLAnnotation annotation : c.getAnnotations(o, df.getRDFSLabel())) {
	  if (annotation.getValue() instanceof OWLLiteral) {
	    OWLLiteral val = (OWLLiteral) annotation.getValue();
	    String label = val.getLiteral() ;
	    label = label.toLowerCase() ;
	    try {
	      allLabels.insert(label, maxLength - label.length()) ;
	    } catch (Exception E) {}
	    if (allLabels2id.get(label) == null) {
	      allLabels2id.put(label, new LinkedHashSet<String>()) ;
	    }
	    allLabels2id.get(label).add(c.getIRI().toString()) ;
			    
	    try {
	      labels.get(uri).insert(label, maxLength - label.length()) ;
	    } catch (Exception E) {}
	    if (labels2id.get(uri).get(label) == null) {
	      labels2id.get(uri).put(label, new LinkedHashSet<String>()) ;
	    }
	    labels2id.get(uri).get(label).add(c.getIRI().toString()) ;
	  }
	}
      }                                                                                                                                          
    }                                                                                                                                            
  }
}

/**
 * Create the ontology manager and load it with the given ontology.
 * 
 * @param ontologyLink URI to the OWL ontology to be queried.
 * @throws OWLOntologyCreationException 
 */
void loadOntologies(List<String> oList) throws OWLOntologyCreationException, IOException {
        
  for(String oListString : oList) {
    IRI iri = IRI.create(oListString);
    try {
      this.oManager = OWLManager.createOWLOntologyManager();
      OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration() ;
      config.setFollowRedirects(true) ;
      config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT) ;
      OWLOntology ontology = this.oManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(iri), config);
      this.ontologies.put(oListString, ontology);
      this.ontologyManagers.put(oListString, this.oManager) ;
    } catch (OWLOntologyAlreadyExistsException E) {
      // do nothing
    } catch (Exception E) {
      E.printStackTrace() ;
    }
	    
  }
}
    
/**
 * Create and run the reasoning on the loaded OWL ontologies, creating a QueryEngine for each.
 */
void createReasoner() {
  List<String> langs = new ArrayList<>();
  Map<OWLAnnotationProperty, List<String>> preferredLanguageMap = new HashMap<>();
  for (OWLAnnotationProperty annotationProperty : this.aProperties) {
    preferredLanguageMap.put(annotationProperty, langs);
  }
        
  OWLReasonerFactory reasonerFactory = new ElkReasonerFactory(); // May be replaced with any reasoner using the standard interface
  Iterator<String> it = this.ontologies.keySet().iterator();
  while (it.hasNext()) {
    String oListString = it.next() ;
    OWLOntology ontology = ontologies.get(oListString) ;
    OWLOntologyManager manager = ontologyManagers.get(oListString) ;
    OWLReasoner oReasoner = reasonerFactory.createReasoner(ontology);
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    NewShortFormProvider sForm = new NewShortFormProvider(aProperties, preferredLanguageMap, manager);
    this.queryEngines.put(oListString, new QueryEngine(oReasoner, sForm));
  }
}
    
/**
 * Create list of RDFS_LABEL annotations to be used by the ShortFormProvider.
 */
void loadAnnotations() {
  OWLDataFactory factory = oManager.getOWLDataFactory();
  OWLAnnotationProperty rdfsLabel = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()) ;                                                       
  aProperties.add(rdfsLabel);
  aProperties.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym"))) ;
  aProperties.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym"))) ;
  aProperties.add(factory.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym"))) ;
}

Set<MyOWLClassInformation> classes2info(Set<OWLClass> classes, OWLOntology o, String uri) {
  Set<MyOWLClassInformation> result = new HashSet<>();
  for (OWLClass c : classes) {
    MyOWLClassInformation info = new MyOWLClassInformation() ;
    info.owlClass = c ;
    info.classURI = c.getIRI().toString() ;
    info.ontologyURI = uri ;
    for (OWLOntology ont : o.getImportsClosure()) {
      for (OWLAnnotation annotation : c.getAnnotations(ont, df.getRDFSLabel())) {
	if (annotation.getValue() instanceof OWLLiteral) {
	  OWLLiteral val = (OWLLiteral) annotation.getValue();
	  info.label = val.getLiteral() ;
	}
      }
      for (OWLAnnotation annotation : c.getAnnotations(o, df.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000115")))) {
	if (annotation.getValue() instanceof OWLLiteral) {
	  OWLLiteral val = (OWLLiteral) annotation.getValue();
	  info.definition = val.getLiteral() ;
	}
      }
    }
    /* definition */
    result.add(info);
  }
  return result ;
}

/**
 * Iterate the query engines, collecting results from each and collating them into a single structure.
 * 
 * @param mOwlQuery Class query in Manchester OWL Syntax.
 * @param requestType Type of class match to be performed. Valid values are: subclass, superclass, equivalent or all.
 * @return Set of OWL Classes.
 */
Set<MyOWLClassInformation> runQuery(String mOwlQuery, RequestType requestType, String ontUri) {
  Set<MyOWLClassInformation> classes = new HashSet<>();
  if (ontUri == null || ontUri.length() == 0) { // query all the ontologies in the repo
    Iterator<String> it = queryEngines.keySet().iterator() ;
    while (it.hasNext()) {
      String oListString = it.next() ;
      QueryEngine queryEngine = queryEngines.get(oListString) ;
      OWLOntology ontology = ontologies.get(oListString) ;
      try {
	Set<OWLClass> resultSet = queryEngine.getClasses(mOwlQuery, requestType) ;
	resultSet.remove(df.getOWLNothing()) ;
	resultSet.remove(df.getOWLThing()) ;
	classes.addAll(classes2info(resultSet, ontology, oListString)) ;
      } catch (org.semanticweb.owlapi.expression.ParserException E) { }
    }
  } else if (queryEngines.get(ontUri) == null) { // download the ontology and query
    Map<OWLAnnotationProperty, List<String>> preferredLanguageMap = new HashMap<>();
    for (OWLAnnotationProperty annotationProperty : this.aProperties) {
      preferredLanguageMap.put(annotationProperty, new ArrayList<String>());
    }
    try {
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager() ;
      OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration() ;                                                                            
      config.setFollowRedirects(true) ;                                                                                                                         
      config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT) ;                                                                           
      OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontUri)), config);
      OWLReasonerFactory reasonerFactory = new ElkReasonerFactory(); 
      OWLReasoner oReasoner = reasonerFactory.createReasoner(ontology);
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
      NewShortFormProvider sForm = new NewShortFormProvider(aProperties, preferredLanguageMap, manager);
      Set<OWLClass> resultSet = new QueryEngine(oReasoner, sForm).getClasses(mOwlQuery, requestType) ;
      resultSet.remove(df.getOWLNothing()) ;
      resultSet.remove(df.getOWLThing()) ;
      classes.addAll(classes2info(resultSet, ontology, ontUri)) ;
    } catch (OWLOntologyCreationException E) {
      E.printStackTrace() ;
    }
  } else { // query one single ontology
    QueryEngine queryEngine = queryEngines.get(ontUri) ;
    OWLOntology ontology = ontologies.get(ontUri) ;
    try {
      Set<OWLClass> resultSet = queryEngine.getClasses(mOwlQuery, requestType) ;
      resultSet.remove(df.getOWLNothing()) ;
      resultSet.remove(df.getOWLThing()) ;
      classes.addAll(classes2info(resultSet, ontology, ontUri)) ;
    } catch (org.semanticweb.owlapi.expression.ParserException E) { 
      E.printStackTrace() ; 
    }
  }
  return classes;
}
    
Map<String, QueryEngine> getQueryEngines() {
  return this.queryEngines;
}

/**
 * @return the oManager
 */
OWLOntologyManager getoManager() {
  return oManager;
}

/**
 * @return the ontologies
 */
Map<String, OWLOntology> getOntologies() {
  return ontologies;
}

