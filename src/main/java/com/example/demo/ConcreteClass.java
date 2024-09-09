package com.example.demo;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

@Service
public class ConcreteClass extends AbstractClass {

    public ConcreteClass() {
        inheritedMethod();
    }


    @Override
    public void inheritedMethod() {
    }

    public void concreteMethod() {
    }
    public void thirdMethod() {
    }

}
