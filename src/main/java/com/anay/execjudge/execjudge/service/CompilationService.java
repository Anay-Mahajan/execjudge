package com.anay.execjudge.execjudge.service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
class CompilationService {
    public boolean compileCpp(String cppFilePath,String path) throws IOException, InterruptedException {

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
            return false;
        }
        process.destroyForcibly();
        return  true;
    }
     public String runCppProgram(String testCase, String path)
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
            return "__TLE__"; 
        }
        reader.join();
        if (process.exitValue() != 0) {
            return "__ERROR__"; 
        }
        return output.toString();
    }
    public int runJudge(int sid,int no_of_testcase)  throws IOException, InterruptedException{
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
    public Process startJudgeWorker() {
        try {
            ProcessBuilder pb = new ProcessBuilder("./judge");
            pb.redirectErrorStream(true); // merge stderr into stdout
            return pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start judge worker", e);
        }
    }
}
