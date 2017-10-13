/*
 * Copyright 2017 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.poseidon.cadfael;

import com.flipkart.poseidon.cadfael.artifact.NonVersionedArtifact;
import com.flipkart.poseidon.cadfael.artifact.Scope;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.util.*;

/**
 * Created by shrey.garg on 02/09/17.
 */
public class Cadfael {
    private final List<RemoteRepository> repositories;
    private final RepositorySystem system;
    private final RepositorySystemSession session;

    private final Set<NonVersionedArtifact> allowedArtifacts;
    private final Set<Scope> ignoredScopes;
    private final boolean rejectSnapshots;

    private Cadfael(List<RemoteRepository> repositories, Set<NonVersionedArtifact> allowedArtifacts, Set<Scope> ignoredScopes, boolean rejectSnapshots) {
        this.repositories = repositories;
        this.allowedArtifacts = allowedArtifacts;
        this.ignoredScopes = ignoredScopes;
        this.rejectSnapshots = rejectSnapshots;

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        this.system = newRepositorySystem(locator);
        this.session = newSession(system);
    }

    public Set<Dependency> getAllDependencies(Artifact artifact) throws ArtifactDescriptorException {
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(artifact, repositories, null);
        ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);

        Set<Dependency> dependencies = new HashSet<>(result.getManagedDependencies());
        dependencies.addAll(result.getDependencies());

        return dependencies;
    }

    public Set<Artifact> getRejectedDependencies(Artifact artifact) throws ArtifactDescriptorException {
        Set<Dependency> allDependencies = getAllDependencies(artifact);

        Set<Artifact> rejected = new HashSet<>();
        for (Dependency dependency : allDependencies) {
            Scope scope = Scope.compile;
            try {
                scope = Scope.valueOf(dependency.getScope().toLowerCase());
            } catch (Throwable ignored) { }

            if (ignoredScopes.contains(scope)) {
                continue;
            }

            if ((dependency.getArtifact().isSnapshot() && rejectSnapshots) || notAllowed(allowedArtifacts, dependency)) {
                rejected.add(dependency.getArtifact());
            }
        }

        return rejected;
    }

    private boolean matches(Dependency dependency, NonVersionedArtifact artifact) {
        return dependency.getArtifact().getGroupId().equals(artifact.getGroupId())
                && dependency.getArtifact().getArtifactId().equals(artifact.getArtifactId());
    }

    private boolean notAllowed(Set<NonVersionedArtifact> artifacts, Dependency dependency) {
        for (NonVersionedArtifact artifact : artifacts) {
            if (matches(dependency, artifact)) {
                return false;
            }
        }

        return true;
    }

    private RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        // set possible proxies and mirrors
//        session.setProxySelector(new DefaultProxySelector().add(new Proxy(Proxy.TYPE_HTTP, "host", 3625), Arrays.asList("localhost", "127.0.0.1")));
//        session.setMirrorSelector(new DefaultMirrorSelector().add("my-mirror", "http://mirror", "default", false, "external:*", null));
        return session;
    }

    public static Cadfael.Builder builder(RemoteRepository repository) {
        return new Cadfael.Builder(repository);
    }

    public static Cadfael.Builder builder(Set<RemoteRepository> repositories) {
        return new Cadfael.Builder(repositories);
    }

    public static class Builder {
        private final Set<RemoteRepository> repositories = new HashSet<>();
        private Set<NonVersionedArtifact> allowedArtifacts = new HashSet<>();
        private final Set<Scope> ignoredScopes = new HashSet<>();
        private boolean rejectSnapshots = false;

        private Builder(RemoteRepository repository) {
            this.repositories.add(repository);
        }

        private Builder(Set<RemoteRepository> repositories) {
            this.repositories.addAll(repositories);
        }

        public Builder addRepository(RemoteRepository repository) {
            if (repository != null) {
                this.repositories.add(repository);
            }
            return this;
        }

        public Builder allowedArtifacts(Set<NonVersionedArtifact> artifacts) {
            this.allowedArtifacts = artifacts == null ? new HashSet<>() : artifacts;
            return this;
        }

        public Builder rejectSnapshots() {
            this.rejectSnapshots = true;
            return this;
        }

        public Builder ignoreProvidedScope() {
            this.ignoredScopes.add(Scope.provided);
            return this;
        }

        public Builder ignoreTestScope() {
            this.ignoredScopes.add(Scope.test);
            return this;
        }

        public Cadfael build() {
            return new Cadfael(new ArrayList<>(repositories), allowedArtifacts, ignoredScopes, rejectSnapshots);
        }
    }
}
