/**
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.overlord.dtgov.jbpm.util;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.servlet.http.HttpServletRequest;

import org.kie.internal.task.api.UserGroupCallback;
import org.overlord.commons.auth.jetty8.HttpRequestThreadLocalFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Loosely based on org.jbpm.task.identity.JAASUserGroupCallbackImpl
 *
 * @author kstam
 *
 */
@Alternative
public class DTGovUserGroupCallbackJetty implements UserGroupCallback {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean existsUser(String userId) {
    	// allow everything as there is no way to ask JAAS/JACC for users in the domain
        return true;
    }

    @Override
    public boolean existsGroup(String groupId) {
    	// allow everything as there is no way to ask JAAS/JACC for groups in the domain
    	return true;
    }

    @Override
    public List<String> getGroupsForUser(String userId,
            List<String> groupIds, List<String> allExistingGroupIds)
    {
    	List<String> roles = new ArrayList<String>();
    	//adding some default roles till authorization can be fixed
    	roles.add("overlorduser");
    	roles.add("admin.sramp");
    	roles.add("dev");
    	roles.add("qa");
    	roles.add("stage");
    	roles.add("prod");
    	roles.add("ba");
    	roles.add("arch");
        try {
        	// https://issues.jboss.org/browse/DTGOV-106 - request comes up null 
        	HttpServletRequest request = HttpRequestThreadLocalFilter.TL_request.get();
//            Principal principal = request.getUserPrincipal();
//            if (principal instanceof GenericPrincipal) {
//                GenericPrincipal gp = (GenericPrincipal) principal;
//                String[] gpRoles = gp.getRoles();
//                roles = new ArrayList<String>(gpRoles.length);
//                for (String role : gpRoles) {
//                    roles.add(role);
//                }
//            } else if (principal instanceof MemoryUser) {
//            	MemoryUser mu = (MemoryUser) principal;
//            	Iterator<Role> iter = mu.getRoles();
//            	roles = new ArrayList<String>();
//            	while (iter.hasNext()) {
//            		roles.add(iter.next().getRolename());
//            	}
//            }
        } catch (Exception e) {
            logger.error("ErrorGettingRoles for user " + userId, e); //$NON-NLS-1$
        }
        return roles;
    }
}
