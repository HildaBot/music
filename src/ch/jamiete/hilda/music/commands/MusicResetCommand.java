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
package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicResetCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicResetCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("reset");
        this.setAliases(Arrays.asList(new String[] { "clear", "disconnect", "restart" }));
        this.setDescription("Clears the music queue and disconnects.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (!member.hasPermission(message.getTextChannel(), Permission.MANAGE_SERVER) && !this.manager.isDJ(message)) {
            this.reply(message, "You must be a DJ to use this command.");
            return;
        }

        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            if (message.getGuild().getAudioManager().isConnected()) {
                message.getGuild().getAudioManager().closeAudioConnection();
                this.reply(message, "Disconnected from the channel.");
            } else {
                this.reply(message, "There isn't anything playing.");
            }

            return;
        }

        if (server.canShutdown()) {
            server.shutdown();
            this.reply(message, "Music reset.");
        } else {
            server.shutdownNow(false);
            this.reply(message, "Music reset. I can't leave the channel right now, but the music server has been reset.");
        }
    }

}
