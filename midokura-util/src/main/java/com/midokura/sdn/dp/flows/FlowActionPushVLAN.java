/*
* Copyright 2012 Midokura Europe SARL
*/
package com.midokura.sdn.dp.flows;

import java.nio.ByteOrder;

import com.midokura.netlink.NetlinkMessage;
import com.midokura.netlink.messages.BaseBuilder;

public class FlowActionPushVLAN implements FlowAction<FlowActionPushVLAN> {

    /** 802.1Q TPID. */
    /*__be16*/ short vlan_tpid;

    /** 802.1Q TCI (VLAN ID and priority). */
    /*__be16*/ short vlan_tci;

    @Override
    public void serialize(BaseBuilder builder) {
        builder.addValue(vlan_tpid, ByteOrder.BIG_ENDIAN);
        builder.addValue(vlan_tci, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public boolean deserialize(NetlinkMessage message) {
        try {
            vlan_tpid = message.getShort(ByteOrder.BIG_ENDIAN);
            vlan_tci = message.getShort(ByteOrder.BIG_ENDIAN);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public NetlinkMessage.AttrKey<FlowActionPushVLAN> getKey() {
        return FlowActionKey.PUSH_VLAN;
    }

    @Override
    public FlowActionPushVLAN getValue() {
        return this;
    }

    public short getTagProtocolIdentifier() {
        return vlan_tpid;
    }

    public FlowActionPushVLAN setTagProtocolIdentifier(short tagProtocolIdentifier) {
        this.vlan_tpid = tagProtocolIdentifier;
        return this;
    }

    public short getTagControlIdentifier() {
        return vlan_tci;
    }

    public FlowActionPushVLAN setTagControlIdentifier(short tagControlIdentifier) {
        this.vlan_tci = tagControlIdentifier;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowActionPushVLAN that = (FlowActionPushVLAN) o;

        if (vlan_tci != that.vlan_tci) return false;
        if (vlan_tpid != that.vlan_tpid) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) vlan_tpid;
        result = 31 * result + (int) vlan_tci;
        return result;
    }

    @Override
    public String toString() {
        return "FlowActionPushVLAN{" +
            "vlan_tpid=" + vlan_tpid +
            ", vlan_tci=" + vlan_tci +
            '}';
    }
}