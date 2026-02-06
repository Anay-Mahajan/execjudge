package com.anay.execjudge.execjudge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anay.execjudge.execjudge.model.Submission;
import com.anay.execjudge.execjudge.repo.ProcessRepo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class BatchSubmissionService {

    @Autowired
    private ProcessRepo processRepo;
    private final BlockingQueue<Submission> updateQueue = new LinkedBlockingQueue<>();
    private final int BATCH_SIZE = 50; 
    private final ExecutorService threadPoolUpdater = Executors.newFixedThreadPool(2);

    public void addToQueue(Submission s) {
        updateQueue.offer(s);
    }
    @PostConstruct
    public void startBatchWriter() {
        for(int i=0;i<2;i++){
           threadPoolUpdater.submit(()-> {
            List<Submission> batch = new ArrayList<>();
            while (true) {
                try {
                    Submission first = updateQueue.take();
                    batch.add(first);
                    updateQueue.drainTo(batch, BATCH_SIZE - 1);
                    processRepo.saveAll(batch);
                    batch.clear();
                } catch (Exception e) {
                    System.err.println("Error saving batch: " + e.getMessage());
                }
            }
        });
        }
    }
    @PreDestroy
    public void shutdownUpdater() {
        threadPoolUpdater.shutdown();
    }
}