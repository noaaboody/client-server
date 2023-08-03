#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/DataClient.h"
#include <mutex>
#include <boost/lexical_cast.hpp>
#include <vector>
#include <condition_variable>

using std::string;
// TODO: implement the STOMP protocol

class ConnectionHandler;
class DataClient;

//bool stop;

class StompProtocol
{
    private:

    public:
        StompProtocol(string host, short port);
        ConnectionHandler *connectionHandler;
        DataClient *dataclient;
        std::vector<string> toFrame (string message);        
        bool shouldTerminate();
        bool terminate;
        void proccess(string message);
        std::string mapUpdateToString(std::map<std::string, std::string> updateMap);
        void serverInput();
        void keyboardInput();
        std::vector<std::string> split(const std::string& str, char delimiter);
        std::mutex mutex;
        std::condition_variable m_condVar;
};
