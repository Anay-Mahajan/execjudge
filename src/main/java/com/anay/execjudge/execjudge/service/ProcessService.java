package com.anay.execjudge.execjudge.service;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.anay.execjudge.execjudge.model.Execution;
import com.anay.execjudge.execjudge.model.Submission;
import com.anay.execjudge.execjudge.model.TestCase;
import com.anay.execjudge.execjudge.repo.ProcessRepo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
@Service
public class ProcessService {
    @Autowired
    ProcessRepo processrepo;
    @Autowired 
    TestCaseService testCaseService;
    private final BlockingQueue<Submission> jobQueue = new LinkedBlockingQueue<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(4);
    public String run(Execution execution){
        // try {
        //     Files.writeString(Path.of("Main.cpp"), execution.getCode());
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        // try {
        //     if (!compileCpp("Main.cpp")) {
        //         System.out.println("Compilation failed");
        //         return "Compilation Failed";
        //     }
        // } catch (IOException | InterruptedException e) {
        //     
        //     e.printStackTrace();
        // }
        String output="";
        // try {
        //     output=runCppProgram(execution.getInput());
        // } catch (IOException | InterruptedException e) {
        //     
        //     e.printStackTrace();
        // }
        return output;
    }
    public int submit(Submission submission){
        submission=processrepo.save(submission);
        try {
            Files.writeString(Path.of(+submission.getId()+".cpp"), submission.getCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        jobQueue.offer(submission);
        return submission.getId();
    }
    @PostConstruct 
    public void startConsumers() {
        for (int i = 0; i < 4; i++) {
            threadPool.submit(() -> {
                while (true) {
                    try {
                        Submission job = jobQueue.take();
                        processJob(job);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    private void  processJob(Submission job){
        job.setStatus(1);
        try {
            if (!compileCpp(job.getId()+".cpp",job.getId())) {
                System.out.println("Compilation failed");
                job.setStatus(4);
                processrepo.save(job);
                return;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("System Error Occured \n");
            return;
        }
        List<TestCase>testcases=testCaseService.getTestCases(job.getQid());
        int no_of_testcase=testcases.size();
        int i;
        boolean flag=true;
        for(i=0;i<no_of_testcase;i++){
             String output="";
            try {
                 output=runCppProgram(testcases.get(i).getInput(),job.getId());
            } catch (IOException | InterruptedException e) {
                flag=false;
                e.printStackTrace();
            }
            String actual = output.trim();
            String expected = testcases.get(i).getExpectedOutput().trim();
            if (actual.equals(expected)) {
                job.setTestCasePassed(job.getTestCasePassed() + 1);
            }
            else{
                System.out.println("Failed on Test Case "+(i+1));
                flag=false;
                break;
            }
        }
        if(flag){
            job.setStatus(2);
        }
        else{
            job.setStatus(3);
        }
        processrepo.save(job);
    }
    @PreDestroy
    public void shutdown() {
        threadPool.shutdown();
    }
    private boolean compileCpp(String cppFilePath,int jid) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "clang++",
                cppFilePath,
                "-o",
                jid+""
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
        return  true;
    }
    private String runCppProgram(String input, int jid)
        throws IOException, InterruptedException {

    ProcessBuilder pb = new ProcessBuilder("./" + jid);
    pb.redirectErrorStream(true); // merge stderr into stdout
    Process process = pb.start();

    // 1. Write input (stdin)
    try (BufferedWriter writer =
                 new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
        writer.write(input);
        writer.flush();
    }

    // 2. Read output ASYNC (important)
    StringBuilder output = new StringBuilder();
    Thread outputReader = new Thread(() -> {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException ignored) {}
    });

    outputReader.start();

    // 3. Enforce time limit
    boolean finished = process.waitFor(2, TimeUnit.SECONDS);

    if (!finished) {
        process.destroyForcibly();
        outputReader.join();
        return "__TLE__";
    }

    outputReader.join();

    int exitCode = process.exitValue();
    if (exitCode != 0) {
        return "__RUNTIME_ERROR__";
    }

    return output.toString();
}
    public int result(int sid){
        Submission S=processrepo.findById(sid).orElseThrow(()->new RuntimeException("Sid not found\n"));
        return S.getStatus();
    }
}
