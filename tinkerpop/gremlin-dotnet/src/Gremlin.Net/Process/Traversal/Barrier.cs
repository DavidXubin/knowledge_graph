#region License

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#endregion

// THIS IS A GENERATED FILE - DO NOT MODIFY THIS FILE DIRECTLY - see pom.xml
using System;
using System.Collections.Generic;

namespace Gremlin.Net.Process.Traversal
{
#pragma warning disable 1591

    public class Barrier : EnumWrapper, IConsumer
    {
        private Barrier(string enumValue)
            : base("Barrier", enumValue)
        {
        }

        public static Barrier NormSack => new Barrier("normSack");

        private static readonly IDictionary<string, Barrier> Properties = new Dictionary<string, Barrier>
        {
            { "normSack", NormSack },
        };

        /// <summary>
        /// Gets the Barrier enumeration by value.
        /// </summary>
        public static Barrier GetByValue(string value)
        {
            if (!Properties.TryGetValue(value, out var property))
            {
                throw new ArgumentException($"No matching Barrier for value '{value}'");
            }
            return property;
        }
    }


#pragma warning restore 1591
}