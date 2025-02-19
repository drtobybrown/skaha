/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
************************************************************************
*/

package org.opencadc.skaha.session;

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.util.StringUtil;

import java.security.AccessControlException;

import org.apache.log4j.Logger;
import org.opencadc.skaha.K8SUtil;

/**
 *
 * @author majorb
 */
public class DeleteAction extends SessionAction {
    
    private static final Logger log = Logger.getLogger(DeleteAction.class);

    public DeleteAction() {
        super();
    }

    @Override
    public void doAction() throws Exception {
        super.initRequest();
        if (requestType.equals(REQUEST_TYPE_SESSION)) {
            if (sessionID == null) {
                throw new UnsupportedOperationException("Cannot kill all sessions.");
            } else {
                
                String k8sNamespace = K8SUtil.getWorkloadNamespace();
                String[] getSessionCMD = new String[] {
                    "kubectl", "get", "--namespace", k8sNamespace, "pod",
                    "--selector=canfar-net-sessionID=" + sessionID,
                    "--no-headers=true",
                    "-o", "custom-columns=" +
                        "TYPE:.metadata.labels.canfar-net-sessionType," +
                        "USERID:.metadata.labels.canfar-net-userid"};
                        
                String session = execute(getSessionCMD);
                if (!StringUtil.hasText(session)) {
                    throw new ResourceNotFoundException(sessionID);
                }
                String[] lines = session.split("\n");
                if (lines.length != 1) {
                    throw new IllegalStateException("found multiple sessions with id " + sessionID);
                }
                String[] parts = lines[0].split("\\s+");
                String type = parts[0];
                String sessionUserid = parts[1];
                if (!userID.equals(sessionUserid)) {
                    throw new AccessControlException("forbidden");
                }   
                
                stopSession(userID, type, sessionID);
            }
            return;
        }
        if (requestType.equals(REQUEST_TYPE_APP)) {
            throw new UnsupportedOperationException("App killing not supported.");
        }
    }
    
    public void stopSession(String userID, String type, String sessionID) throws Exception {
        // kill the session specified by sessionID
        log.debug("Stopping " + type + " session: " + sessionID);
        String k8sNamespace = K8SUtil.getWorkloadNamespace();
        
        String podName = K8SUtil.getJobName(sessionID, type, userID);
        String[] cmd = new String[] {
            "kubectl", "delete", "--namespace", k8sNamespace, "job", podName};
        execute(cmd);
        
        if (!SESSION_TYPE_HEADLESS.equals(type)) {
            String ingressName = K8SUtil.getIngressName(sessionID, type);
            cmd = new String[] {
                "kubectl", "delete", "--namespace", k8sNamespace, "ingress", ingressName};
            execute(cmd);
            
            String serviceName = K8SUtil.getServiceName(sessionID, type);
            cmd = new String[] {
                "kubectl", "delete", "--namespace", k8sNamespace, "service", serviceName};
            execute(cmd);
        }
        
    }
}
