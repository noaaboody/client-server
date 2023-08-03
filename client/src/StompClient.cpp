#include <iostream>
#include <thread>
#include <mutex>
#include "../include/ConnectionHandler.h"
#include "../include/StompClient.h"
#include "../include/StompProtocol.h"
#include <vector>
#include <iostream>
#include <thread>
#include <mutex>
#include <boost/lexical_cast.hpp>
#include <vector>
#include <condition_variable>


std::vector<std::string> split1(const std::string& str, char delimiter) {
    std::vector<std::string> tokens;
    std::string token;
    for (char ch : str) {
        if (ch == delimiter) {
            if (!token.empty()) {
                tokens.push_back(token);
                token.clear();
            }
        } else {
            token += ch;
        }
    }
    if (!token.empty()) {
        tokens.push_back(token);
    }
    return tokens;
}

string login(){
    std::cerr << "Please login.... " << std::endl;
    const short bufsize = 1024;
    char buf[bufsize];
    std::cin.getline(buf, bufsize);
	std::string line(buf);
    return line;
}

int main(int argc, char *argv[]) {
	// TODO: implement the STOMP client
    while (1){
        bool isLogin = false;
        std::vector<string> splitedLine;
        string userName;
        while(!isLogin){
            string line = login();
            splitedLine = split1(line, ' ');
            if (splitedLine[0] == "login"){
                isLogin = true;
                break;
            }
            
        }
        string hostANDport = splitedLine[1];
        std::vector<string> splitHostANDport = split1(hostANDport, ':');
        short port = boost::lexical_cast<short>(splitHostANDport[1]);  
        StompProtocol stomp(splitHostANDport[0], port);

        //ConnectionHandler connectionHandler(splitHostANDport[0],port);  
        if (!stomp.connectionHandler->connect()) {
            std::cerr << "Cannot connect to " << splitHostANDport[0] << ":" << port << std::endl;
            return 1;
        } 
        else{
            userName = splitedLine[2];
            stomp.dataclient->userName = userName;
            string password = splitedLine[3];
            string frame = "CONNECT\naccept-version:1.2\nhost:" + splitHostANDport[0];
            frame += "\nlogin:" + userName + "\npasscode:" + password + "\n \n" + '\0';
            if (!stomp.connectionHandler->sendLine(frame)) {
                std::cerr << "Could not connect to server" << std::endl;        
            }
        }

        std::thread keyboardThread(&StompProtocol::keyboardInput, &stomp);
	    std::thread serverThread(&StompProtocol::serverInput, &stomp);

        keyboardThread.join();
        serverThread.join(); 

    }

     
	return 0;
}

void StompProtocol::serverInput(){
    while(1){
        std::string answer;
        if (!connectionHandler->getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        else{
            if (answer.length() != 0){
                proccess(answer);
            }
        } 
        // else{
        //     while(answer.length() == 0){
        //         connectionHandler->getLine(answer);
        //     }
        //     if(answer.length() != 0){
        //         stop = true;
        //         std::unique_lock<std::mutex> lock(mutex);
        //         m_condVar.wait(lock,[]{return stop;});
        //         proccess(answer);
        //     }
        // }
    }
}

void StompProtocol::keyboardInput(){
	while (1) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
		std::string line(buf);
        std::vector<string> splitedLine = split1(line, ' ');
            if (splitedLine[0] == "logout"){
                login();
            }


        
        std::vector<string> frame(toFrame(line));

        
        // std::unique_lock<std::mutex> lock(mutex);
        // m_condVar.wait(lock,[]{return stop;});
        for ( int i = 0; i<frame.size(); i++){
            if (!connectionHandler->sendLine(frame[i])) {
               std::cerr << "Could not connect to server" << std::endl;        
                break;
            }
        }

    }
}

