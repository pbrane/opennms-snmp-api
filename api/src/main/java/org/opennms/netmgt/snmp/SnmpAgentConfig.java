/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.snmp;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.json.JSONWriter;

/**
 * @author (various previous authors not documented)
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
@XmlRootElement(name = "snmpAgentConfig")
public class SnmpAgentConfig extends SnmpConfiguration implements Serializable {
    private static final long serialVersionUID = -6646744513933866811L;

    public static final String PROFILE_LABEL_FOR_DEFAULT_CONFIG = "default";
    public static final String AGENT_CONFIG_PREFIX = "SNMP_AGENT_CONFIG_";
    private InetAddress m_address;
    private InetAddress m_proxyFor;
    private String profileLabel;
    private boolean isDefault = true;

    public SnmpAgentConfig() {
        this(null);
    }

    public SnmpAgentConfig(InetAddress agentAddress) {
        this(agentAddress, SnmpConfiguration.DEFAULTS);
    }

    public SnmpAgentConfig(InetAddress agentAddress, SnmpConfiguration defaults) {
        super(defaults);
        m_address = agentAddress;
    }

    public static SnmpAgentConfig parseProtocolConfigurationString(String protocolConfigString) {
        if (protocolConfigString == null) {
            throw new IllegalArgumentException("Protocol configuration string for SnmpAgentConfig must not be null.");
        }

        final JSONObject protocolConfig = new JSONObject(new JSONTokener(protocolConfigString)).optJSONObject("snmp");
        if (protocolConfig == null) {
            throw new IllegalStateException("Invalid protocol configuration string for SnmpAgentConfig: Expected it to start with snmp object" + protocolConfigString);
        }

        Map<String, String> attributes = new HashMap<>();
        @SuppressWarnings("unchecked")
        Iterator<String> keysItr = protocolConfig.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            attributes.put(key, protocolConfig.isNull(key) ? null : protocolConfig.getString(key));
        }

        return SnmpAgentConfig.fromMap(attributes);
    }

    public String toProtocolConfigString() {
        final JSONWriter writer = new JSONStringer()
                .object()
                .key("snmp")
                .object();
        toMap().entrySet().stream()
                .forEach(e -> writer.key(e.getKey()).value(e.getValue()));
        return writer.endObject()
                .endObject()
                .toString();
    }

    /**
     * Don't expose credentials here in plaintext in case this object is used in a log message.
     * 
     * http://issues.opennms.org/browse/NMS-1504
     */
    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder("SnmpAgentConfig[");
        buff.append("Address: " + InetAddrUtils.str(m_address));
        buff.append(", ProxyForAddress: " + InetAddrUtils.str(m_proxyFor));
        buff.append(", Port: " + getPort());
        buff.append(", Timeout: " + getTimeout());
        buff.append(", Retries: " + getRetries());
        buff.append(", MaxVarsPerPdu: " + getMaxVarsPerPdu());
        buff.append(", MaxRepetitions: " + getMaxRepetitions());
        buff.append(", MaxRequestSize: " + getMaxRequestSize());
        buff.append(", Version: " + versionToString(getVersion()));
        buff.append(", TTL: " + getTTL());
        if (isVersion3()) {
            buff.append(", SecurityLevel: " + getSecurityLevel());
            buff.append(", SecurityName: " + getSecurityName());
            buff.append(", AuthPassPhrase: XXXXXXXX"); //getAuthPassPhrase()
            buff.append(", AuthProtocol: " + getAuthProtocol());
            buff.append(", PrivPassphrase: XXXXXXXX"); //getPrivPassPhrase()
            buff.append(", PrivProtocol: " + getPrivProtocol());
            buff.append(", ContextName: " + getContextName());
            buff.append(", EngineId: " + getEngineId());
            buff.append(", ContextEngineId: " + getContextEngineId());
            buff.append(", EnterpriseId:" + getEnterpriseId());
        } else {
            buff.append(", ReadCommunity: XXXXXXXX"); //getReadCommunity()
            buff.append(", WriteCommunity: XXXXXXXX"); //getWriteCommunity()
        }
        buff.append("]");
        return buff.toString();
    }


    @XmlJavaTypeAdapter(InetAddrXmlAdapter.class)
    public InetAddress getAddress() {
        return m_address;
    }

    public void setAddress(InetAddress address) {
        m_address = address;
    }

    @XmlJavaTypeAdapter(InetAddrXmlAdapter.class)
    public InetAddress getProxyFor() {
        return m_proxyFor;
    }

    public void setProxyFor(InetAddress address) {
        m_proxyFor = address;
    }

    @XmlTransient
    public InetAddress getEffectiveAddress() {
        if (m_proxyFor == null) return m_address;
        return m_proxyFor;
    }

    @XmlTransient
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @XmlTransient
    public String getProfileLabel() {
        return profileLabel;
    }

    public void setProfileLabel(String profileLabel) {
        this.profileLabel = profileLabel;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(getAddress(),
                                getProxyFor(),
                                getPort(),
                                getTimeout(),
                                getRetries(),
                                getMaxVarsPerPdu(),
                                getMaxRepetitions(),
                                getMaxRequestSize(),
                                getVersion(),
                                getSecurityLevel(),
                                getSecurityName(),
                                getAuthPassPhrase(),
                                getAuthProtocol(),
                                getPrivPassPhrase(),
                                getPrivProtocol(),
                                getContextName(),
                                getEngineId(),
                                getContextEngineId(),
                                getEnterpriseId(),
                                getReadCommunity(),
                                getWriteCommunity(),
                                getTTL());
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            System.err.println("obj = null!");
            return false;
        }
        if (obj instanceof SnmpAgentConfig) {
            final SnmpAgentConfig other = (SnmpAgentConfig) obj;

            return Objects.equals(getAddress(), other.getAddress())
                    && Objects.equals(getProxyFor(), other.getProxyFor())
                    && Objects.equals(getPort(), other.getPort())
                    && Objects.equals(getTimeout(), other.getTimeout())
                    && Objects.equals(getRetries(), other.getRetries())
                    && Objects.equals(getMaxRepetitions(), other.getMaxRepetitions())
                    && Objects.equals(getMaxRequestSize(), other.getMaxRequestSize())
                    && Objects.equals(getMaxVarsPerPdu(), other.getMaxVarsPerPdu())
                    && Objects.equals(getVersion(), other.getVersion())
                    && Objects.equals(getSecurityLevel(), other.getSecurityLevel())
                    && Objects.equals(getSecurityName(), other.getSecurityName())
                    && Objects.equals(getAuthPassPhrase(), other.getAuthPassPhrase())
                    && Objects.equals(getAuthProtocol(), other.getAuthProtocol())
                    && Objects.equals(getPrivPassPhrase(), other.getPrivPassPhrase())
                    && Objects.equals(getPrivProtocol(), other.getPrivProtocol())
                    && Objects.equals(getContextName(), other.getContextName())
                    && Objects.equals(getEngineId(), other.getEngineId())
                    && Objects.equals(getContextEngineId(), other.getContextEngineId())
                    && Objects.equals(getEnterpriseId(), other.getEnterpriseId())
                    && Objects.equals(getReadCommunity(), other.getReadCommunity())
                    && Objects.equals(getWriteCommunity(), other.getWriteCommunity())
                    && Objects.equals(getTTL(), other.getTTL());
        }
        return false;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("address", m_address == null ? null : InetAddrUtils.str(m_address));
        map.put("proxyFor", m_proxyFor == null ? null : InetAddrUtils.str(m_proxyFor));
        map.put("port", Integer.toString(getPort()));
        map.put("timeout", Integer.toString(getTimeout()));
        map.put("retries", Integer.toString(getRetries()));
        map.put("max-vars-per-pdu", Integer.toString(getMaxVarsPerPdu()));
        map.put("max-repetitions", Integer.toString(getMaxRepetitions()));
        map.put("max-request-size", Integer.toString(getMaxRequestSize()));
        map.put("version", Integer.toString(getVersion()));
        map.put("security-level", Integer.toString(getSecurityLevel()));
        map.put("security-name", getSecurityName());
        map.put("auth-passphrase", getAuthPassPhrase());
        map.put("auth-protocol", getAuthProtocol());
        map.put("priv-passphrase", getPrivPassPhrase());
        map.put("priv-protocol", getPrivProtocol());
        map.put("context-name", getContextName());
        map.put("engine-id", getEngineId());
        map.put("context-engine-id", getContextEngineId());
        map.put("enterprise-id", getEnterpriseId());
        map.put("read-community", getReadCommunity());
        map.put("write-community", getWriteCommunity());
        // No default value for ttl.
        if(getTTL() != null) {
            map.put("ttl", Long.toString(getTTL()));
        }
        return map;
    }

    public static SnmpAgentConfig fromMap(Map<String, String> map) {
        SnmpAgentConfig config = new SnmpAgentConfig();
        if (map.get("address") != null) config.setAddress(InetAddrUtils.addr(map.get("address")));
        if (map.get("proxyFor") != null) config.setProxyFor(InetAddrUtils.addr(map.get("proxyFor")));
        if (map.get("port") != null) config.setPort(Integer.parseInt(map.get("port")));
        if (map.get("timeout") != null) config.setTimeout(Integer.parseInt(map.get("timeout")));
        if (map.get("retries") != null) config.setRetries(Integer.parseInt(map.get("retries")));
        if (map.get("max-vars-per-pdu") != null) config.setMaxVarsPerPdu(Integer.parseInt(map.get("max-vars-per-pdu")));
        if (map.get("max-repetitions") != null) config.setMaxRepetitions(Integer.parseInt(map.get("max-repetitions")));
        if (map.get("max-request-size") != null) config.setMaxRequestSize(Integer.parseInt(map.get("max-request-size")));
        if (map.get("version") != null) config.setVersion(Integer.parseInt(map.get("version")));
        if (map.get("security-level") != null) config.setSecurityLevel(Integer.parseInt(map.get("security-level")));
        if (map.get("security-name") != null) config.setSecurityName(map.get("security-name"));
        if (map.get("auth-passphrase") != null) config.setAuthPassPhrase(map.get("auth-passphrase"));
        if (map.get("auth-protocol") != null) config.setAuthProtocol(map.get("auth-protocol"));
        if (map.get("priv-passphrase") != null) config.setPrivPassPhrase(map.get("priv-passphrase"));
        if (map.get("priv-protocol") != null) config.setPrivProtocol(map.get("priv-protocol"));
        if (map.get("context-name") != null) config.setContextName(map.get("context-name"));
        if (map.get("engine-id") != null) config.setEngineId(map.get("engine-id"));
        if (map.get("context-engine-id") != null) config.setContextEngineId(map.get("context-engine-id"));
        if (map.get("enterprise-id") != null) config.setEnterpriseId(map.get("enterprise-id"));
        if (map.get("read-community") != null) config.setReadCommunity(map.get("read-community"));
        if (map.get("write-community") != null) config.setWriteCommunity(map.get("write-community"));
        if (map.get("ttl") != null) config.setTTL(Long.parseLong(map.get("ttl")));
        return config;
    }
}
