#DIBAS

DIBAS (**D**istributed & **I**nteroperable load-**B**alancer by **A**rthur and **S**eibel) 

### Distributed?  🌐
DIBAS runs its load balancing on every machine of the cluster, unlike usual master-slave architectures (free the 
nodes!).

### Interoperable? 🤝
DIBAS interoperable (and, in effect, **portable** as well) because it's built with Kotlin, so it can be compiled to 
several different runtimes, SOs and architectures in general. Each node has communication interoperability with the 
others regardless of their characteristics, it's fully agnostic to that (the cluster could be indeterminately 
heterogeneous, being comprised of PCs, phones, R-Pies, anything that Kotlin compiles to).
 
### Load-Balancer? ⚖
DIBAS makes nodes continuously communicate with it's cluster-neighbors distributing the tasks they have to carry out. 
If a node has to many enqueued tasks, and it's neighbors have less work to, it can cheaply (more on that later) send 
tasks to these neighbors, effectively balancing, homogenizing the load of the cluster. Also, with DIBAS, each node 
can be an entrypoint for jobs/tasks requests, since they automatically distribute the load to the rest of the 
cluster, balancing it in real-time. This avoids bottlenecks in data/tasks ingress on the system, since it can be 
injected on the cluster in parallel.

### Adolfo and Seibel? 👥
This projects is authored by Arthur Adolfo and Gabriel Seibel 😎

## How to DIBAS? 🤔
Some info on the project's characteristics and objectives:

- Language:
    - Kotlin

- Libraries: 
    - [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
    - [ktor](https://ktor.io)

- Modeling:
    - DIBAS will run coroutines to execute jobs and communicate between machines (with websockets).
    - The algorithm for load distribution is similar to a flood-over-graph algorithm.
    - Each node compares it's load (mainly jobs, but possibly it's HW resources usage) to it's neighbors, forwarding 
    some of it to less-busy neighbors.

- HW for experiments: (on which Docker containers will run)
    - Intel(R) Core(TM) i7-4790K CPU @ 4.00GHz
    - 16GB RAM

- Experiments parameters:
    - Cluster size and graph format (neighbors connections)
    - Number of jobs to execute
    - Jobs quantity and entrypoints
    
- Results (artifacts):
    - DIBAS software per-se
    - DIBAS effect on cluster's job distribution over time (graphs of individual nodes or gif of whole cluster)
    - Time measurement's of cluster execution of some sets of tasks