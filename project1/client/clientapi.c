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
#include <sys/file.h>
#include "clientapi.h"
#define PORT     8080 
#define MAXLINE   1024 

int reqid_api;
int reqid_user;
int fd;
pthread_mutex_t lock ;
pthread_mutex_t lock_ack;
pthread_mutex_t lock_acked_reqs;
struct unacked *head_ack;
struct request_couples* head;
struct acked_reqs* head_acked_reqs; 
int socket_fd;
pthread_t thread1;
pthread_t thread2;
pthread_t thread3;

int iret1;
int iret2;
int iret3;

void *send_acked_reqs(){
    struct acked_reqs *current_acked_reqs ;
    while(1){
       // sleep(1);
        pthread_mutex_lock(&lock_acked_reqs);
        current_acked_reqs = head_acked_reqs->next ;
        while(current_acked_reqs != NULL){
            
            if (current_acked_reqs->num_of_requests > 0){
            
                printf("client send acks for reqids :");
                socklen_t  size_addr= sizeof(current_acked_reqs->servaddr);
                unsigned char send_acks[current_acked_reqs->num_of_requests*sizeof(int)+5] ;
                send_acks[0] = 'A';
                int size_of_packet = (current_acked_reqs->num_of_requests) * 4;
                send_acks[1] = (size_of_packet >> 24) & 0xFF;
                send_acks[2] = (size_of_packet>> 16) & 0xFF;
                send_acks[3] = (size_of_packet >> 8) & 0xFF;
                send_acks[4] = size_of_packet & 0xFF;
                int j=5;
                for(int i=0; i<current_acked_reqs->num_of_requests; i++){
                         printf("%d",(current_acked_reqs->request_id_apis)[i]);
                        send_acks[j] = ((current_acked_reqs->request_id_apis)[i] >> 24) & 0xFF;
                        send_acks[j+1] = ((current_acked_reqs->request_id_apis)[i] >> 16) & 0xFF;
                        send_acks[j+2] = ((current_acked_reqs->request_id_apis)[i]>> 8) & 0xFF;
                        send_acks[j+3] = (current_acked_reqs->request_id_apis)[i] & 0xFF;
                        j=4+j;
                }
                current_acked_reqs->num_of_requests =0;
                printf("\n");
                sendto(socket_fd,send_acks,sizeof(send_acks), MSG_CONFIRM, (const struct sockaddr *) &(current_acked_reqs->servaddr),size_addr); 
                
            }
            current_acked_reqs = current_acked_reqs->next ;
        }
         pthread_mutex_unlock(&lock_acked_reqs);


    }
}
void * retransmition(){
    struct unacked * current ;
    
    while(1){
        sleep(1); 
        pthread_mutex_lock(&lock_ack);
        for(current = head_ack->next ; current != NULL ; current = current->next ){
        // time = localtime(&s);
            if((clock()- current->time)/CLOCKS_PER_SEC >= 3 && current->data!=NULL){
                if (current->server_slow >0 ){
                    current->server_slow--;
                    printf("THREAD RETRANSMITION : SERVER BUSY FOR %d\n", current->request_id_api);
                    continue;
                }
                if(current->retransmit_times < 8){
                    socklen_t  size_addr= sizeof(current->servaddr);
                    // printf("retransmit size : %ld",sizeof(*(current->data)));
                    sendto(socket_fd, current->data,1024, MSG_CONFIRM, (const struct sockaddr *) &current->servaddr,size_addr); 
                    current->retransmit_times++;
                    current->time = clock();
                    printf("THREAD RETRANSMITION : RETRANSMITS FOR REQID %d\n", current->request_id_api);
                }
            }
        }
       pthread_mutex_unlock(&lock_ack);
    }

}
void *read_from_socket(){
    struct sockaddr_in  servaddr; 
    servaddr.sin_addr.s_addr = INADDR_ANY; 
    servaddr.sin_family = AF_INET; 
    servaddr.sin_port = htons(PORT); 
    unsigned char type ;
    unsigned char reqid_rcv[sizeof(int)];
    unsigned char size_of_data[sizeof(int)];
   
    int int_reqid_rcv;
    int int_size_of_data;
    void* receiving;
    ssize_t check ;
    
  while(1){
      receiving = (char*)malloc(MAXLINE*sizeof(char));
     
        socklen_t  size_addr= sizeof(servaddr);
        check =  recvfrom(socket_fd, receiving, MAXLINE, MSG_WAITALL, (struct sockaddr *) &servaddr,&size_addr ); 
        //  printf("Thread read something %ld\n",check );
    
        if(check == -1){
            //fprintf(stderr, "recv: %s (%d)\n", strerror(errno), errno);
          //  printf("telos\n");
            free(receiving);
            break;
        }else{
           // fprintf(stderr, "recv: %s (%d)\n", strerror(errno), errno);
            type = ((char*)receiving)[0] ;
            if(type == 'R'){
                size_of_data[0] = ((char*)receiving)[1] ;
                size_of_data[1] = ((char*)receiving)[2] ;
                size_of_data[2] = ((char*)receiving)[3] ;
                size_of_data[3] = ((char*)receiving)[4] ;
                int_size_of_data = (size_of_data[0]<< 24| size_of_data[1]<< 16 | size_of_data[2]<< 8 |size_of_data[3]);
                reqid_rcv[0] = ((char*)receiving)[5] ;
                reqid_rcv[1] = ((char*)receiving)[6] ;
                reqid_rcv[2] = ((char*)receiving)[7] ;
                reqid_rcv[3] = ((char*)receiving)[8] ;
                int_reqid_rcv = (reqid_rcv[0]<< 24| reqid_rcv[1]<< 16 | reqid_rcv[2]<< 8 |reqid_rcv[3]);
            }else{//C D A
                reqid_rcv[0] = ((char*)receiving)[1] ;
                reqid_rcv[1] = ((char*)receiving)[2] ;
                reqid_rcv[2] = ((char*)receiving)[3] ;
                reqid_rcv[3] = ((char*)receiving)[4] ;
                int_reqid_rcv = (reqid_rcv[0]<< 24| reqid_rcv[1]<< 16 | reqid_rcv[2]<< 8 |reqid_rcv[3]);

            }
              
            pthread_mutex_lock(&lock);

            for (struct request_couples*  current = head ; current !=NULL ; current = current->next){
                
                if(current->request_id_api == int_reqid_rcv){
                    if(type == 'C'){
                        printf("client sends that request %d is alive \n", int_reqid_rcv);
                        unsigned char send_alive[5];
                        send_alive[0] = 'C';
                        send_alive[1] = (int_reqid_rcv >> 24) & 0xFF;
                        send_alive[2] = (int_reqid_rcv>> 16) & 0xFF;
                        send_alive[3] = (int_reqid_rcv >> 8) & 0xFF;
                        send_alive[4] = int_reqid_rcv & 0xFF;
                        socklen_t  size_addr= sizeof(servaddr);
                        sendto(socket_fd,send_alive ,5, MSG_CONFIRM, (const struct sockaddr *) &(servaddr),size_addr); 
                        
                    }else if(type == 'D' ){
                        if(current->confirm == -1){
                            printf("client finds an available server for request %d\n", int_reqid_rcv);
                            current->confirm = 0;
                            current->servaddr = servaddr;
                        }
                    }
                    else if(type == 'R') {
                        struct unacked *current_acks ;
                        struct unacked *previous ;
                        pthread_mutex_lock(&lock_ack);
                         //printf("THREAD LOCKS_ACK\n");
                        previous = head_ack ;
                        for(current_acks = head_ack->next ; current_acks!=NULL; current_acks = current_acks->next){
                            
                            if(current_acks->request_id_api == int_reqid_rcv){
                                previous->next = current_acks->next;

                            }
                            previous = current_acks ;
                        }
                        pthread_mutex_unlock(&lock_ack);
                      //   printf("THREAD UNLOCKS\n");
                        unsigned char out  [int_size_of_data];
                        for(int i=0; i< int_size_of_data; i++){
                            out[i] = ((char*)receiving)[i+9] ;
                        }
                   

                        current->replies = (unsigned char*)malloc(sizeof(unsigned char)*sizeof(out));
                        current->replies = out;
                        pthread_mutex_lock(&lock_acked_reqs);
                        struct acked_reqs *current_acked_reqs = head_acked_reqs->next;
                        struct acked_reqs *previous_acked_reqs = head_acked_reqs;
                        int found = 0 ;
                     
                        while(current_acked_reqs != NULL){

                            if(current_acked_reqs->servaddr.sin_port == servaddr.sin_port && current_acked_reqs->servaddr.sin_addr.s_addr == servaddr.sin_addr.s_addr) {
                          
                                int int_reqid_rcv = (reqid_rcv[0]<< 24| reqid_rcv[1]<< 16 | reqid_rcv[2]<< 8 |reqid_rcv[3]);
                                current_acked_reqs->request_id_apis[current_acked_reqs->num_of_requests] = int_reqid_rcv;
                                current_acked_reqs->num_of_requests++;
                                //printf("THREAD 1 PROSTHETI\n");
                                
                                found = 1 ;
                               

                            }
                            previous_acked_reqs = current_acked_reqs ;
                            current_acked_reqs  = current_acked_reqs ->next ;
                            
                        }
                        if (found == 0 ){
                            struct acked_reqs *new_acked_reqs = (struct acked_reqs *)malloc(sizeof(struct acked_reqs));
                            //new_acked_reqs->request_id_api = (char *) malloc(sizeof(char)) ;
                            new_acked_reqs->num_of_requests =0;
                            //*(new_acked_reqs->request_id_api ) = 'A' ;
                           // printf("THREAD 1 PROSTHETI FOUND = 0\n");
                            new_acked_reqs->servaddr = servaddr;
                            new_acked_reqs->next = NULL ;
                            previous_acked_reqs->next = new_acked_reqs ;
                        }
                        
                        pthread_mutex_unlock(&lock_acked_reqs);
                       
                    }
                    else if (type == 'A'){
                        pthread_mutex_lock(&lock_ack);

                        struct unacked *curr_unacked;
                        curr_unacked = head_ack;
                        while(curr_unacked ->next != NULL){
                            curr_unacked  = curr_unacked ->next;
                            if(curr_unacked->request_id_api == int_reqid_rcv)
                                break;
                        }   
                       // printf("THREAD 1 :PHRA ACK KAI PERIMENV FOR REQID %d\n" , int_reqid_rcv);
                        curr_unacked->server_slow = 2;
                        pthread_mutex_unlock(&lock_ack);
                       //  printf("THREAD UNLOCKS ACK\n");
                        
                    }
                    break;
                }
            }
            pthread_mutex_unlock(&lock);
            
            free(receiving);
           // printf("ekana free kai eimai ti thread\n");
        }
    }
    return NULL;

}

