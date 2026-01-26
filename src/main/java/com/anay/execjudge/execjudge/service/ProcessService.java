package com.anay.execjudge.execjudge.service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
        try {
            Files.writeString(Path.of("cpp2/"+execution.getId()+".cpp"), execution.getCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (!compileCpp("cpp2/"+execution.getId()+".cpp","cpp2/"+execution.getId())) {
                System.out.println("Compilation failed");
                return "Compilation Failed";
            }
        } catch (IOException | InterruptedException e) {
            
            e.printStackTrace();
        }
        String output="";
        try {
            output=runCppProgram(execution.getInput(),"cpp2/./"+execution.getId());
        } catch (IOException | InterruptedException e) {
            
            e.printStackTrace();
        }
        return output;
    }
    public int submit(Submission submission){
        long start = System.nanoTime();
        submission=processrepo.save(submission);
        long end=System.nanoTime();
        // System.out.println("Time to save process to database "+(end-start)/ 1_000_000);
        try {
            start= System.nanoTime();
            Files.writeString(Path.of("cpp/"+submission.getId()+".cpp"), submission.getCode());
            end=System.nanoTime();
            // System.out.println("Time to create file "+(end-start)/ 1_000_000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        start= System.nanoTime();
        jobQueue.offer(submission);
        end=System.nanoTime();
        System.out.println("Time to add the process to queue "+(end-start)/ 1_000_000);
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
            if (!compileCpp("cpp/"+job.getId()+".cpp","cpp/"+job.getId())) {
                job.setStatus(4);
                processrepo.save(job);
                return;
            }
        } catch (IOException | InterruptedException e) {
            return;
        }
        List<TestCase> testCases=testCaseService.getTestCases(job.getQid());
        int testCasePassed=0;
        for(int i=0;i<testCases.size();i++){
            try {
                String output=runCppProgram(testCases.get(i).getInput(), "cpp/./"+job.getId());
                if(output!="__TLE__" || output!="__ERROR__"){
                    if(output.equals(testCases.get(i).getExpectedOutput().trim())){
                        testCasePassed++;
                    }
                    else{
                        break;
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
        }
        if(testCasePassed==testCases.size()){
            job.setStatus(2);
        }
        else{
            job.setStatus(3);
        }
        job.setTestCasePassed(testCasePassed);
        processrepo.save(job);
    }
    @PreDestroy
    public void shutdown() {
        threadPool.shutdown();
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
        return output.toString().trim();
    }
    public int result(int sid){
        Submission S=processrepo.findById(sid).orElseThrow(()->new RuntimeException("Sid not found\n"));
        return S.getStatus();
    }
}
