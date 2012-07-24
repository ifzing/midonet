/*
 * Copyright 2011 Midokura KK
 */
package com.midokura.packets;

public interface Transport {

    short getSourcePort();
    short getDestinationPort();
    IPacket getPayload();
}