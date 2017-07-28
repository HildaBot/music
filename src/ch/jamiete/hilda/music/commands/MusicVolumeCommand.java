/*******************************************************************************
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
 *******************************************************************************/
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

public class MusicVolumeCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicVolumeCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("volume");
        this.setAliases(Arrays.asList(new String[] { "v" }));
        this.setDescription("Modifies the volume of the music.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String name) {
        final Member member = message.getGuild().getMember(message.getAuthor());
        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        if (args.length == 0) {
            this.reply(message, "Volume currently at " + server.getPlayer().getVolume() + "%");
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER) && !this.manager.isDJ(message)) {
            this.reply(message, "You must be a DJ to use this command.");
            return;
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            this.reply(message, "You must be in a voice channel to change the volume.");
            return;
        }

        int volume = 0;

        try {
            volume = Integer.parseInt(args[0]);
        } catch (final Exception e) {
            this.usage(message, "[volume 0–150]");
            return;
        }

        if (volume > 150) {
            this.reply(message, "You cannot set the volume higher than 150%.");
            return;
        }

        if (volume < 1) {
            this.reply(message, "You cannot set the volume lower than 1%.");
            return;
        }

        server.getPlayer().setVolume(volume);
        this.reply(message, "OK, set volume to " + volume + "%!");
    }

}
