/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.webcomponent;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Component;

public class OSGiWebComponentDataCollector extends WebComponentRegistry {

    protected AtomicReference<Map<String, Class<? extends Component>>> webComponents = new AtomicReference<>();

    @Override
    public void setWebComponents(
            Map<String, Class<? extends Component>> components) {
        if (components.isEmpty() && webComponents.get() == null) {
            // ignore initial empty targets avoiding error target
            // initialization it they are not yet discovered
            return;
        }

        webComponents.set(Collections.unmodifiableMap(components));
    }
}
