#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sys/wait.h>
#include <cstring>
#include<chrono>
int main(int argc, char *argv[])
{
    auto initial = std::chrono::high_resolution_clock::now();
    
    std::string program = "cpp/./" + std::string(argv[1]);
    int no_of_testcase = std::stoi(argv[2]);
    for (int i = 1; i <= no_of_testcase; i++)
    {
        auto start = std::chrono::high_resolution_clock::now();
        std::ifstream file("tests/" + std::to_string(i) + ".in", std::ios::binary);
        if (!file)
        {
            std::cerr << "Cannot open file\n";
            return -1;
        }
        file.seekg(0, std::ios::end);
        size_t size = file.tellg();
        file.seekg(0, std::ios::beg);
        std::string content(size, '\0');
        file.read(&content[0], size);
        file.close();
        auto end = std::chrono::high_resolution_clock::now();
        std::chrono::duration<double> t =end-start;
        // std::cout<<"TIme to read the input from the file "<<t.count()<<'\n';
        start = std::chrono::high_resolution_clock::now();
        int inputPipe[2];
        int outputPipe[2];
        pipe(inputPipe);
        pipe(outputPipe);
        pid_t pid = fork();
        end = std::chrono::high_resolution_clock::now();
        t =end-start;
        // std::cout<<"Time to fork the process "<<t.count()<<'\n';
        std::string result;
        if (pid == 0)
        {
            dup2(inputPipe[0], STDIN_FILENO);
            dup2(outputPipe[1], STDOUT_FILENO);
            close(inputPipe[1]);
            close(outputPipe[0]);
            execl(program.c_str(), program.c_str(),(char *) NULL);
            perror("exec failed");
            _exit(1);
        }
        else
        {
            close(inputPipe[0]);
            close(outputPipe[1]);
            start = std::chrono::high_resolution_clock::now();
            write(inputPipe[1], content.data(), content.size());
            end = std::chrono::high_resolution_clock::now();
            t =end-start;
            // std::cout<<"Time to send input "<<t.count()<<'\n';
            close(inputPipe[1]);
            char buffer[4096];
            int n;
            start = std::chrono::high_resolution_clock::now();
            while ((n = read(outputPipe[0], buffer, sizeof(buffer))) > 0)
            {
                result.append(buffer, n);
            }

            close(outputPipe[0]);
            waitpid(pid, NULL, 0);
            end = std::chrono::high_resolution_clock::now();
            t =end-start;
            // std::cout<<"Time to get the output and read it (Running time)"<<t.count()<<'\n';
        }
        start = std::chrono::high_resolution_clock::now();
        std::ifstream ansfile("tests/" + std::to_string(i) + ".out", std::ios::binary);
        if (!ansfile)
        {
            std::cerr << "Cannot open ansfile\n";
            return -1;
        }
        ansfile.seekg(0, std::ios::end);
        size_t anssize = ansfile.tellg();
        ansfile.seekg(0, std::ios::beg);
        std::string anscontent(anssize, '\0');
        ansfile.read(&anscontent[0], anssize);
        ansfile.close();
        end = std::chrono::high_resolution_clock::now();
        t =end-start;
        // std::cout<<"Time Read the output file "<<t.count()<<'\n';
        if(anscontent!=result){
            return 0;
        }
    }
    auto final = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double> time =final-initial;
    // std::cout<<"Total time "<<(time.count())*1000<<'\n';
    return 1;
}