/////////////init function/////////////////////
int init_connection(){

    pthread_mutex_init(&lock,NULL);
    pthread_mutex_init(&lock_ack,NULL);
    pthread_mutex_init(&lock_acked_reqs,NULL);

    head = (struct request_couples*)malloc(sizeof(struct request_couples)); 
    head->replies = NULL;
    head->request_id_api = -1;
    head->request_id_user = -1;
    head->confirm = -1;
    head->next = NULL;

    head_ack = (struct unacked*)malloc(sizeof(struct unacked)); 
    head_ack->request_id_api = -1;
    head_ack->time = 0;
    head_ack->next = NULL;

    head_acked_reqs = (struct acked_reqs*)malloc(sizeof(struct acked_reqs)); 
    head_ack->next = NULL;


    reqid_user =0;

    socket_fd=socket(AF_INET,SOCK_DGRAM,0); 
    if(socket_fd < 0){
        perror("Opening datagram socket error");

        return 1;
    }
    else{
        printf("Opening the datagram socket...OK.\n");
          iret1 = pthread_create( &thread1, NULL, read_from_socket, NULL);
           if(iret1){
               fprintf(stderr,"Error - pthread_create() return code: %d\n",iret1);
               exit(EXIT_FAILURE);
           }
           printf("pthread_create() for thread 1 returns: %d\n",iret1);

            iret2 =pthread_create( &thread2, NULL, retransmition, NULL);
            if(iret2){
               fprintf(stderr,"Error - pthread_create() return code: %d\n",iret1);
               exit(EXIT_FAILURE);
           }
           printf("pthread_create() for thread 2 returns: %d\n",iret2);




            iret3 = pthread_create( &thread3, NULL, send_acked_reqs, NULL);
           if(iret3){
               fprintf(stderr,"Error - pthread_create() return code: %d\n",iret1);
               exit(EXIT_FAILURE);
           }
           printf("pthread_create() for thread 3 returns: %d\n",iret1);

        return 0;
    }  
}


