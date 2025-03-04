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
package org.opennms.netmgt.snmp.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import org.opennms.core.concurrent.LogPreservingThreadFactory;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.ErrorStatus;
import org.opennms.netmgt.snmp.SnmpAgentAddress;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpException;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSnmpWalker extends SnmpWalker {
	
	private static final Logger LOG = LoggerFactory.getLogger(MockSnmpWalker.class);

	private static class MockPduBuilder extends WalkerPduBuilder {
        private List<SnmpObjId> m_oids = new ArrayList<>();

        public MockPduBuilder(final int maxVarsPerPdu) {
            super(maxVarsPerPdu);
            reset();
        }

        @Override
        public void reset() {
            m_oids.clear();
        }

        public List<SnmpObjId> getOids() {
            return new ArrayList<SnmpObjId>(m_oids);
        }

        @Override
        public void addOid(final SnmpObjId snmpObjId) {
            m_oids.add(snmpObjId);
        }

        @Override
        public void setNonRepeaters(final int numNonRepeaters) {
        }

        @Override
        public void setMaxRepetitions(final int maxRepetitions) {
        }
    }
	
	private static class MockVarBind {
		SnmpObjId m_oid;
		SnmpValue m_value;
		
		public MockVarBind(SnmpObjId oid, SnmpValue value) {
			m_oid = oid;
			m_value = value;
		}

		public SnmpObjId getOid() {
			return m_oid;
		}

		public SnmpValue getValue() {
			return m_value;
		}
		
		
	}

	private final SnmpAgentAddress m_agentAddress;
	private final int m_snmpVersion;
    private final PropertyOidContainer m_container;
    private final ExecutorService m_executor;

    public MockSnmpWalker(final SnmpAgentAddress agentAddress, final int snmpVersion, final PropertyOidContainer container, final String name, final CollectionTracker tracker, final int maxVarsPerPdu, final int maxRetries) {
        super(agentAddress.getAddress(), name, maxVarsPerPdu, 1, maxRetries, tracker);
        m_agentAddress = agentAddress;
        m_snmpVersion = snmpVersion;
        m_container = container;
		m_executor = Executors.newFixedThreadPool(1);
/*
        m_executor = Executors.newSingleThreadExecutor(
            new LogPreservingThreadFactory(getClass().getSimpleName(), 1)
        );
*/
    }

    @Override
    protected WalkerPduBuilder createPduBuilder(final int maxVarsPerPdu) {
        return new MockPduBuilder(maxVarsPerPdu);
    }

    @Override
    protected void sendNextPdu(final WalkerPduBuilder pduBuilder) throws SnmpException {
        final MockPduBuilder builder = (MockPduBuilder)pduBuilder;
        final List<SnmpObjId> oids = builder.getOids();
        LOG.debug("'Sending' tracker PDU of size {}", oids.size());

        m_executor.submit(new ResponseHandler(oids));
    }

    @Override
    protected void handleDone() {
    	LOG.debug("handleDone()");
    	super.handleDone();
    }

    @Override
    protected void handleAuthError(final String msg) {
    	LOG.debug("handleAuthError({})", msg);
    	super.handleAuthError(msg);
    }
    
    @Override
    protected void handleError(final String msg) {
    	LOG.debug("handleError({})", msg);
    	super.handleError(msg);
    }
    
    @Override
    protected void handleError(final String msg, final Throwable t) {
    	LOG.debug("handleError({}, {})", msg, t.getLocalizedMessage(), t);
    	super.handleError(msg, t);
    }
    
    @Override
    protected void handleFatalError(final Throwable e) {
    	LOG.debug("handleFatalError({})", e.getLocalizedMessage(), e);
    	super.handleFatalError(e);
    }

    @Override
    protected void handleTimeout(final String msg) {
    	LOG.debug("handleTimeout({})", msg);
    	super.handleTimeout(msg);
    }

    @Override
    public void close() {
        m_executor.shutdown();
    }

    @Override
    protected void buildAndSendNextPdu() throws SnmpException {
    	LOG.debug("buildAndSendNextPdu()");
    	super.buildAndSendNextPdu();
    }

    private final class ResponseHandler implements Runnable {
		private final List<SnmpObjId> m_oids;

		private ResponseHandler(final List<SnmpObjId> oids) {
			m_oids = oids;
		}

		@Override
		public void run() {
		    handleResponses();
		}

	    protected void handleResponses() {
	    	LOG.debug("handleResponses({})", m_oids);
	        try {
	            if (m_container == null) {
	            	LOG.info("No SNMP response data configured for {}; pretending we've timed out.", m_agentAddress);
	            	Thread.sleep(100);
	            	handleTimeout("No MockSnmpAgent data configured for '" + m_agentAddress + "'.");
	            	return;
	            }

	            List<MockVarBind> responses = new ArrayList<MockVarBind>(m_oids.size());

	            ErrorStatus errorStatus = ErrorStatus.NO_ERROR;
	            int errorIndex = 0;
	            int index = 1; // snmp index start at 1
	            for (final SnmpObjId oid : m_oids) {
	            	SnmpObjId nextOid = m_container.findNextOidForOid(oid);
	            	if (nextOid == null) {
	            		LOG.debug("No OID following {}", oid);
	            		if (m_snmpVersion == SnmpAgentConfig.VERSION1) {
	            			if (errorStatus == ErrorStatus.NO_ERROR) { // for V1 only record the index of the first failing varbind
	            				errorStatus = ErrorStatus.NO_SUCH_NAME;
	            				errorIndex = index;
	            			}
	            		}
            			responses.add(new MockVarBind(oid, MockSnmpValue.END_OF_MIB));
	            	} else {
	            		responses.add(new MockVarBind(nextOid, m_container.findValueForOid(nextOid)));
	            	}
	            	index++;
	            }

	            if (!processErrors(errorStatus.ordinal(), errorIndex)) {
	            	LOG.debug("Responding with PDU of size {}.", responses.size());
	            	for(MockVarBind vb : responses) {
	                	processResponse(vb.getOid(), vb.getValue());
	                }
	            } 
				buildAndSendNextPdu();

	        } catch (final Throwable t) {
	            handleFatalError(t);
	        }
	    }
    }
}
