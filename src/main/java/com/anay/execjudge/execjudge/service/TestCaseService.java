package com.anay.execjudge.execjudge.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anay.execjudge.execjudge.model.Question;
import com.anay.execjudge.execjudge.model.TestCase;
import com.anay.execjudge.execjudge.repo.TestCaseRepo;

@Service
public class TestCaseService {
    @Autowired
    private TestCaseRepo testRepository;
    public TestCase getTestCasebyId(Question question , int tid ){
        return testRepository.findById(tid).orElseThrow(()->new RuntimeException("Test Case not found with ID "+tid));
    }
    public List<TestCase>getTestCases(int qid){
        return testRepository.findByQid(qid);
    }
    public TestCase createTestCase(TestCase testcase){
        return testRepository.save(testcase);
    }
}
