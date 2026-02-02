#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sys/wait.h>
#include <cstring>
int main(int argc, char *argv[])
{
    std::string program = "cpp/./" + std::string(argv[1]);
    int no_of_testcase = std::stoi(argv[2]);
    for (int i = 1; i <= no_of_testcase; i++)
    {
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
        int inputPipe[2];
        int outputPipe[2];
        pipe(inputPipe);
        pipe(outputPipe);
        pid_t pid = fork();
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
            write(inputPipe[1], content.data(), content.size());
            close(inputPipe[1]);
            char buffer[4096];
            int n;
            while ((n = read(outputPipe[0], buffer, sizeof(buffer))) > 0)
            {
                result.append(buffer, n);
            }

            close(outputPipe[0]);
            waitpid(pid, NULL, 0);
        }
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
        if(anscontent!=result){
            return 0;
        }
    }
    return 1;
}