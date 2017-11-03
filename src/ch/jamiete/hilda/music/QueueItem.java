/*
 * Copyright 2017 jamietech
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
package ch.jamiete.hilda.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class QueueItem {
    private final AudioTrack track;
    private final String user;

    public QueueItem(final AudioTrack track, final String user) {
        this.track = track;
        this.user = user;
    }

    /**
     * Gets the track held in the item.
     * @return The track.
     */
    public final AudioTrack getTrack() {
        return this.track;
    }

    /**
     * Gets the ID of the user who queued the item.
     * @return The user ID.
     */
    public final String getUserId() {
        return this.user;
    }

    @Override
    public final String toString() {
        return "QueueItem{track=" + this.track.getIdentifier() + ", user=" + this.user + '}';
    }

}
