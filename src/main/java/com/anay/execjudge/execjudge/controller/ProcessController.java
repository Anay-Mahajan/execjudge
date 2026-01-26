package com.anay.execjudge.execjudge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anay.execjudge.execjudge.model.Submission;
import com.anay.execjudge.execjudge.service.ProcessService;
import com.anay.execjudge.execjudge.model.Execution;

@RestController
@RequestMapping("api/process")
public class ProcessController {
    @Autowired
    ProcessService processService;

    @PostMapping("/{qid}/run")
    public ResponseEntity<?> run(@RequestBody Execution request){
        String output=processService.run(request);
        return ResponseEntity.ok(new ExecResponse(output));
    }
    @PostMapping("/{qid}/submit")
    public ResponseEntity<?> submit(@RequestBody Submission request , @PathVariable("qid") int qid){
        request.setQid(qid);
        int sid=processService.submit(request);
        return ResponseEntity.ok(sid);
    }
    @GetMapping("/{sid}/result")
    public ResponseEntity<?> result(@PathVariable("sid") int sid){
        return ResponseEntity.ok(processService.result(sid));
    }
} 
