#include "../include/StompProtocol.h"
#include "../include/event.h"
#include <vector>
#include <iostream>  
#include <string>  
#include <boost/lexical_cast.hpp>
#include <fstream>
#include <mutex>
#include <boost/lexical_cast.hpp>
#include <vector>
#include <condition_variable>

StompProtocol::StompProtocol(string host, short port){
    connectionHandler = new ConnectionHandler(host,port);
    dataclient = new DataClient();
}

std::vector<string> StompProtocol::toFrame(string message)
{
    std::vector<string> splitedLine = split(message, ' ');
    string command = splitedLine[0];
    std::vector<string> output;
    string frame = "";
    if (command == "login"){
        //TODO implement
        string hostANDport = splitedLine[1];
        string userName = splitedLine[2];
        string password = splitedLine[3];
        frame += "CONNECT\naccept-version:1.2\nhost:" + split(hostANDport, ':')[0];
        frame += "\nlogin:" + userName + "\npasscode:" + password + "\n \n" + '\0';
        output.push_back(frame);

    }
    else if (command == "join"){
        string dest = splitedLine[1];
        string subscriptionid = std::to_string(dataclient->subscriptionID);  
        string receiptid = std::to_string(dataclient->receiptID);  
        frame += "SUBSCRIBE\ndestination:/" + dest +"\nid:" + subscriptionid + "\nreceipt:" + receiptid + "\n \n" + '\0';
        dataclient->topicSubscription.insert(std::pair<string,int>(dest,dataclient->subscriptionID));
        dataclient->frameByReceipt.insert(std::pair<string,string>(receiptid, frame));
        dataclient->subscriptionID++;
        dataclient->receiptID++;
        output.push_back(frame);


    }
    else if (command == "exit"){
        string dest = splitedLine[1];
        try {
            string subscriptionid = std::to_string(dataclient->topicSubscription.at(dest));
            string receiptid = std::to_string(dataclient->receiptID);  
            frame += "UNSUBSCRIBE\nid:" + subscriptionid + "\nreceipt:" + receiptid + "\n \n" + '\0';
            output.push_back(frame);
            dataclient->frameByReceipt.insert(std::pair<string,string>(receiptid, frame));
            dataclient->receiptID++;
        }
        catch(std::exception &e){
            std::cout << "you are not subscribed to this topic" << std::endl;
        }
        
    }    
    else if (command == "report"){
        string file = splitedLine[1];
        names_and_events name_and_event = parseEventsFile(file);
        for(int i = 0; i<name_and_event.events.size() ; i++){
            frame += "SEND\ndestination:/" + name_and_event.team_a_name + "_" + name_and_event.team_b_name + "\n \nuser:" + dataclient->userName + "\nteam a: " + name_and_event.team_a_name + "\nteam b:" + name_and_event.team_b_name;
            frame += "\nevent name: " + name_and_event.events[i].get_name() + "\ntime: " + std::to_string(name_and_event.events[i].get_time()); 
            frame += "\ngeneral game updates:\n" + mapUpdateToString(name_and_event.events[i].get_game_updates());
            frame += "team a updates:\n" + mapUpdateToString(name_and_event.events[i].get_team_a_updates());
            frame += "team b updates:\n" + mapUpdateToString(name_and_event.events[i].get_team_b_updates());
            frame += "description:\n" + name_and_event.events[1].get_discription();
            frame += "\n \n" + '\0';
            output.push_back(frame);
            frame = "";

        }   
    }
    else if (command == "summary"){
        string game_name = splitedLine[1];
        string user = splitedLine[2];
        string file = splitedLine[3];
        string summary = ""; 
        std::pair<string,string> key = {game_name,user};
        std::vector<Event> gameEventsBefore;
        std::vector<Event> gameEventsAfter;
        if (dataclient->eventsByUserBeforeHalf.find(key) != dataclient->eventsByUserBeforeHalf.end()){
            gameEventsBefore = dataclient->eventsByUserBeforeHalf.at(key);
            sort(gameEventsBefore.begin(),gameEventsBefore.end(), [](Event& ev1, Event& ev2){return ev1.get_time()>ev2.get_time();});
        }
        else if (dataclient->eventsByUserAfterHalf.find(key) != dataclient->eventsByUserAfterHalf.end()) {
            gameEventsAfter = dataclient->eventsByUserAfterHalf.at(key);
            sort(gameEventsAfter.begin(),gameEventsAfter.end(), [](Event& ev1, Event& ev2){return ev1.get_time()>ev2.get_time();});
        }

        Event lastEvent;
        if (!gameEventsAfter.empty()){
            lastEvent = gameEventsAfter.front();
        }
        else{
            lastEvent = gameEventsBefore.front();
        }
        summary += lastEvent.get_team_a_name() + " vs " + lastEvent.get_team_b_name() + "\n";
        summary += "Game stats:\nGeneral stats:\n" + mapUpdateToString(lastEvent.get_game_updates()) + "\n";
        summary += lastEvent.get_team_a_name() + " stats:\n" + mapUpdateToString(lastEvent.get_team_a_updates()) + "\n";
        summary += lastEvent.get_team_b_name() + " stats:\n" + mapUpdateToString(lastEvent.get_team_b_updates()) + "\n";
        summary += "Game event reports:\n";
        for (int i = 0; i<gameEventsBefore.size(); i++){
            summary +=  std::to_string(gameEventsBefore[i].get_time()) + " - " + gameEventsBefore[i].get_name() + ":\n\n";
            summary += gameEventsBefore[i].get_discription() + "\n\n\n";
        }
        for (int i = 0; i<gameEventsAfter.size(); i++){
            summary +=  std::to_string(gameEventsAfter[i].get_time()) + " - " + gameEventsAfter[i].get_name() + ":\n\n";
            summary += gameEventsAfter[i].get_discription() + "\n\n\n";
        }
        
        std::fstream new_file;
        new_file.open(file, std::ios::out);
        new_file<<summary;
        new_file.close();
    }
    else if (command == "logout"){
        string receiptid = std::to_string(dataclient->receiptID);  
        frame += "DISCONNECT\nreceipt:" + receiptid + "\n \n" + '\0';
        output.push_back(frame);
        dataclient->frameByReceipt.insert(std::pair<string,string>(receiptid, frame));
        dataclient->receiptID++;    
    }
    return output;

}

