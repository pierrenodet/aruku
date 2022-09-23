---
id: transition
title: "Transition Probability"
---

## Definition

A transition probability is the probability of a random walker to jump to a node for the next step of his walk.

It's made of three key components :

* Static Component : Reflects the edge component

* Dynamic Component : Reflects the state of the walker

* Extension Component : Reflects the termination of the walker

## Oblivious

An oblivious walk contains only an extension component.

It's the simplest random walk possible as it doesn't follow any kind of edge componenent or change during the walk.

For example let's create a random walk that stop after ```walkLength``` steps :

```scala
import aruku._
import org.apache.spark.graphx.{ Edge, VertexId }

case object ObliviousWalk {

  def transition(walkLength: Long) =
    Transition.oblivious(
      (walker: Walker[ObliviousWalk.type], _: VertexId) => 
        if (walker.step < walkLength) 1.0 else 0.0
    )

}
```

## Static

An static walk contains only an extension component and a static componenent.

It's a more realistic component as often seen a real world graph, the edge component matters to determine the strength of the link between two vertices.

Let's add a static componenent to our previous oblivous walk :

```scala
import aruku._
import org.apache.spark.graphx.{ Edge, VertexId }

case object StaticWalk {

  def transition(walkLength: Long) =
    Transition.static(
      (walker: Walker[StaticWalk.type], _: VertexId) => 
        if (walker.step < walkLength) 1.0 else 0.0,
      (vid: VertexId, edge: Edge[Double]) => edge.attr
    )

}
```

Hey we made DeepWalk !

## Dynamic

An dynamic walk contains all three components, extension, static and dynamic.

For more elaborate random walk, the walker is gonna decide to jump to the next node thanks to it's internal state.

If we wanted to make node2vec, the transition probability would look like this :

```scala
import aruku._
import org.apache.spark.graphx.{ Edge, VertexId }

case class DynamicWalk(previous: VertexId)

object DynamicWalk {

 def transition(p: Double, q: Double, walkLength: Long) =

    Transition.secondOrder(
      (walker: Walker[DynamicWalk], _: VertexId) => 
        if (walker.step < walkLength) 1.0 else 0.0,
      (vid: VertexId, edge: Edge[Double]) => edge.attr,
      (walker: Walker[DynamicWalk], _: VertexId, edges: Array[Edge[Double]]) => 
        Some(edges.map(_.dstId)),
      (
        walker: Walker[DynamicWalk],
        current: VertexId,
        next: Edge[Double],
        msg: Option[Array[VertexId]]
      ) =>
        msg match {
          case Some(previousNeighbors) =>
            if (previousNeighbors.contains(next)) {
              1
            } else if (next == walker.data.previous) {
              1 / p
            } else {
              1 / q
            }
          case None => 1.0
        },
      (_: VertexId, _: Array[Edge[Double]]) => math.max(1 / p, math.max(1, 1 / q)),
      (_: VertexId, _: Array[Edge[Double]]) => math.min(1 / p, math.min(1, 1 / q))
    )

}
```

As you can see, node2vec is a second order random walk, so the walker can actually carry a message with him for his next step. For node2vec the messages contains the neighbors of the previous vertice so we can compute correctly the dynamic component.