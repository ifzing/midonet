// Copyright 2013 Midokura Europe SARL

package org.midonet.midolman

import scala.collection.JavaConversions._

import akka.actor._
import akka.event.LoggingReceive
import javax.inject.Inject

import org.midonet.midolman.io.DatapathConnectionPool
import org.midonet.midolman.logging.ActorLogWithoutPath
import org.midonet.util.BatchCollector

object NetlinkCallbackDispatcher extends Referenceable {
    override val Name = "NetlinkCallbackDispatcher"

    def makeBatchCollector(optionalDispatcher: Option[ActorRef] = None)
                          (implicit as: ActorSystem) =
        new BatchCollector[Runnable] {
            val BATCH_SIZE = 8
            var allCBs: List[Array[Runnable]] = Nil
            var currentCBs: Array[Runnable] = new Array[Runnable](BATCH_SIZE)
            var cursor = 0

            private def cycle() {
                allCBs ::= currentCBs
                currentCBs = new Array[Runnable](BATCH_SIZE)
                cursor = 0
            }

            private def dispatcher(): ActorRef =
                optionalDispatcher.getOrElse(NetlinkCallbackDispatcher.getRef())

            override def endBatch() {

                this.synchronized {
                    if (cursor > 0)
                        cycle()

                    if (allCBs.size > 0) {
                        dispatcher() ! ProcessCallbacks(allCBs)
                        allCBs = Nil
                    }
                }
            }

            override def submit(r: Runnable) {

                this.synchronized {
                    currentCBs(cursor) = r
                    cursor += 1
                    if (cursor == BATCH_SIZE)
                        cycle()
                }
            }
        }

    case class ProcessCallbacks(callbacks: List[Array[Runnable]])
}

class NetlinkCallbackDispatcher extends Actor with ActorLogWithoutPath {
    import NetlinkCallbackDispatcher._

    implicit val as = context.system

    override def logSource = "org.midonet.netlink.callbacks"

    @Inject
    var datapathConnPool: DatapathConnectionPool = null

    override def preStart() {
        super.preStart()
        if (datapathConnPool != null)
            datapathConnPool.getAll foreach {
                case conn => conn.setCallbackDispatcher(makeBatchCollector())
            }
    }

    private def runBatch(batch: Array[Runnable]) {
        var i = 0
        while (i < batch.length && batch(i) != null) {
            try {
                batch(i).run()
            } catch {
                case e: Throwable => log.warn("Callback failed", e)
            }
            i += 1
        }
    }

    def receive = LoggingReceive {
        case ProcessCallbacks(Nil) =>
            log.debug("Received an empty callback batch")

        case ProcessCallbacks(List(head)) =>
            runBatch(head)

        case ProcessCallbacks(head :: tail) =>
            tail foreach { batch =>
                context.dispatcher.execute(new Runnable() {
                    override def run() { runBatch(batch) }
                })
            }
            /* run one batch out of this actor directly, but do it at the end,
             * once the other batches have been scheduled. */
            runBatch(head)
    }
}
