/*
 * Copyright 2020 Jay Ehsaniara
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

package io.github.maven.repository.bos;

/**
 * <p>Progress interface.</p>
 *
 * @author jay
 * @version $Id: $Id
 */
public interface Progress {

    /**
     * <p>progress.</p>
     *
     * @param buffer an array of {@link byte} objects.
     * @param length a int.
     */
    void progress(byte[] buffer, int length);

}
