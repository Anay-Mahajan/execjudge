package com.anay.execjudge.execjudge.repo;
import org.springframework.data.jpa.repository.JpaRepository;

import com.anay.execjudge.execjudge.model.Submission;

public interface ProcessRepo extends JpaRepository<Submission, Integer> {
    
}
