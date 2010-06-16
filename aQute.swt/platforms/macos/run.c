#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

char line[ 100000 ];

int main(int argc, char * argv[]) {
    char path[1000];
    char * home = getenv("AQUTE_HOME");
    int l = strlen(argv[0]);
    char * command;

    while ( l > 0 && argv[0][l] != '/')
        l--;

    command = argv[0] + l+1;

    if ( !home )
       home = "/usr/bundles";

    strncpy(path,home,sizeof(path));
    strcat(path,"/commands.idx");

    FILE * f = fopen(path,"r");
    if ( f == NULL ) {
        printf("Cannot find command index at %s, error %d\n", path, errno );
        return -1;
    }

    int c = getc(f);
    int n=0;
    while ( c != EOF ) {
        if ( c == '\n') {
                printf("%s %s\n", command, line);
           if ( strncmp(command, line, strlen(command)) == 0) {
               int i;
               fclose(f);

               for ( i =1; i<argc; i++ ) {
                    n = append(line,n, " \"", 0);
                    n = append(line,n, argv[i], 1);
                    n = append(line,n, "\"", 0);
               }
               line[n]=0;
               return system(line+strlen(command)+1);
           }
           n = 0;
        } else {
            line[n++]=(char) c;
        }
        c = getc(f);
    }
    printf("Cannot find command %s\n", argv[0]);
    return -1;
}

int append( char * to, int n, char * p ) {
    int i =0;
    int l = strlen(p);
    int offset = 0;

    for (i=0; i<l; i++ ) {
        if ( p[i] == '"' ) { // Other escape chars?
            to[n + i + offset++] = '\\';
        }

        to[n + i + offset] = p[i];

        if ( l + n  + offset > sizeof(line) ) {
            printf("Command line too large");
            exit(-1);
        }
    }
    return n + l + offset;
}
