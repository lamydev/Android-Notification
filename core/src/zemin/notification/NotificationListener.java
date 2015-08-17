/*
 * Copyright (C) 2015 Zemin Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zemin.notification;

/**
 * Listener used to propagate events indicating when notifications
 * are delivered and/or canceled.
 *
 * @see NotificationDelegater#addListener
 * @see NotificationDelegater#removeListener
 */
public interface NotificationListener {

    /**
     * Called when a notification arrives.
     *
     * @param entry
     */
    void onArrival(NotificationEntry entry);

    /**
     * Called when a notification is canceled.
     *
     * @param entry
     */
    void onCancel(NotificationEntry entry);

    /**
     * Called when a notification is updated.
     *
     * @param entry
     */
    void onUpdate(NotificationEntry entry);
}
