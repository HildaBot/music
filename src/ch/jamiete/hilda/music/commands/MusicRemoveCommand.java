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

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import ch.jamiete.hilda.music.QueueItem;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.Collections;
import java.util.List;

class MusicRemoveCommand extends ChannelSubCommand {
    private final MusicManager manager;

    MusicRemoveCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("remove");
        this.setAliases(Collections.singletonList("unqueue"));
        this.setDescription("Removes a song from the queue.");
    }

    @Override
    public final void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());
        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        final List<QueueItem> queue = server.getQueue();

        if ((member.getVoiceState().getChannel() != server.getChannel()) && !MusicManager.isDJ(message)) {
            Hilda.getLogger().fine("Rejected command because user not in my voice channel");
            this.reply(message, "You must be in the same voice channel as me to skip.");
            return;
        }

        if (queue.isEmpty()) {
            Hilda.getLogger().fine("Rejected command because no tracks queued");
            this.reply(message, "There isn't anything queued.");
            return;
        }

        int to_remove = -1;

        try {
            to_remove = Integer.parseInt(args[0]) - 1;
        } catch (final Exception ignored) {
            this.usage(message, "<queue_code>", label);
        }

        if ((to_remove < 0) || (to_remove >= queue.size())) {
            this.reply(message, "There is no song with that queue code.");
            return;
        }

        final QueueItem item = queue.get(to_remove);

        if (!item.getUserId().equals(message.getAuthor().getId()) && !MusicManager.isDJ(message)) {
            this.reply(message, "You can't remove a song you didn't queue.");
            return;
        }

        server.unqueue(item);
        this.reply(message, "OK, I've removed " + Util.sanitise(MusicManager.getFriendly(item.getTrack())) + " from the queue!");
    }

}
