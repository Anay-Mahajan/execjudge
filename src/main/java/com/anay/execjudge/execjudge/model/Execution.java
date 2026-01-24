package com.anay.execjudge.execjudge.model;

import lombok.Data;


@Data
public class Execution {
    private String code;
    private String input;
    public Execution(){}
    public Execution(String code, String input) {
        this.code = code;
        this.input = input;
    }
}
