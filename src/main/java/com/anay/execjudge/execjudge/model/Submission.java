package com.anay.execjudge.execjudge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Data
@Entity
public class Submission{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int qid;
    private int testCasePassed=0;
    private String status="Queue";
    @Column(columnDefinition="TEXT")
    private String code;
    private long recv;
    private long compileStart;
    private long compileEnd;
    private long RunStart;
    private long RunEnd;
    public Submission(Submission s) {
        this.id = s.id;
        this.qid = s.qid;
        this.testCasePassed = s.testCasePassed;
        this.status = s.status;
        this.code = s.code;
    }
    public Submission(){}
}