////////////////
struct sockaddr_in discover_server(int svcid,int request_id_api){
    struct sockaddr_in  servaddr;
    servaddr.sin_addr.s_addr = inet_addr("224.0.0.1");//INADDR_ANY;  
    servaddr.sin_family = AF_INET; 
    servaddr.sin_port = htons(PORT);
    //char *sendtoserver = (char*)malloc(sizeof(char)*4);
    //sprintf(sendtoserver, "%d", svcid);
    unsigned char sendtoserver[9];

    sendtoserver[0] = 'D';
    sendtoserver[1] = (request_id_api >> 24) & 0xFF;
    sendtoserver[2] = (request_id_api >> 16) & 0xFF;
    sendtoserver[3] = (request_id_api >> 8) & 0xFF;
    sendtoserver[4] = request_id_api & 0xFF;
    sendtoserver[5] = (svcid >> 24) & 0xFF;
    sendtoserver[6] = (svcid >> 16) & 0xFF;
    sendtoserver[7] = (svcid >> 8) & 0xFF;
    sendtoserver[8] = svcid & 0xFF;
    printf("client sends multicast to Discover server for reqapi %d  %ld\n", request_id_api,pthread_self());
    pthread_mutex_lock(&lock);

    socklen_t  size_addr= sizeof(servaddr);
    clock_t time =  clock();
    sendto(socket_fd,sendtoserver ,9, MSG_CONFIRM, (const struct sockaddr *) &servaddr,size_addr); 
    struct request_couples*  current;
    int stop = 0;
    pthread_mutex_unlock(&lock);

