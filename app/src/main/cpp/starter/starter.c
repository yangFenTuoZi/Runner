#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <ctype.h>
#include <grp.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <pty.h>
#include <sys/wait.h>

typedef struct {
    uid_t uid;
    gid_t gid;
    gid_t* groups;
    size_t groups_count;
} UserInfo;

// Constants for fixed command execution
static const char* BASH_ARGS[] = {"/data/local/tmp/runner/usr/bin/bash", NULL};

void parse_uid_gid(const char* arg, UserInfo* info) {
    // Initialize structure
    memset(info, 0, sizeof(UserInfo));

    if (!arg || !*arg) return;

    char* str = strdup(arg);
    if (!str) {
        perror("strdup failed");
        return;
    }

    // First token - UID
    char* token = strtok(str, ",");
    if (token) info->uid = (uid_t)atoi(token);

    // Second token - GID
    token = strtok(NULL, ",");
    if (token) info->gid = (gid_t)atoi(token);

    // Count additional groups
    size_t count = 0;
    char* tmp = str;
    while ((tmp = strchr(tmp, ','))) {
        count++;
        tmp++;
    }
    count = count > 2 ? count - 2 : 0;

    if (count > 0) {
        info->groups = calloc(count, sizeof(gid_t));
        if (!info->groups) {
            perror("malloc failed");
            free(str);
            return;
        }

        // Parse groups
        for (size_t i = 0; i < count; i++) {
            token = strtok(NULL, ",");
            if (token) info->groups[i] = (gid_t)atoi(token);
        }
        info->groups_count = count;
    }

    free(str);
}

int set_user_groups(const UserInfo* info) {
    if (info->gid != 0 && setgid(info->gid) != 0) {
        perror("setgid failed");
        return 0;
    }

    if (info->groups_count > 0 && setgroups(info->groups_count, info->groups) != 0) {
        perror("setgroups failed");
        return 0;
    }

    if (info->uid != 0 && setuid(info->uid) != 0) {
        perror("setuid failed");
        return 0;
    }

    return 1;
}

void free_user_info(UserInfo* info) {
    if (info) {
        free(info->groups);
        info->groups = NULL;
        info->groups_count = 0;
    }
}

int main(int argc, char* argv[]) {
    UserInfo user_info = {0};

    // Parse UID/GID argument
    if (argc > 1) {
        if (isdigit(argv[1][0]) && strcmp(argv[1], "-1") != 0) {
            parse_uid_gid(argv[1], &user_info);
        }
    }

    // Set user and group IDs if specified
    if ((user_info.uid != 0 || user_info.gid != 0 || user_info.groups_count > 0) &&
        !set_user_groups(&user_info)) {
        free_user_info(&user_info);
        return EXIT_FAILURE;
    }

    free_user_info(&user_info);

    return execvp(BASH_ARGS[0], (char *const *) BASH_ARGS);
}

