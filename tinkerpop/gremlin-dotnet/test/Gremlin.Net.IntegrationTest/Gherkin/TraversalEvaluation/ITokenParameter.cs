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

using System;
using System.Collections.Generic;

namespace Gremlin.Net.IntegrationTest.Gherkin.TraversalEvaluation
{
    public interface ITokenParameter
    {
        /// <summary>
        /// Gets the value of the parameter 
        /// </summary>
        object GetValue();

        /// <summary>
        /// Gets the type of the parameter
        /// </summary>
        Type GetParameterType();

        /// <summary>
        /// Sets the context parameter values by a given name, ie: "v1Id" = 1
        /// </summary>
        void SetContextParameterValues(IDictionary<string, object> values);
    }
}