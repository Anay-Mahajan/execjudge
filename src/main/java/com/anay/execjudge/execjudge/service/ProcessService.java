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
    private final BlockingQueue<Submission> compileQueue = new LinkedBlockingQueue<>();
    private final ExecutorService threadPoolCompilers = Executors.newFixedThreadPool(2);
    private final BlockingQueue<Submission> runQueue = new LinkedBlockingQueue<>();
    private final ExecutorService threadPoolRunner = Executors.newFixedThreadPool(2);
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
        try {
            Files.writeString(Path.of("cpp/"+submission.getId()+".cpp"), submission.getCode());
        } catch (IOException e) {
           return -1;
        }
        compileQueue.offer(submission);
        return submission.getId();
    }
    @PostConstruct 
    public void startCompilers() {
        for (int i = 0; i < 2; i++) {
            threadPoolCompilers.submit(() -> {
                while (true) {
                    try {
                        Submission job = compileQueue.take();
                        processJob(job);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Thread Exception Occured");
                        break;
                    }
                }
            });
        }
    }
    @PostConstruct
    public void startRunners(){
        for (int i = 0; i < 2; i++) {
            threadPoolRunner.submit(() -> {
                while (true) {
                    try {
                        Submission job = runQueue.take();
                        processRunning(job);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    private void  processJob(Submission job){
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
        processrepo.save(job);
        runQueue.offer(job);
    }
    private void processRunning(Submission job){
        List<TestCase> testCases=testCaseService.getTestCases(job.getQid());
        int testCasePassed=0;
        for(int i=0;i<testCases.size();i++){
            try {
                String output=runCppProgram(testCases.get(i).getInput(), "cpp/./"+job.getId());
                if(!output.equals("__TLE__") && !output.equals("__ERROR__")){
                    if(output.equals(testCases.get(i).getExpectedOutput())){
                        testCasePassed++;
                    }
                    else{
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("IO Excetion ");
                return;
            } catch (InterruptedException e) {
                System.out.println("Interupted Exception Occured ");
                return;
            }
        }
        if(testCasePassed==testCases.size()){
            job.setStatus("Accepted");
        }
        else{
            job.setStatus("Wrong Answer");
        }
        job.setTestCasePassed(testCasePassed);
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
    public String result(int sid){
        Submission S=processrepo.findById(sid).orElseThrow(()->new RuntimeException("Sid not found\n"));
        return S.getStatus();
    }
}
