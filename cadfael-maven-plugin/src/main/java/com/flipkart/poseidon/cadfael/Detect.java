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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by shrey.garg on 02/09/17.
 */
@Mojo(
        name = "cadfael"
)
public class Detect extends AbstractMojo {
    @Parameter
    private Set<NonVersionedArtifact> nativeArtifacts = new HashSet<>();

    @Parameter(defaultValue = "false")
    private boolean verifyProvidedScope;

    @Parameter
    private Set<NonVersionedArtifact> allowedDependencies = new HashSet<>();

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<ArtifactRepository> repositories = project.getRemoteArtifactRepositories();
        getLog().info("Found " + repositories.size() + " remote repositories.");
        getLog().info("");

        final Set<RemoteRepository> remoteRepositories = new HashSet<>();
        for (ArtifactRepository repository : repositories) {
            remoteRepositories.add(new RemoteRepository.Builder(repository.getId(), "default", repository.getUrl()).build());
        }

        final Cadfael.Builder cadfaelBuilder = Cadfael.builder(remoteRepositories)
                .allowedArtifacts(allowedDependencies)
                .rejectSnapshots()
                .ignoreTestScope();

        if (!verifyProvidedScope) {
            cadfaelBuilder.ignoreProvidedScope();
        }

        final Cadfael cadfael = cadfaelBuilder.build();

        final List dependencies = project.getDependencies();
        getLog().info("Found " + dependencies.size() + " dependencies.");
        getLog().info("Verifying transitive dependencies.");
        getLog().info("");

        boolean buildRejected = false;
        for (Object o : dependencies) {
            Dependency dependency = (Dependency) o;
            if (nativeArtifacts.contains(getNonVersionedArtifact(dependency))) {
                continue;
            }

            try {
                Set<Artifact> rejectedArtifacts = cadfael.getRejectedDependencies(getArtifact(dependency));

                if (rejectedArtifacts.size() > 0) {
                    getLog().error("For " + dependency);
                    getLog().error(rejectedArtifacts.size() + " transitive dependencies have been rejected:");
                    for (Artifact artifact : rejectedArtifacts) {
                        getLog().error(artifact.toString());
                    }
                    getLog().error("");
                    buildRejected = true;
                }
            } catch (ArtifactDescriptorException e) {
                throw new MojoExecutionException("Artifact not found", e);
            }
        }

        if (buildRejected) {
            getLog().debug("Allowed transitive dependencies:");
            for (NonVersionedArtifact artifact : allowedDependencies) {
                getLog().debug(artifact.toString());
            }
            throw new MojoExecutionException("There are rejected transitive dependencies. Clean your jar.");
        } else {
            getLog().info("No transitive issues found.");
        }
    }

    private Artifact getArtifact(Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), null, dependency.getVersion());
    }

    private NonVersionedArtifact getNonVersionedArtifact(Dependency dependency) {
        return new NonVersionedArtifact(dependency.getGroupId(), dependency.getArtifactId());
    }
}
