#ifndef __CLIENTAPI_H_
#define __CLIENTAPI_H_
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h> 
#include <arpa/inet.h> 
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>
#include <errno.h>
#include <time.h>
#include <unistd.h>
#define PORT     8080 
#define MAXLINE   1024 
struct unacked{
    int request_id_api;
    clock_t time;
    struct unacked *next;
    int retransmit_times;
    void *data;
    int server_slow;
    struct sockaddr_in  servaddr;
};

struct request_couples{
    int request_id_user;
    int request_id_api;
    int confirm;
    void *replies ;
    struct sockaddr_in  servaddr;
    struct request_couples *next;
};


struct acked_reqs{

    int request_id_apis[255]; 
    int num_of_requests;
    struct sockaddr_in  servaddr;
    struct acked_reqs *next;
};
void *send_acked_reqs();
void * retransmition();
void *read_from_socket();
int init_connection();
struct sockaddr_in discover_server(int svcid,int request_id_api);
int send_request(int svcid, void *buffer, int len);
int getReply (int reqid, void** buf, int *len, int block);
 void terminate();

#endif