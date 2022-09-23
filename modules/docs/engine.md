---
id: engine
title: "Walk Engine"
---

## Custom Graph Partition

One of the bigeast caveats of random walks in distributed environment is the ammount of data exchanged between every steps. Indeed to send a walker to his next vertice, you need to send both the walker (quite small) and a message for second order random walks.

In spark, message passing is costly as it means shuffling of the data. In order to reduce the shuffling, but keep a high level of parallelism, we need to work not at the partition level but at the executor level.

So inspired by the talk of Min Shen at Spark Summit 2017, a custom graph partition is implemented, being an adjancy list shared at the executor level. When sending a worker to his next step, we only need to shuffle the data if the next vertice is not in the same executor instead of being in a different partition. Thus reducing the amount of shuffled data.

**aruku is distributed, but not out of core**

## Routing Table

With this custom graph partition we have actually lost a lot of cool features provided by GraphX to store distributed graph, especially fault tolerance and the automatic distributed message passing.

In order to get the automatic distributed message passing, as we store the graph in our custom parititons, we are going to use a predefined partitioner. As the RDD is keyed with the vertex id, if we want to send the walker to the right parition containing his next vertex, we just need to keyed the RDD of walkers with the next vertex id and repartition them by our predefined partitioner.

To get the fault tolerance back, instead of just repartitioning our RDD of walkers keyed by the vertex id they need to be send to, we are going to zipPartition this RDD to our routing table which is the empty RDD that is the result of storing our input RDD into our custom graph partitions. If we lose an executor we will need to recompute this RDD and fill the custom graph partition in the new executor spawned by SparK.

**aruku is fault tolerant**

## Optimized Edge Sampling

Another difficult part of implementing efficient higher order random walks for big graphs is having an efficient edge sampling. Indeed if the alias sampling works well to precompute sampling for the transition probabilities, it's only suited for static walks. For dynamic walks the number of possiblities is too huge to precompute thus having clever algorithm is required.

KnightKing [[github](https://github.com/KnightKingWalk/KnightKing)] is a general-purpose, distributed graph random walk engine that proposed a number of optimizations to random walks. One of them is an optimized rejection sampling in order to do edge sampling efficiently.

They decomposed the transition probability in a static and dynamic componenent. For the dynamic sampling they compare it to throwing a dart in a target, with bars of width of the static component and height of the dynamic one. The static componenent is dealt with alias sampling. The dynamic is using rejection sampling and clever optimizations with lower bound bypass and clever folding.

**aruku is fast, but probably slower than KnightKing**
