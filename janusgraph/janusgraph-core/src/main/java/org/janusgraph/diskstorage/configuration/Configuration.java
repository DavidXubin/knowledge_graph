// Copyright 2017 JanusGraph Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.janusgraph.diskstorage.configuration;

import my_guava.common.collect.Maps;
import my_guava.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public interface Configuration {

    default boolean has(ConfigOption option, String... umbrellaElements) {
        return has(option, false, umbrellaElements);
    }

    boolean has(ConfigOption option, boolean includeRoot, String... umbrellaElements);

    default <O> O get(ConfigOption<O> option, String... umbrellaElements) {
        return get(option, false, umbrellaElements);
    }

    <O> O get(ConfigOption<O> option, boolean includeRoot, String... umbrellaElements);

    Set<String> getContainedNamespaces(ConfigNamespace umbrella, String... umbrellaElements);

    Map<String,Object> getSubset(ConfigNamespace umbrella, String... umbrellaElements);

    Configuration restrictTo(final String... umbrellaElements);


    //--------------------

    Configuration EMPTY = new Configuration() {
        @Override
        public boolean has(ConfigOption option, boolean includeRoot, String... umbrellaElements) {
            return false;
        }

        @Override
        public <O> O get(ConfigOption<O> option, boolean includeRoot, String... umbrellaElements) {
            return option.getDefaultValue();
        }

        @Override
        public Set<String> getContainedNamespaces(ConfigNamespace umbrella, String... umbrellaElements) {
            return Sets.newHashSet();
        }

        @Override
        public Map<String, Object> getSubset(ConfigNamespace umbrella, String... umbrellaElements) {
            return Maps.newHashMap();
        }

        @Override
        public Configuration restrictTo(String... umbrellaElements) {
            return EMPTY;
        }
    };


}
