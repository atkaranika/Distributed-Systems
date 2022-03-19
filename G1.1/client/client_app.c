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
#define PORT     8080 
#define MAXLINE   1024 
#include "clientapi.h" 


pthread_mutex_t lock1 ;
pthread_mutex_t lock2 ;
int reqid_api;

struct thread_info{
    int *num ; 
    int available ;
    pthread_mutex_t lock ;
};


void *thread_code(void *struct_info){
    struct_info = (struct thread_info*) struct_info;
    
     
   unsigned char *buf= (unsigned char*)malloc(1024*sizeof(char));
  //  int len_send = sizeof(*sendtoserver) ;
    int len ;
    while(1){
            pthread_mutex_lock(&((struct thread_info*)struct_info)->lock) ;
            if(((struct thread_info*)struct_info)->available == 1){
                pthread_mutex_unlock(&((struct thread_info*)struct_info)->lock) ;
                break;
            }
            pthread_mutex_unlock(&((struct thread_info*)struct_info)->lock) ;

        while(1){
      
            if(((struct thread_info*)struct_info)->num != NULL){
                pthread_mutex_unlock(&((struct thread_info*)struct_info)->lock) ;
                break ;
            }
            pthread_mutex_unlock(&((struct thread_info*)struct_info)->lock) ;

        }
        
       
       struct thread_info info = *(struct thread_info*) struct_info;;
       printf("thread  %d, %ld\n",*(info.num), pthread_self());


        //clock_t start_t , end_t ;
        //double total_t ;
        //start_t = clock();
        int req_id_user =  send_request(1, ((struct thread_info*)struct_info)->num,  sizeof(*((struct thread_info*)struct_info)->num));

            if(req_id_user == -1){
                printf("CLIENT PID %ld :SERVICE NOT FOUND FROM SEND REQUEST \n", pthread_self());
                continue ;
             
            }
            
            int what = getReply(req_id_user, (void**)&buf,  &len,1); // block ???
            if(what == -1){
                printf("-----------------------CLIENT PID %ld :SERVER DIES%d\n",pthread_self(), *((struct thread_info*)struct_info)->num);
            }else{
                /////////////////////////////mideniki leitourgia
              //  end_t = clock();
               // total_t = (double)(end_t - start_t)/CLOCKS_PER_SEC;
                //printf("mideniki litourgia %lf\n", total_t);
                int result =(((unsigned char*)buf)[0]<< 24| ((unsigned char*)buf)[1]<< 16 | ((unsigned char*)buf)[2]<< 8 |((unsigned char*)buf)[3]);

              
                if(result == 0){
                    printf("-------------------CLIENT PID %ld :PRIME NUMBER %d, %ld\n", pthread_self(),*((struct thread_info*)struct_info)->num, pthread_self());

                }else{
                    printf("---------------------CLIENT PID %ld :NOT PRIME NUMBER %d, %ld\n",pthread_self(),*((struct thread_info*)struct_info)->num, pthread_self());
                }
            }
            pthread_mutex_lock(&((struct thread_info*)struct_info)->lock);
            free(((struct thread_info*) struct_info)->num );
            ((struct thread_info*) struct_info)->num = NULL;
            pthread_mutex_unlock(&((struct thread_info*)struct_info)->lock);

    }
    return NULL ;
}

int main(int argc, char * argv[]) {
    time_t t;
    
    struct thread_info *sendtoserver_client1 = (struct thread_info *)malloc(sizeof(struct thread_info ));
    struct thread_info *sendtoserver_client2 = (struct thread_info *)malloc(sizeof(struct thread_info ));

   
    int ckeck =  init_connection();
    reqid_api = 0;
 
    pthread_mutex_init(&sendtoserver_client1->lock,NULL);
    pthread_mutex_init(&sendtoserver_client2->lock,NULL);
    pthread_t client1 ;
    pthread_t client2 ;
    if(ckeck == 1){
        exit(1);
    }
    else{
        sendtoserver_client1->num = NULL ;
        sendtoserver_client2->num = NULL;
        sendtoserver_client1->available = 0;
        sendtoserver_client2->available = 0;
        pthread_create(&client1, NULL, thread_code,(void*)(sendtoserver_client1));
        pthread_create(&client2, NULL, thread_code,(void*)(sendtoserver_client2));
       
         /* Intializes random number generator */
        srand((unsigned) time(&t));

        /* Print 5 random numbers from 0 to 49 */


        for( int i = 0 ; i < 30 ; i++ ) {
            while(1){
                pthread_mutex_lock(&sendtoserver_client1->lock);
                if(sendtoserver_client1->num == NULL){
                
                    sendtoserver_client1->num = (int*)malloc(sizeof(int));
                    *(sendtoserver_client1->num) = rand() % 500;
                    printf("main client %d\n", *(sendtoserver_client1->num));
                    pthread_mutex_unlock(&sendtoserver_client1->lock);
                    break ;
                    
                }
                
                pthread_mutex_unlock(&sendtoserver_client1->lock);
            
                pthread_mutex_lock(&sendtoserver_client2->lock);
                if (sendtoserver_client2->num == NULL){
                    sendtoserver_client2->num = (int*)malloc(sizeof(int));
                    *(sendtoserver_client2->num) = rand() % 500;
                    printf("main client  %d\n", *(sendtoserver_client2->num));
                    pthread_mutex_unlock(&sendtoserver_client2->lock);
                    break ;
                }
                pthread_mutex_unlock(&sendtoserver_client2->lock);
                
            }
        }
        pthread_mutex_lock(&sendtoserver_client1->lock);
        sendtoserver_client1->available = 1;
        pthread_mutex_unlock(&sendtoserver_client1->lock);
        pthread_mutex_lock(&sendtoserver_client2->lock);
        sendtoserver_client2->available = 1;
        pthread_mutex_unlock(&sendtoserver_client2->lock);
   
}
    pthread_join(client1,NULL);
    pthread_join(client2,NULL);
    terminate();
    return 0;
}

