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
import org.apache.spark.mllib.linalg.{Vector,Vectors}
import com.github.fommil.netlib.BLAS.{getInstance => blas}
import breeze.linalg.{DenseVector=>BDV}
import breeze.linalg._
import breeze.numerics._

sealed trait EdgeEmbedding extends Serializable
case object AverageEmbedding extends EdgeEmbedding
case object L1Embedding extends EdgeEmbedding
case object L2Embedding extends EdgeEmbedding
case object HadamardEmbedding extends EdgeEmbedding

object EdgeEmbedding {

    def embedder(edgeEmbedding:EdgeEmbedding):(Vector,Vector)=>Vector = edgeEmbedding match {
        case AverageEmbedding => (v1,v2) => Vectors.dense(((BDV(v1.toArray) + BDV(v2.toArray))/2.0).toArray)
        case L1Embedding =>(v1,v2) => Vectors.dense((abs(BDV(v1.toArray) - BDV(v2.toArray))).toArray)
        case L2Embedding =>(v1,v2) => Vectors.dense((pow(BDV(v1.toArray) - BDV(v2.toArray),2)).toArray)
        case HadamardEmbedding =>(v1,v2) => Vectors.dense((BDV(v1.toArray) *:* BDV(v2.toArray)).toArray)
    }

}