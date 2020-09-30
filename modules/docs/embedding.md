---
id: embedding
title: "Embeddings"
---

aruku provide ways to create node and edge embedding from the walks generated.

At the moment it's based on word2vec embeddings thanks to [[glint-word2vec](https://github.com/MGabr/glint-word2vec)].

```scala
import aruku.walks._
import aruku._
import aruku.embedding._

val model =
  new RandomWalkEmbedding(
    Node2Vec.config(10*graph.vertices.count()),
    Node2Vec.transition(0.5, 2, 20),
    new ServerSideGlintWord2Vec()
      .setVectorSize(100)
      .setMinCount(0)
      .setNumParameterServers(1)
      .setSubsampleRatio(1.0)
  ).fit(graph) 
  
val nodeEmbeddings = graph.mapVertices { case (vid, _) => model.transform(vid) }
val edgeEmbeddings = graph.mapEdges(edge => model.transform(edge,HadamardEmbedding))
```