    while(1){
        pthread_mutex_lock(&lock);
 
        current = head ;
        while ( current!=NULL  ){
            if(current->request_id_api == request_id_api){
                stop =1;
                break;
            }
            current = current->next;
        }
        if(stop == 1){
            pthread_mutex_unlock(&lock);
           
            break;
        }
        pthread_mutex_unlock(&lock);
  
    }
    
    int redescover_times = 0;
    while(1){
        if(redescover_times > 5){
            printf("client not finds available server for  %d pid  %ld\n", request_id_api, pthread_self());
            struct sockaddr_in return_null;
            return_null.sin_port = 0;
            return return_null;
        }else{
            pthread_mutex_lock(&lock);
            if(current->confirm == 0){
             
                pthread_mutex_unlock(&lock);
                break;
            }
            if((clock()- time)/CLOCKS_PER_SEC >=3 ){
                             

                redescover_times ++;
                printf("client redescovers : redescover times %d  REID API %d pid %ld:\n",redescover_times, request_id_api, pthread_self());
                sendto(socket_fd,sendtoserver ,9, MSG_CONFIRM, (const struct sockaddr *) &servaddr,size_addr); 
                time =  clock();
            }

            pthread_mutex_unlock(&lock);
        }
    }             
    return current->servaddr;
}
int send_request(int svcid, void *buffer, int len){
    pthread_mutex_lock(&lock);
    
    if( access( "client_data.txt", F_OK ) == 0 ) {
        
        fd  =open("client_data.txt", O_RDWR | O_CREAT );
        flock(fd,LOCK_EX);
        
        read(fd,&reqid_api,sizeof(int));

        lseek(fd,0,SEEK_SET);
    } else {
        fd  =open("client_data.txt", O_RDWR | O_CREAT );
        flock(fd,LOCK_EX);
        reqid_api = 0;
        write(fd,&reqid_api,sizeof(int));
        lseek(fd,0,SEEK_SET);
    }
    reqid_user++;
    reqid_api++;
    
    write(fd,&reqid_api,sizeof(int));
    lseek(fd,0,SEEK_SET);
    close(fd);
    flock(fd, LOCK_UN);
///////////////////////////////////////////////
    //add new node on request list 
    struct request_couples* curr ;
    struct unacked* curr_unacked ;

   
    curr = head;
    while(curr->next != NULL){
        curr = curr->next;
    }

 
    struct request_couples* new_request = (struct request_couples*)malloc(sizeof(struct request_couples)); 
    struct unacked *new_unacked = (struct unacked*)malloc(sizeof(struct unacked));

    new_request->replies = NULL;
    new_request->request_id_api = reqid_api;
    new_request->request_id_user = reqid_user;
    new_request->confirm = -1;
    new_request->next = NULL;
    curr->next = new_request;

///////////////////////////////////////////////////////PROSTHIKI STIN LISTA ANACKED////////////////
    
    new_unacked->request_id_api = reqid_api;
    new_unacked->next = NULL;
    new_unacked->time = clock();
    new_unacked-> retransmit_times = 0;
    new_unacked->server_slow = 0 ;
    pthread_mutex_lock(&lock_ack);
     //printf("SEND REQUEST LOCKS\n");
    curr_unacked = head_ack;
    while(curr_unacked ->next != NULL){
        curr_unacked  = curr_unacked ->next;
    }
    curr_unacked->next = new_unacked;
    pthread_mutex_unlock(&lock_ack);
   // printf("SEND REQUEST UNLOCKS\n");
  
    ///////////////////////////////

    struct sockaddr_in  servaddr;
    //discover
    pthread_mutex_unlock(&lock);


    servaddr =  discover_server(svcid,new_request->request_id_api );
    if(servaddr.sin_port == 0){
        return -1;
    }
    pthread_mutex_lock(&lock);

    unsigned char message[len+2*sizeof(int)+1];
    int size_of_packet = len;
    printf("clients %ld for reqid api %d and num  %d pid %ld\n",pthread_self(),new_request->request_id_api , *((int*)buffer), pthread_self());
    message[0] = 'R';
    message[1] = (size_of_packet >> 24) & 0xFF;
    message[2] = (size_of_packet>> 16) & 0xFF;
    message[3] = (size_of_packet >> 8) & 0xFF;
    message[4] = size_of_packet & 0xFF;
    message[5] = (new_request->request_id_api  >> 24) & 0xFF;
    message[6] = (new_request->request_id_api >> 16) & 0xFF;
    message[7] = (new_request->request_id_api  >> 8) & 0xFF;
    message[8] = new_request->request_id_api  & 0xFF;

    int j = 0;
    int data;
    
    for(int i = 9 ; i < len+9; i=i+4){
        data = ((int*)buffer)[j];
        message[i] = (data>> 24) & 0xFF;
        message[i+1] = (data>> 16) & 0xFF;
        message[i+2] = (data>> 8) & 0xFF;
        message[i+3] = data & 0xFF;
        j++;
    }

    pthread_mutex_lock(&lock_ack);
   
    new_unacked->data = (unsigned char*)malloc(sizeof(unsigned char) * sizeof(message));
    new_unacked->servaddr = servaddr;
     for(int i = 0 ; i < sizeof(message); i++){
        *((unsigned char*)(new_unacked->data+i)) = message[i];
        
    }
    pthread_mutex_unlock(&lock_ack);
  
    
    pthread_mutex_unlock(&lock);
 
    socklen_t  size_addr= sizeof(servaddr);
    sendto(socket_fd, message ,sizeof(message) , MSG_CONFIRM, (const struct sockaddr *) &servaddr,size_addr); 
    
    return  new_request->request_id_user;
}

