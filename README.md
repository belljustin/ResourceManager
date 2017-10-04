# Resource Manager

COMP 512 - Distributed Computing Project

## How to
1. `cp java.policy.template` to `java.policy`
2. Edit the policy to point to your project bin path
3. run `ant`
4. `source .env` to set project and class path
5. use launch files in `scripts` to start system. `cd scripts`
    1. `./server_start.sh`
    2. `./middleware_start.sh`
    3. `./client_start.sh`
