/*
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
package org.overlord.sramp.governance.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactEnum;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.ExtendedArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.Property;
import org.overlord.dtgov.server.i18n.Messages;
import org.overlord.sramp.atom.MediaType;
import org.overlord.sramp.atom.err.SrampAtomException;
import org.overlord.sramp.client.SrampAtomApiClient;
import org.overlord.sramp.client.query.ArtifactSummary;
import org.overlord.sramp.client.query.QueryResultSet;
import org.overlord.sramp.governance.GovernanceConstants;
import org.overlord.sramp.governance.SlashDecoder;
import org.overlord.sramp.governance.SrampAtomApiClientFactory;
import org.overlord.sramp.governance.ValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JAX-RS resource that handles deployment specific tasks.
 *
 *
 */
@Path("/update")
public class UpdateMetaDataResource {

    private static Logger logger = LoggerFactory.getLogger(UpdateMetaDataResource.class);

    /**
     * Constructor.
     */
    public UpdateMetaDataResource() {
    }

    /**
     * Governance PUT add a classification.
     *
     * @param uuid
     * @param classification value
     *
     * @throws SrampAtomException
     */
    @PUT
    @Path("classification/{value}/{uuid}")
    @Produces("application/xml")
    public Map<String,ValueEntity> addClassification(@Context HttpServletRequest request,
            @PathParam("value") String value,
            @PathParam("uuid") String uuid) throws Exception {

        Map<String,ValueEntity> results = new HashMap<String, ValueEntity>();
        
        OutputStream os = null;
        try {
            // 0. run the decoder on the argument
            value = SlashDecoder.decode(value);
            uuid  = SlashDecoder.decode(uuid);

            // 1. get the artifact from the repo
            SrampAtomApiClient client = SrampAtomApiClientFactory.createAtomApiClient();
            BaseArtifactType artifact = client.getArtifactMetaData(uuid);

            // 2. add the classification
            artifact.getClassifiedBy().add(value);
            client.updateArtifactMetaData(artifact);

            // 3. build the response
            results.put(GovernanceConstants.STATUS, new ValueEntity("success")); //$NON-NLS-1$
            results.put(GovernanceConstants.ARTIFACT_NAME, new ValueEntity(artifact.getName()));
            results.put(GovernanceConstants.ARTIFACT_CREATED_BY, new ValueEntity(artifact.getCreatedBy()));
            results.put(GovernanceConstants.ARTIFACT_DESCRIPTION, new ValueEntity(artifact.getDescription()));
        } catch (Exception e) {
            logger.error(Messages.i18n.format("UpdateMetaDataResource.ErrorUpdating", e.getMessage()), e); //$NON-NLS-1$
            results.put(GovernanceConstants.STATUS, new ValueEntity("fail")); //$NON-NLS-1$
            results.put(GovernanceConstants.MESSAGE, new ValueEntity(Messages.i18n.format("UpdateMetaDataResource.ArtifactNotFound"))); //$NON-NLS-1$
        } finally {
            IOUtils.closeQuietly(os);
        }
        return results;
    }

    /**
     * Governance POST to create a ExtendedArtifactType with
     * given ExtentedType, related to the artifact uuid passed in.
     *
     * @param uuid
     * @param classification value
     *
     * @throws SrampAtomException
     */
    @POST
    @Path("{extendedType}/relatedto/{uuid}")
    @Produces(MediaType.APPLICATION_XML)
    public Response create(@PathParam("extendedType") String extendedType,
                           @PathParam("uuid") String uuid) throws Exception {

        try {
            uuid  = SlashDecoder.decode(uuid);

            // 1. get the artifact from the repo
            SrampAtomApiClient client = SrampAtomApiClientFactory.createAtomApiClient();
            QueryResultSet queryResultSet = client.buildQuery("/s-ramp[@uuid = ?]").parameter(uuid).query(); //$NON-NLS-1$
            if (queryResultSet.size() == 0) {
                return Response.serverError().status(0).build();
            }
            ArtifactSummary artifactSummary = queryResultSet.iterator().next();
            BaseArtifactType artifact = client.getArtifactMetaData(artifactSummary.getType(), uuid);

            // 2. create a projectArtifactType
            ExtendedArtifactType extendedArtifactType = new ExtendedArtifactType();
            extendedArtifactType.setArtifactType(BaseArtifactEnum.EXTENDED_ARTIFACT_TYPE);
            extendedArtifactType.setExtendedType(extendedType);

            for (Property property : artifact.getProperty()) {
                if ("maven.groupId".equals(property.getPropertyName())) { //$NON-NLS-1$
                    extendedArtifactType.getProperty().add(property);
                }
            }
            client.createArtifact(extendedArtifactType);
            InputStream reply = IOUtils.toInputStream("success"); //$NON-NLS-1$
            return Response.ok(reply, MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Exception e) {
            logger.error(Messages.i18n.format("UpdateMetaDataResource.ErrorCreatingArtifact", e.getMessage()), e); //$NON-NLS-1$
            throw new SrampAtomException(e);
        }
    }

}
