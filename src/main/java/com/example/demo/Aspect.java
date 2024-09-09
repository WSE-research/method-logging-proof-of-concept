package com.example.demo;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorVirtuoso;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.BagImpl;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFSyntax;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jena.driver.VirtModel;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

@org.aspectj.lang.annotation.Aspect
public class Aspect {

    private QanaryTripleStoreConnectorVirtuoso qanaryTripleStoreConnectorVirtuoso = new QanaryTripleStoreConnectorVirtuoso("jdbc:virtuoso://localhost:1111", "dba", "dba", 10);
    Logger logger = LoggerFactory.getLogger(Aspect.class);
    private String PROV_O_NAMESPACE = "http://www.w3.org/ns/prov#";
    private String SIO_NAMESPACE = "http://semanticscience.org/ontology/sio.owl#";
    private String QANARY_NAMESPACE = "http://qanary#";
    private Property rdfType = RDF.type;
    private Property wasGeneratedBy = ResourceFactory.createProperty(PROV_O_NAMESPACE, "wasGeneratedBy");
    private Property hasDataItem = ResourceFactory.createProperty(SIO_NAMESPACE, "hasDataItem");
    private Property hasExplanation = ResourceFactory.createProperty(QANARY_NAMESPACE, "hasExplanation");
    private Property rdfValue = ResourceFactory.createProperty(RDF.getURI(), "value");
    private Property annotatedAt = ResourceFactory.createProperty(OA.getURI(),"annotatedAt");
    private Property actedOnBehalfOf = ResourceFactory.createProperty(PROV_O_NAMESPACE, "actedOnBehalfOf");
    private String graph;

    public Aspect() {
    }

    public void setupModel() {
    }

    @AfterReturning(value = "execution(* com.example..*(..))", returning = "result")
    public void logBeforeMethod(JoinPoint joinPoint, Object result) throws IOException, SparqlQueryFailed {
        // Create an empty model
        Model model = ModelFactory.createDefaultModel();

        // Retrieve the stack trace to find the caller method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Create resources and properties
        Resource annotationId = model.createResource(UUID.randomUUID().toString());
        Resource explanationType = model.createResource("TextualExplanation");
        Resource callerMethodClass = model.createResource(stackTrace[3].getMethodName());

        // Create a blank node for the explanation
        Resource explanationNode = model.createResource()
                .addProperty(RDF.type, explanationType)
                .addProperty(RDF.value, "Some explanation")
                .addProperty(wasGeneratedBy,"GPT3.5");

        Resource inputData = model.createResource(RDF.li(1))
                .addProperty(rdfType, "INPUT")
                .addProperty(rdfValue, joinPoint.getArgs().toString());

        Resource outputData = model.createResource()
                .addProperty(rdfType, "OUTPUT")
                .addProperty(rdfValue, result == null ? "null" : result.toString());

        // Create a blank node for the Bag
        Resource bagNode = model.createResource();  // This is a blank node

        // Add items to the Bag (using the blank node as subject)
        bagNode.addProperty(RDF.type, RDF.Bag);
        bagNode.addProperty(RDF.li(1), inputData);
        bagNode.addProperty(RDF.li(2), outputData);

        // Add the blank node as the object of another triple
        annotationId.addProperty(RDF.type, joinPoint.getSignature().getName())
                .addProperty(actedOnBehalfOf, callerMethodClass.toString().replace("<", "").replace(">", ""))
                .addProperty(hasExplanation, explanationNode)
                .addProperty(hasDataItem, bagNode);  // The blank node is reused here

        // Optionally print the model in RDF/XML or TTL for debugging purposes
        StringWriter writer = new StringWriter();
        model.write(writer, "TTL");
        String ttlData = writer.toString();
        String query = QanaryTripleStoreConnector.readFileFromResources("/insert_explanation.rq").replace("?g", "<urn:graph:" + UUID.randomUUID().toString() + ">").replace("?DATA", ttlData);
        qanaryTripleStoreConnectorVirtuoso.update(query);
    }


    /**
     ?annotationId  rdf:type METHOD ;
     qa:subComponentOf
     qa:hasExplanation [
     a qa:TextualExplanation ;
     prov:wasGeneratedBy ?generatedBy ;
     rdf:value VALUE
     ] ;
     sio:hasDataItem ITEM .
     */

    /**
     ?annotationId  eo:addresses qa:questionId ;
     rdf:value VALUE ;
     prov:wasGeneratedBy GENERATOR ;
     sio:hasDataItem [
     a eo:ObjectRecord ;
     rdf:value DATA
     ] ;
     XX:wasCalledBy CALLER          // Method ontology?
     */



}
