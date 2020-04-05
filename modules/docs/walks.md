---
id: walks
title: "Provided Walks"
---

As of now three random walks are provided with aruku

## Personalized Page Rank

```scala title="modules/aruku/walks/PersonalizedPageRank.scala"
case object PersonalizedPageRank {

  def config(numWalkers: Long)
  def transition(pi: Double)

}
```

## DeepWalk

```scala title="modules/aruku/walks/DeepWalk.scala"
case object DeepWalk {

  def config(numWalkers: Long)
  def transition(walkLength: Long)

}
```

## node2vec

```scala title="modules/aruku/walks/Node2Vec.scala"
object Node2Vec {

  def config(numWalkers: Long)
  def transition(p: Double, q: Double, walkLength: Long)

}
```