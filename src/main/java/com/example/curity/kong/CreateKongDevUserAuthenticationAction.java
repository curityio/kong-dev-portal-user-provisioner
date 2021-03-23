/*
 *  Copyright 2020 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example.curity.kong;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticatedSessions;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationAction;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationActionResult;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.*;
import se.curity.identityserver.sdk.service.authenticationaction.AuthenticatorDescriptor;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;

public final class CreateKongDevUserAuthenticationAction implements AuthenticationAction
{
    private final CreateKongDevUserAuthenticationActionConfig _configuration;
    private final ExceptionFactory _exceptionFactory;
    private final WebServiceClient _client;

    private final static Logger _logger = LoggerFactory.getLogger(CreateKongDevUserAuthenticationAction.class);

    public CreateKongDevUserAuthenticationAction(CreateKongDevUserAuthenticationActionConfig configuration)
    {
        _configuration = configuration;
        _exceptionFactory = configuration.getExceptionFactory();
        _client = getWebServiceClient(configuration.getKongDevPortalUrl());
    }

    @Override
    public AuthenticationActionResult apply(AuthenticationAttributes authenticationAttributes,
                                            AuthenticatedSessions authenticatedSessions,
                                            String authenticationTransactionId,
                                            AuthenticatorDescriptor authenticatorDescriptor)
    {
        SubjectAttributes sa = authenticationAttributes.getSubjectAttributes();
        Attribute email = sa.get("email");

        JSONObject attributes = new JSONObject(sa.get("attributes").getValue().toString());
        JSONObject name = attributes.getJSONObject("name");
        String firstName = name.getString("givenName");
        String lastName = name.getString("familyName");

        try
        {
            JSONObject postData = new JSONObject();

            postData.put("email", email.getValue().toString());
            postData.put("meta", "{\"full_name\":\"" + firstName + " " + lastName + "\"}");

            HttpResponse response = _client
                    .request()
                    .contentType("application/json")
                    .body(HttpRequest.fromString(postData.toString(), Charset.defaultCharset()))
                    .post()
                    .response();

            if(response.statusCode() == 409)
            {
                _logger.warn("User already exists: {}",
                        response.body(HttpResponse.asString()));

            }
            else if (response.statusCode() != 200) {
                _logger.warn("Event posted to Kong Dev Portal but response was not successful: {}",
                        response.body(HttpResponse.asString()));
            }
            else
            {
                _logger.debug("Successfully created user in Kong Dev Portal: {}", postData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return AuthenticationActionResult.successfulResult(authenticationAttributes);
    }

    private WebServiceClient getWebServiceClient(String uri) {
        WebServiceClientFactory factory = _configuration.getWebServiceClientFactory();

        HttpClient httpClient = _configuration.getHttpClient();
        URI u = URI.create(uri);

        String configuredScheme = httpClient.getScheme();
        String requiredScheme = u.getScheme();

        if (!Objects.equals(configuredScheme, requiredScheme)) {
            _logger.debug("HTTP client was configured with the scheme {} but {} was expected. Ensure that the " +
                    "configuration is correct.", configuredScheme, requiredScheme);

            throw _exceptionFactory.internalServerException(ErrorCode.CONFIGURATION_ERROR,
                    String.format("HTTP scheme of client is not acceptable; %s is required but %s was found",
                            requiredScheme, configuredScheme));
        }

        return factory.create(httpClient, u.getPort()).withHost(u.getHost()).withPath(u.getPath());
    }
}