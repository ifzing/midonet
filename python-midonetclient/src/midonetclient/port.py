# Copyright 2013 Midokura PTE LTD.
# All Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.


from midonetclient import admin_state_up_mixin
from midonetclient import bgp
from midonetclient import port_group_port
from midonetclient import resource_base
from midonetclient import vendor_media_type
from vendor_media_type import APPLICATION_PORTGROUP_PORT_COLLECTION_JSON


PORT_TYPE_VXLAN = 'Vxlan'


class Port(resource_base.ResourceBase,
           admin_state_up_mixin.AdminStateUpMixin):

    media_type = vendor_media_type.APPLICATION_PORT_JSON

    def __init__(self, uri, dto, auth):
        super(Port, self).__init__(uri, dto, auth)

    def get_id(self):
        return self.dto['id']

    def get_type(self):
        return self.dto['type']

    def get_device_id(self):
        return self.dto['deviceId']

    def get_inbound_filter_id(self):
        return self.dto['inboundFilterId']

    def get_outbound_filter_id(self):
        return self.dto['outboundFilterId']

    def get_vif_id(self):
        return self.dto['vifId']

    def get_host_id(self):
        return self.dto['hostId']

    def get_interface_name(self):
        return self.dto['interfaceName']

    def get_vlan_id(self):
        if self.dto['type'] == PORT_TYPE_VXLAN:
            return None
        return self.dto['vlanId']

    def get_peer_id(self):
        return self.dto['peerId']

    def get_network_address(self):
        return self.dto['networkAddress']

    def get_network_length(self):
        return self.dto['networkLength']

    def get_port_address(self):
        return self.dto['portAddress']

    def get_port_mac(self):
        return self.dto['portMac']

    def get_bgps(self):
        query = {}
        headers = {'Accept':
                   vendor_media_type.APPLICATION_BGP_COLLECTION_JSON}
        return self.get_children(self.dto['bgps'], query, headers, bgp.Bgp)

    def get_mgmt_ip(self):
        return self.dto['mgmtIpAddr']

    def get_vni(self):
        return self.dto['vni']

    def inbound_filter_id(self, id_):
        self.dto['inboundFilterId'] = id_
        return self

    def outbound_filter_id(self, id_):
        self.dto['outboundFilterId'] = id_
        return self

    def vif_id(self, id_):
        self.dto['vifId'] = id_
        return self

    def vlan_id(self, id_):
        self.dto['vlanId'] = id_
        return self

    def port_address(self, port_address):
        self.dto['portAddress'] = port_address
        return self

    def network_address(self, network_address):
        self.dto['networkAddress'] = network_address
        return self

    def network_length(self, network_length):
        self.dto['networkLength'] = network_length
        return self

    def port_mac(self, port_mac):
        self.dto['portMac'] = port_mac
        return self

    def type(self, type_):
        self.dto['type'] = type_
        return self

    def add_bgp(self):
        return bgp.Bgp(self.dto['bgps'], {}, self.auth)

    def link(self, peer_uuid):
        self.dto['peerId'] = peer_uuid
        headers = {'Content-Type':
                   vendor_media_type.APPLICATION_PORT_LINK_JSON}
        self.auth.do_request(self.dto['link'], 'POST', self.dto,
                             headers=headers)

        self.get()
        return self

    def unlink(self):
        self.auth.do_request(self.dto['link'], 'DELETE')
        self.get()
        return self

    def get_port_groups(self, query=None):
        headers = {'Accept': APPLICATION_PORTGROUP_PORT_COLLECTION_JSON}
        return self.get_children(self.dto['portGroups'], query, headers,
                                 port_group_port.PortGroupPort)
