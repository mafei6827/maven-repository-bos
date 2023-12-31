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

import org.apache.maven.wagon.events.SessionListener;

public interface SessionListenerContainer {

    /**
     * <p>addSessionListener.</p>
     *
     * @param sessionListener sessionListener
     */
    void addSessionListener(SessionListener sessionListener);

    /**
     * <p>removeSessionListener.</p>
     *
     * @param sessionListener sessionListener
     */
    void removeSessionListener(SessionListener sessionListener);

    /**
     * <p>hasSessionListener.</p>
     *
     * @param sessionListener sessionListener
     * @return boolean
     */
    boolean hasSessionListener(SessionListener sessionListener);

    /**
     * void fireSessionOpening
     */
    void fireSessionOpening();

    /**
     * void fireSessionOpened
     */
    void fireSessionOpened();

    /**
     * void fireSessionDisconnecting
     */
    void fireSessionDisconnecting();

    /**
     * void fireSessionDisconnected
     */
    void fireSessionDisconnected();

    /**
     * void fireSessionLoggedIn
     */
    void fireSessionLoggedIn();

    /**
     * void fireSessionLoggedOff
     */
    void fireSessionLoggedOff();

}
