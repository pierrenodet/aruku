---
id: overview
title: Overview
---

## What's aruku?

aruku is a random walk engine for Apache Spark. It helps you program and model your random walk easily and lets a distributed, fault-tolerant, and optimized engine take care of running it. 

## How do I use it?

To run node2vec on a graph from Apache Spark Graphx :

```scala
import aruku._
import aruku.implicits._
import aruku.walks._
import org.apache.spark.graphx._
import org.apache.spark.graphx.utils._

val graph: Graph[Long, Int] = GraphGenerators
    .logNormalGraph(sc, numVertices = 150000)

val numWalkers = 150000
val walkLength = 80
val p          = 0.5
val q          = 2

graph.randomWalk(edge => edge.attr.toDouble)
    (Node2Vec.config(150000), Node2Vec.transition(0.5, 2, 80))
```

## Ready to install?

```scala
libraryDependencies += "com.github.pierrenodet" %% "aruku-core" % "@VERSION@"
```

## Acknowledgement

This library is inspired by KnightKing [[engine](https://github.com/KnightKingWalk/KnightKing)] and the [[talk](https://www.youtube.com/watch?v=lyVZNZZUdOk&t=1473s)] of Min Shen at Spark Summit 2017.