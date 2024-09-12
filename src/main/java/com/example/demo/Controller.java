package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@org.springframework.stereotype.Controller
public class Controller {

    @Autowired
    private Service service;
    private Logger logger = LoggerFactory.getLogger(Controller.class);

    public void setGraph(String uuid) throws IOException {
        logger.info("Graph: {}", uuid);
                    Files.write(Paths.get("graph"), uuid.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.CREATE);
    }

    @PostMapping("/testontology/{scenario}")
    public ResponseEntity<?> executeScenario(@PathVariable("scenario") String scenario) throws IOException {
        UUID uuid = UUID.randomUUID();
        setGraph(uuid.toString());
        logger.info("UUID: {}", uuid.toString());
        switch (scenario) {
            case "recursion": {
                service.recursion(5);
                break;
            }
            case "nested": {
                service.nested(3);
                break;
            }
            case "multiple": {
                service.multiple("foo", 1);
                break;
            }
            default: {
                return new ResponseEntity<>("Please pass a valid scenario", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(scenario + " successfully executed on graph " + uuid.toString(), HttpStatus.OK);
    }

}
