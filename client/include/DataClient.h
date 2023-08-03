#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/event.h"


using std::string;
using std::map;
// TODO: implement the STOMP protocol

class DataClient
{
    private:
 
    public:
        DataClient();
        ~DataClient();
        int subscriptionID;
        int receiptID;
        string userName;
        map<std::string, int> topicSubscription;
        map<std::string,std::string> frameByReceipt;
        map<std::pair<string, string>, std::vector<Event>> eventsByUserBeforeHalf;
        map<std::pair<string, string>, std::vector<Event>> eventsByUserAfterHalf;
};