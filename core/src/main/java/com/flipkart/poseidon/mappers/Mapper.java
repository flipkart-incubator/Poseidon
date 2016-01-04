/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.mappers;

import flipkart.lego.api.helpers.Describable;
import flipkart.lego.api.helpers.Identifiable;

import java.util.Map;

/**
 * Mapper maps the model which is available in {@link flipkart.lego.api.entities.Buildable} <code>build()</code>
 * to any bean/entity/pojo (ex: an event for data governance).
 * <p/>
 * Mapped beans are accessible through {@link com.flipkart.poseidon.core.PoseidonResponse}
 * <code>getMappedBeans()</code> in {@link flipkart.lego.api.entities.Filter} <code>filterResponse()</code>
 * <p/>
 */
public interface Mapper extends Identifiable, Describable {
    Object map(Map<String, Object> map);
}
