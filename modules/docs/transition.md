---
id: transition
title: "Transition Probability"
---

## Definition

A transition probability is the probability of a random walker jumping to a node for the next step of his walk.

It's made of three key components :

* Static Component : Reflects the edge component

* Dynamic Component : Reflects the state of the walker

* Extension Component : Reflects the termination of the walker

## Oblivious

An oblivious walk contains only an extension component.

It's the most straightforward random walk possible as it doesn't follow any edge component or change during the walk.

For example, let's create a random walk that stops after ```walkLength``` steps :

```scala mdoc
import aruku._
import org.apache.spark.graphx._

case object ObliviousWalk {

  def transition(walkLength: Long) =
    Transition.oblivious(
      (walker: Walker[ObliviousWalk.type], _: VertexId) => 
        if (walker.step < walkLength) 1.0 else 0.0
    )

}
```

## Static

A static walk contains only an extension component and a static component.

It's a more realistic component, as often seen a real-world graph, the edge component matters to determine the strength of the link between two vertices.

Let's add a static component to our previous oblivious walk :

```scala mdoc
import aruku._
import org.apache.spark.graphx._

case object StaticWalk {

  def transition(walkLength: Long) =
    Transition.static(
      (walker: Walker[StaticWalk.type], _: VertexId) => 
        if (walker.step < walkLength) 1.0 else 0.0,
      (vid: VertexId, edge: Edge[Double]) => edge.attr
    )

}
```

Hey, we made DeepWalk !

## Dynamic

A dynamic walk contains all three components, extension, static and dynamic.

For a more elaborate random walk, the walker will decide to jump to the next node thanks to its internal state.

If we wanted to make node2vec, the transition probability would look like this :

```scala mdoc
import aruku._
import org.apache.spark.graphx._

case class DynamicWalk(previous: VertexId)

object DynamicWalk {

 def transition(p: Double, q: Double, walkLength: Long) =

    Transition.secondOrder(
      (walker: Walker[DynamicWalk], _: VertexId) => if (walker.step < walkLength) 1.0 else 0.0,
      (_: VertexId, edge: Edge[Double]) => edge.attr,
      (walker: Walker[DynamicWalk], _: VertexId, edges: Array[Edge[Double]]) => Some(edges),
      (
        walker: Walker[DynamicWalk],
        current: VertexId,
        next: Edge[Double],
        msg: Option[Array[Edge[Double]]]
      ) =>
        msg match {
          case Some(previousNeighbors) =>
            val dst = next.dstId
            if (dst == walker.data.previous) {
              1.0 / p
            } else if (previousNeighbors.exists(_.dstId == dst)) {
              1.0
            } else {
              1.0 / q
            }
          case None                    => 1.0
        },
      (_: VertexId, _: Array[Edge[Double]]) => math.max(1.0 / p, math.max(1.0, 1.0 / q)),
      (_: VertexId, _: Array[Edge[Double]]) => math.min(1.0 / p, math.min(1.0, 1.0 / q))
    )

}
```

As you can see, node2vec is a second-order random walk so the walker can carry a message with him for his next step. For node2vec, the messages contain the neighbors of the previous vertice so we can compute the dynamic component correctly.