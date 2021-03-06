/*
 * Copyright 2014 Midokura SARL
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

package org.midonet.netlink

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Selector

import scala.concurrent.duration._

/**
 * Utility class to write Netlink messages into a channel.
 */
class NetlinkWriter(val channel: NetlinkChannel) {

    /**
     * Writes from the source buffer into the the channel. This method will not
     * modify the buffer's position and limit, returning only the amount of
     * bytes written. We assume the buffers contains one or more correctly
     * formatted Netlink messages.
     */
    @throws(classOf[IOException])
    def write(src: ByteBuffer): Int = {
        val oldLimit = src.limit()
        val start = src.position()
        try {
            channel.write(src)
        } finally {
            src.position(start)
            src.limit(oldLimit)
        }
    }
}

class NetlinkBlockingWriter(channel: NetlinkChannel) extends NetlinkWriter(channel) {

    private val timeout = (100 millis).toMillis
    private val selector = channel.selector()

    /**
     * Writes into the underlying channel, blocking regardless of the channel
     * mode while the write doesn't succeed.
     */
    @throws(classOf[IOException])
    override def write(src: ByteBuffer): Int = {
        var nbytes = 0
        while ({nbytes = super.write(src); nbytes } == 0) {
            if (!channel.isOpen)
                return 0
            selector.select(timeout)
        }
        nbytes
    }
}
