package com.anay.execjudge.execjudge.model;

import java.util.ArrayList;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Question {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    private String title;
    private String content;
    private String constraints;
    private ArrayList<String> exampleInputOutput=new ArrayList<>();
    private String inputFormat;
    private String outputFormmat;
    Question(){}
}
