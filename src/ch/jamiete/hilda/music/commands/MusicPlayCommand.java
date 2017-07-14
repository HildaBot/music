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
import com.google.gson.JsonElement;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.LoadResults;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class MusicPlayCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicPlayCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("play");
        this.setAliases(Arrays.asList(new String[] { "search", "p" }));
        this.setDescription("Queues a sound file to be played. Accepts URLs or YouTube search queries.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (args.length == 0) {
            this.usage(message, "<url/query string>", label);
            return;
        }

        // User requirement logic
        if (!member.getVoiceState().inVoiceChannel()) {
            MusicManager.getLogger().fine("Rejected command because user not in voice channel");
            this.reply(message, "You must be in a voice channel to queue music.");
            return;
        }

        final MusicServer server = this.manager.hasServer(message.getGuild()) ? this.manager.getServer(message.getGuild()) : this.manager.createServer(message.getGuild());

        if (server.getChannel() == null) {
            final JsonElement lock = server.getConfig().get().get("lock");

            if (lock != null) {
                final VoiceChannel req = message.getGuild().getVoiceChannelById(lock.getAsString());

                if (req != null && !member.getVoiceState().getChannel().equals(req)) {
                    MusicManager.getLogger().fine("Rejected command because user not in locked voice channel");
                    this.reply(message, "You can only queue music in " + req.getName());
                    return;
                }
            }

            server.setChannel(member.getVoiceState().getChannel()); // Join channel
        } else {
            if (server.getChannel() != member.getVoiceState().getChannel()) {
                MusicManager.getLogger().fine("Rejected command because user not in same voice channel");
                this.reply(message, "You must be in the same voice channel as me to queue music.");
                return;
            }
        }

        // URL logic
        if (args.length == 1 && args[0].toLowerCase().startsWith("http")) {
            MusicManager.getLogger().info("Attempting to load URL " + args[0]);
            message.getChannel().sendTyping().queue();
            this.manager.getAudioPlayerManager().loadItemOrdered(server.getPlayer(), args[0], new LoadResults(server, this.manager, message));
            return;
        }

        // Search logic
        final String search = Util.combineSplit(0, args, " ");
        MusicManager.getLogger().info("Attempting to search YouTube for " + search);
        message.getChannel().sendTyping().queue();
        this.manager.getAudioPlayerManager().loadItemOrdered(server.getPlayer(), "ytsearch:" + search, new LoadResults(server, this.manager, message, true));
    }

}
