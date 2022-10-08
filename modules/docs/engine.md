---
id: engine
title: "Walk Engine"
---

## Custom Graph Partition

One of the most significant caveats of random walks in a distributed environment is the amount of data exchanged between nodes at every step. Indeed, to send a walker to his next vertice, you need to send both the walker (relatively small) and a message for second-order random walks.

In spark, message passing is costly as it's equivalent to data shuffling. To reduce the overall shuffling but still keep a high level of parallelism, we need to work at the executor level instead of the partition level.

So, inspired by the talk of Min Shen at Spark Summit 2017 [[talk](https://www.youtube.com/watch?v=lyVZNZZUdOk)], a custom graph partition is implemented, being an adjacency list shared at the executor level. When sending a worker to his next step, we only need to shuffle the data if the next vertice is not in the same executor instead of being in a different partition, thus reducing the amount of shuffled data.

**aruku is distributed, but not out of core**

## Routing Table

With this custom graph partition, we have lost many cool features provided by GraphX being : 
- distributed graph storing,
- distributed message passing,
- fault tolerance.

To get the distributed message passing, we need to repartition our walkers with the right partitioner to get them in the required executors. We first aggregate the input graph by collecting neighbor edges of each vertex of the graph resulting in a PairRDD keyed by vertex ID. This PairRDD is repartitioned with the partitioner from the original VertexRDD, and custom graph partitions are built. Then the partitioner is used to send the walkers to the suitable executors by repartitioning the walkers PairRDD keyed by the vertex ID to which they need to be sent.

To get the fault tolerance back, we will use a routing table instead of just repartitioning our PairRDD of walkers. When building custom graph partitions from the PairRDD of neighbor edges, we keep the resulting empty RDD. So, after repartitioning the walkers PairRDD, we use the zipPartition function of this empty RDD with the walkers PairRDD to create a RDD lineage that would break if spark executors are lost, or tasks are failed.

**aruku is fault tolerant**

## Optimized Edge Sampling

Another tricky part of implementing efficient higher-order random walks for big graphs is the implementation of an efficient edge sampling algorithm. Indeed if the alias sampling works well to precompute sampling for the transition probabilities, it's only suited for static walks. For dynamic walks, the space of possible transitions is too huge to precompute, thus requiring a clever algorithm.

KnightKing [[github](https://github.com/KnightKingWalk/KnightKing)] is a general-purpose, distributed graph random walk engine that proposed several optimizations to random walk engines. One of these optimizations is an optimized rejection sampling to do edge sampling efficiently.

**aruku is fast, but probably slower than KnightKing**