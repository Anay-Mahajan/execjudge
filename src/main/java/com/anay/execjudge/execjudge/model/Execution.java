package com.anay.execjudge.execjudge.model;

import java.util.concurrent.atomic.AtomicInteger;


import lombok.Data;


@Data
public class Execution {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    private final int id;
    private String code;
    private String input;
    public Execution(){
        this.id = ID_GEN.getAndIncrement();
    }
    public Execution(String code, String input) {
        this.id = ID_GEN.getAndIncrement();
        this.code = code;
        this.input = input;
    }
}
