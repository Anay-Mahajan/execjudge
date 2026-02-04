package com.anay.execjudge.execjudge.service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final MpmcArrayQueue<Submission> compileQueue = new MpmcArrayQueue<Submission>(8192);
    private final ExecutorService threadPoolCompilers = Executors.newFixedThreadPool(2);
    private final MpmcArrayQueue<Submission> runQueue = new MpmcArrayQueue<Submission>(8192);
    private final ExecutorService threadPoolRunner = Executors.newFixedThreadPool(4);
    public String run(Execution execution){
        try {
            Files.writeString(Path.of("cpp2/"+execution.getId()+".cpp"), execution.getCode());
        } catch (IOException e) {
            return "System Error";
        }
        try {
            if (!compileCpp("cpp2/"+execution.getId()+".cpp","cpp2/"+execution.getId())) {
                System.out.println("Compilation failed");
                return "Compilation Failed";
            }
        } catch (IOException | InterruptedException e) {
            
           return "System Error";
        }
        String output="";
        try {
            output=runCppProgram(execution.getInput(),"cpp2/./"+execution.getId());
        } catch (IOException | InterruptedException e) {
            
           return "System Error";
        }
        return output;
    }
    public int submit(Submission submission){
        submission=processrepo.save(submission);
        submission.setRecv(System.currentTimeMillis());
        while (!compileQueue.offer(submission)) {
            Thread.onSpinWait();
        }
        return submission.getId();
    }
    @PostConstruct 
    public void startCompilers() {
        for (int i = 0; i < 2; i++) {
            threadPoolCompilers.submit(() -> {
                while (true) {
                    Submission s;
                    int idle = 0;
                    while ((s = compileQueue.poll()) == null) {
                        if (idle < 10) {
                            Thread.onSpinWait();
                        } else if (idle < 20) {
                            Thread.yield();
                        } else {
                            LockSupport.parkNanos(1_000_000);
                        }
                        idle++;
                    }
                    idle = 0;
                    processJob(s);
                }
            });
        }
    }
    @PostConstruct
    public void startRunners(){
        for (int i = 0; i < 4; i++) {
            threadPoolRunner.submit(() -> {
                while (true) {
                    Submission s;
                    int idle = 0;
                    while ((s = runQueue.poll()) == null) {
                        if (idle < 50) {
                            Thread.onSpinWait();
                        } else if (idle < 100) {
                            Thread.yield();
                        } else {
                            LockSupport.parkNanos(1_000);
                        }
                        idle++;
                    }
                    idle = 0;
                    processRunning(s);
                }
            });
        }
    }
    private void  processJob(Submission job){
        try {
            Files.writeString(Path.of("cpp/"+job.getId()+".cpp"), job.getCode());
        } catch (IOException e) {
            job.setStatus("System Error");
            processrepo.save(job);
            return;
        }
        job.setCompileStart(System.currentTimeMillis());
        job.setStatus("Running");
        try {
            if (!compileCpp("cpp/"+job.getId()+".cpp","cpp/"+job.getId())) {
                job.setStatus("Compilation Error");
                processrepo.save(job);
                return;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Excetion has occured ");
            return;
        }
        job.setCompileEnd(System.currentTimeMillis());
        processrepo.save(job);
        if(runQueue.size()>2000){
            LockSupport.parkNanos(1_000_000);
        }
        while(!runQueue.offer(job)){
            Thread.onSpinWait();
        }
    }
    private void processRunning(Submission job){
        int testCasePassed=0;
         job.setRunStart(System.currentTimeMillis());
        try {
            testCasePassed=runJudge(job.getId(),14 );
        } catch (IOException e) {
          System.out.println("System error");
        } catch (InterruptedException e) {
          System.out.println("System error");
        }
        job.setTestCasePassed(testCasePassed);
        if(testCasePassed==1)   job.setStatus("Aceepted");
        else    job.setStatus("Wrong Answer");
        job.setRunEnd(System.currentTimeMillis());
        processrepo.save(job);
    }
    @PreDestroy
    public void shutdownCompile() {
        threadPoolCompilers.shutdown();
    }
    @PreDestroy
    public void shutdownRun() {
        threadPoolRunner.shutdown();
    }
    private boolean compileCpp(String cppFilePath,String path) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "clang++",
                cppFilePath,
                "-o",
                path
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); 
            }
        }
        boolean finished = process.waitFor(2, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
        }
        process.destroyForcibly();
        return  true;
    }
    
    private String runCppProgram(String testCase, String path)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(path);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append('\n');
                    if (output.length() > 64_000)
                        break;
                }
            } catch (IOException ignored) {
            }
        });
        reader.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(testCase.getBytes(StandardCharsets.UTF_8));
        }
        boolean finished = process.waitFor(100, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join();
            return "__TLE__"; 
        }

        reader.join();
        if (process.exitValue() != 0) {
            return "__ERROR__"; 
        }
        return output.toString();
    }
    private  int runJudge(int sid,int no_of_testcase)  throws IOException, InterruptedException{
        String path="./judge";
         ProcessBuilder pb = new ProcessBuilder(
            path,
            String.valueOf(sid),
            String.valueOf(no_of_testcase)
         );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        process.destroyForcibly();
        return exitCode;
    }
    public String result(int sid){
        Submission S=processrepo.findById(sid).orElseThrow(()->new RuntimeException("Sid not found\n"));
        return S.getStatus();
    }
}
