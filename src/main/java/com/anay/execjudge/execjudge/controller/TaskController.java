package com.anay.execjudge.execjudge.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anay.execjudge.execjudge.model.Question;
import com.anay.execjudge.execjudge.model.TestCase;
import com.anay.execjudge.execjudge.service.QuestionService;
import com.anay.execjudge.execjudge.service.TestCaseService;

@RestController
@RequestMapping("api/task")
public class TaskController {
    @Autowired
    QuestionService questionService;
    @Autowired
    TestCaseService testCaseService;
    @GetMapping("/{qid}")
    public ResponseEntity<?> getQuestion(@PathVariable("qid") int qid ){
        Question Q=questionService.getQuestion(qid);
        return ResponseEntity.ok(Q);
    }
    @PostMapping("/create")
    public ResponseEntity<?> createQuestion(@RequestBody Question question){
        return ResponseEntity.ok(questionService.createQuestion(question));
    }
    @PostMapping("/addTestCase")
    public ResponseEntity<?> createTestCase(@RequestBody TestCase testCase){
        return ResponseEntity.ok(testCaseService.createTestCase(testCase));
    }
}
