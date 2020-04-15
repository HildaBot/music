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
package ch.jamiete.hilda.music.tasks;

import java.util.TimerTask;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.api.entities.Guild;

public class MusicServerChecker extends TimerTask {
    private final MusicManager manager;

    public MusicServerChecker(final MusicManager manager) {
        this.manager = manager;
    }

    @Override
    public final void run() {
        if (this.manager.getNumber() > 0) {
            for (final MusicServer server : this.manager.getServers()) {
                server.prompt();

                if (server.isStopping()) {
                    continue;
                }

                if (server.getGuild().getAudioManager().isConnected() && (server.getGuild().getAudioManager().getConnectedChannel() != server.getChannel())) {
                    Hilda.getLogger().info("Moved from " + server.getGuild().getAudioManager().getConnectedChannel().getName() + " to expected channel");
                    server.getGuild().getAudioManager().openAudioConnection(server.getChannel());
                }

                if (!server.getQueue().isEmpty() && !server.isLeaveQueued() && (server.getPlayer().getPlayingTrack() == null)) {
                    server.play(server.getQueue().get(0));
                }
            }
        }

        for (final Guild guild : this.manager.getHilda().getBot().getGuilds()) {
            final MusicServer server = this.manager.getServer(guild);

            if (guild.getAudioManager().isConnected() && (server == null)) {
                Hilda.getLogger().info("Disconnecting from voice chat in untracked server " + guild.getName());
                guild.getAudioManager().closeAudioConnection();
            }
        }
    }

}
