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

import java.util.Collections;
import java.util.List;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import ch.jamiete.hilda.music.QueueItem;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

class MusicSkipCommand extends ChannelSubCommand {
    private final MusicManager manager;

    MusicSkipCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("skip");
        this.setAliases(Collections.singletonList("next"));
        this.setDescription("Requests that the song be skipped.");
    }

    @Override
    public final void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (!member.getVoiceState().inVoiceChannel()) {
            Hilda.getLogger().fine("Rejected command because user not in a voice channel");
            this.reply(message, "You must be in a voice channel to skip.");
            return;
        }

        if (member.getVoiceState().isDeafened()) {
            Hilda.getLogger().fine("Rejected command because user deafened");
            this.reply(message, "You must undeafen to skip.");
            return;
        }

        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            if ((System.currentTimeMillis() - this.manager.getRecent(message.getGuild().getIdLong())) >= 60000L) {
                this.reply(message, "There isn't anything playing.");
            }

            return;
        }

        if (member.getVoiceState().getChannel() != server.getChannel()) {
            Hilda.getLogger().fine("Rejected command because user not in my voice channel");
            this.reply(message, "You must be in the same voice channel as me to skip.");
            return;
        }

        if ((server.getPlayer().getPlayingTrack() == null) && !server.getQueue().isEmpty()) {
            Hilda.getLogger().info("The queue was stuck!");
            List<QueueItem> queue = server.getQueue();
            server.play(queue.isEmpty() ? null : queue.get(0));
            this.reply(message, "Oops! Skipping...");
            return;
        }

        if (server.getPlayer().getPlayingTrack() == null) {
            Hilda.getLogger().fine("Rejected command because no track playing");
            this.reply(message, "There isn't anything playing.");
            return;
        }

        if (server.getPlaying().getUserId().equals(member.getUser().getId())) {
            Hilda.getLogger().info("Skipped song because user had requested it");
            this.reply(message, "Skipping...");
            server.getPlayer().stopTrack();
            return;
        }

        if (server.hasSkipped(member.getUser().getId())) {
            Hilda.getLogger().fine("Rejected command because user already voted to skip");
            this.reply(message, "You've already voted to skip the current song.");
        } else {
            final StringBuilder sb = new StringBuilder();

            sb.append(member.getEffectiveName()).append(" has voted to skip the song!");
            server.addSkip(member.getUser().getId());

            if (server.shouldSkip()) {
                sb.append(" Skipping...");
            } else {
                final int needed = (int) Math.ceil((double) server.getUsers() / 2);
                sb.append(" **").append(server.getSkips()).append('/').append(needed).append("**");
                Hilda.getLogger().fine("Skips: " + server.getSkips() + '/' + needed);
            }

            this.reply(message, sb.toString());

            if (server.shouldSkip()) { // So that message sends before track changes
                Hilda.getLogger().info("Skipping song...");
                server.getPlayer().stopTrack();
            }
        }
    }

}
