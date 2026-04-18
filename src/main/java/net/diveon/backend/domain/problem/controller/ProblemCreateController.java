package net.diveon.backend.domain.problem.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.diveon.backend.domain.problem.service.ProblemCreateService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/problems")
public class ProblemCreateController {
    
    private final ProblemCreateService problemCreateService;

    public ProblemCreateController(ProblemCreateService problemCreateService){
        this.problemCreateService = problemCreateService;
    }


    @PostMapping("/objective")
    public String postMethodName(@Valid @RequestBody String entity) {
        //TODO: process POST request


        
        return entity;
    }
    
}
