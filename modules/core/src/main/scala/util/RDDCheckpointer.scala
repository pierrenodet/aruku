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

package aruku.util

import scala.collection.mutable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

/**
 * Sadly need to copy paste it because it's spark private
 */
private[aruku] class RDDCheckpointer[T](checkpointInterval: Int, sc: SparkContext, storageLevel: StorageLevel) {

  def this(checkpointInterval: Int, sc: SparkContext) =
    this(checkpointInterval, sc, StorageLevel.MEMORY_ONLY)

  /** FIFO queue of past checkpointed Datasets */
  private val checkpointQueue = mutable.Queue[RDD[T]]()

  /** FIFO queue of past persisted Datasets */
  private val persistedQueue = mutable.Queue[RDD[T]]()

  /** Number of times [[update()]] has been called */
  private var updateCount = 0

  /**
   * Update with a new Dataset. Handle persistence and checkpointing as needed.
   * Since this handles persistence and checkpointing, this should be called before the Dataset
   * has been materialized.
   *
   * @param newData  New Dataset created from previous Datasets in the lineage.
   */
  def update(newData: RDD[T]): Unit = {
    newData.persist()
    persistedQueue.enqueue(newData)
    // We try to maintain 2 Datasets in persistedQueue to support the semantics of this class:
    // Users should call [[update()]] when a new Dataset has been created,
    // before the Dataset has been materialized.
    while (persistedQueue.size > 3) {
      val dataToUnpersist = persistedQueue.dequeue()
      dataToUnpersist.unpersist()
    }
    updateCount += 1

    // Handle checkpointing (after persisting)
    if (checkpointInterval != -1 && (updateCount % checkpointInterval) == 0
        && sc.getCheckpointDir.nonEmpty) {
      // Add new checkpoint before removing old checkpoints.
      newData.checkpoint()
      checkpointQueue.enqueue(newData)
      // Remove checkpoints before the latest one.
      var canDelete = true
      while (checkpointQueue.size > 1 && canDelete) {
        // Delete the oldest checkpoint only if the next checkpoint exists.
        if (checkpointQueue(1).isCheckpointed) {
          removeCheckpointFile()
        } else {
          canDelete = false
        }
      }
    }
  }

  /**
   * Call this to unpersist the Dataset.
   */
  def unpersistDataSet(): Unit =
    while (persistedQueue.nonEmpty) {
      val dataToUnpersist = persistedQueue.dequeue()
      dataToUnpersist.unpersist()
    }

  /**
   * Call this at the end to delete any remaining checkpoint files.
   */
  def deleteAllCheckpoints(): Unit =
    while (checkpointQueue.nonEmpty) {
      removeCheckpointFile()
    }

  /**
   * Call this at the end to delete any remaining checkpoint files, except for the last checkpoint.
   * Note that there may not be any checkpoints at all.
   */
  def deleteAllCheckpointsButLast(): Unit =
    while (checkpointQueue.size > 1) {
      removeCheckpointFile()
    }

  /**
   * Get all current checkpoint files.
   * This is useful in combination with [[deleteAllCheckpointsButLast()]].
   */
  def getAllCheckpointFiles: Array[String] =
    checkpointQueue.flatMap(_.getCheckpointFile).toArray

  /**
   * Dequeue the oldest checkpointed Dataset, and remove its checkpoint files.
   * This prints a warning but does not fail if the files cannot be removed.
   */
  private def removeCheckpointFile(): Unit = {
    val old = checkpointQueue.dequeue()
    // Since the old checkpoint is not deleted by Spark, we manually delete it.
    old.getCheckpointFile.foreach(RDDCheckpointer.removeCheckpointFile(_, sc.hadoopConfiguration))
  }
}

object RDDCheckpointer {

  /** Delete a checkpoint file, and log a warning if deletion fails. */
  def removeCheckpointFile(checkpointFile: String, conf: Configuration): Unit =
    try {
      val path = new Path(checkpointFile)
      val fs   = path.getFileSystem(conf)
      fs.delete(path, true)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
}
