/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.springframework.security.fido.server.endpoint;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.client.AuthenticationExtensionClientInput;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientInputs;
import com.webauthn4j.springframework.security.webauthn.options.AssertionOptions;
import com.webauthn4j.springframework.security.webauthn.options.OptionsProvider;
import com.webauthn4j.util.Base64UrlUtil;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FIDO Server Endpoint for assertion options processing
 * With this endpoint, non-authorized user can observe requested username existence and his/her credentialId list.
 */
public class FidoServerAssertionOptionsEndpointFilter extends ServerEndpointFilterBase {

    /**
     * Default name of path suffix which will validate this filter.
     */
    public static final String FILTER_URL = "/webauthn/assertion/options";

    //~ Instance fields
    // ================================================================================================

    private final OptionsProvider optionsProvider;

    public FidoServerAssertionOptionsEndpointFilter(ObjectConverter objectConverter, OptionsProvider optionsProvider) {
        super(FILTER_URL, objectConverter);
        this.optionsProvider = optionsProvider;
        checkConfig();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        checkConfig();
    }

    @SuppressWarnings("squid:S2177")
    private void checkConfig() {
        Assert.notNull(optionsProvider, "optionsProvider must not be null");
    }


    @Override
    protected ServerResponse processRequest(HttpServletRequest request) {
        InputStream inputStream;
        try {
            inputStream = request.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ServerPublicKeyCredentialGetOptionsRequest serverRequest =
                objectConverter.getJsonConverter().readValue(inputStream, ServerPublicKeyCredentialGetOptionsRequest.class);
        String username = serverRequest.getUsername();
        Challenge challenge = serverEndpointFilterUtil.encodeUserVerification(new DefaultChallenge(), serverRequest.getUserVerification());
        AssertionOptions options = optionsProvider.getAssertionOptions(request, username, challenge);
        List<ServerPublicKeyCredentialDescriptor> credentials = options.getCredentials().stream().map(ServerPublicKeyCredentialDescriptor::new).collect(Collectors.toList());
        AuthenticationExtensionsClientInputs<AuthenticationExtensionClientInput<?>> authenticationExtensionsClientInputs;
        if (serverRequest.getExtensions() != null) {
            authenticationExtensionsClientInputs = serverRequest.getExtensions();
        } else {
            authenticationExtensionsClientInputs = options.getAuthenticationExtensions();
        }

        return new ServerPublicKeyCredentialGetOptionsResponse(
                Base64UrlUtil.encodeToString(options.getChallenge().getValue()),
                options.getAuthenticationTimeout(),
                options.getRpId(),
                credentials,
                serverRequest.getUserVerification(),
                authenticationExtensionsClientInputs);
    }

}