std::string StompProtocol::mapUpdateToString(std::map<std::string, std::string> updateMap){
    std::string result = "";
    for(std::pair<std::string,std::string> update : updateMap){
        std::string line = "" + update.first + ":" + update.second;
        result += "\t" + line + "\n";
    }
    return result;
}

void StompProtocol::proccess(string message)
{
    std::vector<std::string> splitMessageByLineBreak = split(message, '\n');
    string stompCommand = splitMessageByLineBreak[0];
    if(stompCommand == "CONNECTED"){
        std::cout << "Login successful" << std::endl;
    }
    else if(stompCommand == "RECEIPT"){
        string receiptId = (split(splitMessageByLineBreak[1], ':'))[1];
        string originFrame = dataclient->frameByReceipt[receiptId];
        std::vector<std::string> splitOriginFrameByLineBreak = split(originFrame, '\n');
        string originCommand = splitOriginFrameByLineBreak[0];
        if(originCommand == "SUBSCRIBE"){
            string destination = (split(splitOriginFrameByLineBreak[1], ':'))[1];
            std::cout << "joined channel " << destination << std::endl;
        }
        else if(originCommand == "UNSUBSCRIBE"){
            int id = std::stoi((split(splitOriginFrameByLineBreak[1], ':'))[1]);
            string topic;
            for(std::pair<string,int> frame: dataclient->topicSubscription){
                if(frame.second == id){
                    topic = frame.first;
                }
            }
            // string destinationLine = (split(subscribeFrame, '\n'))[1];
            // string destination = (split(destinationLine, ':'))[1];
            std::cout << "exited channel /" << topic << std::endl;
        }
        else if(originCommand == "DISCONNECT"){
            std::cout << "closing connection ...  " << std::endl;
            connectionHandler->login = false;
            connectionHandler->close();
        }
    }
    else if(stompCommand == "ERROR"){
        string message = (split(splitMessageByLineBreak[2], ':'))[1];
        std::cout << message << std::endl;
    }
    else if(stompCommand == "MESSAGE"){
        std::vector<std::string> event_name = split(splitMessageByLineBreak[7], ':');
        string eventName = event_name[1];
        std::vector<std::string> teamNames_withoutSlash = split(splitMessageByLineBreak[1], '/');
        std::vector<std::string> teamNames = split(teamNames_withoutSlash[1],'_');
        string team_a_name = teamNames[0];
        string team_b_name = teamNames[1];
        int time = std::stoi((split(splitMessageByLineBreak[8], ':'))[1]);
        std::map<std::string, std::string> game_updates;
        int lineIndex = 10;
        while ((split(splitMessageByLineBreak[lineIndex], ':'))[0] != "team a updates"){
            std::vector <string> line = split(splitMessageByLineBreak[lineIndex], ':');
            game_updates.insert(std::pair<string,string>(line[0], line[1]));
            lineIndex = lineIndex + 1;
        }
        lineIndex = lineIndex + 1;
        std::map<std::string, std::string> team_a_updates;
        while((split(splitMessageByLineBreak[lineIndex], ':'))[0] != "team b updates"){
            team_a_updates.insert(std::pair<string,string>((split(splitMessageByLineBreak[lineIndex], ':'))[0], (split(splitMessageByLineBreak[lineIndex], ':'))[1]));
            lineIndex = lineIndex + 1;
        }
        lineIndex = lineIndex + 1;
        std::map<std::string, std::string> team_b_updates;
        while((split(splitMessageByLineBreak[lineIndex], ':'))[0] != "description"){
            team_b_updates.insert(std::pair<string,string>((split(splitMessageByLineBreak[lineIndex], ':'))[0], (split(splitMessageByLineBreak[lineIndex], ':'))[1]));
            lineIndex = lineIndex + 1;
        }
        string description = splitMessageByLineBreak[lineIndex + 1];
        Event event(team_a_name , team_b_name, eventName, time, game_updates, team_a_updates, team_b_updates, description);
        std::cout << event.get_name() << "\n" << event.get_team_a_name() << "\n"  << event.get_team_b_name() << std::endl;
        std::pair<string,string> key = {teamNames_withoutSlash[1],
                                                        (split(splitMessageByLineBreak[4], ':'))[1]}; 
        if(game_updates.find("before halftime") != game_updates.end() &&
                                        game_updates["before halftime"] == "false"){
            if(dataclient->eventsByUserAfterHalf.find(key) != dataclient->eventsByUserAfterHalf.end()){
                dataclient->eventsByUserAfterHalf[key].push_back(event);
            }
            else{
                std::vector<Event> events;
                events.push_back(event);                                  
                dataclient->eventsByUserAfterHalf.insert(std::make_pair(key, events));
            }
        }
        else{
            if(dataclient->eventsByUserBeforeHalf.find(key) != dataclient->eventsByUserBeforeHalf.end()){
                dataclient->eventsByUserBeforeHalf[key].push_back(event);
            }
            else{
                std::vector<Event> events;
                events.push_back(event);
                dataclient->eventsByUserBeforeHalf.insert(std::make_pair(key, events));
            }
        }
        std::cout << "message from server : " << message << std::endl;

    }
}

std::vector<std::string> StompProtocol::split(const std::string& str, char delimiter) {
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
