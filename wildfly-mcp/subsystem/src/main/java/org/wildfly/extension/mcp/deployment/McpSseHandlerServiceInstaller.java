/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mcp.deployment;

import static org.jboss.as.server.security.VirtualDomainMarkerUtility.virtualDomainName;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryName;
import static org.wildfly.extension.mcp.MCPLogger.ROOT_LOGGER;

import org.wildfly.extension.mcp.McpEndpointConfiguration;
import java.util.List;

import io.undertow.Handlers;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.as.domain.http.server.security.ElytronIdentityHandler;
import org.jboss.as.server.deployment.Attachments;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.wildfly.elytron.web.undertow.server.ElytronContextAssociationHandler;
import org.wildfly.elytron.web.undertow.server.ElytronHttpExchange;
import org.wildfly.extension.mcp.Capabilities;
import org.wildfly.extension.mcp.api.ConnectionManager;
import org.wildfly.extension.mcp.injection.WildFlyMCPRegistry;
import org.wildfly.extension.mcp.server.McpServerSentConnectionCallBack;
import org.wildfly.extension.mcp.server.MessagesHttpHandler;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.http.HttpAuthenticationFactory;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.oidc.Oidc;
import org.wildfly.security.http.oidc.OidcClientConfiguration;
import org.wildfly.security.http.oidc.OidcClientConfigurationBuilder;
import org.wildfly.security.http.oidc.OidcClientContext;
import org.wildfly.security.http.oidc.OidcMechanismFactory;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

public class McpSseHandlerServiceInstaller implements DeploymentServiceInstaller {

    @Override
    public void install(DeploymentPhaseContext context) {
        DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        ModelNode model = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        WildFlyMCPRegistry registry = deploymentUnit.getAttachment(MCPAttachements.MCP_REGISTRY_METADATA);
        final org.jboss.modules.Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();
        String serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
        String hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();
        String webContext = model.get(DeploymentDefinition.CONTEXT_ROOT.getName()).asString();
        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<HttpServerAuthenticationMechanismFactory> httpServerAuthenticationMechanismFactory = ServiceDependency.on(virtualMechanismFactoryName(deploymentUnit));
        ServiceDependency<SecurityDomain> securityDomain = ServiceDependency.on(virtualDomainName(deploymentUnit));
        final McpEndpointConfiguration configuration = deploymentUnit.getAttachment(MCPAttachements.MCP_ENDPOINT_CONFIGURATION);
        final String messagesEndpoint = "/".equals(webContext) ? webContext + configuration.getMessagesPath() : webContext + '/' + configuration.getMessagesPath();
        final ConnectionManager connectionManager = new ConnectionManager();
        final McpServerSentConnectionCallBack mcpServerSentConnectionCallBack = new McpServerSentConnectionCallBack(messagesEndpoint, connectionManager);
        final MessagesHttpHandler messagesHttpHandler = new MessagesHttpHandler(connectionManager, registry, classLoader, serverName, deploymentUnit.getName());
        final String ssePath = "/".equals(webContext) ? webContext + configuration.getSsePath() : webContext + '/' + configuration.getSsePath();
        Runnable start = new Runnable() {
            @Override
            public void run() {
                HttpAuthenticationFactory httpAuthenticationFactory = HttpAuthenticationFactory.builder()
                        .setFactory(getJsonConfiguration(deploymentUnit))
                        .setSecurityDomain(securityDomain.get())
                        .build();
                host.get().registerHandler(ssePath, secureHandler(Handlers.serverSentEvents(mcpServerSentConnectionCallBack), httpAuthenticationFactory));
                host.get().registerHandler(messagesEndpoint, secureHandler(messagesHttpHandler, httpAuthenticationFactory));
                ROOT_LOGGER.endpointRegistered(ssePath, host.get().getName());
            }
        };
        Runnable stop = new Runnable() {
            @Override
            public void run() {
                host.get().unregisterHandler(ssePath);
                host.get().unregisterHandler(messagesEndpoint);
                ROOT_LOGGER.endpointUnregistered(ssePath, host.get().getName());
            }
        };
        ServiceInstaller.builder(start, stop)
                .requires(List.of(host, ServiceDependency.on(Capabilities.MCP_SERVER_PROVIDER_DESCRIPTOR),httpServerAuthenticationMechanismFactory, securityDomain))
                .asActive()
                .build()
                .install(context);
    }
    
