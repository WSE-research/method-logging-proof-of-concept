package com.example.demo;

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

import java.io.StringWriter;
import java.util.UUID;

@org.aspectj.lang.annotation.Aspect
public class Aspect {

    Logger logger = LoggerFactory.getLogger(Aspect.class);
    Model model;
    private String PROV_O_NAMESPACE = "http://www.w3.org/ns/prov#";
    private String SIO_NAMESPACE = "http://semanticscience.org/ontology/sio.owl#";
    private String QANARY_NAMESPACE = "";
    private Property rdfType = RDF.type;
    private Property wasGeneratedBy = model.createProperty(PROV_O_NAMESPACE, "wasGeneratedBy");
    private Property hasDataItem = model.createProperty(SIO_NAMESPACE, "hasDataItem");
    private Property hasExplanation = model.createProperty(QANARY_NAMESPACE, "hasExplanation");
    private Property subComponentOf = model.createProperty(QANARY_NAMESPACE, "subComponentOf");
    private Property rdfValue = model.createProperty(RDF.getURI(), "value");
    private Property annotatedAt = model.createProperty(OA.getURI(),"annotatedAt");
    private Property actedOnBehalfOf = model.createProperty(PROV_O_NAMESPACE, "actedOnBehalfOf");
    private String graph;

    public Aspect() {

    }

    public void setupModel() {
        this.graph = UUID.randomUUID().toString();
        this.model.setNsPrefix("rdf", RDF.getURI());
        this.model.setNsPrefix("qa", QANARY_NAMESPACE);
        this.model.setNsPrefix("prov", PROV_O_NAMESPACE);
        this.model.setNsPrefix("sio", SIO_NAMESPACE);
        this.model.setNsPrefix("oa", OA.getURI());
        VirtModel.openDatabaseModel(graph, "jdbc:virtuoso://localhost:1111", "dba", "dba");

    }

    @AfterReturning(value = "execution(* com.example..*(..))", returning = "result")
    public void logBeforeMethod(JoinPoint joinPoint, Object result) {
        setupModel();
        // Get the method name being executed
        String currentMethod = joinPoint.getSignature().getName();

        // Retrieve the stack trace to find the caller method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // The calling method should be at index 3 in the stack trace
        // (Index 0 is getStackTrace, 1 is this method, 2 is the current method, and 3 is the caller method)
        if (stackTrace.length > 3) {
            String callerMethod = stackTrace[3].getMethodName();
            System.out.println("Caller method: " + callerMethod);
        } else {
            System.out.println("No caller method found in the stack trace.");
        }

        Resource annotationId = model.createResource(UUID.randomUUID().toString());
        Resource method = model.createResource(currentMethod.toString());
        Resource callerMethod = model.createResource(stackTrace[3].getMethodName().toString().replace("<", "").replace(">", ""));

        // Create data
       // Bag dataBag = model.createBag();

        Resource inputData = model.createResource(RDF.li(1))
                .addProperty(rdfType, "INPUT")
                .addProperty(rdfValue, joinPoint.getArgs().toString());
/*
        Resource outputData = model.createResource()
                .addProperty(rdfType, "OUTPUT")
                .addProperty(rdfValue, result == null ? "null" : result.toString());        
        dataBag.add(inputData);
        dataBag.add(outputData);

 */

     
        Resource explanation = model.createResource()
                .addProperty(rdfType, QANARY_NAMESPACE + "TextualExplanation")
                .addProperty(wasGeneratedBy, "GPT_3.5")
                .addProperty(rdfValue, "randomExplanationWithUuid" + UUID.randomUUID().toString());

        annotationId.addProperty(rdfType, method)
                .addProperty(actedOnBehalfOf, callerMethod)
                .addProperty(hasExplanation, explanation)
                .addProperty(hasDataItem, inputData);

        StringWriter out = new StringWriter();
        model.write(out, "TURTLE");
        String ntriplesData = out.toString();
        logger.info("Model as string: {}", ntriplesData);
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
