/*
 * Copyright 2012 Midokura KK
 * Copyright 2012 Midokura PTE LTD.
 */
package org.midonet.api.bgp.rest_api;

import org.midonet.api.auth.ForbiddenHttpException;
import org.midonet.api.rest_api.ResourceFactory;
import org.midonet.api.auth.AuthAction;
import org.midonet.api.bgp.auth.BgpAuthorizer;
import org.midonet.api.rest_api.RestApiConfig;
import org.midonet.cluster.DataClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestBgpResource {

    private BgpResource testObject;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private RestApiConfig config;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private SecurityContext context;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ResourceFactory factory;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private BgpAuthorizer auth;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private UriInfo uriInfo;

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private DataClient dataClient;

    @Before
    public void setUp() throws Exception {
        testObject = new BgpResource(config, uriInfo, context, auth, dataClient,
                factory);
    }

    @Test(expected = ForbiddenHttpException.class)
    public void testDeleteUnauthorized() throws Exception {
        // Set up
        UUID id = UUID.randomUUID();
        doReturn(false).when(auth).authorize(context, AuthAction.WRITE, id);

        // Execute
        testObject.delete(id);
    }

    @Test
    public void testDeleteNonExistentData() throws Exception {
        // Set up
        UUID id = UUID.randomUUID();
        doReturn(true).when(auth).authorize(context, AuthAction.WRITE, id);
        doReturn(null).when(dataClient).bgpGet(id);

        // Execute
        testObject.delete(id);

        // Verify
        verify(dataClient, never()).bgpDelete(id);
    }

    @Test(expected = ForbiddenHttpException.class)
    public void testGetUnauthorized() throws Exception {
        // Set up
        UUID id = UUID.randomUUID();
        doReturn(false).when(auth).authorize(context, AuthAction.READ, id);

        // Execute
        testObject.get(id);
    }
}