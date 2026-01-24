package com.anay.execjudge.execjudge.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.anay.execjudge.execjudge.model.Question;
import com.anay.execjudge.execjudge.model.QuestionTitleId;
public interface QuestionRepo extends JpaRepository<Question,Integer>{
    @Query("select new com.anay.execjudge.execjudge.model.QuestionTitleId(q.id,q.title) from Question q")
    List<QuestionTitleId> findalltitle();
}
