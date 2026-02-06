package com.anay.execjudge.execjudge.service;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.anay.execjudge.execjudge.model.Execution;
import com.anay.execjudge.execjudge.model.Submission;
import org.jctools.queues.MpmcArrayQueue;
import com.anay.execjudge.execjudge.repo.ProcessRepo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
@Service
public class ProcessService {
    @Autowired
    ProcessRepo processrepo;
    @Autowired
    CompilationService compilationService;
    @Autowired
    BatchSubmissionService batchSubmissionService;
    private final MpmcArrayQueue<Submission> compileQueue = new MpmcArrayQueue<Submission>(8192);
    private final ExecutorService threadPoolCompilers = Executors.newFixedThreadPool(4);
    private final MpmcArrayQueue<Submission> runQueue = new MpmcArrayQueue<Submission>(8192);
    private final ExecutorService threadPoolRunner = Executors.newFixedThreadPool(8);
    public String run(Execution execution){
        try {
            Files.writeString(Path.of("cpp2/"+execution.getId()+".cpp"), execution.getCode());
        } catch (IOException e) {
            return "System Error";
        }
        try {
            if (!compilationService.compileCpp("cpp2/"+execution.getId()+".cpp","cpp2/"+execution.getId())) {
                System.out.println("Compilation failed");
                return "Compilation Failed";
            }
        } catch (IOException | InterruptedException e) {
            
           return "System Error";
        }
        String output="";
        try {
            output=compilationService.runCppProgram(execution.getInput(),"cpp2/./"+execution.getId());
        } catch (IOException | InterruptedException e) {
            
           return "System Error";
        }
        return output;
    }
    public int submit(Submission submission){
        submission.setRecv(System.currentTimeMillis());
        submission=processrepo.save(submission);
        while (!compileQueue.offer(submission)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                submission.setStatus("System Error");
                return -1;
            }
        }
        return submission.getId();
    }
    @PostConstruct 
    public void startCompilers() {
        for (int i = 0; i < 4; i++) {
            threadPoolCompilers.submit(() -> {
                while (true) {
                    Submission s;
                    while ((s = compileQueue.poll()) == null) {
                        LockSupport.parkNanos(1_000_000);
                    }
                    try {
                        Files.writeString(Path.of("cpp/" + s.getId()+ ".cpp"),s.getCode()+"");
                    } catch (IOException e) {
                        s.setStatus("System Error");
                        batchSubmissionService.addToQueue(s);
                        continue;
                    }
                    processJob(s);
                }
            });
        }
    }
    @PostConstruct
    public void startRunners(){
        for (int i = 0; i < 8; i++) {
            threadPoolRunner.submit(() -> {
                Process judgeProcess = compilationService.startJudgeWorker();
                BufferedWriter judgeIn = new BufferedWriter(
                        new OutputStreamWriter(judgeProcess.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader judgeOut = new BufferedReader(
                        new InputStreamReader(judgeProcess.getInputStream(), StandardCharsets.UTF_8));
                while (true) {
                    Submission s;
                    while ((s = runQueue.poll()) == null) {
                        LockSupport.parkNanos(1_000_000);
                    }
                    s.setRunStart(System.currentTimeMillis());
                    try {
                        int result = sendJobToJudge(judgeIn, judgeOut, s);

                        s.setTestCasePassed(result);
                        if(result==1){
                            s.setStatus("Accepted");
                        }
                        else if(result==0){
                            s.setStatus("Wrong Answer");
                        }
                        else{
                            System.out.println(result);
                            s.setStatus("System Error");
                        }

                    } catch (IOException e) {
                        s.setStatus("Judge Error");
                    }
                    s.setRunEnd(System.currentTimeMillis());
                    batchSubmissionService.addToQueue(s);
                }
            });
        }
    }
    
    private int sendJobToJudge(BufferedWriter judgeIn,
            BufferedReader judgeOut,
            Submission job) throws IOException {
        String request = job.getId() + " " + 16; 
        judgeIn.write(request);
        judgeIn.newLine(); 
        judgeIn.flush(); 
        String response = judgeOut.readLine();
        if (response == null) {
            throw new IOException("Judge process closed unexpectedly");
        }
        try {
            return Integer.parseInt(response.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Invalid response from judge: " + response, e);
        }
    }
    private void  processJob(Submission job){
        job.setCompileStart(System.currentTimeMillis());
        job.setStatus("Running");
        try {
            if (!compilationService.compileCpp("cpp/"+job.getId()+".cpp","cpp/"+job.getId())) {
                job.setStatus("Compilation Error");
                batchSubmissionService.addToQueue(job);
                return;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Excetion has occured ");
            return;
        }
        job.setCompileEnd(System.currentTimeMillis());
        batchSubmissionService.addToQueue(job);
        if(runQueue.size()>2000){
            LockSupport.parkNanos(1_000_000);
        }
        while(!runQueue.offer(job)){
            LockSupport.parkNanos(1_000_000);
        }
    }
    // private void processRunning(Submission job){
    //     int testCasePassed=0;
    //     job.setRunStart(System.currentTimeMillis());
    //     try {
    //         testCasePassed=runJudge(job.getId(),14 );
    //     } catch (IOException e) {
    //       System.out.println("System error");
    //     } catch (InterruptedException e) {
    //       System.out.println("System error");
    //     }
    //     job.setTestCasePassed(testCasePassed);
    //     if(testCasePassed==1)   job.setStatus("Aceepted");
    //     else    job.setStatus("Wrong Answer");
    //     job.setRunEnd(System.currentTimeMillis());
    // batchSubmissionService.addToQueue(s);
    // }
    @PreDestroy
    public void shutdownCompile() {
        threadPoolCompilers.shutdown();
    }
    @PreDestroy
    public void shutdownRun() {
        threadPoolRunner.shutdown();
    }
    public String result(int sid){
        Submission S=processrepo.findById(sid).orElseThrow(()->new RuntimeException("Sid not found\n"));
        return S.getStatus();
    }
}
