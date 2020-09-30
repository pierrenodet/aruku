/*
 * Copyright 2020 Pierre Nodet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aruku.embedding

import aruku._
import aruku.implicits._
import aruku.walks._

import org.apache.spark.mllib.feature.ServerSideGlintWord2Vec
import org.apache.spark.mllib.feature.ServerSideGlintWord2VecModel
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.util.{Loader, Saveable}
import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import scala.reflect.ClassTag
import com.typesafe.config.{Config, ConfigFactory}

class RandomWalkEmbedding[T, M](
  var walkerConfig: WalkerConfig[T],
  var transition: Transition[T, M],
  var embedding: ServerSideGlintWord2Vec,
  var edgeDirection: EdgeDirection = EdgeDirection.Out
) extends Serializable {

  def fit(graph: Graph[_, Double]): RandomWalkEmbeddingModel = {
    val walks          = graph.randomWalk(_.attr, edgeDirection)(walkerConfig, transition)
    val doc            = walks.map(_._2.map(_.toString).toSeq)
    val embeddingModel = embedding.fit(doc)
    new RandomWalkEmbeddingModel(embeddingModel)
  }

}

class RandomWalkEmbeddingModel(val word2vecModel: ServerSideGlintWord2VecModel) extends Serializable with Saveable {

    override protected def formatVersion = "1.0"

    def transform(node:VertexId):Vector = {
        word2vecModel.transform(node.toString)
    }

    def transform(edge:Edge[_],edgeEmbedding:EdgeEmbedding = HadamardEmbedding):Vector = {
        EdgeEmbedding.embedder(edgeEmbedding).apply(word2vecModel.transform(edge.srcId.toString),word2vecModel.transform(edge.dstId.toString))
    }

    override def save(sc: SparkContext, path: String): Unit = {
    word2vecModel.save(sc,path)
  }
  
}

object RandomWalkEmbeddingModel extends Loader[RandomWalkEmbeddingModel] {

      def load(sc: SparkContext,
           path: String): RandomWalkEmbeddingModel = load(sc,path,"",ConfigFactory.empty())
  
  def load(sc: SparkContext,
           path: String,
           parameterServerHost: String ,
           parameterServerConfig: Config ): RandomWalkEmbeddingModel = {
    new RandomWalkEmbeddingModel(ServerSideGlintWord2VecModel.load(sc,path,parameterServerHost,parameterServerConfig))
  }

}