int getReply (int reqid, void** buf, int *len, int block){
    int reqid_api;
    int check_for_stop = 0;
    int stop = 0;
    *buf= (unsigned char*)malloc(30*sizeof(char));
    // read_from_socket();
    pthread_mutex_lock(&lock);
    struct request_couples*  prev = head;

    for (struct request_couples*  current = head ; current !=NULL ; current = current->next){
        if(current->request_id_user == reqid ){
            reqid_api = current->request_id_api;
            if(block == 1){
                pthread_mutex_unlock(&lock);
             
                while(1){
                    if(check_for_stop == 20){
                        pthread_mutex_lock(&lock_ack);
                        struct unacked *current_ack,*prev_ack;
                        prev_ack = head_ack;
                        for(current_ack = head_ack->next ; current_ack != NULL ; current_ack = current_ack->next ){
                        if(current_ack->request_id_api == reqid_api){
                            if(current_ack->retransmit_times < 8){
                                check_for_stop =0;
                            }else{   
                                prev_ack->next = current_ack->next;
                                //free(current_ack->data);
                                //free(current_ack);
                                stop =1;
                            }
                        }
                        prev_ack = current_ack;
                        }
                        pthread_mutex_unlock(&lock_ack);
                        if(stop == 1){
                            return -1;
                        }
                    }
                    pthread_mutex_lock(&lock);
                    if(current->replies != NULL){
                         break;
                    }
                    pthread_mutex_unlock(&lock);
                    check_for_stop++;
                }
                *buf =current->replies ;
               
                *len = sizeof(*current->replies);
                prev->next = current->next;
                // free(current->replies);
                // free(current);
                break;
            }else{
                //den thelei na mplokarei
                if(current->replies == NULL){
                    prev->next = current->next;
                    // free(current->replies);
                    // free(current);
                    pthread_mutex_unlock(&lock);
                    pthread_mutex_lock(&lock_ack);
                    struct unacked *current_ack,*prev_ack;
                    prev_ack = head_ack;
                    for(current_ack = head_ack->next ; current_ack != NULL ; current_ack = current_ack->next ){
                        if(current_ack->request_id_api == reqid_api){
                            prev_ack->next = current_ack->next;
                            // free(current_ack->data);
                            // free(current_ack);
                        }
                        prev_ack = current_ack;
                    }
                    pthread_mutex_unlock(&lock_ack);
                    return -1;
                }else{
                    *buf =current->replies ;
                    int result =(((char*)current->replies)[0]<< 24| ((char*)current->replies)[1]<< 16 | ((char*)current->replies)[2]<< 8 |((char*)current->replies)[3]);
                   printf("GETREPLY :current->replies %d\n",result);
                    result =(((char*)buf)[0]<< 24| ((char*)buf)[1]<< 16 | ((char*)buf)[2]<< 8 |((char*)buf)[3]);
                    *len = sizeof(*current->replies);
                    prev->next = current->next;
                    // free(current->replies);
                    // free(current);
                    break;
                }
            
                

            }
        }
        prev = current;
    }
  
    pthread_mutex_unlock(&lock);
    return 0;
  
}


 void terminate(){
    close(socket_fd);

    pthread_join( thread1, NULL);
    
    pthread_cancel(thread2);
    printf("join re \n");
    pthread_join(thread3,NULL);
    //pthread_exit( thread1);
 }