    private HttpServerAuthenticationMechanismFactory getJsonConfiguration(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return null;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        for (ParamValueMetaData param : webMetaData.getContextParams()) {
            if (Oidc.JSON_CONFIG_CONTEXT_PARAM.equals(param.getParamName())) {
                OidcClientConfiguration oidcClientConfiguration = OidcClientConfigurationBuilder.build(new ByteArrayInputStream(param.getParamValue().getBytes(StandardCharsets.UTF_8)));
                OidcClientContext oidcClientContext = new OidcClientContext(oidcClientConfiguration);
                return new OidcMechanismFactory(oidcClientContext);
            }
        }
        return null;
    }
 
    private HttpHandler secureHandler(HttpHandler handler, HttpAuthenticationFactory httpAuthenticationFactory) {
        HttpHandler domainHandler = new AuthenticationCallHandler(handler);
        domainHandler = new AuthenticationConstraintHandler(domainHandler);
        Supplier<List<HttpServerAuthenticationMechanism>> mechanismSupplier = () ->
            httpAuthenticationFactory.getMechanismNames().stream()
            .map(s -> {
                    try {
                        return httpAuthenticationFactory.createMechanism(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
            .collect(Collectors.toList());
        domainHandler = ElytronContextAssociationHandler.builder()
                .setNext(domainHandler)
                .setMechanismSupplier(mechanismSupplier)
                .setHttpExchangeSupplier(h -> new ElytronHttpExchange(h) {

                    @Override
                    public void authenticationComplete(SecurityIdentity securityIdentity, String mechanismName) {
                        super.authenticationComplete(securityIdentity, mechanismName);
                        h.putAttachment(ElytronIdentityHandler.IDENTITY_KEY, securityIdentity);
                    }

                })
                .build();
        return domainHandler;
    }
//
//    private HttpHandler createRootHttpHandler(DeploymentUnit deploymentUnit, String webContext) {
//        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
//        if (warMetaData == null) {
//            return null;
//        }
//        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
//        for (ParamValueMetaData param : webMetaData.getContextParams()) {
//            if (Oidc.JSON_CONFIG_CONTEXT_PARAM.equals(param.getParamName())) {
//                OidcClientConfiguration oidcClientConfiguration = OidcClientConfigurationBuilder.build(new ByteArrayInputStream(param.getParamValue().getBytes(StandardCharsets.UTF_8)));
//                OidcClientContext oidcClientContext = new OidcClientContext(oidcClientConfiguration);
//                final HttpServerAuthenticationMechanismFactory oidcMechanismFactory = new OidcMechanismFactory(oidcClientContext);
//
//                HttpAuthenticationFactory httpAuthenticationFactory = HttpAuthenticationFactory.builder()
//                        .setFactory(oidcMechanismFactory)
//                        .setSecurityDomain(securityDomain)
//                        .build();
//                HttpHandler rootHandler = new ElytronRunAsHandler();
//
//                rootHandler = new BlockingHandler(rootHandler);
//                rootHandler = new AuthenticationCallHandler(rootHandler);
//                rootHandler = new AuthenticationConstraintHandler(rootHandler) {
//                    @Override
//                    protected boolean isAuthenticationRequired(HttpServerExchange exchange) {
//                        return true;
//                    }
//                };
//                ElytronContextAssociationHandler.Builder elytronContextHandlerBuilder = ElytronContextAssociationHandler.builder()
//                        .setNext(rootHandler)
//                        .setAuthenticationMode(AuthenticationMode.PRO_ACTIVE)
//                        .setMechanismSupplier(() -> httpAuthenticationFactory.getMechanismNames().stream()
//                        .map(mechanismName -> {
//                            try {
//                                return httpAuthenticationFactory.createMechanism(mechanismName);
//                            } catch (HttpAuthenticationException e) {
//                                throw new RuntimeException("Failed to create mechanism.", e);
//                            }
//                        })
//                        .filter(m -> m != null)
//                        .collect(Collectors.toList()));
//            }
//        }

//        OidcConfigService configService = OidcConfigService.getInstance();
//        boolean subsystemConfigured = configService.isSecureDeployment(deploymentUnit) && configService.isDeploymentConfigured(deploymentUnit);
//        if (subsystemConfigured) {
//            addOidcAuthDataAndConfig(phaseContext, configService, webMetaData);
//        }
//         final OidcClientContext oidcClientContext = new OidcClientContext(OidcClientConfigurationBuilder
//                        .build(new ByteArrayInputStream(oidcConfigService.getJSON(secureDeploymentName).getBytes())));
//                final HttpServerAuthenticationMechanismFactory virtualMechanismFactory = new OidcMechanismFactory(oidcClientContext);
//            try {
//                String deploymentName = (sessionManager != null) ? sessionManager.getDeploymentName() : null;
//                String contextPath = (deploymentName != null) ? "/" + deploymentName : "";
//
//                HttpAuthenticationFactory httpAuthenticationFactory = createHttpAuthenticationFactory(contextPath);
//                HttpHandler rootHandler = new ElytronRunAsHandler(new SessionInvalidationHandler(new TestResponseHandler(securityDomain)));
//
//                rootHandler = new BlockingHandler(rootHandler);
//                rootHandler = new AuthenticationCallHandler(rootHandler);
//                rootHandler = new AuthenticationConstraintHandler(rootHandler) {
//                    @Override
//                    protected boolean isAuthenticationRequired(HttpServerExchange exchange) {
//                        if (exchange.getRelativePath().equals("/unsecure")) {
//                            return false;
//                        } else {
//                            return true;
//                        }
//                    }
//                };
//                ElytronContextAssociationHandler.Builder elytronContextHandlerBuilder = ElytronContextAssociationHandler.builder()
//                        .setNext(rootHandler)
//                        .setAuthenticationMode(authenticationMode)
//                        .setMechanismSupplier(() -> httpAuthenticationFactory.getMechanismNames().stream()
//                                .map(mechanismName -> {
//                                    try {
//                                        return httpAuthenticationFactory.createMechanism(mechanismName);
//                                    } catch (HttpAuthenticationException e) {
//                                        throw new RuntimeException("Failed to create mechanism.", e);
//                                    }
//                                })
//                                .filter(m -> m != null)
//                                .collect(Collectors.toList()));
//
//                if (sessionManager != null) {
//                    ScopeSessionListener sessionListener = ScopeSessionListener.builder().build();
//
//                    sessionManager.registerSessionListener(sessionListener);
//
//                    elytronContextHandlerBuilder.setHttpExchangeSupplier(exchange ->
//                            {
//                                exchange.putAttachment(HttpServerExchange.REMOTE_USER, remoteUser);
//                                return new ElytronHttpExchange(exchange, Collections.emptyMap(), sessionListener);
//                            });
//
//                    sessionManager.start();
//                }
//
//                rootHandler = elytronContextHandlerBuilder.build();
//
//                if (sessionManager != null) {
//                    SessionCookieConfig sessionConfig = new SessionCookieConfig();
//                    if (!contextPath.isEmpty()) {
//                        sessionConfig.setPath(contextPath);
//                    }
//                    rootHandler = Handlers.path(new SessionAttachmentHandler(rootHandler, sessionManager, sessionConfig));
//                }
//
//                PathHandler finalHandler = Handlers.path();
//
//                finalHandler = finalHandler
//                        .addExactPath(contextPath + "/login.html", exchange -> {
//                            exchange.getResponseSender().send("Login Page");
//                            exchange.endExchange();
//                        })
//                        .addPrefixPath(contextPath.isEmpty() ? "/" : contextPath, rootHandler);
//
//                return finalHandler;
//            } catch (Exception cause) {
//                throw new RuntimeException("Could not create root http handler.", cause);
//            }
//        }
//    }
}
