package com.anay.execjudge.execjudge.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.anay.execjudge.execjudge.model.QuestionTitleId;
import com.anay.execjudge.execjudge.service.QuestionService;
@RestController
@RequestMapping("api/list")
public class ListController {
    @Autowired
    QuestionService questionService;
    @GetMapping("allquestion")
    public ResponseEntity<?> allQuestion(){
        List<QuestionTitleId> L=questionService.getAllQuestion();
        return ResponseEntity.ok(L);
    }
}
