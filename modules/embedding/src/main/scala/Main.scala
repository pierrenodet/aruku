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
import org.apache.spark.mllib.feature.ServerSideGlintWord2Vec
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import scala.util.Try

object Main extends App {

  val sc    = SparkContext.getOrCreate()
  val walks = sc.textFile(args(0), 20 * 16).map(path => path.split(",").toSeq)

  val word2Vec = new ServerSideGlintWord2Vec()
    .setVectorSize(100)
    .setMinCount(0)
    .setNumPartitions(20 * 16)
    .setParameterServerHost(args(2))
    .setBatchSize(40)
    .setLearningRate(0.015)
    .setNumParameterServers(5)
    .setNumIterations(1)

  sc.hadoopConfiguration.set("mapred.output.compress", "false")
  val model = word2Vec.fit(walks)
  model.getVectors.rdd.repartition(1).saveAsTextFile(args(1));
}
