package com.example.demo;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorVirtuoso;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Stack;
import java.util.UUID;

@org.aspectj.lang.annotation.Aspect
public class Aspect {

    private String virtuosoUrl = "jdbc:virtuoso://localhost:1111";
    private String virtuosoUser = "dba";
    private String virtuosoPassword = "dba";

    private QanaryTripleStoreConnectorVirtuoso qanaryTripleStoreConnectorVirtuoso = new QanaryTripleStoreConnectorVirtuoso(virtuosoUrl, virtuosoUser, virtuosoPassword, 10);
    Logger logger = LoggerFactory.getLogger(Aspect.class);

    // Namespaces
    private String PROV_O_NAMESPACE = "http://www.w3.org/ns/prov#";
    private String SIO_NAMESPACE = "http://semanticscience.org/ontology/sio.owl#";
    private String QANARY_NAMESPACE = "http://qanary#";
    private String PLACEHOLDER_NAMESPACE = "http://placeholder#";

    // Properties
    private Property rdfType = RDF.type;
    private Property wasGeneratedBy = ResourceFactory.createProperty(PROV_O_NAMESPACE, "wasGeneratedBy");
    private Property inputProperty = ResourceFactory.createProperty(SIO_NAMESPACE, "input");
    private Property outputProperty = ResourceFactory.createProperty(SIO_NAMESPACE, "output");
    private Property hasExplanation = ResourceFactory.createProperty(QANARY_NAMESPACE, "hasExplanation");
    private Property rdfValue = ResourceFactory.createProperty(RDF.getURI(), "value");
    private Property annotatedAt = ResourceFactory.createProperty(OA.getURI(),"annotatedAt");
    private Property actedOnBehalfOf = ResourceFactory.createProperty(PROV_O_NAMESPACE, "actedOnBehalfOf");
    private Property wasGeneratedAt = ResourceFactory.createProperty(PLACEHOLDER_NAMESPACE, "wasGeneratedAt");
    private Property score = ResourceFactory.createProperty(PLACEHOLDER_NAMESPACE, "score");
    private String graph = "";
    private Stack<Object> stack = new Stack<>();

    public void readGraph() throws IOException {
        try {
            this.graph = Files.readAllLines(Paths.get("graph"), StandardCharsets.UTF_8).get(0);
            Files.delete(Paths.get("graph"));
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Before(value = "execution(* com.example.demo.Service..*(..))")
    public void storeMethodExecutionOnBegin(JoinPoint joinPoint) throws Throwable {
        String uuid = UUID.randomUUID().toString();
        if(this.stack.empty())
            readGraph();
        Object caller = this.stack.empty() ? "Caller" : this.stack.peek();
        this.stack.push(uuid);
        LocalDateTime time = LocalDateTime.now();
        String query = QanaryTripleStoreConnector.readFileFromResources("/store_method_call_before.rq");
        query = query.replace("?g", "<" + ResourceFactory.createResource(this.graph).toString() + ">");
        query = query.replace("?type", "\"" + ResourceFactory.createPlainLiteral(joinPoint.getSignature().getName()) + "\"");
        query = query.replace("?methodInstance", "<" + ResourceFactory.createResource(uuid).toString() + ">");
        query = query.replace("?parent", "<" + ResourceFactory.createResource(caller.toString()).toString() + ">");
        query = query.replace("?startedAtTime", "\"" + time.toString() + "\"");
        query = query.replace("?input", createInputBagAsResource(joinPoint.getArgs()));
        qanaryTripleStoreConnectorVirtuoso.update(query);
    }

    @AfterReturning(value = "execution(* com.example.demo.Service..*(..))", returning = "result")
    public void storeMethodExecutionAfterReturn(JoinPoint joinPoint, Object result) throws IOException, SparqlQueryFailed {
        String uuid = (String) this.stack.peek();
        LocalDateTime time = LocalDateTime.now();
        String query = QanaryTripleStoreConnector.readFileFromResources("/store_method_call_after_return.rq");
        query = query.replace("?g", "<" + this.graph + ">");
        query = query.replace("?methodInstance", "<" + ResourceFactory.createResource(uuid).toString() + ">");
        query = query.replace("?explanation", createExplanationResource());
        query = query.replace("?endedAtTime", "\"" + time.toString() + "\"");
        String output = createOutputBagAsResource(result);
        if (output == null) {
            query = query.replace("x:output ?output ;", "");
        }
        else {
            query = query.replace("?output", output);
        }
        qanaryTripleStoreConnectorVirtuoso.update(query);
        this.stack.pop();
        if(!this.stack.empty()) {
            if (this.stack.peek() == "Caller")
                this.stack.pop();
        }
    }

    public String createExplanationResource() {
        Model model = ModelFactory.createDefaultModel();
        model.createResource()
                .addProperty(RDF.value, "Some explanation")
                .addProperty(wasGeneratedBy,"GPT3.5")
                .addProperty(wasGeneratedAt, "Time_placeholder");
        String explanationString = writeModelToTurtleString(model);
        return explanationString.substring(0, explanationString.lastIndexOf("."));
    }

    public String createInputBagAsResource(Object[] args) {
        Model model = ModelFactory.createDefaultModel();
        model.createResource();
        Resource input = model.createResource();
        input.addProperty(rdfType, RDF.Bag);
        for (int i = 0; i < args.length; i++) {
            input.addProperty(
                    RDF.li(i+1),
                    model.createResource().addProperty(rdfType, args[i].getClass().toString()).addProperty(rdfValue, args[i].toString())
            );
        }
        String inputbag = writeModelToTurtleString(model);
        return inputbag.substring(0, inputbag.lastIndexOf("."));
    }

    public String createOutputBagAsResource(Object result) {
        Model model = ModelFactory.createDefaultModel();
        model.createResource();
        Resource output = model.createResource();
        output.addProperty(rdfType, RDF.Bag);
        try {
            output.addProperty(RDF.li(1),model.createResource().addProperty(rdfType,result.getClass().toString()).addProperty(rdfValue, result.toString()));
        } catch (NullPointerException e) {
            return null;
        }
        String outputAsString = writeModelToTurtleString(model);
        return outputAsString.substring(0, outputAsString.lastIndexOf("."));
    }

    public String writeModelToTurtleString(Model model) {
        StringWriter out = new StringWriter();
        model.write(out, "TURTLE");
        return out.toString();
    }


}
