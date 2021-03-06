/*
 * Copyright 2016 Microprofile.io
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
package com.nosoftskills.kafka.user.services;

import com.nosoftskills.kafka.user.model.User;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class UserService {

    @Inject
    private EntityManager em;

    @Inject
    private Event<User> userEvent;

    @GET
    public Response getAllUsers() {
        List<User> users = em.createQuery("SELECT users FROM User users", User.class)
                .getResultList();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        users.stream()
                .map(User::toJson)
                .forEach(arrayBuilder::add);
        return Response.ok(arrayBuilder.build()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createUser(String userJson) {
        User user = User.fromJsonString(userJson);
        em.persist(user);
        userEvent.fire(user);
        return Response
                .created(URI.create("/" + user.getId()))
                .entity(user.toJson())
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateUser(String userJson) {
        User user = User.fromJsonString(userJson);
        if (user.getId() != null) {
            User found = em.find(User.class, user.getId());
            if (found != null) {
                em.merge(user);
                userEvent.fire(user);
                return Response
                        .ok()
                        .entity(user.toJson())
                        .build();
            }
            user.setId(null);
        }
        return createUser(userJson);
    }

    @GET
    @Path("/reset")
    @Transactional
    public Response reset() {
        try {
            em.createQuery("DELETE FROM User u").executeUpdate();
        } catch (PersistenceException pe) {
            System.out.println("Tables don't exist");
        }
        User ivko = new User("ivko3", "ivan", "Ivan", "Ivanov", "ivan.ivanov@example.org");
        em.persist(ivko);
        User kocko = new User("kocko07", "koko", "Konstantin", "Stefanov", "konstantin.stefanov@example.org");
        em.persist(kocko);
        User boboran = new User("boboran", "boyan", "Boyan", "Stefanov", "boyan.stefanov@example.org");
        em.persist(boboran);

        userEvent.fire(ivko);
        userEvent.fire(kocko);
        userEvent.fire(boboran);
        return Response.noContent().build();
    }


}
