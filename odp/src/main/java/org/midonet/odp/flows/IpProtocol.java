/*
* Copyright 2012 Midokura Europe SARL
*/
package org.midonet.odp.flows;

/**
* // TODO: mtoader ! Please explain yourself.
*/
public enum IpProtocol {
    ICMP(1),
    TCP(6),
    UDP(17),
    ICMPV6(58);

    byte value;
    IpProtocol(int value) { this.value = (byte)value; }
}