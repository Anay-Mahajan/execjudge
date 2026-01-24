package com.anay.execjudge.execjudge.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anay.execjudge.execjudge.model.TestCase;
@Repository
public interface TestCaseRepo extends JpaRepository<TestCase,Integer> {
    List<TestCase> findByQid(Integer qid);
}
