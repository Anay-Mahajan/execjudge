package com.anay.execjudge.execjudge.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anay.execjudge.execjudge.model.Question;
import com.anay.execjudge.execjudge.model.QuestionTitleId;

import com.anay.execjudge.execjudge.repo.QuestionRepo;

@Service
public class QuestionService {
    @Autowired
    private QuestionRepo QuestionRepository;
    public Question createQuestion(Question question){
        return QuestionRepository.save(question);
    }
    public void deleteQuestion(Question question){
        QuestionRepository.delete(question);
    }
    public List<QuestionTitleId> getAllQuestion(){
        return QuestionRepository.findalltitle();
    }
    public Question getQuestion(int qid){
        return QuestionRepository.findById(qid).orElseThrow(()->new RuntimeException("Question not found with Question ID"+qid));
    }
    
}
