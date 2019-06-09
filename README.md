#DIBAS

DIBAS (**D**istributed & **I**nteroperable load-**B**alancer by **A**rthur and **S**eibel) 

### Distributed?
DIBAS runs it's load balancing on every machine of the cluster, unlike usual master-slave architectures (free the 
nodes!).

### Interoperable?
DIBAS is considered interoperable (and, in effect, **portable** as well) because it's built with Kotlin, so it can be
 compiled to several different runtimes, SOs and architectures in general. Each node has communication 
 interoperability with the others regardless of their characteristics, it's fully agnostic to that (the cluster could
 be indeterminately heterogeneous, being comprised of PCs, phones, R-Pies, anything that Kotlin compiles to).
 
### Load-Balancer?
DIBAS makes nodes continuously communicate with it's cluster-neighbors distributing the tasks they have to carry out. 
If a node has to many enqueued tasks, and it's neighbors have less work to, it can cheaply (more on that later) send 
tasks to these neighbors, effectively balancing, homogenizing the load of the cluster. Also, with DIBAS, each node 
can be an entrypoint for jobs/tasks requests, since they automatically distribute the load to the rest of the 
cluster, balancing it in real-time. This avoids bottlenecks in data/tasks ingress on the system, since it can be 
injected on the cluster in parallel.

### 
# Spec

Cada grupo deverá entregar uma especificação mais detalhada com um plano de atividades do TF contendo itens como:

- [ ] linguagens;
- [ ] bibliotecas;
- [ ] modelagem dos algoritmos ou aplicação;
- [ ] hw dos experimentos;
- [ ] variações nos experimentos como: quantidade de cpus e/ou cores, tamanhos e conteúdos das entradas, variações
em parâmetros da plataforma de sw e/ou da aplicação (algoritmos), ...;
- [ ] gráficos a serem produzidos com os resultados dos experimentos.

Esse plano será usado para verificação de uma complexidade mínima do TF.

- [ ] Volume do plano: de 1 a 2 páginas de texto, itemizado. Ou entre 5 a 10 slides.

Prazo: até 14/06/2019, 12h00